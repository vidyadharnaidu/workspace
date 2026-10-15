-- =====================================================================
-- 03 JOINS
--
-- REMEMBER:
--   INNER JOIN  -> only matching rows
--   LEFT JOIN   -> all rows from left, NULLs where no match
--   FULL JOIN   -> all rows from both sides (MySQL: LEFT JOIN UNION RIGHT JOIN)
--   CROSS JOIN  -> every combination (rows A x rows B)
--   SELF JOIN   -> same table twice with different aliases (employee / manager)
--   ANTI JOIN   -> "rows in A with NO match in B": LEFT JOIN ... WHERE b.key IS NULL  or  NOT EXISTS
--   TRAP: a filter on the RIGHT table in WHERE turns a LEFT JOIN into an INNER JOIN -> put it in ON.
-- =====================================================================


-- Q1. Employee name with department name (INNER JOIN drops Oscar, who has no department)
SELECT e.name, d.dept_name
FROM employees e
JOIN departments d ON d.dept_id = e.dept_id
ORDER BY d.dept_name, e.name;


-- Q2. Every department with its employee count, INCLUDING empty ones (Legal = 0)
-- TRICK: COUNT(e.emp_id), not COUNT(*) - COUNT(*) would count the NULL row as 1
SELECT d.dept_name, COUNT(e.emp_id) AS employees
FROM departments d
LEFT JOIN employees e ON e.dept_id = d.dept_id
GROUP BY d.dept_name
ORDER BY employees DESC, d.dept_name;


-- Q3. Departments with no employees (ANTI JOIN, two ways)
SELECT d.dept_name
FROM departments d
LEFT JOIN employees e ON e.dept_id = d.dept_id
WHERE e.emp_id IS NULL;

SELECT d.dept_name
FROM departments d
WHERE NOT EXISTS (SELECT 1 FROM employees e WHERE e.dept_id = d.dept_id);


-- Q4. Customers who never placed an order
SELECT c.name
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.customer_id
WHERE o.order_id IS NULL;


-- Q5. Products that were never sold
SELECT p.name
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.product_id = p.product_id);


-- Q6. SELF JOIN: each employee with their manager's name (LEFT so the CEO still appears)
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON m.emp_id = e.manager_id
ORDER BY e.emp_id;


-- Q7. Employees who earn MORE than their manager (classic!)
SELECT e.name AS employee, e.salary, m.name AS manager, m.salary AS manager_salary
FROM employees e
JOIN employees m ON m.emp_id = e.manager_id
WHERE e.salary > m.salary;


-- Q8. Managers with the number of direct reports
SELECT m.name AS manager, COUNT(*) AS direct_reports
FROM employees e
JOIN employees m ON m.emp_id = e.manager_id
GROUP BY m.name
ORDER BY direct_reports DESC, manager;


-- Q9. FULL OUTER JOIN: employees without a department AND departments without employees
SELECT e.name AS employee, d.dept_name
FROM employees e
FULL OUTER JOIN departments d ON d.dept_id = e.dept_id
WHERE e.emp_id IS NULL OR d.dept_id IS NULL;


-- Q10. Order totals with customer name (join 3 tables)
SELECT o.order_id, c.name AS customer, o.status,
       SUM(oi.quantity * oi.unit_price) AS order_total
FROM orders o
JOIN customers c    ON c.customer_id = o.customer_id
JOIN order_items oi ON oi.order_id = o.order_id
GROUP BY o.order_id, c.name, o.status
ORDER BY o.order_id;


-- Q11. CROSS JOIN: report grid of every customer x every category with revenue (0 when nothing bought)
-- TRICK: build the full grid first (CROSS JOIN), then LEFT JOIN the pre-aggregated facts onto it
WITH sales AS (
    SELECT o.customer_id, p.category, SUM(oi.quantity * oi.unit_price) AS revenue
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    JOIN products p     ON p.product_id = oi.product_id
    WHERE o.status = 'completed'
    GROUP BY o.customer_id, p.category
)
SELECT c.name AS customer, cat.category, COALESCE(s.revenue, 0) AS revenue
FROM customers c
CROSS JOIN (SELECT DISTINCT category FROM products) cat
LEFT JOIN sales s ON s.customer_id = c.customer_id AND s.category = cat.category
WHERE c.name IN ('Acme', 'Hooli')
ORDER BY c.name, cat.category;


-- Q12. THE LEFT JOIN TRAP: all customers with their COMPLETED order count
-- WRONG: filter in WHERE removes customers with no completed orders (Hooli disappears)
SELECT c.name, COUNT(o.order_id) AS completed_orders
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.customer_id
WHERE o.status = 'completed'
GROUP BY c.name
ORDER BY c.name;

-- RIGHT: filter inside ON keeps every customer
SELECT c.name, COUNT(o.order_id) AS completed_orders
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.customer_id AND o.status = 'completed'
GROUP BY c.name
ORDER BY c.name;


-- Q13. NON-EQUI JOIN: map salary to a grade using ranges
SELECT e.name, e.salary, g.grade
FROM employees e
JOIN (VALUES ('C', 0, 89999), ('B', 90000, 149999), ('A', 150000, 999999)) AS g (grade, low, high)
  ON e.salary BETWEEN g.low AND g.high
ORDER BY e.salary DESC;


-- Q14. Colleagues: pairs of employees in the same department (each pair once)
-- TRICK: a.emp_id < b.emp_id removes (x, x) and duplicate (y, x)
SELECT a.name AS emp1, b.name AS emp2, a.dept_id
FROM employees a
JOIN employees b ON a.dept_id = b.dept_id AND a.emp_id < b.emp_id
WHERE a.dept_id = 2;
