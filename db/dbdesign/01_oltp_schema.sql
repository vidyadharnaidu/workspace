-- =====================================================================
-- DB DESIGN CASE STUDY: MULTI-COMPANY PAYROLL  (part 1 of 7: OLTP / source schema)
--
-- Scenario: companies have offices in cities. Employees are paid either
--   SALARY (annual amount) or HOURLY (rate x approved hours, overtime at 1.5x).
-- Pay frequency is per employee: MONTHLY, SEMI_MONTHLY (every ~15 days: 15th + last day)
--   or QUARTERLY. Payroll produces one paycheck per employee per pay period.
-- Reports: total payroll by company / city / dept / month / quarter, YTD,
--   overtime, missing paychecks, and "WHO GOT A RAISE SINCE THE PREVIOUS PAY".
--
-- REMEMBER (design points interviewers look for):
--   * Pay rate is HISTORY, not a column on employee -> compensation table with
--     effective_from / effective_to (open-ended = NULL). This is SCD Type 2 at the source.
--   * The paycheck SNAPSHOTS the rate it was calculated with (immutable financial record).
--     Never recompute old pay from today's rate.
--   * Money = NUMERIC(p,2), never FLOAT.
--   * Pay frequency is a lookup table with periods_per_year -> lets you annualize
--     any paycheck and compare monthly vs semi-monthly vs quarterly vs hourly people.
--   * UNIQUE (emp_id, period_id) on paycheck = re-running payroll can't pay twice (idempotency).
--   * CHECK makes SALARY rows carry annual_salary and HOURLY rows carry hourly_rate, never both.
--
-- Table names are singular so they don't clash with the shared 00_setup tables.
-- =====================================================================

DROP VIEW  IF EXISTS v_paycheck;
DROP VIEW  IF EXISTS v_employee_current;
DROP TABLE IF EXISTS paycheck;
DROP TABLE IF EXISTS bonus;
DROP TABLE IF EXISTS timesheet;
DROP TABLE IF EXISTS pay_period;
DROP TABLE IF EXISTS compensation;
DROP TABLE IF EXISTS employee;
DROP TABLE IF EXISTS pay_frequency;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS office;
DROP TABLE IF EXISTS company;
DROP TABLE IF EXISTS city;

CREATE TABLE city (
    city_id    INTEGER PRIMARY KEY,
    city_name  VARCHAR(50) NOT NULL,
    state      CHAR(2)     NOT NULL,
    country    VARCHAR(50) NOT NULL
);

CREATE TABLE company (
    company_id    INTEGER PRIMARY KEY,
    company_name  VARCHAR(100) NOT NULL UNIQUE,
    hq_city_id    INTEGER NOT NULL REFERENCES city (city_id)
);

-- "company in city": a company has many offices, a city hosts offices of many companies (M:N resolved by office)
CREATE TABLE office (
    office_id    INTEGER PRIMARY KEY,
    company_id   INTEGER NOT NULL REFERENCES company (company_id),
    city_id      INTEGER NOT NULL REFERENCES city (city_id),
    office_name  VARCHAR(100) NOT NULL
);

CREATE TABLE department (
    dept_id     INTEGER PRIMARY KEY,
    company_id  INTEGER NOT NULL REFERENCES company (company_id),
    dept_name   VARCHAR(50) NOT NULL,
    UNIQUE (company_id, dept_name)
);

CREATE TABLE pay_frequency (
    freq_code         VARCHAR(20) PRIMARY KEY,      -- MONTHLY | SEMI_MONTHLY | QUARTERLY | BIWEEKLY
    description       VARCHAR(100) NOT NULL,
    periods_per_year  INTEGER NOT NULL CHECK (periods_per_year > 0)
);

-- Current-state employee record. Pay is NOT here (it changes over time -> compensation).
CREATE TABLE employee (
    emp_id            INTEGER PRIMARY KEY,
    first_name        VARCHAR(50) NOT NULL,
    last_name         VARCHAR(50) NOT NULL,
    email             VARCHAR(100) UNIQUE,
    office_id         INTEGER NOT NULL REFERENCES office (office_id),
    dept_id           INTEGER NOT NULL REFERENCES department (dept_id),
    manager_id        INTEGER,                      -- self reference (org chart)
    hire_date         DATE NOT NULL,
    termination_date  DATE,
    CHECK (termination_date IS NULL OR termination_date >= hire_date)
);

-- Effective-dated pay history. One open row (effective_to IS NULL) per employee.
CREATE TABLE compensation (
    comp_id         INTEGER PRIMARY KEY,
    emp_id          INTEGER NOT NULL REFERENCES employee (emp_id),
    job_title       VARCHAR(50) NOT NULL,
    pay_type        VARCHAR(10) NOT NULL CHECK (pay_type IN ('SALARY', 'HOURLY')),
    pay_freq_code   VARCHAR(20) NOT NULL REFERENCES pay_frequency (freq_code),
    annual_salary   NUMERIC(12, 2),
    hourly_rate     NUMERIC(8, 2),
    effective_from  DATE NOT NULL,
    effective_to    DATE,                           -- NULL = current
    change_reason   VARCHAR(30) NOT NULL,           -- HIRE | MERIT | PROMOTION | MARKET_ADJUSTMENT | CONVERSION | DEMOTION
    UNIQUE (emp_id, effective_from),
    CHECK ((pay_type = 'SALARY' AND annual_salary > 0 AND hourly_rate IS NULL)
        OR (pay_type = 'HOURLY' AND hourly_rate  > 0 AND annual_salary IS NULL)),
    CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

-- Pay calendar: one row per frequency per period. pay_date = check date.
CREATE TABLE pay_period (
    period_id     INTEGER PRIMARY KEY,
    freq_code     VARCHAR(20) NOT NULL REFERENCES pay_frequency (freq_code),
    period_start  DATE NOT NULL,
    period_end    DATE NOT NULL,
    pay_date      DATE NOT NULL,
    UNIQUE (freq_code, period_start),
    CHECK (period_end >= period_start)
);

-- Approved hours per hourly employee per pay period (no row = nothing to pay)
CREATE TABLE timesheet (
    emp_id          INTEGER NOT NULL REFERENCES employee (emp_id),
    period_id       INTEGER NOT NULL REFERENCES pay_period (period_id),
    regular_hours   NUMERIC(6, 2) NOT NULL CHECK (regular_hours >= 0),
    overtime_hours  NUMERIC(6, 2) NOT NULL DEFAULT 0 CHECK (overtime_hours >= 0),
    PRIMARY KEY (emp_id, period_id)
);

-- One-off payments: NOT a raise (classic trap when comparing gross pay)
CREATE TABLE bonus (
    bonus_id   INTEGER PRIMARY KEY,
    emp_id     INTEGER NOT NULL REFERENCES employee (emp_id),
    period_id  INTEGER NOT NULL REFERENCES pay_period (period_id),
    amount     NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    reason     VARCHAR(100)
);

-- Output of the payroll run. Rate columns are a SNAPSHOT of compensation at pay time.
CREATE TABLE paycheck (
    paycheck_id     INTEGER PRIMARY KEY,
    emp_id          INTEGER NOT NULL REFERENCES employee (emp_id),
    period_id       INTEGER NOT NULL REFERENCES pay_period (period_id),
    comp_id         INTEGER NOT NULL,               -- lineage: which compensation row was used
    pay_date        DATE NOT NULL,
    pay_type        VARCHAR(10) NOT NULL,
    pay_freq_code   VARCHAR(20) NOT NULL,
    annual_salary   NUMERIC(12, 2),
    hourly_rate     NUMERIC(8, 2),
    regular_hours   NUMERIC(6, 2),
    overtime_hours  NUMERIC(6, 2),
    base_pay        NUMERIC(12, 2) NOT NULL,
    overtime_pay    NUMERIC(12, 2) NOT NULL,
    bonus_pay       NUMERIC(12, 2) NOT NULL,
    gross_pay       NUMERIC(12, 2) NOT NULL,
    tax_amount      NUMERIC(12, 2) NOT NULL,
    net_pay         NUMERIC(12, 2) NOT NULL,
    UNIQUE (emp_id, period_id)                      -- never pay the same period twice
);

-- Indexes that the reports and the payroll run use (PostgreSQL; DuckDB ignores the benefit but accepts them)
CREATE INDEX ix_comp_emp_dates  ON compensation (emp_id, effective_from);
CREATE INDEX ix_paycheck_date   ON paycheck (pay_date);
CREATE INDEX ix_employee_office ON employee (office_id);
