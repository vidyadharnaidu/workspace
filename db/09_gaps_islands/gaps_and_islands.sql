-- =====================================================================
-- 09 GAPS AND ISLANDS (consecutive days, streaks, missing ids)
--
-- TRICK FOR ISLANDS (consecutive runs):
--   value - ROW_NUMBER() is CONSTANT inside a run of consecutive values.
--     date        rn   date - rn
--     2025-01-01  1    2024-12-31  \
--     2025-01-02  2    2024-12-31   } same group -> one streak
--     2025-01-03  3    2024-12-31  /
--     2025-01-05  4    2025-01-01  <- new group
--   Then GROUP BY that key: MIN = start, MAX = end, COUNT = length.
--   (De-duplicate first! Two logins on the same day would break the math.)
--
-- TRICK FOR GAPS: LEAD(value) - value > 1  -> a gap starts after this row.
-- =====================================================================


-- Q1. Login streaks per user (start, end, length)
WITH days AS (
    SELECT DISTINCT user_id, login_date FROM logins
),
grouped AS (
    SELECT user_id, login_date,
           login_date - CAST(ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) AS INTEGER) AS grp
    FROM days
)
SELECT user_id,
       MIN(login_date) AS streak_start,
       MAX(login_date) AS streak_end,
       COUNT(*)        AS streak_days
FROM grouped
GROUP BY user_id, grp
ORDER BY user_id, streak_start;


-- Q2. Users who logged in at least 3 CONSECUTIVE days
WITH days AS (
    SELECT DISTINCT user_id, login_date FROM logins
),
grouped AS (
    SELECT user_id,
           login_date - CAST(ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) AS INTEGER) AS grp
    FROM days
)
SELECT DISTINCT user_id
FROM grouped
GROUP BY user_id, grp
HAVING COUNT(*) >= 3
ORDER BY user_id;


-- Q3. Same question with LAG/LEAD (no islands): today, yesterday and the day before all present
SELECT DISTINCT user_id
FROM (SELECT user_id, login_date,
             LAG(login_date, 2) OVER (PARTITION BY user_id ORDER BY login_date) AS two_back
      FROM (SELECT DISTINCT user_id, login_date FROM logins) d) t
WHERE login_date - two_back = 2
ORDER BY user_id;


-- Q4. LONGEST streak per user
WITH days AS (
    SELECT DISTINCT user_id, login_date FROM logins
),
streaks AS (
    SELECT user_id, COUNT(*) AS len
    FROM (SELECT user_id,
                 login_date - CAST(ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) AS INTEGER) AS grp
          FROM days) g
    GROUP BY user_id, grp
)
SELECT user_id, MAX(len) AS longest_streak
FROM streaks
GROUP BY user_id
ORDER BY user_id;


-- Q5. GAPS: missing login days between a user's first and last login
SELECT user_id,
       CAST(login_date + 1 AS DATE)      AS gap_start,
       CAST(next_login - 1 AS DATE)      AS gap_end,
       next_login - login_date - 1       AS missing_days
FROM (SELECT user_id, login_date,
             LEAD(login_date) OVER (PARTITION BY user_id ORDER BY login_date) AS next_login
      FROM (SELECT DISTINCT user_id, login_date FROM logins) d) t
WHERE next_login - login_date > 1
ORDER BY user_id, gap_start;


-- Q6. GAPS in an id sequence: which employee ids are missing? (make a gap with a filtered view)
WITH ids AS (
    SELECT emp_id FROM employees WHERE emp_id NOT IN (4, 5, 9)
)
SELECT emp_id + 1 AS missing_from,
       next_id - 1 AS missing_to
FROM (SELECT emp_id, LEAD(emp_id) OVER (ORDER BY emp_id) AS next_id FROM ids) t
WHERE next_id - emp_id > 1;


-- Q7. ISLANDS of consecutive ids -> compact ranges "1-3, 6-8, 10-12"
WITH ids AS (
    SELECT emp_id FROM employees WHERE emp_id NOT IN (4, 5, 9)
)
SELECT MIN(emp_id) AS range_start, MAX(emp_id) AS range_end
FROM (SELECT emp_id, emp_id - ROW_NUMBER() OVER (ORDER BY emp_id) AS grp FROM ids) t
GROUP BY grp
ORDER BY range_start;


-- Q8. Consecutive days with RISING temperature streaks (islands on a condition)
WITH flagged AS (
    SELECT record_date, temperature,
           CASE WHEN temperature > LAG(temperature) OVER (ORDER BY record_date) THEN 1 ELSE 0 END AS is_up
    FROM weather
),
groups AS (
    -- running count of "not up" rows: stays constant while temperature keeps rising
    SELECT record_date, temperature, is_up,
           SUM(CASE WHEN is_up = 0 THEN 1 ELSE 0 END) OVER (ORDER BY record_date) AS grp
    FROM flagged
)
SELECT MIN(record_date) AS run_start, MAX(record_date) AS run_end, COUNT(*) AS days_in_run
FROM groups
GROUP BY grp
ORDER BY run_start;
