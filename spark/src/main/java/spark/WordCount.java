package spark;

import org.apache.avro.Schema;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;
import shaded.parquet.org.apache.thrift.partial.TFieldData;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.spark.sql.functions.*;

/**
 * Q: Word count (the "hello world" of Spark) - RDD API, DataFrame API and Spark SQL.
 *    Follow-up: top K words.
 *
 * TRICK: SPLIT -> (word,1) -> reduceByKey(+)          (RDD)
 *        explode(split(line)) -> groupBy(word).count  (DataFrame)
 */
public class WordCount {

    static List<Tuple2<String, Integer>> rddWordCount(JavaSparkContext sc, List<String> lines) {
        JavaRDD<String> words = sc.parallelize(lines)
                .flatMap(line -> Arrays.asList(line.toLowerCase().split("\\W+")).iterator())
                .filter(w -> !w.isEmpty());

        return words.mapToPair(w -> new Tuple2<>(w, 1))
                .reduceByKey(Integer::sum)          // combines map-side BEFORE shuffle (not groupByKey!)
                .mapToPair(Tuple2::swap)            // JavaPairRDD can only sort by KEY -> swap, sort, swap back
                .sortByKey(false)
                .mapToPair(Tuple2::swap)
                .collect();
    }

    static Dataset<Row> dfWordCount(SparkSession spark, List<String> lines) {
        return spark.createDataset(lines, Encoders.STRING()).toDF("line")
                .select(explode(split(lower(col("line")), "\\W+")).as("word"))
                .filter(col("word").notEqual(""))
                .groupBy("word").count()
                .orderBy(col("count").desc(), col("word"));
    }

    static Dataset<Row> sqlWordCount(SparkSession spark, List<String> lines) {
        spark.createDataset(lines, Encoders.STRING()).toDF("line").createOrReplaceTempView("lines");
        return spark.sql("""
                SELECT word, COUNT(*) AS cnt
                FROM (SELECT explode(split(lower(line), '\\\\W+')) AS word FROM lines)
                WHERE word <> ''
                GROUP BY word
                ORDER BY cnt DESC, word
                """);
    }

    // Top K with RDD: takeOrdered avoids sorting the whole dataset
    static List<Tuple2<String, Integer>> topK(JavaSparkContext sc, List<String> lines, int k) {
        return sc.parallelize(lines)
                .flatMap(line -> Arrays.asList(line.toLowerCase().split("\\W+")).iterator())
                .filter(w -> !w.isEmpty())
                .mapToPair(w -> new Tuple2<>(w, 1))
                .reduceByKey(Integer::sum)
                .takeOrdered(k, new CountDesc());
    }

    // Comparator passed to Spark must be Serializable
    static class CountDesc implements java.util.Comparator<Tuple2<String, Integer>>, java.io.Serializable {
        public int compare(Tuple2<String, Integer> a, Tuple2<String, Integer> b) {
            return b._2.compareTo(a._2);
        }
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("WordCount");
        JavaSparkContext sc = JavaSparkContext.fromSparkContext(spark.sparkContext());
        List<String> lines = List.of(
                "Spark is fast and Spark is general",
                "Hadoop is slow, Spark is in memory",
                "spark spark SPARK");

        SparkUtil.title("RDD word count");
        rddWordCount(sc, lines).forEach(System.out::println);

        SparkUtil.title("DataFrame word count");
        dfWordCount(spark, lines).show();

        SparkUtil.title("Spark SQL word count");
        sqlWordCount(spark, lines).show();

        SparkUtil.title("Top 3 words");
        System.out.println(topK(sc, lines, 3));


        spark.stop();

    }
}
