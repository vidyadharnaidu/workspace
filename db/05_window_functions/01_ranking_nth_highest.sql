-- =====================================================================
-- 05a WINDOW FUNCTIONS: RANKING, NTH HIGHEST, TOP N PER GROUP
--
-- REMEMBER:  func() OVER (PARTITION BY group ORDER BY sort)
--   ROW_NUMBER  1,2,3,4  always unique (ties broken arbitrarily)
--   RANK        1,2,2,4  ties share, then GAP
--   DENSE_RANK  1,2,2,3  ties share, NO gap   <- use for "Nth highest salary"
--   Window functions run AFTER WHERE/GROUP BY, so you can't filter on them directly:
--   wrap in a subquery / CTE, then WHERE rnk <= N.
-- =====================================================================


-- Q1. ROW_NUMBER vs RANK vs DENSE_RANK side by side (see the Bob/Carol tie at 120k)
SELECT name, salary,
       ROW_NUMBER() OVER (ORDER BY salary DESC) AS row_num,
       RANK()       OVER (ORDER BY salary DESC) AS rnk,
       DENSE_RANK() OVER (ORDER BY salary DESC) AS dense_rnk
FROM employees
ORDER BY salary DESC, name;


-- Q2. SECOND HIGHEST SALARY - 4 ways (answer: 180000)

-- (a) MAX below MAX - simplest, only works for 2nd
SELECT MAX(salary) AS second_highest
FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);

-- (b) DISTINCT + ORDER BY + OFFSET. Wrapped in a scalar subquery so it returns NULL (not 0 rows) if missing
SELECT (SELECT DISTINCT salary FROM employees ORDER BY salary DESC LIMIT 1 OFFSET 1) AS second_highest;

-- (c) DENSE_RANK - works for any N and handles ties
SELECT DISTINCT salary AS second_highest
FROM (SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk FROM employees) t
WHERE rnk = 2;

-- (d) Correlated COUNT: N-1 distinct salaries are higher (works on old databases without window functions)
SELECT DISTINCT e.salary AS second_highest
FROM employees e
WHERE 1 = (SELECT COUNT(DISTINCT x.salary) FROM employees x WHERE x.salary > e.salary);


-- Q3. Nth highest salary (N = 3 -> 160000) and WHO earns it
SELECT name, salary
FROM (SELECT name, salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk FROM employees) t
WHERE rnk = 3;


-- Q4. HIGHEST PAID EMPLOYEE(S) IN EACH DEPARTMENT (keeps ties)
SELECT dept_id, name, salary
FROM (SELECT dept_id, name, salary,
             RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rnk
      FROM employees
      WHERE dept_id IS NOT NULL) t
WHERE rnk = 1
ORDER BY dept_id;

-- same without window functions: join to per-department max
SELECT e.dept_id, e.name, e.salary
FROM employees e
JOIN (SELECT dept_id, MAX(salary) AS max_salary FROM employees GROUP BY dept_id) m
  ON m.dept_id = e.dept_id AND m.max_salary = e.salary
ORDER BY e.dept_id;


-- Q5. TOP 3 SALARIES PER DEPARTMENT (LeetCode 185 "Department Top Three Salaries")
-- DENSE_RANK -> "top 3 distinct salaries", so Bob and Carol both appear in Engineering
SELECT d.dept_name, t.name, t.salary, t.rnk
FROM (SELECT dept_id, name, salary,
             DENSE_RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rnk
      FROM employees) t
JOIN departments d ON d.dept_id = t.dept_id
WHERE t.rnk <= 3
ORDER BY d.dept_name, t.rnk, t.name;


-- Q6. PostgreSQL shortcut: DISTINCT ON keeps the first row per group (by ORDER BY)
SELECT DISTINCT ON (dept_id) dept_id, name, salary
FROM employees
WHERE dept_id IS NOT NULL
ORDER BY dept_id, salary DESC, name;


-- Q7. LATEST ORDER PER CUSTOMER (ROW_NUMBER = exactly one row per group)
SELECT customer_id, order_id, order_date, status
FROM (SELECT o.*, ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY order_date DESC) AS rn
      FROM orders o) t
WHERE rn = 1
ORDER BY customer_id;


-- Q8. Each customer's FIRST order and their 2nd order date
SELECT customer_id,
       MAX(CASE WHEN rn = 1 THEN order_date END) AS first_order,
       MAX(CASE WHEN rn = 2 THEN order_date END) AS second_order
FROM (SELECT customer_id, order_date,
             ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY order_date) AS rn
      FROM orders) t
GROUP BY customer_id
ORDER BY customer_id;


-- Q9. Salary quartiles (NTILE) and percentile rank
SELECT name, salary,
       NTILE(4) OVER (ORDER BY salary DESC)                          AS quartile,
       ROUND(CAST(PERCENT_RANK() OVER (ORDER BY salary) AS NUMERIC), 2) AS pct_rank
FROM employees
ORDER BY salary DESC;


-- Q10. Rank departments by total payroll (window over an aggregate)
SELECT dept_id, SUM(salary) AS payroll,
       DENSE_RANK() OVER (ORDER BY SUM(salary) DESC) AS payroll_rank
FROM employees
WHERE dept_id IS NOT NULL
GROUP BY dept_id
ORDER BY payroll_rank;


-- Q11. MEDIAN without PERCENTILE_CONT (works in MySQL 8): average the middle 1 or 2 rows
SELECT AVG(salary) AS median_salary
FROM (SELECT salary,
             ROW_NUMBER() OVER (ORDER BY salary) AS rn,
             COUNT(*) OVER ()                    AS cnt
      FROM employees) t
WHERE rn BETWEEN cnt / 2.0 AND cnt / 2.0 + 1;   -- 12 rows -> rn 6 and 7;  11 rows -> rn 6 only
