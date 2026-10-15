-- =====================================================================
-- 11 BUSINESS / ANALYTICS QUESTIONS (what data & product interviews actually ask)
--
-- REMEMBER THE RECIPE:
--   1. Say the GRAIN out loud ("one row per customer per month").
--   2. Build it in a CTE.
--   3. Aggregate / rank / compare on top of it.
--   4. Use 100.0 * a / b for percentages (integer division gives 0!).
-- =====================================================================


-- Q1. Top 3 customers by revenue (completed orders)
SELECT c.name, SUM(oi.quantity * oi.unit_price) AS revenue
FROM orders o
JOIN order_items oi ON oi.order_id = o.order_id
JOIN customers c    ON c.customer_id = o.customer_id
WHERE o.status = 'completed'
GROUP BY c.name
ORDER BY revenue DESC
LIMIT 3;


-- Q2. Best-selling product (by quantity) in each category
WITH product_sales AS (
    SELECT p.category, p.name, SUM(oi.quantity) AS units
    FROM order_items oi
    JOIN orders o   ON o.order_id = oi.order_id
    JOIN products p ON p.product_id = oi.product_id
    WHERE o.status = 'completed'
    GROUP BY p.category, p.name
)
SELECT category, name, units
FROM (SELECT ps.*, RANK() OVER (PARTITION BY category ORDER BY units DESC) AS rnk FROM product_sales ps) t
WHERE rnk = 1
ORDER BY category;


-- Q3. Average order value (AOV), cancellation rate and total revenue in one row
WITH order_totals AS (
    SELECT o.order_id, o.status, SUM(oi.quantity * oi.unit_price) AS total
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    GROUP BY o.order_id, o.status
)
SELECT ROUND(AVG(total) FILTER (WHERE status = 'completed'), 2)                        AS avg_order_value,
       ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'cancelled') / COUNT(*), 2)      AS cancellation_rate_pct,
       SUM(total) FILTER (WHERE status = 'completed')                                  AS revenue
FROM order_totals;


-- Q4. Repeat customers (more than one completed order) and repeat rate
WITH per_customer AS (
    SELECT customer_id, COUNT(*) AS completed_orders
    FROM orders
    WHERE status = 'completed'
    GROUP BY customer_id
)
SELECT COUNT(*)                                                         AS buying_customers,
       COUNT(*) FILTER (WHERE completed_orders > 1)                     AS repeat_customers,
       ROUND(100.0 * COUNT(*) FILTER (WHERE completed_orders > 1) / COUNT(*), 2) AS repeat_rate_pct
FROM per_customer;


-- Q5. Customers who bought BOTH a Laptop and a Phone
SELECT c.name
FROM orders o
JOIN order_items oi ON oi.order_id = o.order_id
JOIN products p     ON p.product_id = oi.product_id
JOIN customers c    ON c.customer_id = o.customer_id
WHERE p.name IN ('Laptop', 'Phone')
GROUP BY c.name
HAVING COUNT(DISTINCT p.name) = 2;


-- Q6. Customers who bought Headphones but NEVER a Laptop
SELECT DISTINCT c.name
FROM customers c
JOIN orders o       ON o.customer_id = c.customer_id
JOIN order_items oi ON oi.order_id = o.order_id
JOIN products p     ON p.product_id = oi.product_id AND p.name = 'Headphones'
WHERE NOT EXISTS (SELECT 1
                  FROM orders o2
                  JOIN order_items oi2 ON oi2.order_id = o2.order_id
                  JOIN products p2     ON p2.product_id = oi2.product_id
                  WHERE o2.customer_id = c.customer_id AND p2.name = 'Laptop');


-- Q7. Products frequently bought TOGETHER (pairs in the same order)
SELECT p1.name AS product_a, p2.name AS product_b, COUNT(*) AS times_together
FROM order_items a
JOIN order_items b ON a.order_id = b.order_id AND a.product_id < b.product_id
JOIN products p1   ON p1.product_id = a.product_id
JOIN products p2   ON p2.product_id = b.product_id
GROUP BY p1.name, p2.name
ORDER BY times_together DESC, product_a, product_b;


-- Q8. Monthly active customers and NEW customers per month (first order month)
WITH first_order AS (
    SELECT customer_id, CAST(DATE_TRUNC('month', MIN(order_date)) AS DATE) AS first_month
    FROM orders
    GROUP BY customer_id
)
SELECT CAST(DATE_TRUNC('month', o.order_date) AS DATE)                AS month,
       COUNT(DISTINCT o.customer_id)                             AS active_customers,
       COUNT(DISTINCT o.customer_id) FILTER (
           WHERE f.first_month = CAST(DATE_TRUNC('month', o.order_date) AS DATE)) AS new_customers
FROM orders o
JOIN first_order f ON f.customer_id = o.customer_id
GROUP BY CAST(DATE_TRUNC('month', o.order_date) AS DATE)
ORDER BY month;


-- Q9. RETENTION: of customers active in a month, how many ordered again the NEXT month?
WITH active AS (
    SELECT DISTINCT customer_id, CAST(DATE_TRUNC('month', order_date) AS DATE) AS month
    FROM orders
)
SELECT a.month,
       COUNT(*)                   AS active_customers,
       COUNT(n.customer_id)       AS retained_next_month,
       ROUND(100.0 * COUNT(n.customer_id) / COUNT(*), 2) AS retention_pct
FROM active a
LEFT JOIN active n
       ON n.customer_id = a.customer_id
      AND n.month = CAST(a.month + INTERVAL '1 month' AS DATE)
GROUP BY a.month
ORDER BY a.month;


-- Q10. Each category's share of revenue and rank
SELECT p.category,
       SUM(oi.quantity * oi.unit_price) AS revenue,
       ROUND(100.0 * SUM(oi.quantity * oi.unit_price) / SUM(SUM(oi.quantity * oi.unit_price)) OVER (), 2) AS share_pct,
       RANK() OVER (ORDER BY SUM(oi.quantity * oi.unit_price) DESC) AS rnk
FROM order_items oi
JOIN orders o   ON o.order_id = oi.order_id
JOIN products p ON p.product_id = oi.product_id
WHERE o.status = 'completed'
GROUP BY p.category
ORDER BY rnk;


-- Q11. Daily active users (DAU) and average DAU
WITH dau AS (
    SELECT login_date, COUNT(DISTINCT user_id) AS users
    FROM logins
    GROUP BY login_date
)
SELECT login_date, users, ROUND(AVG(users) OVER (), 2) AS avg_dau
FROM dau
ORDER BY login_date;


-- Q12. Customers whose EVERY order was completed (no cancelled/pending ever)
SELECT c.name
FROM customers c
JOIN orders o ON o.customer_id = c.customer_id
GROUP BY c.name
HAVING COUNT(*) = COUNT(*) FILTER (WHERE o.status = 'completed');
-- portable: HAVING SUM(CASE WHEN o.status <> 'completed' THEN 1 ELSE 0 END) = 0


-- Q13. Price paid vs list price: discounted order lines and discount %
SELECT oi.order_id, p.name, p.price AS list_price, oi.unit_price,
       ROUND(100.0 * (p.price - oi.unit_price) / p.price, 2) AS discount_pct
FROM order_items oi
JOIN products p ON p.product_id = oi.product_id
WHERE oi.unit_price < p.price
ORDER BY discount_pct DESC;


-- Q14. City with the most revenue per customer
SELECT c.city,
       COUNT(DISTINCT c.customer_id)                               AS customers,
       COALESCE(SUM(oi.quantity * oi.unit_price), 0)               AS revenue,
       ROUND(COALESCE(SUM(oi.quantity * oi.unit_price), 0) / COUNT(DISTINCT c.customer_id), 2) AS revenue_per_customer
FROM customers c
LEFT JOIN orders o       ON o.customer_id = c.customer_id AND o.status = 'completed'
LEFT JOIN order_items oi ON oi.order_id = o.order_id
GROUP BY c.city
ORDER BY revenue_per_customer DESC;
