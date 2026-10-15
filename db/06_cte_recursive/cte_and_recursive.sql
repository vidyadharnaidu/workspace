-- =====================================================================
-- 06 CTEs (WITH) AND RECURSIVE QUERIES
--
-- REMEMBER:
--   WITH name AS (query) SELECT ... FROM name        -> a named, readable subquery
--   Several CTEs: WITH a AS (...), b AS (SELECT ... FROM a) SELECT ...
--
--   RECURSIVE TEMPLATE:
--     WITH RECURSIVE r AS (
--         SELECT ...                      -- ANCHOR: starting rows
--         UNION ALL
--         SELECT ... FROM r JOIN t ...    -- STEP: uses previous level
--     )                                   -- stops when STEP returns no rows
--   Use cases: org charts, category trees, bill of materials, generating number/date series.
-- =====================================================================


-- Q1. CTE for readability: employees above their department average
WITH dept_avg AS (
    SELECT dept_id, AVG(salary) AS avg_salary
    FROM employees
    GROUP BY dept_id
)
SELECT e.name, e.dept_id, e.salary, ROUND(d.avg_salary, 2) AS dept_avg
FROM employees e
JOIN dept_avg d ON d.dept_id = e.dept_id
WHERE e.salary > d.avg_salary
ORDER BY e.dept_id, e.salary DESC;


-- Q2. Chained CTEs: customer totals -> average -> customers above average
WITH customer_totals AS (
    SELECT o.customer_id, SUM(oi.quantity * oi.unit_price) AS total
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    WHERE o.status = 'completed'
    GROUP BY o.customer_id
),
overall AS (
    SELECT AVG(total) AS avg_total FROM customer_totals
)
SELECT c.name, t.total, ROUND(ov.avg_total, 2) AS avg_total
FROM customer_totals t
CROSS JOIN overall ov
JOIN customers c ON c.customer_id = t.customer_id
WHERE t.total > ov.avg_total
ORDER BY t.total DESC;


-- Q3. Generate numbers 1..10 (simplest recursive CTE)
WITH RECURSIVE nums AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM nums WHERE n < 10
)
SELECT n FROM nums;


-- Q4. ORG CHART: level and full reporting path for every employee
WITH RECURSIVE org AS (
    SELECT emp_id, name, manager_id, 1 AS level, CAST(name AS TEXT) AS path
    FROM employees
    WHERE manager_id IS NULL                               -- anchor: the CEO
    UNION ALL
    SELECT e.emp_id, e.name, e.manager_id, o.level + 1, o.path || ' > ' || e.name
    FROM employees e
    JOIN org o ON e.manager_id = o.emp_id                  -- step: direct reports of previous level
)
SELECT level, name, path
FROM org
ORDER BY path;


-- Q5. ALL subordinates (direct and indirect) of Alice
WITH RECURSIVE subordinates AS (
    SELECT emp_id, name
    FROM employees
    WHERE manager_id = (SELECT emp_id FROM employees WHERE name = 'Alice')
    UNION ALL
    SELECT e.emp_id, e.name
    FROM employees e
    JOIN subordinates s ON e.manager_id = s.emp_id
)
SELECT name FROM subordinates ORDER BY name;


-- Q6. Management chain UP from Dan to the CEO
WITH RECURSIVE chain AS (
    SELECT emp_id, name, manager_id, 0 AS steps_up
    FROM employees
    WHERE name = 'Dan'
    UNION ALL
    SELECT m.emp_id, m.name, m.manager_id, c.steps_up + 1
    FROM employees m
    JOIN chain c ON m.emp_id = c.manager_id
)
SELECT steps_up, name FROM chain ORDER BY steps_up;


-- Q7. Headcount under each manager (total team size, not just direct reports)
WITH RECURSIVE team AS (
    SELECT manager_id AS boss, emp_id
    FROM employees
    WHERE manager_id IS NOT NULL
    UNION ALL
    SELECT e.manager_id, t.emp_id                -- climb one level: my manager's manager also "owns" this employee
    FROM team t
    JOIN employees e ON e.emp_id = t.boss
    WHERE e.manager_id IS NOT NULL
)
SELECT m.name AS manager, COUNT(*) AS team_size
FROM team t
JOIN employees m ON m.emp_id = t.boss
GROUP BY m.name
ORDER BY team_size DESC, manager;


-- Q8. FILL MISSING DATES: daily revenue for 2025-03-08 .. 2025-03-14, showing 0 on days without sales
WITH RECURSIVE calendar AS (
    SELECT DATE '2025-03-08' AS day
    UNION ALL
    SELECT CAST(day + 1 AS DATE) FROM calendar WHERE day < DATE '2025-03-14'
),
daily AS (
    SELECT o.order_date, SUM(oi.quantity * oi.unit_price) AS revenue
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    WHERE o.status = 'completed'
    GROUP BY o.order_date
)
SELECT c.day, COALESCE(d.revenue, 0) AS revenue
FROM calendar c
LEFT JOIN daily d ON d.order_date = c.day
ORDER BY c.day;
-- PostgreSQL alternative: SELECT generate_series(DATE '2025-03-08', DATE '2025-03-14', INTERVAL '1 day')


-- Q9. Fibonacci numbers with recursion (two columns carry state)
WITH RECURSIVE fib AS (
    SELECT 1 AS n, CAST(0 AS BIGINT) AS a, CAST(1 AS BIGINT) AS b
    UNION ALL
    SELECT n + 1, b, a + b FROM fib WHERE n < 10
)
SELECT n, a AS fibonacci FROM fib;
