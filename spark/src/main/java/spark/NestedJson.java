package spark;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.spark.sql.functions.*;
import static spark.SparkUtil.r;

/**
 * Semi-structured data questions:
 *  1. Read nested JSON, access struct fields
 *  2. explode vs explode_outer vs posexplode
 *  3. Parse a JSON string column (from_json) / build JSON (to_json)
 *  4. Flatten ANY nested schema generically (recursive)
 *  5. Split a comma-separated column into rows / array functions
 *
 * TRICK: explode DROPS rows whose array is null/empty -> use explode_outer to keep them.
 */
public class NestedJson {

    static final List<String> JSON = List.of(
            "{\"id\":1,\"name\":\"Alice\",\"address\":{\"city\":\"Pune\",\"zip\":\"411001\"},\"orders\":[{\"oid\":101,\"amt\":250.0},{\"oid\":102,\"amt\":99.5}]}",
            "{\"id\":2,\"name\":\"Bob\",\"address\":{\"city\":\"Delhi\",\"zip\":\"110001\"},\"orders\":[]}",
            "{\"id\":3,\"name\":\"Carol\",\"address\":null,\"orders\":[{\"oid\":103,\"amt\":10.0}]}");

    static Dataset<Row> readJson(SparkSession spark) {
        // In real life: spark.read().option("multiLine", true).json("path/file.json")
        return spark.read().json(spark.createDataset(JSON, Encoders.STRING()));
    }

    static Dataset<Row> explodeOrders(Dataset<Row> df, boolean keepEmpty) {
        Column exploded = keepEmpty ? explode_outer(col("orders")) : explode(col("orders"));
        return df.select(col("id"), col("name"), col("address.city").as("city"), exploded.as("order"))
                .select(col("id"), col("name"), col("city"), col("order.oid"), col("order.amt"));
    }

    // posexplode gives the index too
    static Dataset<Row> posExplode(Dataset<Row> df) {
        return df.select(col("id"), posexplode(col("orders")).as(new String[]{"pos", "order"}));
    }

    static Dataset<Row> parseJsonString(SparkSession spark) {
        Dataset<Row> raw = SparkUtil.df(spark, "event_id INT, payload STRING",
                r(1, "{\"user\":\"u1\",\"action\":\"click\",\"tags\":[\"a\",\"b\"]}"),
                r(2, "{\"user\":\"u2\",\"action\":\"view\",\"tags\":[]}"),
                r(3, "not json"));                       // -> null struct in PERMISSIVE mode
        StructType schema = StructType.fromDDL("user STRING, action STRING, tags ARRAY<STRING>");
        return raw.withColumn("data", from_json(col("payload"), schema))
                .select(col("event_id"), col("data.*"));
    }

    // Generic flattener: struct -> parent_child columns, arrays of struct -> explode_outer, repeat.
    static Dataset<Row> flatten(Dataset<Row> df) {
        while (true) {
            StructField nested = Arrays.stream(df.schema().fields())
                    .filter(f -> f.dataType() instanceof StructType
                            || (f.dataType() instanceof ArrayType at && at.elementType() instanceof StructType))
                    .findFirst().orElse(null);
            if (nested == null) return df;

            String name = nested.name();
            if (nested.dataType() instanceof ArrayType) {
                df = df.withColumn(name, explode_outer(col(name)));   // array<struct> -> struct, next loop expands it
                continue;
            }
            List<Column> cols = new ArrayList<>();
            for (StructField f : df.schema().fields()) {
                if (!f.name().equals(name)) cols.add(col("`" + f.name() + "`"));
            }
            for (StructField child : ((StructType) nested.dataType()).fields()) {
                cols.add(col("`" + name + "`.`" + child.name() + "`").as(name + "_" + child.name()));
            }
            df = df.select(cols.toArray(new Column[0]));
        }
    }

    static Dataset<Row> csvColumnToRows(SparkSession spark) {
        Dataset<Row> df = SparkUtil.df(spark, "user STRING, skills STRING",
                r("u1", "java,spark,sql"), r("u2", "python"), r("u3", null));
        return df.withColumn("skill_arr", split(col("skills"), ","))
                .withColumn("n_skills", size(col("skill_arr")))           // size(null) = -1
                .withColumn("knows_spark", array_contains(col("skill_arr"), "spark"))
                .withColumn("skill", explode_outer(col("skill_arr")));
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("NestedJson");
        Dataset<Row> df = readJson(spark);

        SparkUtil.title("Inferred schema");
        df.printSchema();
        df.show(false);

        SparkUtil.title("explode (Bob with no orders disappears)");
        explodeOrders(df, false).show();

        SparkUtil.title("explode_outer (Bob kept with nulls)");
        explodeOrders(df, true).show();

        SparkUtil.title("posexplode");
        posExplode(df).show(false);

        SparkUtil.title("from_json on a string column");
        parseJsonString(spark).show(false);

        SparkUtil.title("to_json - struct back to string");
        df.select(col("id"), to_json(col("address")).as("address_json")).show(false);

        SparkUtil.title("Generic flatten");
        Dataset<Row> flat = flatten(df);
        flat.printSchema();
        flat.show();

        SparkUtil.title("Comma separated column -> rows + array functions");
        csvColumnToRows(spark).show();

        spark.stop();
    }
}
