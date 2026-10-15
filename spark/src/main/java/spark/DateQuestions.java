package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;

import static org.apache.spark.sql.functions.*;
import static spark.SparkUtil.r;

/**
 * Date / time questions:
 *  1. Parse strings in mixed formats into dates
 *  2. Extract year / month / day-of-week, format dates
 *  3. Days between dates, add months, last day of month
 *  4. Monthly aggregation + month-over-month growth
 *  5. Fill missing dates in a series (calendar join)
 *  6. Tumbling time windows on timestamps (batch)
 *
 * TRICK: coalesce(to_date(c, fmt1), to_date(c, fmt2)) parses mixed formats
 *        (to_date returns null on mismatch in non-ANSI mode).
 */
public class DateQuestions {

    static Dataset<Row> parseMixed(Dataset<Row> df) {
        return df.withColumn("d", coalesce(
                to_date(col("raw"), "yyyy-MM-dd"),
                to_date(col("raw"), "dd/MM/yyyy"),
                to_date(col("raw"), "MMM d, yyyy")));
    }

    static Dataset<Row> dateParts(Dataset<Row> df) {
        return df.select(col("d"),
                year(col("d")).as("year"), month(col("d")).as("month"),
                dayofweek(col("d")).as("dow_1_is_sun"),
                date_format(col("d"), "EEEE").as("day_name"),
                datediff(current_date(), col("d")).as("days_ago"),
                add_months(col("d"), 1).as("plus_1_month"),
                last_day(col("d")).as("month_end"),
                trunc(col("d"), "month").as("month_start"));
    }

    static Dataset<Row> monthOverMonth(Dataset<Row> orders) {
        Dataset<Row> monthly = orders
                .groupBy(date_trunc("month", to_timestamp(col("order_date"))).as("month"))
                .agg(sum("amount").as("revenue"));
        return monthly.withColumn("prev", lag("revenue", 1).over(Window.orderBy("month")))
                .withColumn("growth_pct",
                        round(col("revenue").minus(col("prev")).multiply(100.0).divide(col("prev")), 2))
                .orderBy("month");
    }

    // Fill gaps: generate a calendar with sequence(min, max, 1 day), left join actuals, nulls -> 0
    static Dataset<Row> fillMissingDates(Dataset<Row> daily) {
        Row bounds = daily.agg(min(to_date(col("day"))), max(to_date(col("day")))).first();
        Dataset<Row> calendar = daily.sparkSession().range(1)
                .select(explode(sequence(lit(bounds.getDate(0)), lit(bounds.getDate(1)), expr("interval 1 day")))
                        .as("day"));
        return calendar.join(daily.withColumn("day", to_date(col("day"))), "day", "left")
                .na().fill(0, new String[]{"amount"})
                .orderBy("day");
    }

    // Tumbling 1-hour windows over event timestamps
    static Dataset<Row> hourlyWindows(Dataset<Row> events) {
        return events.groupBy(window(to_timestamp(col("ts")), "1 hour"))
                .agg(count(lit(1)).as("events"))
                .select(col("window.start"), col("window.end"), col("events"))
                .orderBy("start");
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("DateQuestions");
        Dataset<Row> raw = SparkUtil.df(spark, "raw STRING",
                r("2024-02-29"), r("15/08/2023"), r("Jan 5, 2024"), r("garbage"));

        SparkUtil.title("Parse mixed formats");
        Dataset<Row> parsed = parseMixed(raw);
        parsed.show();

        SparkUtil.title("Date parts");
        dateParts(parsed.filter(col("d").isNotNull())).show(false);

        Dataset<Row> orders = SparkUtil.df(spark, "order_date STRING, amount INT",
                r("2024-01-05", 100), r("2024-01-20", 50), r("2024-02-10", 300),
                r("2024-03-01", 150), r("2024-03-15", 300));
        SparkUtil.title("Month-over-month growth");
        monthOverMonth(orders).show(false);

        SparkUtil.title("Fill missing dates");
        fillMissingDates(SparkUtil.df(spark, "day STRING, amount INT",
                r("2024-01-01", 10), r("2024-01-04", 40), r("2024-01-06", 60))).show();

        SparkUtil.title("Hourly tumbling windows");
        hourlyWindows(SparkUtil.df(spark, "ts STRING",
                r("2024-01-01 10:05:00"), r("2024-01-01 10:55:00"),
                r("2024-01-01 11:10:00"), r("2024-01-01 13:00:00"))).show(false);

        spark.stop();
    }
}
