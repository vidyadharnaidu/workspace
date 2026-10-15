-- =====================================================================
-- 01 BASICS: SELECT, WHERE, ORDER BY, LIMIT, DISTINCT, CASE, NULL
--
-- REMEMBER - LOGICAL ORDER OF EXECUTION (not the order you write it):
--   FROM/JOIN -> WHERE -> GROUP BY -> HAVING -> SELECT -> DISTINCT -> ORDER BY -> LIMIT
--   => a SELECT alias can NOT be used in WHERE (WHERE runs first), but CAN be used in ORDER BY.
-- =====================================================================


-- Q1. Engineering employees hired after 2018-01-01, newest first
SELECT name, salary, hire_date
FROM employees
WHERE dept_id = 1
  AND hire_date > DATE '2018-01-01'
ORDER BY hire_date DESC;


-- Q2. Employees with salary between 90k and 150k (BETWEEN is INCLUSIVE on both ends)
SELECT name, salary
FROM employees
WHERE salary BETWEEN 90000 AND 150000
ORDER BY salary DESC, name;


-- Q3. Employees in Sales or HR (IN list)
SELECT name, dept_id
FROM employees
WHERE dept_id IN (2, 3)
ORDER BY dept_id, name;


-- Q4. Names starting with 'A' or containing 'an' (case-insensitive)
-- TRICK: % = any number of chars, _ = exactly one char. ILIKE is PostgreSQL (MySQL LIKE is already case-insensitive)
SELECT name
FROM employees
WHERE name LIKE 'A%'
   OR LOWER(name) LIKE '%an%'
ORDER BY name;


-- Q5. Employees with NO department
-- TRAP: "dept_id = NULL" is never true. NULL compared with anything is UNKNOWN. Use IS NULL.
SELECT name, dept_id
FROM employees
WHERE dept_id IS NULL;


-- Q6. Top 3 highest paid employees
SELECT name, salary
FROM employees
ORDER BY salary DESC
LIMIT 3;
-- SQL Server: SELECT TOP 3 ...    Oracle / standard: FETCH FIRST 3 ROWS ONLY


-- Q7. Pagination: page 2 with 5 rows per page -> OFFSET (page - 1) * size
SELECT emp_id, name
FROM employees
ORDER BY emp_id
LIMIT 5 OFFSET 5;


-- Q8. Distinct department locations
SELECT DISTINCT location
FROM departments
ORDER BY location;


-- Q9. Salary band with CASE (first matching WHEN wins)
SELECT name,
       salary,
       CASE
           WHEN salary >= 150000 THEN 'High'
           WHEN salary >= 90000  THEN 'Medium'
           ELSE 'Low'
       END AS salary_band
FROM employees
ORDER BY salary DESC;


-- Q10. Replace NULL email with a default (COALESCE returns the first non-NULL argument)
SELECT name, COALESCE(email, 'no-email') AS email
FROM employees
WHERE emp_id >= 11;


-- Q11. Why does "WHERE annual > 1500000" fail? Alias doesn't exist yet in WHERE.
-- FIX: repeat the expression (or wrap in a subquery / CTE). ORDER BY can use the alias.
SELECT name, salary * 12 AS annual
FROM employees
WHERE salary * 12 > 1500000
ORDER BY annual DESC;


-- Q12. Sort with NULLs last (PostgreSQL puts NULLs LAST for ASC by default, FIRST for DESC)
SELECT name, dept_id
FROM employees
ORDER BY dept_id DESC NULLS LAST, name
LIMIT 4;
