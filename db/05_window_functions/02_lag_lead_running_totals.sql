-- =====================================================================
-- 05b WINDOW FUNCTIONS: RUNNING TOTALS, MOVING AVERAGES, LAG / LEAD, FIRST / LAST VALUE
--
-- REMEMBER:
--   SUM(x) OVER (ORDER BY d)                        -> running total
--   AVG(x) OVER (ORDER BY d ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) -> 3-row moving average
--   LAG(x)  = previous row's value,  LEAD(x) = next row's value
--   TRAP: default frame with ORDER BY ends at CURRENT ROW, so LAST_VALUE looks wrong.
--         Use ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING.
--   TRAP: ORDER BY with ties + default RANGE frame adds all tied rows at once -> use ROWS for strict running totals.
-- =====================================================================


-- Q1. RUNNING BALANCE per account (bank statement)
SELECT account_id, txn_date, amount,
       SUM(amount) OVER (PARTITION BY account_id ORDER BY txn_date, txn_id
                         ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS balance
FROM transactions
ORDER BY account_id, txn_date;


-- Q2. First date each account went NEGATIVE (filter on a window result -> subquery)
SELECT account_id, MIN(txn_date) AS first_overdrawn
FROM (SELECT account_id, txn_date,
             SUM(amount) OVER (PARTITION BY account_id ORDER BY txn_date, txn_id) AS balance
      FROM transactions) t
WHERE balance < 0
GROUP BY account_id;


-- Q3. Daily revenue and running total (completed orders)
WITH daily AS (
    SELECT o.order_date, SUM(oi.quantity * oi.unit_price) AS revenue
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    WHERE o.status = 'completed'
    GROUP BY o.order_date
)
SELECT order_date, revenue,
       SUM(revenue) OVER (ORDER BY order_date) AS running_total
FROM daily
ORDER BY order_date;


-- Q4. 3-order moving average of order value
WITH order_totals AS (
    SELECT o.order_id, o.order_date, SUM(oi.quantity * oi.unit_price) AS total
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    GROUP BY o.order_id, o.order_date
)
SELECT order_id, order_date, total,
       ROUND(AVG(total) OVER (ORDER BY order_date, order_id ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), 2) AS moving_avg_3
FROM order_totals
ORDER BY order_date, order_id;


-- Q5. MONTH-OVER-MONTH revenue growth %
WITH monthly AS (
    SELECT CAST(DATE_TRUNC('month', o.order_date) AS DATE) AS month,
           SUM(oi.quantity * oi.unit_price)              AS revenue
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    WHERE o.status = 'completed'
    GROUP BY CAST(DATE_TRUNC('month', o.order_date) AS DATE)
)
SELECT month, revenue,
       LAG(revenue) OVER (ORDER BY month) AS prev_month,
       ROUND(100.0 * (revenue - LAG(revenue) OVER (ORDER BY month))
             / LAG(revenue) OVER (ORDER BY month), 2) AS growth_pct
FROM monthly
ORDER BY month;


-- Q6. Days between each customer's consecutive orders (LAG with PARTITION)
SELECT customer_id, order_id, order_date,
       order_date - LAG(order_date) OVER (PARTITION BY customer_id ORDER BY order_date) AS days_since_prev
FROM orders
ORDER BY customer_id, order_date;


-- Q7. RISING TEMPERATURE (LeetCode 197): days warmer than the PREVIOUS CALENDAR DAY
-- TRAP: LAG compares with the previous ROW. Jan 6 follows Jan 4 (Jan 5 missing), so LAG wrongly returns it.
SELECT record_date, temperature
FROM (SELECT record_date, temperature,
             LAG(temperature) OVER (ORDER BY record_date) AS prev_temp
      FROM weather) t
WHERE temperature > prev_temp;

-- RIGHT: self join on date = yesterday
SELECT today.record_date, today.temperature
FROM weather today
JOIN weather yesterday ON today.record_date = yesterday.record_date + 1
WHERE today.temperature > yesterday.temperature
ORDER BY today.record_date;


-- Q8. FIRST_VALUE / LAST_VALUE: highest and lowest paid name in each department on every row
SELECT dept_id, name, salary,
       FIRST_VALUE(name) OVER w AS top_earner,
       LAST_VALUE(name)  OVER w AS lowest_earner
FROM employees
WHERE dept_id IS NOT NULL
WINDOW w AS (PARTITION BY dept_id ORDER BY salary DESC, name
             ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
ORDER BY dept_id, salary DESC;


-- Q9. Each salary vs department average and difference (no GROUP BY needed)
SELECT name, dept_id, salary,
       ROUND(AVG(salary) OVER (PARTITION BY dept_id), 2)          AS dept_avg,
       ROUND(salary - AVG(salary) OVER (PARTITION BY dept_id), 2) AS diff_from_avg
FROM employees
WHERE dept_id IS NOT NULL
ORDER BY dept_id, salary DESC;


-- Q10. PARETO: cumulative % of revenue by customer (who makes up the first 80%?)
WITH spend AS (
    SELECT o.customer_id, SUM(oi.quantity * oi.unit_price) AS revenue
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    WHERE o.status = 'completed'
    GROUP BY o.customer_id
)
SELECT customer_id, revenue,
       ROUND(100.0 * SUM(revenue) OVER (ORDER BY revenue DESC ROWS UNBOUNDED PRECEDING)
             / SUM(revenue) OVER (), 2) AS cumulative_pct
FROM spend
ORDER BY revenue DESC;


-- Q11. Next order date per customer (LEAD) - "did they come back?"
SELECT customer_id, order_id, order_date,
       LEAD(order_date) OVER (PARTITION BY customer_id ORDER BY order_date) AS next_order_date
FROM orders
ORDER BY customer_id, order_date;
