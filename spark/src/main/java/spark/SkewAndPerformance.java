package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.storage.StorageLevel;
import scala.jdk.javaapi.CollectionConverters;

import java.util.List;

import static org.apache.spark.sql.functions.*;

/**
 * Performance questions:
 *  1. Data skew in a JOIN -> salting
 *  2. Data skew in a GROUP BY -> two-stage (salted) aggregation
 *  3. repartition vs coalesce, partition distribution
 *  4. cache vs persist, when to unpersist
 *  5. Broadcast join + AQE (adaptive query execution) settings
 *  6. Reading explain() plans
 *
 * TRICK (salting): add random salt 0..N-1 on the BIG skewed side,
 *                  replicate the SMALL side N times (explode(sequence(0, N-1))), join on (key, salt).
 */
public class SkewAndPerformance {

    static final int SALT = 4;

    // 90% of rows have key "hot"
    static Dataset<Row> skewedFacts(SparkSession spark) {
        return spark.range(0, 10_000)
                .withColumn("key", when(col("id").mod(10).lt(9), lit("hot")).otherwise(concat(lit("k"), floor(col("id").divide(10)).mod(5))))
                .withColumn("amount", col("id").mod(100));
    }

    static Dataset<Row> dims(SparkSession spark) {
        return SparkUtil.df(spark, "key STRING, label STRING",
                SparkUtil.r("hot", "Hot key"), SparkUtil.r("k0", "zero"), SparkUtil.r("k1", "one"),
                SparkUtil.r("k2", "two"), SparkUtil.r("k3", "three"), SparkUtil.r("k4", "four"));
    }

    // 1. Salted join
    static Dataset<Row> saltedJoin(Dataset<Row> facts, Dataset<Row> dim) {
        Dataset<Row> saltedFacts = facts.withColumn("salt", floor(rand(42).multiply(SALT)).cast("int"));
        Dataset<Row> saltedDim = dim.withColumn("salt", explode(sequence(lit(0), lit(SALT - 1))));
        // join USING (key, salt) - Java needs a Scala Seq for multi-column name joins
        return saltedFacts.join(saltedDim, CollectionConverters.asScala(List.of("key", "salt")).toSeq())
                .drop("salt");
    }

    // 2. Two-stage aggregation: partial sum per (key, salt), then final sum per key
    static Dataset<Row> saltedGroupBy(Dataset<Row> facts) {
        return facts.withColumn("salt", floor(rand(7).multiply(SALT)).cast("int"))
                .groupBy("key", "salt").agg(sum("amount").as("partial"))
                .groupBy("key").agg(sum("partial").as("total"));
    }

    static Dataset<Row> rowsPerPartition(Dataset<Row> df) {
        return df.groupBy(spark_partition_id().as("partition")).count().orderBy("partition");
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("SkewAndPerformance");
        Dataset<Row> facts = skewedFacts(spark);
        Dataset<Row> dim = dims(spark);

        SparkUtil.title("Skew: rows per key");
        facts.groupBy("key").count().orderBy(col("count").desc()).show();

        SparkUtil.title("Skew: rows per partition after repartition(key) - one partition is huge");
        rowsPerPartition(facts.repartition(4, col("key"))).show();

        SparkUtil.title("After salting - hot key split across several partitions");
        rowsPerPartition(facts.withColumn("salt", floor(rand(1).multiply(SALT)))
                .repartition(4, col("key"), col("salt"))).show();

        SparkUtil.title("Salted join result (count per key must match unsalted)");
        saltedJoin(facts, dim).groupBy("key", "label").count().orderBy("key").show();

        SparkUtil.title("Two-stage salted aggregation");
        saltedGroupBy(facts).orderBy("key").show();

        // 3. repartition vs coalesce
        SparkUtil.title("repartition vs coalesce");
        System.out.println("default     : " + facts.rdd().getNumPartitions());
        System.out.println("repartition : " + facts.repartition(8).rdd().getNumPartitions() + " (full shuffle)");
        System.out.println("coalesce    : " + facts.coalesce(1).rdd().getNumPartitions() + " (no shuffle, e.g. before writing 1 file)");

        // 4. cache / persist. cache() == persist(MEMORY_AND_DISK) for DataFrames. Lazy until an action.
        SparkUtil.title("cache / persist");
        Dataset<Row> cached = facts.filter(col("amount").gt(50)).persist(StorageLevel.MEMORY_AND_DISK());
        System.out.println("count #1 (materializes cache): " + cached.count());
        System.out.println("count #2 (from cache)        : " + cached.count());
        System.out.println("storage level: " + cached.storageLevel());
        cached.unpersist();

        // 5. AQE: coalesces small shuffle partitions, switches to broadcast join, splits skewed partitions
        spark.conf().set("spark.sql.adaptive.enabled", "true");
        spark.conf().set("spark.sql.adaptive.skewJoin.enabled", "true");
        spark.conf().set("spark.sql.autoBroadcastJoinThreshold", String.valueOf(10 * 1024 * 1024));

        // 6. explain - look for Exchange (= shuffle), BroadcastHashJoin vs SortMergeJoin, PushedFilters
        SparkUtil.title("explain(formatted) of broadcast join + aggregation");
        facts.join(broadcast(dim), "key").groupBy("label").agg(sum("amount")).explain("formatted");

        spark.stop();
    }
}
