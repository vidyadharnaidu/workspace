-- =====================================================================
-- 04 SUBQUERIES: scalar, correlated, IN / EXISTS, derived tables, ANY / ALL
--
-- REMEMBER:
--   * Scalar subquery returns ONE value -> can be used like a column or constant.
--   * Correlated subquery references the OUTER row -> re-evaluated per row.
--   * EXISTS stops at the first match; good for "has any".
--   * TRAP: NOT IN (subquery) returns NOTHING if the subquery contains a NULL -> use NOT EXISTS.
--   * Subquery in FROM (derived table) needs an alias.
-- =====================================================================


-- Q1. Employees earning above the company average (scalar subquery)
SELECT name, salary
FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees)
ORDER BY salary DESC;


-- Q2. Employees earning above THEIR OWN department's average (correlated subquery)
SELECT e.name, e.dept_id, e.salary
FROM employees e
WHERE e.salary > (SELECT AVG(x.salary) FROM employees x WHERE x.dept_id = e.dept_id)
ORDER BY e.dept_id, e.salary DESC;


-- Q3. Customers who placed at least one completed order (IN vs EXISTS)
SELECT name
FROM customers
WHERE customer_id IN (SELECT customer_id FROM orders WHERE status = 'completed')
ORDER BY name;

SELECT c.name
FROM customers c
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id AND o.status = 'completed')
ORDER BY c.name;


-- Q4. THE NOT IN + NULL TRAP: employees who are NOT anyone's manager
-- WRONG: manager_id contains NULL (the CEO) -> "x NOT IN (..., NULL)" is never TRUE -> 0 rows!
SELECT name
FROM employees
WHERE emp_id NOT IN (SELECT manager_id FROM employees);

-- RIGHT (1): NOT EXISTS is NULL-safe
SELECT e.name
FROM employees e
WHERE NOT EXISTS (SELECT 1 FROM employees r WHERE r.manager_id = e.emp_id)
ORDER BY e.name;

-- RIGHT (2): filter the NULLs out of the subquery
SELECT name
FROM employees
WHERE emp_id NOT IN (SELECT manager_id FROM employees WHERE manager_id IS NOT NULL)
ORDER BY name;


-- Q5. Department(s) with the highest average salary (handles ties, unlike ORDER BY ... LIMIT 1)
SELECT dept_id, ROUND(AVG(salary), 2) AS avg_salary
FROM employees
GROUP BY dept_id
HAVING AVG(salary) = (SELECT MAX(avg_sal)
                      FROM (SELECT AVG(salary) AS avg_sal FROM employees GROUP BY dept_id) t);


-- Q6. Derived table: average of per-department payrolls
SELECT ROUND(AVG(payroll), 2) AS avg_department_payroll
FROM (SELECT dept_id, SUM(salary) AS payroll
      FROM employees
      WHERE dept_id IS NOT NULL
      GROUP BY dept_id) AS dept_totals;


-- Q7. ALL / ANY: non-Sales employees earning more than EVERY Sales employee / non-HR employees earning less than ANY HR employee
SELECT name, salary
FROM employees
WHERE salary > ALL (SELECT salary FROM employees WHERE dept_id = 2);

SELECT name, salary
FROM employees
WHERE dept_id IS DISTINCT FROM 3        -- "dept_id <> 3" would silently drop Oscar (NULL dept)
  AND salary < ANY (SELECT salary FROM employees WHERE dept_id = 3);


-- Q8. Customer(s) with the highest total spend (completed orders)
SELECT c.name, t.total
FROM (SELECT o.customer_id, SUM(oi.quantity * oi.unit_price) AS total
      FROM orders o
      JOIN order_items oi ON oi.order_id = o.order_id
      WHERE o.status = 'completed'
      GROUP BY o.customer_id) t
JOIN customers c ON c.customer_id = t.customer_id
WHERE t.total = (SELECT MAX(total2)
                 FROM (SELECT SUM(oi.quantity * oi.unit_price) AS total2
                       FROM orders o
                       JOIN order_items oi ON oi.order_id = o.order_id
                       WHERE o.status = 'completed'
                       GROUP BY o.customer_id) t2);


-- Q9. Scalar subquery in SELECT: each employee's salary vs department max
SELECT e.name,
       e.salary,
       (SELECT MAX(x.salary) FROM employees x WHERE x.dept_id = e.dept_id) AS dept_max
FROM employees e
WHERE e.dept_id = 1
ORDER BY e.salary DESC;


-- Q10. Products more expensive than the average price of their category
SELECT p.name, p.category, p.price
FROM products p
WHERE p.price > (SELECT AVG(x.price) FROM products x WHERE x.category = p.category)
ORDER BY p.category, p.price DESC;
