package spark;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.util.LongAccumulator;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Frequent RDD questions:
 *  1. reduceByKey vs groupByKey
 *  2. Average per key (aggregateByKey / combineByKey)
 *  3. map vs mapPartitions
 *  4. Broadcast variables & accumulators
 *  5. Transformations (lazy) vs actions, narrow vs wide
 *  6. repartition vs coalesce
 */
public class RddBasics {

    // 1. groupByKey ships EVERY value across the network; reduceByKey pre-aggregates per partition.
    static void reduceVsGroup(JavaPairRDD<String, Integer> sales) {
        SparkUtil.title("reduceByKey (preferred)");
        System.out.println(sales.reduceByKey(Integer::sum).collectAsMap());

        SparkUtil.title("groupByKey (shuffles all values - avoid for aggregations)");
        System.out.println(sales.groupByKey()
                .mapValues(vals -> { int s = 0; for (int v : vals) s += v; return s; })
                .collectAsMap());
    }

    // 2. Average per key: accumulate (sum, count) then divide.
    // TRICK: zeroValue, seqOp (within partition), combOp (across partitions)
    static Map<String, Double> averagePerKey(JavaPairRDD<String, Integer> sales) {
        return sales.aggregateByKey(
                        new Tuple2<>(0, 0),
                        (acc, v) -> new Tuple2<>(acc._1 + v, acc._2 + 1),
                        (a, b) -> new Tuple2<>(a._1 + b._1, a._2 + b._2))
                .mapValues(t -> (double) t._1 / t._2)
                .collectAsMap();
    }

    // Same with combineByKey (most general: createCombiner, mergeValue, mergeCombiners)
    static Map<String, Double> averageCombineByKey(JavaPairRDD<String, Integer> sales) {
        return sales.combineByKey(
                        v -> new Tuple2<>(v, 1),
                        (acc, v) -> new Tuple2<>(acc._1 + v, acc._2 + 1),
                        (a, b) -> new Tuple2<>(a._1 + b._1, a._2 + b._2))
                .mapValues(t -> (double) t._1 / t._2)
                .collectAsMap();
    }

    // 3. mapPartitions: expensive setup (DB connection, parser) once per PARTITION, not per record.
    static List<String> mapPartitionsExample(JavaRDD<Integer> nums) {
        return nums.mapPartitionsWithIndex((idx, it) -> {
            List<String> out = new ArrayList<>();   // imagine: open connection here
            while (it.hasNext()) out.add("p" + idx + ":" + it.next());
            return out.iterator();                  // ...and close it here
        }, false).collect();
    }

    // 4. Broadcast: ship a read-only lookup once per executor (map-side join).
    //    Accumulator: write-only counter from executors, read on driver.
    static void broadcastAndAccumulator(JavaSparkContext sc) {
        Broadcast<Map<String, String>> countryNames = sc.broadcast(Map.of("IN", "India", "US", "USA"));
        LongAccumulator unknown = sc.sc().longAccumulator("unknownCodes");

        List<String> resolved = sc.parallelize(List.of("IN", "US", "XX", "IN", "ZZ"))
                .map(code -> {
                    String name = countryNames.value().get(code);
                    if (name == null) unknown.add(1);
                    return name == null ? "UNKNOWN" : name;
                })
                .collect();                         // accumulator is only filled after an ACTION

        SparkUtil.title("Broadcast + accumulator");
        System.out.println(resolved + " unknown=" + unknown.value());
    }

    // 5. Lazy evaluation: nothing runs until an action; the lineage (DAG) is printed by toDebugString.
    static void lazyEvaluation(JavaSparkContext sc) {
        JavaRDD<Integer> rdd = sc.parallelize(List.of(1, 2, 3, 4, 5, 6), 3)
                .map(x -> x * 2)                    // narrow - no shuffle
                .filter(x -> x > 4);                // narrow
        JavaPairRDD<Integer, Integer> byMod = rdd
                .mapToPair(x -> new Tuple2<>(x % 3, x))
                .reduceByKey(Integer::sum);         // wide - shuffle = new stage

        SparkUtil.title("Lineage (stages split at shuffle)");
        System.out.println(byMod.toDebugString());
        System.out.println("action result: " + byMod.collect());
    }

    // 6. repartition(n) = full shuffle, can increase/decrease. coalesce(n) = merge partitions, no shuffle, decrease only.
    static void repartitionVsCoalesce(JavaSparkContext sc) {
        JavaRDD<Integer> rdd = sc.parallelize(List.of(1, 2, 3, 4, 5, 6, 7, 8), 8);
        SparkUtil.title("repartition vs coalesce");
        System.out.println("original    = " + rdd.getNumPartitions());
        System.out.println("coalesce(2) = " + rdd.coalesce(2).getNumPartitions());
        System.out.println("repartition(16) = " + rdd.repartition(16).getNumPartitions());
        System.out.println("coalesce(16) = " + rdd.coalesce(16).getNumPartitions() + "  <- can't grow without shuffle");
    }

    // Second highest number with RDD (classic warm-up)
    static int secondHighest(JavaRDD<Integer> nums) {
        List<Integer> top2 = nums.distinct().top(2);
        return top2.size() < 2 ? -1 : top2.get(1);
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("RddBasics");
        JavaSparkContext sc = JavaSparkContext.fromSparkContext(spark.sparkContext());

        JavaPairRDD<String, Integer> sales = sc.parallelizePairs(List.of(
                new Tuple2<>("apple", 10), new Tuple2<>("banana", 5),
                new Tuple2<>("apple", 20), new Tuple2<>("banana", 15), new Tuple2<>("cherry", 7)));

        reduceVsGroup(sales);

        SparkUtil.title("Average per key");
        System.out.println("aggregateByKey: " + averagePerKey(sales));
        System.out.println("combineByKey:   " + averageCombineByKey(sales));

        SparkUtil.title("mapPartitionsWithIndex");
        System.out.println(mapPartitionsExample(sc.parallelize(List.of(1, 2, 3, 4, 5), 2)));

        broadcastAndAccumulator(sc);
        lazyEvaluation(sc);
        repartitionVsCoalesce(sc);

        SparkUtil.title("Second highest");
        System.out.println(secondHighest(sc.parallelize(List.of(5, 9, 9, 3, 7))));

        spark.stop();
    }
}
