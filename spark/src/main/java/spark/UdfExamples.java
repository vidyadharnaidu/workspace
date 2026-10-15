package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.api.java.UDF2;
import org.apache.spark.sql.expressions.UserDefinedFunction;
import org.apache.spark.sql.types.DataTypes;

import static org.apache.spark.sql.functions.*;
import static spark.SparkUtil.r;

/**
 * Q: Write a UDF. Why are UDFs slow? What to prefer?
 *
 * TRICK: UDFs are a black box to Catalyst -> no predicate pushdown, no codegen, row-by-row
 *        (de)serialization. Prefer built-in functions (when/regexp_replace/...) whenever possible.
 *        UDFs must handle null input themselves.
 */
public class UdfExamples {

    // DataFrame-API UDF
    static final UserDefinedFunction MASK_EMAIL = udf((UDF1<String, String>) email -> {
        if (email == null || !email.contains("@")) return email;
        String[] p = email.split("@", 2);
        return p[0].charAt(0) + "***@" + p[1];
    }, DataTypes.StringType);

    // SQL-registered UDF with two arguments
    static void registerSqlUdfs(SparkSession spark) {
        spark.udf().register("salary_band", (UDF2<Integer, Integer, String>) (salary, step) -> {
            if (salary == null) return "unknown";
            int low = (salary / step) * step;
            return low + "-" + (low + step - 1);
        }, DataTypes.StringType);
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("UdfExamples");
        Dataset<Row> users = SparkUtil.df(spark, "name STRING, email STRING, salary INT",
                r("alice", "alice@gmail.com", 52000), r("bob", "bob@corp.in", 87000), r("carol", null, null));

        SparkUtil.title("UDF via DataFrame API");
        users.withColumn("masked", MASK_EMAIL.apply(col("email"))).show();

        SparkUtil.title("Same result with BUILT-IN functions (preferred - Catalyst can optimize)");
        users.withColumn("masked", regexp_replace(col("email"), "^(.)[^@]*", "$1***")).show();

        registerSqlUdfs(spark);
        users.createOrReplaceTempView("users");

        SparkUtil.title("UDF via SQL");
        spark.sql("SELECT name, salary, salary_band(salary, 25000) AS band FROM users").show();

        SparkUtil.title("call_udf from DataFrame API");
        users.select(col("name"), call_udf("salary_band", col("salary"), lit(10000)).as("band")).show();

        SparkUtil.title("Built-in alternative: initcap / when");
        users.select(initcap(col("name")).as("name"),
                when(col("salary").geq(80000), "high").when(col("salary").isNotNull(), "normal")
                        .otherwise("unknown").as("level")).show();

        spark.stop();
    }
}
