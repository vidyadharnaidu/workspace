-- =====================================================================
-- DB DESIGN CASE STUDY: PAYROLL  (part 4 of 7: data warehouse star schema)
--
-- REMEMBER (Kimball checklist: process -> grain -> dimensions -> facts):
--   Business process : payroll
--   Grain            : fact_payroll = ONE ROW PER PAYCHECK (employee x pay period)
--   Dimensions       : date (role-played as pay / period start / period end), employee (SCD2),
--                      office (company + city), department, pay frequency
--   Facts            : hours, base, overtime, bonus, gross, tax, net (additive),
--                      annualized_rate (NON-additive: never SUM it, use AVG/MAX or compare)
--
--   Fact table types used here:
--     fact_payroll        -> transaction fact (one row per event)
--     fact_comp_change    -> transaction fact for pay changes (precomputed raise/cut %)
--     agg_payroll_monthly -> aggregate / periodic-snapshot style summary for dashboards
--
--   * Surrogate keys (employee_sk) instead of natural keys: SCD2 needs several rows per emp_id.
--   * emp_id is kept on the fact as a DURABLE key: "all history of this person" without a dim join.
--   * Unknown member (employee_sk = -1) so late-arriving / bad keys don't drop fact rows.
--   * dim_employee is HYBRID: pay/title columns are Type 2 (new row on change),
--     company/city/dept are Type 1 here (overwritten; current value on every row).
--   * No foreign keys enforced in the DW (typical: ETL guarantees them, loads stay fast).
--   * date_key = yyyymmdd integer: readable, sortable, and the usual partition key.
-- =====================================================================

DROP TABLE IF EXISTS agg_payroll_monthly;
DROP TABLE IF EXISTS fact_comp_change;
DROP TABLE IF EXISTS fact_payroll;
DROP TABLE IF EXISTS dim_employee;
DROP TABLE IF EXISTS dim_pay_frequency;
DROP TABLE IF EXISTS dim_department;
DROP TABLE IF EXISTS dim_office;
DROP TABLE IF EXISTS dim_date;

CREATE TABLE dim_date (
    date_key        INTEGER PRIMARY KEY,           -- 20260315
    full_date       DATE NOT NULL UNIQUE,
    cal_year        INTEGER NOT NULL,
    cal_quarter     INTEGER NOT NULL,
    cal_month       INTEGER NOT NULL,
    cal_day         INTEGER NOT NULL,
    year_month      INTEGER NOT NULL,               -- 202603
    year_quarter    VARCHAR(7) NOT NULL,            -- 2026-Q1
    is_month_end    BOOLEAN NOT NULL,
    is_quarter_end  BOOLEAN NOT NULL
);

-- Conformed "location" dimension: company + city in one place (Type 1)
CREATE TABLE dim_office (
    office_key    INTEGER PRIMARY KEY,
    office_id     INTEGER NOT NULL,                 -- natural key
    office_name   VARCHAR(100) NOT NULL,
    company_name  VARCHAR(100) NOT NULL,
    city_name     VARCHAR(50)  NOT NULL,
    state         CHAR(2)      NOT NULL
);

CREATE TABLE dim_department (
    dept_key      INTEGER PRIMARY KEY,
    dept_id       INTEGER NOT NULL,
    dept_name     VARCHAR(50)  NOT NULL,
    company_name  VARCHAR(100) NOT NULL
);

CREATE TABLE dim_pay_frequency (
    pay_freq_key      INTEGER PRIMARY KEY,
    freq_code         VARCHAR(20) NOT NULL,
    description       VARCHAR(100) NOT NULL,
    periods_per_year  INTEGER NOT NULL
);

-- SCD Type 2: one row per version of the employee's pay / title
CREATE TABLE dim_employee (
    employee_sk       INTEGER PRIMARY KEY,          -- surrogate key
    emp_id            INTEGER NOT NULL,             -- natural / durable key
    full_name         VARCHAR(101) NOT NULL,
    company_name      VARCHAR(100),                 -- Type 1
    city_name         VARCHAR(50),                  -- Type 1
    dept_name         VARCHAR(50),                  -- Type 1
    manager_emp_id    INTEGER,                      -- Type 1
    hire_date         DATE,
    termination_date  DATE,
    job_title         VARCHAR(50),                  -- Type 2 from here down
    pay_type          VARCHAR(10),
    pay_freq_code     VARCHAR(20),
    annual_salary     NUMERIC(12, 2),
    hourly_rate       NUMERIC(8, 2),
    annualized_comp   NUMERIC(12, 2),
    change_reason     VARCHAR(30),
    effective_from    DATE NOT NULL,
    effective_to      DATE NOT NULL,                -- '9999-12-31' for the current row (no NULLs -> simple BETWEEN)
    is_current        BOOLEAN NOT NULL
);

CREATE TABLE fact_payroll (
    paycheck_id       INTEGER NOT NULL,             -- degenerate dimension
    employee_sk       INTEGER NOT NULL,
    emp_id            INTEGER NOT NULL,             -- durable key
    office_key        INTEGER NOT NULL,
    dept_key          INTEGER NOT NULL,
    pay_freq_key      INTEGER NOT NULL,
    pay_date_key      INTEGER NOT NULL,             -- role-playing dim_date (partition key in a lake)
    period_start_key  INTEGER NOT NULL,
    period_end_key    INTEGER NOT NULL,
    pay_type          VARCHAR(10) NOT NULL,
    regular_hours     NUMERIC(6, 2),
    overtime_hours    NUMERIC(6, 2),
    base_pay          NUMERIC(12, 2) NOT NULL,
    overtime_pay      NUMERIC(12, 2) NOT NULL,
    bonus_pay         NUMERIC(12, 2) NOT NULL,
    gross_pay         NUMERIC(12, 2) NOT NULL,
    tax_amount        NUMERIC(12, 2) NOT NULL,
    net_pay           NUMERIC(12, 2) NOT NULL,
    annualized_rate   NUMERIC(12, 2) NOT NULL       -- non-additive
);

-- One row per compensation version: what changed and by how much
CREATE TABLE fact_comp_change (
    employee_sk         INTEGER NOT NULL,           -- the NEW version
    prev_employee_sk    INTEGER,                    -- NULL for the hire row
    emp_id              INTEGER NOT NULL,
    effective_date_key  INTEGER NOT NULL,
    change_reason       VARCHAR(30) NOT NULL,
    change_type         VARCHAR(10) NOT NULL,       -- NEW_HIRE | RAISE | CUT | NO_CHANGE
    prev_annualized     NUMERIC(12, 2),
    new_annualized      NUMERIC(12, 2) NOT NULL,
    change_amount       NUMERIC(12, 2),
    change_pct          NUMERIC(8, 2)
);

CREATE TABLE agg_payroll_monthly (
    year_month      INTEGER NOT NULL,
    company_name    VARCHAR(100) NOT NULL,
    city_name       VARCHAR(50) NOT NULL,
    dept_name       VARCHAR(50) NOT NULL,
    freq_code       VARCHAR(20) NOT NULL,
    employees_paid  INTEGER NOT NULL,
    paychecks       INTEGER NOT NULL,
    total_gross     NUMERIC(14, 2) NOT NULL,
    total_overtime  NUMERIC(14, 2) NOT NULL,
    total_tax       NUMERIC(14, 2) NOT NULL,
    total_net       NUMERIC(14, 2) NOT NULL
);
