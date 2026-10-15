package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;

import static org.apache.spark.sql.functions.*;

/**
 * The #1 Spark interview topic. Questions:
 *  1. Top N salaries per department
 *  2. Nth highest salary (overall and per dept)
 *  3. row_number vs rank vs dense_rank
 *  4. Running total / cumulative sum
 *  5. Difference from previous row (lag / lead)
 *  6. Moving average (last 3 rows)
 *  7. Salary as % of dept total, compare to dept avg
 *  8. Employees earning more than their dept average
 *  9. First / last value per group
 *
 * TRICK: Window.partitionBy(group).orderBy(sortCol) + function.over(window)
 *        Ties? row_number = 1,2,3 | rank = 1,1,3 | dense_rank = 1,1,2
 */
public class WindowFunctions {

    // 1. Top N per group - dense_rank keeps ties (both 8000s in Eng are "2nd")
    static Dataset<Row> topNPerDept(Dataset<Row> emp, int n) {
        WindowSpec w = Window.partitionBy("dept").orderBy(col("salary").desc());
        return emp.withColumn("rnk", dense_rank().over(w))
                .filter(col("rnk").leq(n))
                .orderBy("dept", "rnk");
    }

    // 2. Nth highest salary overall (no partitionBy -> single partition, fine for small data)
    static Dataset<Row> nthHighestSalary(Dataset<Row> emp, int n) {
        WindowSpec w = Window.orderBy(col("salary").desc());
        return emp.withColumn("rnk", dense_rank().over(w))
                .filter(col("rnk").equalTo(n))
                .select("salary").distinct();
    }

    // 2b. Second highest without window: max of salaries below the max
    static Dataset<Row> secondHighestNoWindow(Dataset<Row> emp) {
        int max = emp.agg(max("salary")).first().getInt(0);
        return emp.filter(col("salary").lt(max)).agg(max("salary").as("second_highest"));
    }

    // 3. Show all three ranking functions side by side
    static Dataset<Row> rankingComparison(Dataset<Row> emp) {
        WindowSpec w = Window.partitionBy("dept").orderBy(col("salary").desc());
        return emp.select(col("dept"), col("name"), col("salary"),
                row_number().over(w).as("row_number"),
                rank().over(w).as("rank"),
                dense_rank().over(w).as("dense_rank"),
                percent_rank().over(w).as("percent_rank"));
    }

    // 4. Running total. TRICK: explicit frame - unboundedPreceding .. currentRow
    static Dataset<Row> runningTotal(Dataset<Row> emp) {
        WindowSpec w = Window.partitionBy("dept").orderBy("id")
                .rowsBetween(Window.unboundedPreceding(), Window.currentRow());
        return emp.withColumn("running_total", sum("salary").over(w));
    }

    // 5. lag / lead - difference from previous employee (by id) in the same dept
    static Dataset<Row> lagLead(Dataset<Row> emp) {
        WindowSpec w = Window.partitionBy("dept").orderBy("id");
        return emp.withColumn("prev_salary", lag("salary", 1).over(w))
                .withColumn("next_salary", lead("salary", 1).over(w))
                .withColumn("diff_from_prev", col("salary").minus(col("prev_salary")));
    }

    // 6. Moving average of current + 2 previous rows
    static Dataset<Row> movingAverage(Dataset<Row> sales) {
        WindowSpec w = Window.orderBy("day").rowsBetween(-2, Window.currentRow());
        return sales.withColumn("moving_avg_3", round(avg("amount").over(w), 2));
    }

    // 7. % of dept total + diff from dept avg. Window with only partitionBy = whole group frame.
    static Dataset<Row> percentOfDept(Dataset<Row> emp) {
        WindowSpec byDept = Window.partitionBy("dept");
        return emp.withColumn("dept_total", sum("salary").over(byDept))
                .withColumn("pct_of_dept", round(col("salary").multiply(100.0).divide(col("dept_total")), 2))
                .withColumn("diff_from_avg", col("salary").minus(avg("salary").over(byDept)));
    }

    // 8. Earning more than dept average - window way (no self-join needed)
    static Dataset<Row> aboveDeptAverage(Dataset<Row> emp) {
        return emp.withColumn("dept_avg", avg("salary").over(Window.partitionBy("dept")))
                .filter(col("salary").gt(col("dept_avg")));
    }

    // 8b. Same with groupBy + join (interviewers often ask for both)
    static Dataset<Row> aboveDeptAverageJoin(Dataset<Row> emp) {
        Dataset<Row> avgs = emp.groupBy("dept").agg(avg("salary").as("dept_avg"));
        return emp.join(avgs, "dept").filter(col("salary").gt(col("dept_avg")));
    }

    // 9. Highest and lowest paid name per dept.
    // TRICK: last() needs the frame to cover the whole partition, otherwise it returns the current row.
    static Dataset<Row> firstLastPerDept(Dataset<Row> emp) {
        WindowSpec w = Window.partitionBy("dept").orderBy(col("salary").desc())
                .rowsBetween(Window.unboundedPreceding(), Window.unboundedFollowing());
        return emp.select(col("dept"),
                        first("name").over(w).as("highest_paid"),
                        last("name").over(w).as("lowest_paid"))
                .distinct();
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("WindowFunctions");
        Dataset<Row> emp = SparkUtil.employees(spark);

        SparkUtil.title("Top 2 salaries per dept (dense_rank)");
        topNPerDept(emp, 2).show();

        SparkUtil.title("3rd highest salary");
        nthHighestSalary(emp, 3).show();

        SparkUtil.title("2nd highest without window");
        secondHighestNoWindow(emp).show();

        SparkUtil.title("row_number vs rank vs dense_rank");
        rankingComparison(emp).show();

        SparkUtil.title("Running total per dept");
        runningTotal(emp).show();

        SparkUtil.title("lag / lead");
        lagLead(emp).show();

        SparkUtil.title("3-day moving average");
        Dataset<Row> sales = SparkUtil.df(spark, "day INT, amount INT",
                SparkUtil.r(1, 100), SparkUtil.r(2, 200), SparkUtil.r(3, 300),
                SparkUtil.r(4, 400), SparkUtil.r(5, 500));
        movingAverage(sales).show();

        SparkUtil.title("% of dept total");
        percentOfDept(emp).show();

        SparkUtil.title("Above dept average (window)");
        aboveDeptAverage(emp).show();

        SparkUtil.title("Above dept average (join)");
        aboveDeptAverageJoin(emp).show();

        SparkUtil.title("Highest / lowest paid per dept");
        firstLastPerDept(emp).show();

        spark.stop();
    }
}
