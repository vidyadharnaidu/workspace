package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;

import static org.apache.spark.sql.functions.*;

/**
 * Structured Streaming questions:
 *  1. Windowed count over event time (tumbling / sliding)
 *  2. Watermark - how late data is handled and state is cleaned up
 *  3. Output modes: append vs update vs complete
 *  4. Exactly-once: checkpointLocation + replayable source + idempotent sink
 *
 * Uses the built-in "rate" source (no Kafka needed). Kafka version would be:
 *   spark.readStream().format("kafka")
 *        .option("kafka.bootstrap.servers", "localhost:9092").option("subscribe", "events").load()
 *        .selectExpr("CAST(value AS STRING)")
 *
 * TRICK: watermark = max(event_time) - delay. Rows older than it are dropped, windows older than it are finalized.
 *        append mode + aggregation REQUIRES a watermark.
 *
 * NOTE (Windows): checkpointing needs Hadoop winutils.exe (HADOOP_HOME).
 */
public class StructuredStreaming {

    public static void main(String[] args) throws Exception {
        SparkSession spark = SparkUtil.session("StructuredStreaming");

        Dataset<Row> events = spark.readStream()
                .format("rate")                         // columns: timestamp, value
                .option("rowsPerSecond", 5)
                .load()
                .withColumn("user", concat(lit("user_"), col("value").mod(3)));

        Dataset<Row> counts = events
                .withWatermark("timestamp", "10 seconds")
                .groupBy(window(col("timestamp"), "10 seconds", "5 seconds"), col("user"))   // sliding window
                .count();

        StreamingQuery query = counts.writeStream()
                .outputMode("update")                   // only changed rows each trigger
                .format("console")
                .option("truncate", false)
                .trigger(Trigger.ProcessingTime("5 seconds"))
                .start();

        query.awaitTermination(20_000);                 // run ~20s for the demo
        query.stop();
        spark.stop();
    }
}
