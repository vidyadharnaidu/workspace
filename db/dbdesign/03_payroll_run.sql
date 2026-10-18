-- =====================================================================
-- DB DESIGN CASE STUDY: PAYROLL  (part 3 of 7: the payroll run = generating paychecks)
--
-- REMEMBER:
--   * POINT-IN-TIME (as-of) JOIN: the rate used for a period is the compensation row where
--       period_end BETWEEN effective_from AND COALESCE(effective_to, '9999-12-31')
--   * Pay frequency comes from the compensation row too, so an employee who switches
--     MONTHLY -> SEMI_MONTHLY is paid on the right calendar automatically.
--   * Salaried:  base = annual_salary / periods_per_year
--     Hourly:    base = rate * regular_hours, overtime = rate * 1.5 * overtime_hours,
--                and no approved timesheet = no paycheck.
--   * Employment window: hired on/before period_end, not terminated before period_start.
--   * Simplifications (say them out loud in an interview): no proration for mid-period
--     hires/changes, flat 22% tax, pay_date = period_end.
--   * One INSERT ... SELECT, guarded by UNIQUE (emp_id, period_id) -> safe to re-run
--     (in PostgreSQL add ON CONFLICT (emp_id, period_id) DO NOTHING).
-- =====================================================================

INSERT INTO paycheck
WITH eligible AS (
    SELECT e.emp_id, p.period_id, p.pay_date,
           c.comp_id, c.pay_type, c.pay_freq_code, c.annual_salary, c.hourly_rate,
           f.periods_per_year
    FROM employee e
    JOIN compensation  c ON c.emp_id = e.emp_id
    JOIN pay_period    p ON p.freq_code = c.pay_freq_code
                        AND p.period_end BETWEEN c.effective_from AND COALESCE(c.effective_to, DATE '9999-12-31')
    JOIN pay_frequency f ON f.freq_code = c.pay_freq_code
    WHERE e.hire_date <= p.period_end
      AND (e.termination_date IS NULL OR e.termination_date >= p.period_start)
),
calc AS (
    SELECT el.*,
           t.regular_hours, t.overtime_hours,
           CASE el.pay_type
                WHEN 'SALARY' THEN ROUND(el.annual_salary / el.periods_per_year, 2)
                ELSE ROUND(el.hourly_rate * t.regular_hours, 2)
           END AS base_pay,
           CASE el.pay_type
                WHEN 'HOURLY' THEN ROUND(el.hourly_rate * 1.5 * t.overtime_hours, 2)
                ELSE 0
           END AS overtime_pay,
           COALESCE(b.amount, 0) AS bonus_pay
    FROM eligible el
    LEFT JOIN timesheet t ON t.emp_id = el.emp_id AND t.period_id = el.period_id
    LEFT JOIN bonus     b ON b.emp_id = el.emp_id AND b.period_id = el.period_id
    WHERE el.pay_type = 'SALARY' OR t.emp_id IS NOT NULL          -- hourly needs approved hours
),
gross AS (
    SELECT calc.*, base_pay + overtime_pay + bonus_pay AS gross_pay
    FROM calc
)
SELECT ROW_NUMBER() OVER (ORDER BY pay_date, emp_id) AS paycheck_id,
       emp_id, period_id, comp_id, pay_date, pay_type, pay_freq_code,
       annual_salary, hourly_rate, regular_hours, overtime_hours,
       base_pay, overtime_pay, bonus_pay, gross_pay,
       ROUND(gross_pay * 0.22, 2)             AS tax_amount,
       gross_pay - ROUND(gross_pay * 0.22, 2) AS net_pay
FROM gross;


-- Q1. Paychecks produced per pay frequency (expect 34 monthly, 55 semi-monthly, 4 quarterly = 93)
SELECT pay_freq_code, pay_type, COUNT(*) AS paychecks, SUM(gross_pay) AS total_gross
FROM paycheck
GROUP BY pay_freq_code, pay_type
ORDER BY pay_freq_code, pay_type;


-- Q2. Pay stub sample: Dan's March checks (overtime + bonus)
SELECT pc.paycheck_id, pc.pay_date, pc.hourly_rate, pc.regular_hours, pc.overtime_hours,
       pc.base_pay, pc.overtime_pay, pc.bonus_pay, pc.gross_pay, pc.tax_amount, pc.net_pay
FROM paycheck pc
WHERE pc.emp_id = 104 AND pc.pay_date BETWEEN DATE '2026-03-01' AND DATE '2026-03-31'
ORDER BY pc.pay_date;


-- Reusable reporting view: paycheck + who/where + an ANNUALIZED rate so that
-- monthly, semi-monthly, quarterly and hourly pay can be compared fairly.
-- Hourly annualized = rate * 2080 (40 h x 52 weeks).
CREATE VIEW v_paycheck AS
SELECT pc.*,
       e.first_name || ' ' || e.last_name AS emp_name,
       co.company_name,
       ci.city_name,
       d.dept_name,
       p.period_start,
       p.period_end,
       CASE pc.pay_type WHEN 'SALARY' THEN pc.annual_salary ELSE pc.hourly_rate * 2080 END AS annualized_rate
FROM paycheck pc
JOIN employee   e  ON e.emp_id = pc.emp_id
JOIN pay_period p  ON p.period_id = pc.period_id
JOIN office     o  ON o.office_id = e.office_id
JOIN company    co ON co.company_id = o.company_id
JOIN city       ci ON ci.city_id = o.city_id
JOIN department d  ON d.dept_id = e.dept_id;
