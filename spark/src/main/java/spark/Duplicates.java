package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;

import static org.apache.spark.sql.functions.*;
import static spark.SparkUtil.r;

/**
 * Duplicate questions:
 *  1. Find duplicate rows (and how many times)
 *  2. Remove exact duplicates: distinct() vs dropDuplicates()
 *  3. Remove duplicates on a key, keep the LATEST record (CDC / upsert pattern)
 *  4. Find keys that appear more than once
 *
 * TRICK: dropDuplicates(cols) keeps an ARBITRARY row -> when "which one" matters, use row_number().
 */
public class Duplicates {

    static Dataset<Row> findDuplicateRows(Dataset<Row> df) {
        return df.groupBy(df.columns()[0], java.util.Arrays.copyOfRange(df.columns(), 1, df.columns().length))
                .count()
                .filter(col("count").gt(1));
    }

    static Dataset<Row> duplicateKeys(Dataset<Row> df, String key) {
        return df.groupBy(key).agg(count(lit(1)).as("cnt")).filter(col("cnt").gt(1));
    }

    // Latest record per key: row_number over key ordered by timestamp desc, keep rn = 1
    static Dataset<Row> latestPerKey(Dataset<Row> df) {
        return df.withColumn("rn", row_number().over(
                        Window.partitionBy("customer_id").orderBy(col("updated_at").desc())))
                .filter(col("rn").equalTo(1))
                .drop("rn");
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("Duplicates");
        Dataset<Row> df = SparkUtil.df(spark, "customer_id INT, email STRING, updated_at STRING",
                r(1, "a@x.com", "2024-01-01"),
                r(1, "a@x.com", "2024-01-01"),      // exact duplicate
                r(1, "a_new@x.com", "2024-03-01"),  // newer version of 1
                r(2, "b@x.com", "2024-02-01"),
                r(3, "c@x.com", "2024-01-15"),
                r(3, "c2@x.com", "2024-01-10"));

        SparkUtil.title("Exact duplicate rows");
        findDuplicateRows(df).show();

        SparkUtil.title("distinct() - removes exact duplicates");
        df.distinct().show();

        SparkUtil.title("dropDuplicates(customer_id) - arbitrary row kept!");
        df.dropDuplicates("customer_id").show();

        SparkUtil.title("Keys appearing more than once");
        duplicateKeys(df, "customer_id").show();

        SparkUtil.title("Latest record per customer (row_number)");
        latestPerKey(df).show();

        spark.stop();
    }
}
