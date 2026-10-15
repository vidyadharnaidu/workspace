-- =====================================================================
-- 07 DUPLICATES AND NULLS
--
-- REMEMBER:
--   FIND duplicates    -> GROUP BY key HAVING COUNT(*) > 1
--   SHOW all dup rows  -> COUNT(*) OVER (PARTITION BY key) > 1
--   KEEP ONE           -> ROW_NUMBER() OVER (PARTITION BY key ORDER BY id) = 1
--   DELETE the extras  -> delete rows whose id is not the MIN(id) of their group
--   NULL: = NULL is never true; use IS NULL / IS DISTINCT FROM. Aggregates skip NULLs.
--
-- Statements that change data are wrapped in BEGIN ... ROLLBACK so the sample data stays intact.
-- =====================================================================


-- Q1. Find duplicate emails (LeetCode 182)
SELECT email, COUNT(*) AS copies
FROM contacts
GROUP BY email
HAVING COUNT(*) > 1
ORDER BY email;


-- Q2. Case-insensitive duplicates ('john@x.com' and 'John@X.com' are the same)
SELECT LOWER(email) AS email, COUNT(*) AS copies
FROM contacts
GROUP BY LOWER(email)
HAVING COUNT(*) > 1
ORDER BY email;


-- Q3. Show every duplicated ROW (not just the key) with its group size
SELECT id, name, email, copies
FROM (SELECT c.*, COUNT(*) OVER (PARTITION BY LOWER(email)) AS copies FROM contacts c) t
WHERE copies > 1
ORDER BY LOWER(email), id;


-- Q4. De-duplicate in a SELECT: keep the lowest id per email
SELECT id, name, email
FROM (SELECT c.*, ROW_NUMBER() OVER (PARTITION BY LOWER(email) ORDER BY id) AS rn FROM contacts c) t
WHERE rn = 1
ORDER BY id;


-- Q5. DELETE duplicates, keep the lowest id (LeetCode 196) - two ways
BEGIN;

-- (a) portable: delete ids that are not the minimum of their group
--     (MySQL needs the subquery wrapped once more: ... NOT IN (SELECT id FROM (SELECT MIN(id) id ...) x))
DELETE FROM contacts
WHERE id NOT IN (SELECT MIN(id) FROM contacts GROUP BY LOWER(email));

SELECT * FROM contacts ORDER BY id;

ROLLBACK;

BEGIN;

-- (b) PostgreSQL self-join delete: remove a row if a SAME email with a SMALLER id exists
--     MySQL: DELETE c1 FROM contacts c1 JOIN contacts c2 ON c1.email = c2.email AND c1.id > c2.id;
DELETE FROM contacts c
USING contacts d
WHERE LOWER(c.email) = LOWER(d.email)
  AND c.id > d.id;

SELECT COUNT(*) AS remaining_rows FROM contacts;

ROLLBACK;


-- Q6. Duplicate ORDER LINES check: same (order_id, product_id) twice? (should be 0 thanks to PRIMARY KEY)
SELECT order_id, product_id, COUNT(*) AS copies
FROM order_items
GROUP BY order_id, product_id
HAVING COUNT(*) > 1;


-- Q7. Duplicate logins on the same day, and a de-duplicated count of active days per user
SELECT user_id, login_date, COUNT(*) AS logins_that_day
FROM logins
GROUP BY user_id, login_date
HAVING COUNT(*) > 1;

SELECT user_id, COUNT(*) AS login_rows, COUNT(DISTINCT login_date) AS active_days
FROM logins
GROUP BY user_id
ORDER BY user_id;


-- ---------------------------------------------------------------------
-- NULL questions
-- ---------------------------------------------------------------------

-- Q8. NULL comparisons: what does each expression return?
SELECT NULL = NULL                  AS null_equals_null,      -- NULL (not TRUE!)
       NULL IS NULL                 AS null_is_null,          -- TRUE
       NULL <> 1                    AS null_not_equal_1,      -- NULL
       NULL IS DISTINCT FROM 1      AS is_distinct,           -- TRUE  (NULL-safe <>)
       NULL IS NOT DISTINCT FROM NULL AS not_distinct,        -- TRUE  (NULL-safe =)   MySQL: <=>
       1 + NULL                     AS add_null,              -- NULL
       'a' || NULL                  AS concat_null;           -- NULL (MySQL CONCAT too; CONCAT_WS skips NULLs)


-- Q9. Aggregates ignore NULLs: AVG over 3 values with one NULL divides by 2, not 3
SELECT AVG(v)                  AS avg_ignoring_nulls,   -- (10 + 20) / 2 = 15
       AVG(COALESCE(v, 0))     AS avg_nulls_as_zero,    -- (10 + 20 + 0) / 3 = 10
       COUNT(*)                AS all_rows,
       COUNT(v)                AS non_null_rows
FROM (VALUES (10), (20), (NULL)) AS t (v);


-- Q10. Safe division: NULLIF turns a zero divisor into NULL instead of an error
SELECT account_id,
       SUM(amount)                                            AS net,
       SUM(CASE WHEN amount < 0 THEN -amount ELSE 0 END)      AS debits,
       ROUND(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END)
             / NULLIF(SUM(CASE WHEN amount < 0 THEN -amount ELSE 0 END), 0), 2) AS credit_to_debit_ratio
FROM transactions
GROUP BY account_id
ORDER BY account_id;


-- Q11. Rows where a column is NULL vs "not equal": != excludes NULL rows silently!
SELECT COUNT(*) AS not_dept_1_wrong FROM employees WHERE dept_id <> 1;                              -- 6 (Oscar missing)
SELECT COUNT(*) AS not_dept_1_right FROM employees WHERE dept_id <> 1 OR dept_id IS NULL;           -- 7
SELECT COUNT(*) AS not_dept_1_short FROM employees WHERE dept_id IS DISTINCT FROM 1;                -- 7
