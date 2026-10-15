package spark;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;
import static spark.SparkUtil.r;

/**
 * Q: Rows -> columns (pivot) and columns -> rows (unpivot / melt).
 *
 * TRICK: pass the pivot values explicitly -> Spark skips an extra job to find distinct values.
 *        Unpivot: Dataset.unpivot (Spark 3.4+) or the classic stack() SQL function.
 */
public class PivotUnpivot {

    static Dataset<Row> pivot(Dataset<Row> sales) {
        return sales.groupBy("product")
                .pivot("quarter", java.util.List.of("Q1", "Q2", "Q3", "Q4"))
                .agg(sum("amount"))
                .na().fill(0)
                .orderBy("product");
    }

    // Classic way - works on every Spark version
    static Dataset<Row> unpivotWithStack(Dataset<Row> wide) {
        return wide.selectExpr("product",
                "stack(4, 'Q1', Q1, 'Q2', Q2, 'Q3', Q3, 'Q4', Q4) AS (quarter, amount)");
    }

    // Spark 3.4+
    static Dataset<Row> unpivotBuiltIn(Dataset<Row> wide) {
        return wide.unpivot(
                new Column[]{col("product")},
                new Column[]{col("Q1"), col("Q2"), col("Q3"), col("Q4")},
                "quarter", "amount");
    }

    // Bonus: transpose-like "one row per key with a list of values"
    static Dataset<Row> collectPerProduct(Dataset<Row> sales) {
        return sales.groupBy("product")
                .agg(collect_list(concat_ws(":", col("quarter"), col("amount"))).as("quarters"));
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("PivotUnpivot");
        Dataset<Row> sales = SparkUtil.df(spark, "product STRING, quarter STRING, amount INT",
                r("Laptop", "Q1", 100), r("Laptop", "Q2", 150), r("Laptop", "Q4", 300),
                r("Phone", "Q1", 200), r("Phone", "Q2", 250), r("Phone", "Q3", 50),
                r("Phone", "Q3", 70));

        SparkUtil.title("Pivot");
        Dataset<Row> wide = pivot(sales);
        wide.show();

        SparkUtil.title("Unpivot with stack()");
        unpivotWithStack(wide).show();

        SparkUtil.title("Unpivot with Dataset.unpivot()");
        unpivotBuiltIn(wide).show();

        SparkUtil.title("collect_list per product");
        collectPerProduct(sales).show(false);

        spark.stop();
    }
}
