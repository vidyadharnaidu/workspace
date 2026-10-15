-- =====================================================================
-- 10 PIVOT / UNPIVOT AND STRING FUNCTIONS
--
-- REMEMBER:
--   PIVOT (rows -> columns)   = GROUP BY row key + MAX(CASE WHEN category = 'X' THEN value END) per column
--   UNPIVOT (columns -> rows) = UNION ALL one SELECT per column
--   Strings (PostgreSQL -> MySQL):
--     a || b            -> CONCAT(a, b)
--     STRING_AGG(x, ',')-> GROUP_CONCAT(x SEPARATOR ',')
--     SPLIT_PART(s,'@',2)-> SUBSTRING_INDEX(s, '@', -1)
--     POSITION('@' IN s)-> LOCATE('@', s)
-- =====================================================================


-- Q1. PIVOT: one row per student, one column per subject (NULL where missing)
SELECT student,
       MAX(CASE WHEN subject = 'Math'    THEN score END) AS math,
       MAX(CASE WHEN subject = 'Science' THEN score END) AS science,
       MAX(CASE WHEN subject = 'English' THEN score END) AS english,
       SUM(score)                                        AS total
FROM scores
GROUP BY student
ORDER BY student;


-- Q2. PIVOT counts: orders per status per month (FILTER is the PostgreSQL shorthand)
SELECT CAST(DATE_TRUNC('month', order_date) AS DATE)     AS month,
       COUNT(*) FILTER (WHERE status = 'completed')  AS completed,
       COUNT(*) FILTER (WHERE status = 'cancelled')  AS cancelled,
       COUNT(*) FILTER (WHERE status = 'pending')    AS pending
FROM orders
GROUP BY CAST(DATE_TRUNC('month', order_date) AS DATE)
ORDER BY month;


-- Q3. UNPIVOT: turn a wide row back into (student, subject, score) rows
WITH wide AS (
    SELECT student,
           MAX(CASE WHEN subject = 'Math'    THEN score END) AS math,
           MAX(CASE WHEN subject = 'Science' THEN score END) AS science
    FROM scores
    GROUP BY student
)
SELECT student, 'Math' AS subject, math AS score FROM wide
UNION ALL
SELECT student, 'Science', science FROM wide
ORDER BY student, subject;


-- Q4. Top scorer per subject (pivot-adjacent classic)
SELECT subject, student, score
FROM (SELECT s.*, RANK() OVER (PARTITION BY subject ORDER BY score DESC) AS rnk FROM scores s) t
WHERE rnk = 1
ORDER BY subject;


-- Q5. Comma-separated employee names per department
SELECT d.dept_name,
       STRING_AGG(e.name, ', ' ORDER BY e.name) AS employees
FROM departments d
JOIN employees e ON e.dept_id = d.dept_id
GROUP BY d.dept_name
ORDER BY d.dept_name;


-- Q6. Email domain and username
SELECT email,
       SPLIT_PART(email, '@', 1)                     AS username,
       SPLIT_PART(email, '@', 2)                     AS domain,
       SUBSTR(email, POSITION('@' IN email) + 1)     AS domain_portable
FROM contacts
ORDER BY id;


-- Q7. Count contacts per email domain (case-insensitive)
SELECT LOWER(SPLIT_PART(email, '@', 2)) AS domain, COUNT(*) AS contacts
FROM contacts
GROUP BY LOWER(SPLIT_PART(email, '@', 2));


-- Q8. Fix name capitalisation: 'JOHN' -> 'John'  (PostgreSQL also has INITCAP)
SELECT name,
       UPPER(SUBSTR(name, 1, 1)) || LOWER(SUBSTR(name, 2)) AS fixed_name
FROM contacts
ORDER BY id;


-- Q9. Common string functions in one query
SELECT name,
       LENGTH(name)                   AS len,
       UPPER(name)                    AS upper_name,
       LEFT(name, 3)                  AS first3,
       RIGHT(name, 2)                 AS last2,
       REPLACE(name, 'a', '@')        AS replaced,
       REVERSE(name)                  AS reversed,
       TRIM('  ' || name || '  ')     AS trimmed,
       LPAD(CAST(emp_id AS TEXT), 4, '0') AS padded_id,
       'EMP-' || emp_id               AS code
FROM employees
WHERE emp_id <= 3;


-- Q10. Names that are palindromes (case-insensitive)
SELECT name,
       LOWER(name) = REVERSE(LOWER(name)) AS is_palindrome
FROM (VALUES ('Anna'), ('Bob'), ('Carol'), ('Otto')) AS t (name);


-- Q11. Products whose name starts with C, D or P (regular expression)
-- PostgreSQL: ~ (case-sensitive), ~* (case-insensitive).  MySQL: REGEXP.
-- The trailing .* makes the pattern also work where ~ must match the WHOLE string (DuckDB).
SELECT name
FROM products
WHERE name ~ '^[CDP].*'
ORDER BY name;
