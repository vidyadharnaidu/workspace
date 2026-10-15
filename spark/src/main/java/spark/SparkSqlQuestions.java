package spark;

import org.apache.spark.sql.SparkSession;

import static spark.SparkUtil.r;

/**
 * The same classic questions answered in pure Spark SQL (interviewers often say "now do it in SQL").
 *
 * TRICK: createOrReplaceTempView = session scoped; createGlobalTempView = across sessions (global_temp.name).
 */
public class SparkSqlQuestions {

    static void run(SparkSession spark, String title, String sql) {
        SparkUtil.title(title);
        spark.sql(sql).show(false);
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("SparkSqlQuestions");
        SparkUtil.employees(spark).createOrReplaceTempView("emp");
        SparkUtil.departments(spark).createOrReplaceTempView("dept");
        SparkUtil.df(spark, "order_id INT, customer STRING, order_date STRING, amount INT",
                r(1, "c1", "2024-01-05", 100), r(2, "c1", "2024-02-10", 200),
                r(3, "c2", "2024-01-20", 50), r(4, "c3", "2024-03-01", 500),
                r(5, "c1", "2024-03-15", 300), r(6, "c2", "2024-03-20", 75))
                .createOrReplaceTempView("orders");

        run(spark, "2nd highest salary", """
                SELECT MAX(salary) AS second_highest FROM emp
                WHERE salary < (SELECT MAX(salary) FROM emp)
                """);

        run(spark, "Top 2 per dept", """
                SELECT dept, name, salary FROM (
                  SELECT *, DENSE_RANK() OVER (PARTITION BY dept ORDER BY salary DESC) AS rnk FROM emp
                ) WHERE rnk <= 2
                ORDER BY dept, salary DESC
                """);

        run(spark, "Dept with highest total salary", """
                SELECT dept, SUM(salary) AS total FROM emp
                GROUP BY dept ORDER BY total DESC LIMIT 1
                """);

        run(spark, "Depts with more than 2 employees (HAVING)", """
                SELECT dept, COUNT(*) AS cnt FROM emp GROUP BY dept HAVING COUNT(*) > 2
                """);

        run(spark, "Employees without department (NOT EXISTS / LEFT ANTI)", """
                SELECT e.* FROM emp e LEFT ANTI JOIN dept d ON e.dept = d.dept
                """);

        run(spark, "Employee and manager", """
                SELECT e.name AS employee, m.name AS manager
                FROM emp e LEFT JOIN emp m ON e.manager_id = m.id
                """);

        run(spark, "Monthly revenue + month-over-month growth %", """
                WITH monthly AS (
                  SELECT date_format(to_date(order_date), 'yyyy-MM') AS month, SUM(amount) AS revenue
                  FROM orders GROUP BY 1
                )
                SELECT month, revenue,
                       LAG(revenue) OVER (ORDER BY month) AS prev,
                       ROUND((revenue - LAG(revenue) OVER (ORDER BY month)) * 100.0
                             / LAG(revenue) OVER (ORDER BY month), 2) AS growth_pct
                FROM monthly ORDER BY month
                """);

        run(spark, "Customers' first order (row_number)", """
                SELECT customer, order_id, order_date FROM (
                  SELECT *, ROW_NUMBER() OVER (PARTITION BY customer ORDER BY order_date) AS rn FROM orders
                ) WHERE rn = 1
                """);

        run(spark, "Cumulative revenue per customer", """
                SELECT customer, order_date, amount,
                       SUM(amount) OVER (PARTITION BY customer ORDER BY order_date
                                         ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cum_amount
                FROM orders ORDER BY customer, order_date
                """);

        run(spark, "Customers who ordered in every month (relational division)", """
                SELECT customer FROM orders
                GROUP BY customer
                HAVING COUNT(DISTINCT date_format(to_date(order_date), 'yyyy-MM'))
                     = (SELECT COUNT(DISTINCT date_format(to_date(order_date), 'yyyy-MM')) FROM orders)
                """);

        run(spark, "Pivot in SQL", """
                SELECT * FROM (SELECT dept, salary FROM emp)
                PIVOT (SUM(salary) FOR dept IN ('Eng', 'Sales', 'HR'))
                """);

        run(spark, "CASE WHEN bucketing", """
                SELECT name, salary,
                       CASE WHEN salary >= 8000 THEN 'high'
                            WHEN salary >= 5000 THEN 'mid'
                            ELSE 'low' END AS band
                FROM emp
                """);

        spark.stop();
    }
}
