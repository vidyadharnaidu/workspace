-- =====================================================================
-- 02 AGGREGATION: GROUP BY, HAVING, COUNT variants, conditional aggregation, ROLLUP
--
-- REMEMBER:
--   * Every SELECT column must be in GROUP BY or inside an aggregate.
--   * WHERE filters ROWS before grouping; HAVING filters GROUPS after grouping.
--   * COUNT(*) counts rows, COUNT(col) skips NULLs, COUNT(DISTINCT col) counts unique non-NULLs.
--   * SUM/AVG/MIN/MAX ignore NULLs.
-- =====================================================================


-- Q1. Number of employees, average / min / max salary per department
SELECT dept_id,
       COUNT(*)              AS employees,
       ROUND(AVG(salary), 2) AS avg_salary,
       MIN(salary)           AS min_salary,
       MAX(salary)           AS max_salary
FROM employees
GROUP BY dept_id
ORDER BY dept_id;


-- Q2. Departments with more than 2 employees (HAVING)
SELECT dept_id, COUNT(*) AS employees
FROM employees
GROUP BY dept_id
HAVING COUNT(*) > 2
ORDER BY dept_id;


-- Q3. WHERE vs HAVING together: among employees hired after 2017, departments whose avg salary > 100k
SELECT dept_id, ROUND(AVG(salary), 2) AS avg_salary
FROM employees
WHERE hire_date > DATE '2017-01-01'   -- row filter first
GROUP BY dept_id
HAVING AVG(salary) > 100000           -- group filter after
ORDER BY dept_id;


-- Q4. COUNT(*) vs COUNT(column) vs COUNT(DISTINCT column)
SELECT COUNT(*)                AS all_rows,          -- 12
       COUNT(email)            AS with_email,        -- 11 (Oscar is NULL)
       COUNT(dept_id)          AS with_department,   -- 11
       COUNT(DISTINCT dept_id) AS distinct_depts     -- 4
FROM employees;


-- Q5. Revenue per product category (completed orders only)
SELECT p.category,
       SUM(oi.quantity * oi.unit_price) AS revenue
FROM order_items oi
JOIN orders o   ON o.order_id = oi.order_id
JOIN products p ON p.product_id = oi.product_id
WHERE o.status = 'completed'
GROUP BY p.category
ORDER BY revenue DESC;


-- Q6. CONDITIONAL AGGREGATION: completed / cancelled / pending orders per customer in ONE row
-- TRICK: SUM(CASE WHEN ... THEN 1 ELSE 0 END)  works everywhere.
--        COUNT(*) FILTER (WHERE ...)          is shorter (PostgreSQL, not MySQL).
SELECT customer_id,
       COUNT(*)                                                AS total,
       SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END)   AS completed,
       COUNT(*) FILTER (WHERE status = 'cancelled')            AS cancelled,
       COUNT(*) FILTER (WHERE status = 'pending')              AS pending
FROM orders
GROUP BY customer_id
ORDER BY customer_id;


-- Q7. Each department's share of total payroll (%)
-- TRICK: SUM(SUM(x)) OVER () = grand total computed over the grouped rows
SELECT dept_id,
       SUM(salary)                                           AS payroll,
       ROUND(100.0 * SUM(salary) / SUM(SUM(salary)) OVER (), 2) AS pct_of_total
FROM employees
GROUP BY dept_id
ORDER BY payroll DESC;


-- Q8. Median salary per department
-- PostgreSQL: PERCENTILE_CONT (MySQL has no median function -> use window functions, see 05_window_functions)
SELECT dept_id,
       CAST(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY salary) AS NUMERIC(12, 2)) AS median_salary
FROM employees
WHERE dept_id IS NOT NULL
GROUP BY dept_id
ORDER BY dept_id;


-- Q9. Subtotals + grand total with ROLLUP (category subtotal, then overall total)
-- GROUPING(col) = 1 when that row is a subtotal for col
SELECT CASE WHEN GROUPING(p.category) = 1 THEN 'ALL CATEGORIES' ELSE p.category END AS category,
       CASE WHEN GROUPING(p.name) = 1 THEN 'SUBTOTAL' ELSE p.name END             AS product,
       SUM(oi.quantity * oi.unit_price)                                           AS revenue
FROM order_items oi
JOIN products p ON p.product_id = oi.product_id
GROUP BY ROLLUP (p.category, p.name)
ORDER BY GROUPING(p.category), p.category, GROUPING(p.name), p.name;


-- Q10. Count of employees hired per year
SELECT EXTRACT(YEAR FROM hire_date) AS hire_year, COUNT(*) AS hires
FROM employees
GROUP BY EXTRACT(YEAR FROM hire_date)
ORDER BY hire_year;


-- Q11. Gender count per department in one row
SELECT dept_id,
       COUNT(*) FILTER (WHERE gender = 'F') AS female,
       COUNT(*) FILTER (WHERE gender = 'M') AS male
FROM employees
GROUP BY dept_id
ORDER BY dept_id;
