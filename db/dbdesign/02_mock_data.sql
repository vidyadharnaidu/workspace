-- =====================================================================
-- DB DESIGN CASE STUDY: PAYROLL  (part 2 of 7: mock data)
--
-- Period covered: Jan - Jun 2026 (H1 = Q1 + Q2).
-- Every row is chosen to make a report or a trap visible:
--
--   Alice  (101) MONTHLY salary, MERIT raise 120k -> 132k on 2026-04-01
--   Bob    (102) MONTHLY salary, NO raise, but a 5,000 BONUS in March   -> gross jumps, NOT a raise
--   Carol  (103) SEMI_MONTHLY salary, PROMOTION 72k -> 84k on 2026-03-01
--   Dan    (104) HOURLY semi-monthly, rate 25.00 -> 27.50 on 2026-05-01, overtime most periods
--   Eva    (105) HOURLY semi-monthly, no rate change, gross moves with hours; NO timesheet Feb 16-28
--   Frank  (106) QUARTERLY salary, 80k -> 88k on 2026-04-01; earns more than his manager Carol
--   Grace  (107) NEW HIRE 2026-03-01 (first paycheck has no "previous" pay)
--   Henry  (201) MONTHLY salary, CFO, no change
--   Irene  (202) MONTHLY 110k -> SEMI_MONTHLY 115k on 2026-04-01: a raise, but the per-check
--                amount HALVES (9,166.67 -> 4,791.67). Comparing gross says "pay cut". TRAP.
--   Jack   (203) HOURLY 20.00 (=41,600/yr) -> SALARY 52k MONTHLY on 2026-04-01 (CONVERSION, +25%)
--   Kate   (204) MONTHLY 98k -> 90k on 2026-05-01 (DEMOTION = pay cut)
--   Leo    (205) HOURLY, TERMINATED 2026-04-30 -> no paychecks after April
--   Mia    (206) QUARTERLY salary, no change
-- =====================================================================

INSERT INTO city VALUES
    (1, 'New York', 'NY', 'USA'),
    (2, 'Austin',   'TX', 'USA'),
    (3, 'Chicago',  'IL', 'USA');

INSERT INTO company VALUES
    (1, 'Acme Corp',  1),
    (2, 'Globex Inc', 3);

-- Austin has offices of BOTH companies (good for "by city" reports)
INSERT INTO office VALUES
    (10, 1, 1, 'Acme NYC HQ'),
    (11, 1, 2, 'Acme Austin'),
    (20, 2, 3, 'Globex Chicago HQ'),
    (21, 2, 2, 'Globex Austin DC');

INSERT INTO department VALUES
    (100, 1, 'Engineering'),
    (101, 1, 'Sales'),
    (102, 1, 'Operations'),
    (200, 2, 'Engineering'),
    (201, 2, 'Warehouse'),
    (202, 2, 'Finance');

INSERT INTO pay_frequency VALUES
    ('MONTHLY',      'Paid on the last day of the month',              12),
    ('SEMI_MONTHLY', 'Paid every ~15 days: on the 15th and month end', 24),
    ('QUARTERLY',    'Paid on the last day of the quarter',             4),
    ('BIWEEKLY',     'Paid every other Friday (no employees yet)',     26);

INSERT INTO employee VALUES
    (101, 'Alice', 'Chen',   'alice@acme.com',   10, 100, NULL, DATE '2019-03-01', NULL),
    (102, 'Bob',   'Smith',  'bob@acme.com',     10, 100, 101,  DATE '2021-06-15', NULL),
    (103, 'Carol', 'Diaz',   'carol@acme.com',   10, 101, 101,  DATE '2022-01-10', NULL),
    (104, 'Dan',   'Patel',  'dan@acme.com',     11, 102, 101,  DATE '2023-05-01', NULL),
    (105, 'Eva',   'Novak',  'eva@acme.com',     11, 102, 104,  DATE '2024-02-01', NULL),
    (106, 'Frank', 'Moore',  'frank@acme.com',   11, 101, 103,  DATE '2020-09-01', NULL),
    (107, 'Grace', 'Kim',    'grace@acme.com',   10, 100, 101,  DATE '2026-03-01', NULL),
    (201, 'Henry', 'Ford',   'henry@globex.com', 20, 202, NULL, DATE '2018-01-02', NULL),
    (202, 'Irene', 'Lopez',  'irene@globex.com', 20, 200, 201,  DATE '2021-11-01', NULL),
    (203, 'Jack',  'Brown',  'jack@globex.com',  21, 201, 201,  DATE '2024-07-01', NULL),
    (204, 'Kate',  'Wilson', 'kate@globex.com',  20, 200, 202,  DATE '2022-04-18', NULL),
    (205, 'Leo',   'Garcia', 'leo@globex.com',   21, 201, 203,  DATE '2025-02-01', DATE '2026-04-30'),
    (206, 'Mia',   'Rossi',  'mia@globex.com',   20, 202, 201,  DATE '2023-08-01', NULL);

-- comp_id, emp_id, job_title, pay_type, freq, annual_salary, hourly_rate, effective_from, effective_to, reason
INSERT INTO compensation VALUES
    ( 1, 101, 'Engineering Manager',      'SALARY', 'MONTHLY',      105000, NULL,  DATE '2019-03-01', DATE '2024-12-31', 'HIRE'),
    ( 2, 101, 'Director of Engineering',  'SALARY', 'MONTHLY',      120000, NULL,  DATE '2025-01-01', DATE '2026-03-31', 'PROMOTION'),
    ( 3, 101, 'Director of Engineering',  'SALARY', 'MONTHLY',      132000, NULL,  DATE '2026-04-01', NULL,              'MERIT'),
    ( 4, 102, 'Software Engineer',        'SALARY', 'MONTHLY',       95000, NULL,  DATE '2021-06-15', NULL,              'HIRE'),
    ( 5, 103, 'Account Executive',        'SALARY', 'SEMI_MONTHLY',  72000, NULL,  DATE '2022-01-10', DATE '2026-02-28', 'HIRE'),
    ( 6, 103, 'Senior Account Executive', 'SALARY', 'SEMI_MONTHLY',  84000, NULL,  DATE '2026-03-01', NULL,              'PROMOTION'),
    ( 7, 104, 'Operations Associate',     'HOURLY', 'SEMI_MONTHLY',   NULL, 25.00, DATE '2023-05-01', DATE '2026-04-30', 'HIRE'),
    ( 8, 104, 'Operations Lead',          'HOURLY', 'SEMI_MONTHLY',   NULL, 27.50, DATE '2026-05-01', NULL,              'PROMOTION'),
    ( 9, 105, 'Operations Associate',     'HOURLY', 'SEMI_MONTHLY',   NULL, 22.00, DATE '2024-02-01', NULL,              'HIRE'),
    (10, 106, 'Sales Consultant',         'SALARY', 'QUARTERLY',     80000, NULL,  DATE '2020-09-01', DATE '2026-03-31', 'HIRE'),
    (11, 106, 'Sales Consultant',         'SALARY', 'QUARTERLY',     88000, NULL,  DATE '2026-04-01', NULL,              'MARKET_ADJUSTMENT'),
    (12, 107, 'Software Engineer',        'SALARY', 'MONTHLY',       70000, NULL,  DATE '2026-03-01', NULL,              'HIRE'),
    (13, 201, 'CFO',                      'SALARY', 'MONTHLY',      150000, NULL,  DATE '2018-01-02', NULL,              'HIRE'),
    (14, 202, 'Senior Engineer',          'SALARY', 'MONTHLY',      110000, NULL,  DATE '2021-11-01', DATE '2026-03-31', 'HIRE'),
    (15, 202, 'Senior Engineer',          'SALARY', 'SEMI_MONTHLY', 115000, NULL,  DATE '2026-04-01', NULL,              'MERIT'),
    (16, 203, 'Warehouse Associate',      'HOURLY', 'SEMI_MONTHLY',   NULL, 20.00, DATE '2024-07-01', DATE '2026-03-31', 'HIRE'),
    (17, 203, 'Warehouse Supervisor',     'SALARY', 'MONTHLY',       52000, NULL,  DATE '2026-04-01', NULL,              'CONVERSION'),
    (18, 204, 'Engineering Manager',      'SALARY', 'MONTHLY',       98000, NULL,  DATE '2022-04-18', DATE '2026-04-30', 'HIRE'),
    (19, 204, 'Senior Engineer',          'SALARY', 'MONTHLY',       90000, NULL,  DATE '2026-05-01', NULL,              'DEMOTION'),
    (20, 205, 'Warehouse Associate',      'HOURLY', 'SEMI_MONTHLY',   NULL, 19.00, DATE '2025-02-01', NULL,              'HIRE'),
    (21, 206, 'Financial Analyst',        'SALARY', 'QUARTERLY',     72000, NULL,  DATE '2023-08-01', NULL,              'HIRE');

-- Pay calendar H1 2026. ids: 1-6 monthly, 11-22 semi-monthly, 31-32 quarterly
INSERT INTO pay_period VALUES
    ( 1, 'MONTHLY',      DATE '2026-01-01', DATE '2026-01-31', DATE '2026-01-31'),
    ( 2, 'MONTHLY',      DATE '2026-02-01', DATE '2026-02-28', DATE '2026-02-28'),
    ( 3, 'MONTHLY',      DATE '2026-03-01', DATE '2026-03-31', DATE '2026-03-31'),
    ( 4, 'MONTHLY',      DATE '2026-04-01', DATE '2026-04-30', DATE '2026-04-30'),
    ( 5, 'MONTHLY',      DATE '2026-05-01', DATE '2026-05-31', DATE '2026-05-31'),
    ( 6, 'MONTHLY',      DATE '2026-06-01', DATE '2026-06-30', DATE '2026-06-30'),
    (11, 'SEMI_MONTHLY', DATE '2026-01-01', DATE '2026-01-15', DATE '2026-01-15'),
    (12, 'SEMI_MONTHLY', DATE '2026-01-16', DATE '2026-01-31', DATE '2026-01-31'),
    (13, 'SEMI_MONTHLY', DATE '2026-02-01', DATE '2026-02-15', DATE '2026-02-15'),
    (14, 'SEMI_MONTHLY', DATE '2026-02-16', DATE '2026-02-28', DATE '2026-02-28'),
    (15, 'SEMI_MONTHLY', DATE '2026-03-01', DATE '2026-03-15', DATE '2026-03-15'),
    (16, 'SEMI_MONTHLY', DATE '2026-03-16', DATE '2026-03-31', DATE '2026-03-31'),
    (17, 'SEMI_MONTHLY', DATE '2026-04-01', DATE '2026-04-15', DATE '2026-04-15'),
    (18, 'SEMI_MONTHLY', DATE '2026-04-16', DATE '2026-04-30', DATE '2026-04-30'),
    (19, 'SEMI_MONTHLY', DATE '2026-05-01', DATE '2026-05-15', DATE '2026-05-15'),
    (20, 'SEMI_MONTHLY', DATE '2026-05-16', DATE '2026-05-31', DATE '2026-05-31'),
    (21, 'SEMI_MONTHLY', DATE '2026-06-01', DATE '2026-06-15', DATE '2026-06-15'),
    (22, 'SEMI_MONTHLY', DATE '2026-06-16', DATE '2026-06-30', DATE '2026-06-30'),
    (31, 'QUARTERLY',    DATE '2026-01-01', DATE '2026-03-31', DATE '2026-03-31'),
    (32, 'QUARTERLY',    DATE '2026-04-01', DATE '2026-06-30', DATE '2026-06-30');

-- Approved hours (emp_id, period_id, regular, overtime). Eva (105) has NO row for period 14.
INSERT INTO timesheet VALUES
    (104, 11, 88, 4), (104, 12, 88, 0), (104, 13, 80, 6), (104, 14, 72, 0), (104, 15, 88, 2), (104, 16, 88, 8),
    (104, 17, 88, 0), (104, 18, 88, 5), (104, 19, 80, 0), (104, 20, 88, 10), (104, 21, 88, 0), (104, 22, 88, 3),
    (105, 11, 80, 0), (105, 12, 88, 2), (105, 13, 88, 0),                    (105, 15, 88, 0), (105, 16, 88, 0),
    (105, 17, 80, 4), (105, 18, 88, 0), (105, 19, 88, 0), (105, 20, 88, 2), (105, 21, 80, 0), (105, 22, 88, 0),
    (203, 11, 88, 6), (203, 12, 88, 4), (203, 13, 80, 0), (203, 14, 72, 8), (203, 15, 88, 2), (203, 16, 88, 0),
    (205, 11, 88, 0), (205, 12, 88, 0), (205, 13, 80, 2), (205, 14, 72, 0), (205, 15, 88, 0), (205, 16, 88, 3),
    (205, 17, 88, 0), (205, 18, 80, 0);

INSERT INTO bonus VALUES
    (1, 102,  3, 5000.00, 'Q1 spot bonus'),
    (2, 104, 16,  500.00, 'Safety award');
