-- =====================================================================
-- SAMPLE DATA (small on purpose, so you can check answers by eye)
--
-- Org chart:
--   Grace (CEO, Engineering, 250k)
--   |-- Alice (Engineering, 180k)
--   |   |-- Bob (Engineering, 120k)
--   |   |   `-- Dan (Engineering, 95k)
--   |   `-- Carol (Engineering, 120k)     <- same salary as Bob (tie)
--   |-- Eve (Sales, 150k)
--   |   |-- Frank (Sales, 90k)
--   |   `-- Heidi (Sales, 160k)           <- earns MORE than her manager
--   |-- Ivan (HR, 70k)
--   |   `-- Judy (HR, 65k)
--   |-- Mallory (Marketing, 110k)
--   `-- Oscar (NO department, NULL email, 60k)
--
-- Legal department has no employees. Customer Hooli never ordered. Monitor never sold.
-- =====================================================================

INSERT INTO departments (dept_id, dept_name, location) VALUES
    (1, 'Engineering', 'New York'),
    (2, 'Sales',       'Chicago'),
    (3, 'HR',          'New York'),
    (4, 'Marketing',   'Boston'),
    (5, 'Legal',       'Austin');

INSERT INTO employees (emp_id, name, email, dept_id, manager_id, salary, hire_date, gender) VALUES
    (1,  'Grace',   'grace@corp.com',   1,    NULL, 250000, DATE '2015-03-01', 'F'),
    (2,  'Alice',   'alice@corp.com',   1,    1,    180000, DATE '2016-06-15', 'F'),
    (3,  'Bob',     'bob@corp.com',     1,    2,    120000, DATE '2018-01-10', 'M'),
    (4,  'Carol',   'carol@corp.com',   1,    2,    120000, DATE '2019-07-22', 'F'),
    (5,  'Dan',     'dan@corp.com',     1,    3,     95000, DATE '2021-02-01', 'M'),
    (6,  'Eve',     'eve@corp.com',     2,    1,    150000, DATE '2017-09-05', 'F'),
    (7,  'Frank',   'frank@corp.com',   2,    6,     90000, DATE '2020-11-11', 'M'),
    (8,  'Heidi',   'heidi@corp.com',   2,    6,    160000, DATE '2022-04-18', 'F'),
    (9,  'Ivan',    'ivan@corp.com',    3,    1,     70000, DATE '2019-01-30', 'M'),
    (10, 'Judy',    'judy@corp.com',    3,    9,     65000, DATE '2023-08-01', 'F'),
    (11, 'Mallory', 'mallory@corp.com', 4,    1,    110000, DATE '2020-05-05', 'F'),
    (12, 'Oscar',   NULL,               NULL, 1,     60000, DATE '2024-01-15', 'M');

INSERT INTO customers (customer_id, name, city, signup_date) VALUES
    (1, 'Acme',     'New York', DATE '2025-01-10'),
    (2, 'Globex',   'Chicago',  DATE '2025-02-20'),
    (3, 'Initech',  'Austin',   DATE '2025-03-05'),
    (4, 'Umbrella', 'Boston',   DATE '2025-03-18'),
    (5, 'Hooli',    'New York', DATE '2025-04-01'),
    (6, 'Stark',    'Chicago',  DATE '2025-05-12');

INSERT INTO products (product_id, name, category, price) VALUES
    (1, 'Laptop',     'Electronics', 1200.00),
    (2, 'Phone',      'Electronics',  800.00),
    (3, 'Headphones', 'Electronics',  150.00),
    (4, 'Desk',       'Furniture',    300.00),
    (5, 'Chair',      'Furniture',    120.00),
    (6, 'Notebook',   'Stationery',     5.00),
    (7, 'Pen',        'Stationery',     2.00),
    (8, 'Monitor',    'Electronics',  250.00);

INSERT INTO orders (order_id, customer_id, order_date, status) VALUES
    (101, 1, DATE '2025-01-15', 'completed'),
    (102, 1, DATE '2025-02-03', 'completed'),
    (103, 2, DATE '2025-02-25', 'completed'),
    (104, 3, DATE '2025-03-10', 'cancelled'),
    (105, 1, DATE '2025-03-12', 'completed'),
    (106, 2, DATE '2025-03-28', 'completed'),
    (107, 4, DATE '2025-04-02', 'completed'),
    (108, 3, DATE '2025-04-15', 'completed'),
    (109, 6, DATE '2025-05-20', 'completed'),
    (110, 2, DATE '2025-05-21', 'pending'),
    (111, 1, DATE '2025-06-01', 'completed'),
    (112, 4, DATE '2025-06-10', 'cancelled');

INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
    (101, 1, 1, 1200.00), (101, 6, 10,   5.00),
    (102, 3, 2,  150.00),
    (103, 2, 1,  800.00), (103, 3, 1,  150.00),
    (104, 4, 1,  300.00),
    (105, 5, 4,  120.00), (105, 7, 20,   2.00),
    (106, 1, 2, 1150.00),
    (107, 4, 2,  300.00), (107, 5, 2,  120.00),
    (108, 2, 1,  780.00),
    (109, 6, 50,   5.00), (109, 7, 50,   2.00),
    (110, 1, 1, 1200.00),
    (111, 2, 2,  800.00),
    (112, 3, 1,  150.00);

-- user 1: Jan 1-3 (3-day streak), Jan 5-6.  user 2: Jan 1, Jan 3-6 (4-day streak).  user 3: one day
INSERT INTO logins (user_id, login_date) VALUES
    (1, DATE '2025-01-01'), (1, DATE '2025-01-02'), (1, DATE '2025-01-02'), (1, DATE '2025-01-03'),
    (1, DATE '2025-01-05'), (1, DATE '2025-01-06'),
    (2, DATE '2025-01-01'), (2, DATE '2025-01-03'), (2, DATE '2025-01-04'), (2, DATE '2025-01-05'),
    (2, DATE '2025-01-06'),
    (3, DATE '2025-01-02');

INSERT INTO transactions (txn_id, account_id, txn_date, amount) VALUES
    (1, 1, DATE '2025-01-01', 1000.00),
    (2, 1, DATE '2025-01-05', -200.00),
    (3, 1, DATE '2025-01-10', -300.00),
    (4, 1, DATE '2025-01-15',  500.00),
    (5, 2, DATE '2025-01-02',  300.00),
    (6, 2, DATE '2025-01-08', -400.00),
    (7, 2, DATE '2025-01-20',  200.00);

INSERT INTO contacts (id, name, email) VALUES
    (1, 'John', 'john@x.com'),
    (2, 'Jane', 'jane@x.com'),
    (3, 'John', 'john@x.com'),
    (4, 'Mike', 'mike@x.com'),
    (5, 'JOHN', 'John@X.com'),
    (6, 'Jane', 'jane@x.com');

INSERT INTO scores (student, subject, score) VALUES
    ('Amy', 'Math', 90), ('Amy', 'Science', 85), ('Amy', 'English', 78),
    ('Ben', 'Math', 70), ('Ben', 'Science', 95), ('Ben', 'English', 88),
    ('Cal', 'Math', 85), ('Cal', 'Science', 60);

INSERT INTO weather (id, record_date, temperature) VALUES
    (1, DATE '2025-01-01', 10),
    (2, DATE '2025-01-02', 25),
    (3, DATE '2025-01-03', 20),
    (4, DATE '2025-01-04', 30),
    (5, DATE '2025-01-06', 35);
