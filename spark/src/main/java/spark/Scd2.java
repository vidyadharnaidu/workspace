package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;
import static spark.SparkUtil.r;

/**
 * Q: Implement Slowly Changing Dimension Type 2 with plain DataFrames (no Delta MERGE).
 *
 * dim:   id, name, city, start_date, end_date, is_current
 * input: id, name, city            (today's snapshot / changes)
 *
 * Result = history (already closed rows)
 *        + current rows that did NOT change
 *        + current rows that changed -> CLOSED (end_date = today, is_current = false)
 *        + NEW versions for changed ids
 *        + brand-new ids
 *
 * TRICK: compare a hash of the tracked columns instead of column-by-column.
 *        With Delta Lake this is one MERGE INTO ... WHEN MATCHED ... WHEN NOT MATCHED.
 */
public class Scd2 {

    static Dataset<Row> applyScd2(Dataset<Row> dim, Dataset<Row> incoming, String today) {
        Dataset<Row> history = dim.filter(col("is_current").equalTo(false));
        Dataset<Row> current = dim.filter(col("is_current").equalTo(true));

        Dataset<Row> cur = current.withColumn("hash", sha2(concat_ws("|", col("name"), col("city")), 256)).as("c");
        Dataset<Row> inc = incoming.withColumn("hash", sha2(concat_ws("|", col("name"), col("city")), 256)).as("i");

        Dataset<Row> joined = cur.join(inc, col("c.id").equalTo(col("i.id")), "full");

        Dataset<Row> unchanged = joined
                .filter(col("c.id").isNotNull().and(
                        col("i.id").isNull().or(col("c.hash").equalTo(col("i.hash")))))
                .select("c.id", "c.name", "c.city", "c.start_date", "c.end_date", "c.is_current");

        Dataset<Row> changed = joined.filter(col("c.id").isNotNull().and(col("i.id").isNotNull())
                .and(col("c.hash").notEqual(col("i.hash"))));

        Dataset<Row> expired = changed.select(col("c.id"), col("c.name"), col("c.city"), col("c.start_date"),
                lit(today).as("end_date"), lit(false).as("is_current"));

        Dataset<Row> newVersions = changed.select(col("i.id"), col("i.name"), col("i.city"),
                lit(today).as("start_date"), lit(null).cast("string").as("end_date"), lit(true).as("is_current"));

        Dataset<Row> inserts = joined.filter(col("c.id").isNull())
                .select(col("i.id"), col("i.name"), col("i.city"),
                        lit(today).as("start_date"), lit(null).cast("string").as("end_date"), lit(true).as("is_current"));

        return history.unionByName(unchanged).unionByName(expired)
                .unionByName(newVersions).unionByName(inserts)
                .orderBy("id", "start_date");
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("Scd2");
        String ddl = "id INT, name STRING, city STRING, start_date STRING, end_date STRING, is_current BOOLEAN";
        Dataset<Row> dim = SparkUtil.df(spark, ddl,
                r(1, "Alice", "Pune", "2020-01-01", "2022-01-01", false),
                r(1, "Alice", "Mumbai", "2022-01-01", null, true),
                r(2, "Bob", "Delhi", "2021-06-01", null, true),
                r(3, "Carol", "Chennai", "2021-06-01", null, true));

        Dataset<Row> incoming = SparkUtil.df(spark, "id INT, name STRING, city STRING",
                r(1, "Alice", "Bangalore"),   // changed city
                r(2, "Bob", "Delhi"),         // unchanged
                r(4, "Dan", "Hyderabad"));    // new

        SparkUtil.title("Before");
        dim.show();
        SparkUtil.title("After SCD2 (2024-06-01)");
        applyScd2(dim, incoming, "2024-06-01").show();

        spark.stop();
    }
}
