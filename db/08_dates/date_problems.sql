-- =====================================================================
-- 08 DATES
--
-- REMEMBER (PostgreSQL -> MySQL):
--   DATE_TRUNC('month', d)        -> DATE_FORMAT(d, '%Y-%m-01')
--   EXTRACT(YEAR FROM d)          -> YEAR(d)  (EXTRACT also works)
--   d2 - d1  (integer days)       -> DATEDIFF(d2, d1)
--   d + INTERVAL '7 days'         -> d + INTERVAL 7 DAY  /  DATE_ADD(d, INTERVAL 7 DAY)
--   CURRENT_DATE                  -> CURDATE()
--   AGE(d2, d1)                   -> TIMESTAMPDIFF(YEAR, d1, d2)
--
-- The queries use a FIXED "today" (2025-06-10) so results don't change. In real life use CURRENT_DATE.
-- =====================================================================


-- Q1. Number of orders and revenue per month
SELECT CAST(DATE_TRUNC('month', o.order_date) AS DATE) AS month,
       COUNT(DISTINCT o.order_id)                   AS orders,
       SUM(oi.quantity * oi.unit_price)             AS revenue
FROM orders o
JOIN order_items oi ON oi.order_id = o.order_id
GROUP BY CAST(DATE_TRUNC('month', o.order_date) AS DATE)
ORDER BY month;


-- Q2. Orders placed in the last 30 days (relative to "today")
SELECT order_id, order_date
FROM orders
WHERE order_date >  DATE '2025-06-10' - INTERVAL '30 days'
  AND order_date <= DATE '2025-06-10'
ORDER BY order_date;


-- Q3. Revenue by year and quarter
SELECT EXTRACT(YEAR FROM o.order_date)    AS yr,
       EXTRACT(QUARTER FROM o.order_date) AS qtr,
       SUM(oi.quantity * oi.unit_price)   AS revenue
FROM orders o
JOIN order_items oi ON oi.order_id = o.order_id
WHERE o.status = 'completed'
GROUP BY EXTRACT(YEAR FROM o.order_date), EXTRACT(QUARTER FROM o.order_date)
ORDER BY yr, qtr;


-- Q4. Employee tenure in full years as of 2026-01-01
SELECT name, hire_date,
       EXTRACT(YEAR FROM AGE(DATE '2026-01-01', hire_date)) AS years_of_service
FROM employees
ORDER BY hire_date;


-- Q5. Employees who have a work anniversary in the same month as "today" (June)
SELECT name, hire_date
FROM employees
WHERE EXTRACT(MONTH FROM hire_date) = 6;


-- Q6. Days from signup to first order per customer (NULL if never ordered)
SELECT c.name, c.signup_date,
       MIN(o.order_date)                 AS first_order,
       MIN(o.order_date) - c.signup_date AS days_to_first_order
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.customer_id
GROUP BY c.name, c.signup_date
ORDER BY c.signup_date;


-- Q7. Orders by day of week (0 = Sunday in PostgreSQL; MySQL DAYOFWEEK: 1 = Sunday)
SELECT EXTRACT(DOW FROM order_date) AS day_of_week, COUNT(*) AS orders
FROM orders
GROUP BY EXTRACT(DOW FROM order_date)
ORDER BY day_of_week;


-- Q8. Customers who have NOT ordered in the last 45 days (churn risk) but ordered before
SELECT c.name, MAX(o.order_date) AS last_order
FROM customers c
JOIN orders o ON o.customer_id = c.customer_id
GROUP BY c.name
HAVING MAX(o.order_date) < DATE '2025-06-10' - INTERVAL '45 days'
ORDER BY last_order;


-- Q9. First 21 days of June vs the same period of May (period-over-period in ONE scan)
SELECT SUM(CASE WHEN o.order_date BETWEEN DATE '2025-06-01' AND DATE '2025-06-21'
                THEN oi.quantity * oi.unit_price ELSE 0 END) AS june_1_to_21,
       SUM(CASE WHEN o.order_date BETWEEN DATE '2025-05-01' AND DATE '2025-05-21'
                THEN oi.quantity * oi.unit_price ELSE 0 END) AS may_1_to_21
FROM orders o
JOIN order_items oi ON oi.order_id = o.order_id
WHERE o.status = 'completed';


-- Q10. Hires grouped by year with the list of names
SELECT EXTRACT(YEAR FROM hire_date) AS hire_year,
       STRING_AGG(name, ', ' ORDER BY hire_date) AS hired
FROM employees
GROUP BY EXTRACT(YEAR FROM hire_date)
ORDER BY hire_year;


-- Q11. SARGABLE date filter (index-friendly): use a range, not a function on the column
-- SLOW (can't use an index on order_date):  WHERE EXTRACT(YEAR FROM order_date) = 2025
-- FAST:
SELECT COUNT(*) AS orders_2025
FROM orders
WHERE order_date >= DATE '2025-01-01'
  AND order_date <  DATE '2026-01-01';
