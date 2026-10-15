package spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;

import static org.apache.spark.sql.functions.*;
import static spark.SparkUtil.r;

/**
 * "Gaps and islands" + sessionization - asked a LOT for data engineer roles.
 *  1. Users who logged in N or more consecutive days
 *  2. Longest login streak per user
 *  3. Sessionize clickstream: new session after 30 min of inactivity
 *  4. Session duration and clicks per session
 *
 * TRICK (islands): date - row_number() is CONSTANT inside a run of consecutive dates.
 * TRICK (sessions): flag = gap > 30min ? 1 : 0, then session_id = running sum(flag).
 */
public class ConsecutiveAndSessions {

    // 1 + 2. Consecutive login days
    static Dataset<Row> streaks(Dataset<Row> logins) {
        Dataset<Row> days = logins
                .select(col("user_id"), to_date(col("login_date")).as("d"))
                .distinct();                                     // multiple logins per day count once

        WindowSpec w = Window.partitionBy("user_id").orderBy("d");
        return days.withColumn("rn", row_number().over(w))
                .withColumn("grp", date_sub(col("d"), col("rn")))  // same grp = same island
                .groupBy("user_id", "grp")
                .agg(min("d").as("start"), max("d").as("end"), count(lit(1)).as("streak"));
    }

    static Dataset<Row> usersWithNConsecutiveDays(Dataset<Row> logins, int n) {
        return streaks(logins).filter(col("streak").geq(n)).select("user_id").distinct();
    }

    static Dataset<Row> longestStreak(Dataset<Row> logins) {
        return streaks(logins).groupBy("user_id").agg(max("streak").as("longest_streak"));
    }

    // 3 + 4. Sessionization
    static Dataset<Row> sessionize(Dataset<Row> clicks, int gapMinutes) {
        WindowSpec byUser = Window.partitionBy("user_id").orderBy("ts");
        Dataset<Row> withFlag = clicks
                .withColumn("ts", to_timestamp(col("ts")))
                .withColumn("prev_ts", lag("ts", 1).over(byUser))
                .withColumn("gap_sec", unix_timestamp(col("ts")).minus(unix_timestamp(col("prev_ts"))))
                .withColumn("new_session",
                        when(col("prev_ts").isNull().or(col("gap_sec").gt(gapMinutes * 60)), 1).otherwise(0));

        return withFlag.withColumn("session_id",
                sum("new_session").over(byUser.rowsBetween(Window.unboundedPreceding(), Window.currentRow())));
    }

    static Dataset<Row> sessionStats(Dataset<Row> sessions) {
        return sessions.groupBy("user_id", "session_id")
                .agg(min("ts").as("start"), max("ts").as("end"), count(lit(1)).as("clicks"))
                .withColumn("duration_min",
                        unix_timestamp(col("end")).minus(unix_timestamp(col("start"))).divide(60));
    }

    public static void main(String[] args) {
        SparkSession spark = SparkUtil.session("ConsecutiveAndSessions");

        Dataset<Row> logins = SparkUtil.df(spark, "user_id INT, login_date STRING",
                r(1, "2024-01-01"), r(1, "2024-01-02"), r(1, "2024-01-02"), r(1, "2024-01-03"),
                r(1, "2024-01-05"),
                r(2, "2024-01-01"), r(2, "2024-01-03"), r(2, "2024-01-04"),
                r(3, "2024-01-10"), r(3, "2024-01-11"), r(3, "2024-01-12"), r(3, "2024-01-13"));

        SparkUtil.title("Streaks (islands)");
        streaks(logins).orderBy("user_id", "start").show();

        SparkUtil.title("Users with >= 3 consecutive days");
        usersWithNConsecutiveDays(logins, 3).show();

        SparkUtil.title("Longest streak per user");
        longestStreak(logins).orderBy("user_id").show();

        Dataset<Row> clicks = SparkUtil.df(spark, "user_id INT, ts STRING, page STRING",
                r(1, "2024-01-01 10:00:00", "home"),
                r(1, "2024-01-01 10:10:00", "search"),
                r(1, "2024-01-01 10:20:00", "product"),
                r(1, "2024-01-01 11:30:00", "home"),     // > 30 min gap -> new session
                r(1, "2024-01-01 11:35:00", "cart"),
                r(2, "2024-01-01 09:00:00", "home"),
                r(2, "2024-01-01 09:45:00", "home"));    // > 30 min gap -> new session

        Dataset<Row> sessions = sessionize(clicks, 30);
        SparkUtil.title("Sessionized clicks");
        sessions.show(false);

        SparkUtil.title("Session stats");
        sessionStats(sessions).orderBy("user_id", "session_id").show();

        spark.stop();
    }
}
