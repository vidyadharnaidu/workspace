package spark;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;

import java.util.Arrays;
import java.util.Map;

import static org.apache.spark.sql.functions.*;
import static spark.SparkUtil.r;

/**
 * Null questions:
 *  1. Count nulls in EVERY column
 *  2. fill / drop nulls (na functions), per-column defaults
 *  3. coalesce across columns
 *  4. Fill null with previous non-null value (forward fill)
 *  5. Null-safe equality (<=>)
 *
 * TRICK: col == null is NEVER true in SQL -> use isNull() or eqNullSafe().
 */
public class NullHandling {

    static Dataset<Row> countNullsPerColumn(Dataset<Row> df) {
        Column[] counts = Arrays.stream(df.columns())
                .map(c -> count(when(col(c).isNull(), 1)).as(c))   // count ignores nulls from when()
                .toArray(Column[]::new);
        return df.select(counts);
    }

    static Dataset<Row> fillDefaults(Dataset<Row> df) {
        return df.na().fill(Map.of("city", "UNKNOWN", "temp", 0.0));
    }

    // Forward fill: last non-null value so far, per sensor
    static Dataset<Row> forwardFill(Dataset<Row> df) {
        return df.withColumn("temp_ffill", last("temp", true).over(        // ignoreNulls = true
                Window.partitionBy("sensor").orderBy("ts")
                        .rowsBetween(Window.unboundedPreceding(), Window.currentRow())));
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("NullHandling");
        Dataset<Row> df = SparkUtil.df(spark, "sensor STRING, ts INT, city STRING, backup_city STRING, temp DOUBLE",
                r("s1", 1, "Pune", null, 20.5),
                r("s1", 2, null, "Mumbai", null),
                r("s1", 3, null, null, null),
                r("s1", 4, "Pune", null, 23.0),
                r("s2", 1, null, "Delhi", 30.0),
                r("s2", 2, "Delhi", null, null));

        SparkUtil.title("Null count per column");
        countNullsPerColumn(df).show();

        SparkUtil.title("na.drop() - any null");
        df.na().drop().show();

        SparkUtil.title("na.drop(\"all\", [city, backup_city])");
        df.na().drop("all", new String[]{"city", "backup_city"}).show();

        SparkUtil.title("na.fill per column");
        fillDefaults(df).show();

        SparkUtil.title("coalesce(city, backup_city, 'N/A')");
        df.withColumn("best_city", coalesce(col("city"), col("backup_city"), lit("N/A"))).show();

        SparkUtil.title("Forward fill temp");
        forwardFill(df).show();

        SparkUtil.title("== vs <=> (null-safe)");
        df.select(col("city"), col("backup_city"),
                col("city").equalTo(col("backup_city")).as("eq"),
                col("city").eqNullSafe(col("backup_city")).as("null_safe_eq")).show();

        spark.stop();
    }
}
