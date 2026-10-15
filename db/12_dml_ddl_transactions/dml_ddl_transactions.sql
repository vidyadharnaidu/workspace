-- =====================================================================
-- 12 DML, DDL, CONSTRAINTS, UPSERT, TRANSACTIONS
--
-- REMEMBER:
--   DDL = CREATE / ALTER / DROP / TRUNCATE (structure)     DML = INSERT / UPDATE / DELETE / MERGE (data)
--   DELETE   -> removes chosen rows, WHERE allowed, fires triggers, can roll back
--   TRUNCATE -> removes ALL rows fast, no WHERE, resets storage (rollback-able in PostgreSQL, NOT in MySQL)
--   DROP     -> removes the table itself
--   Always test UPDATE/DELETE as a SELECT with the same WHERE first!
--
-- Everything here runs inside BEGIN ... ROLLBACK so the sample data is unchanged afterwards.
-- =====================================================================


-- Q1. CREATE TABLE with every common constraint
BEGIN;

CREATE TABLE accounts (
    account_id  INTEGER PRIMARY KEY,                       -- unique + not null
    owner       VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) UNIQUE,                       -- NULLs allowed, duplicates not
    balance     NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    dept_id     INTEGER REFERENCES departments (dept_id),  -- foreign key
    created_on  DATE NOT NULL DEFAULT CURRENT_DATE
);

-- Q2. INSERT multiple rows / INSERT ... SELECT
INSERT INTO accounts (account_id, owner, email, balance) VALUES
    (1, 'Alice', 'alice@bank.com', 500.00),
    (2, 'Bob',   'bob@bank.com',   100.00);

INSERT INTO accounts (account_id, owner, email, balance, dept_id)
SELECT emp_id + 100, name, email, 0, dept_id
FROM employees
WHERE dept_id = 3;

SELECT account_id, owner, balance, dept_id FROM accounts ORDER BY account_id;


-- Q3. UPSERT: insert, or update if the key already exists
-- MySQL: INSERT ... ON DUPLICATE KEY UPDATE balance = VALUES(balance)
INSERT INTO accounts (account_id, owner, email, balance) VALUES (1, 'Alice', 'alice@bank.com', 999.00)
ON CONFLICT (account_id) DO UPDATE SET balance = excluded.balance;

SELECT account_id, owner, balance FROM accounts WHERE account_id = 1;


-- Q4. TRANSACTION: transfer 50 from Alice to Bob - both updates succeed or neither does
--     (in an app: BEGIN; UPDATE ...; UPDATE ...; COMMIT;  and ROLLBACK on any error)
UPDATE accounts SET balance = balance - 50 WHERE account_id = 1;
UPDATE accounts SET balance = balance + 50 WHERE account_id = 2;

SELECT account_id, owner, balance FROM accounts WHERE account_id IN (1, 2) ORDER BY account_id;

ROLLBACK;   -- the accounts table and all its rows disappear again


-- Q5. UPDATE with CASE: raise salaries by department (10% Engineering, 5% Sales, 2% others)
BEGIN;

UPDATE employees
SET salary = salary * CASE dept_id WHEN 1 THEN 1.10 WHEN 2 THEN 1.05 ELSE 1.02 END;

SELECT name, dept_id, salary FROM employees WHERE emp_id IN (2, 6, 9) ORDER BY emp_id;

ROLLBACK;


-- Q6. UPDATE using another table (PostgreSQL UPDATE ... FROM)
-- MySQL: UPDATE employees e JOIN departments d ON d.dept_id = e.dept_id SET e.salary = ... WHERE d.location = 'New York';
BEGIN;

UPDATE employees
SET salary = salary + 1000
FROM departments d
WHERE d.dept_id = employees.dept_id
  AND d.location = 'New York';

SELECT e.name, d.location, e.salary
FROM employees e
JOIN departments d ON d.dept_id = e.dept_id
WHERE d.location = 'New York'
ORDER BY e.emp_id;

ROLLBACK;


-- Q7. SWAP values in one statement (LeetCode 627: swap 'M' and 'F')
BEGIN;

UPDATE employees
SET gender = CASE gender WHEN 'M' THEN 'F' WHEN 'F' THEN 'M' ELSE gender END;

SELECT name, gender FROM employees WHERE emp_id <= 3 ORDER BY emp_id;

ROLLBACK;


-- Q8. DELETE with a subquery: remove cancelled orders' line items
BEGIN;

DELETE FROM order_items
WHERE order_id IN (SELECT order_id FROM orders WHERE status = 'cancelled');

SELECT COUNT(*) AS remaining_order_items FROM order_items;

ROLLBACK;


-- Q9. ALTER TABLE: add a column with a default, then a computed update
BEGIN;

ALTER TABLE customers ADD COLUMN tier VARCHAR(10) DEFAULT 'basic';

UPDATE customers
SET tier = 'gold'
WHERE customer_id IN (SELECT o.customer_id
                      FROM orders o
                      WHERE o.status = 'completed'
                      GROUP BY o.customer_id
                      HAVING COUNT(*) >= 2);

SELECT name, tier FROM customers ORDER BY customer_id;

ROLLBACK;


-- Q10. VIEW: save a query under a name (a stored SELECT, no data copied)
--      MATERIALIZED VIEW (PostgreSQL) stores the result and needs REFRESH MATERIALIZED VIEW.
BEGIN;

CREATE VIEW employee_details AS
SELECT e.emp_id, e.name, d.dept_name, m.name AS manager, e.salary
FROM employees e
LEFT JOIN departments d ON d.dept_id = e.dept_id
LEFT JOIN employees m   ON m.emp_id = e.manager_id;

SELECT * FROM employee_details WHERE dept_name = 'Sales' ORDER BY salary DESC;

ROLLBACK;


-- Q11. Copy a table's structure and data (backup before a risky change)
-- MySQL: CREATE TABLE employees_backup LIKE employees; INSERT INTO employees_backup SELECT * FROM employees;
BEGIN;

CREATE TABLE employees_backup AS SELECT * FROM employees;
SELECT COUNT(*) AS backed_up_rows FROM employees_backup;

ROLLBACK;


-- Q12. RETURNING (PostgreSQL): get the changed rows back without a second query
BEGIN;

UPDATE scores SET score = LEAST(score + 5, 100) WHERE subject = 'Math'
RETURNING student, subject, score;

ROLLBACK;
