-- =====================================================================
-- SCHEMA used by every question in this folder.
-- Dialect: PostgreSQL (also runs on DuckDB). MySQL notes are in comments.
-- Run this first, then 02_sample_data.sql.
-- =====================================================================

DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS logins;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS contacts;
DROP TABLE IF EXISTS scores;
DROP TABLE IF EXISTS weather;

CREATE TABLE departments (
    dept_id    INTEGER PRIMARY KEY,
    dept_name  VARCHAR(50) NOT NULL UNIQUE,
    location   VARCHAR(50)
);

-- manager_id points to another employee (self reference -> hierarchy questions)
CREATE TABLE employees (
    emp_id      INTEGER PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    email       VARCHAR(100),
    dept_id     INTEGER REFERENCES departments (dept_id),
    manager_id  INTEGER,
    salary      NUMERIC(12, 2) NOT NULL CHECK (salary > 0),
    hire_date   DATE NOT NULL,
    gender      CHAR(1)
);

CREATE TABLE customers (
    customer_id  INTEGER PRIMARY KEY,
    name         VARCHAR(50) NOT NULL,
    city         VARCHAR(50),
    signup_date  DATE NOT NULL
);

CREATE TABLE products (
    product_id  INTEGER PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    category    VARCHAR(50) NOT NULL,
    price       NUMERIC(12, 2) NOT NULL
);

CREATE TABLE orders (
    order_id     INTEGER PRIMARY KEY,
    customer_id  INTEGER NOT NULL REFERENCES customers (customer_id),
    order_date   DATE NOT NULL,
    status       VARCHAR(20) NOT NULL   -- completed | cancelled | pending
);

CREATE TABLE order_items (
    order_id    INTEGER NOT NULL REFERENCES orders (order_id),
    product_id  INTEGER NOT NULL REFERENCES products (product_id),
    quantity    INTEGER NOT NULL,
    unit_price  NUMERIC(12, 2) NOT NULL,   -- price actually paid (can differ from list price)
    PRIMARY KEY (order_id, product_id)
);

-- one row per login (a user can log in more than once per day)
CREATE TABLE logins (
    user_id     INTEGER NOT NULL,
    login_date  DATE NOT NULL
);

-- positive amount = credit, negative amount = debit
CREATE TABLE transactions (
    txn_id      INTEGER PRIMARY KEY,
    account_id  INTEGER NOT NULL,
    txn_date    DATE NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL
);

-- contains duplicate emails on purpose
CREATE TABLE contacts (
    id     INTEGER PRIMARY KEY,
    name   VARCHAR(50),
    email  VARCHAR(100)
);

-- long format (student, subject, score) -> pivot questions
CREATE TABLE scores (
    student  VARCHAR(50) NOT NULL,
    subject  VARCHAR(50) NOT NULL,
    score    INTEGER NOT NULL,
    PRIMARY KEY (student, subject)
);

-- has a missing day on purpose (2025-01-05)
CREATE TABLE weather (
    id           INTEGER PRIMARY KEY,
    record_date  DATE NOT NULL UNIQUE,
    temperature  INTEGER NOT NULL
);
