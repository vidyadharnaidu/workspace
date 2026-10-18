-- =====================================================================
-- DB DESIGN CASE STUDY: PAYROLL  (part 7 of 7: star-schema reports, data quality, SCD2 incremental)
--
-- REMEMBER:
--   * Star query = fact JOIN dims ON surrogate keys, filter/group by dim attributes.
--   * Point-in-time ("as of") on SCD2:  WHERE :as_of BETWEEN effective_from AND effective_to
--   * Current view on SCD2:             WHERE is_current
--   * Data-quality checks every ETL run: reconcile totals with source, grain uniqueness,
--     no SCD2 overlaps/gaps, unknown-member count. Each should return 0 rows / zero diff.
--   * SCD2 incremental = (1) find changed/new source rows (2) EXPIRE the current dim row
--     (3) INSERT the new version with a new surrogate key. In Spark/Delta: one MERGE.
-- =====================================================================


-- Q1. Raises and cuts in 2026 - trivial now, the LAG work was done once in ETL (fact_comp_change)
SELECT de.full_name, de.company_name, dd.full_date AS effective_date, f.change_reason, f.change_type,
       f.prev_annualized, f.new_annualized, f.change_amount, f.change_pct
FROM fact_comp_change f
JOIN dim_employee de ON de.employee_sk = f.employee_sk
JOIN dim_date dd     ON dd.date_key = f.effective_date_key
WHERE f.change_type IN ('RAISE', 'CUT')
  AND dd.cal_year = 2026
ORDER BY dd.full_date, de.full_name;


-- Q2. Payroll cost by city and quarter (classic star join)
SELECT dof.city_name, dd.year_quarter,
       COUNT(DISTINCT f.emp_id) AS employees_paid,
       SUM(f.gross_pay)         AS gross,
       SUM(f.overtime_pay)      AS overtime,
       SUM(f.bonus_pay)         AS bonus
FROM fact_payroll f
JOIN dim_office dof ON dof.office_key = f.office_key
JOIN dim_date dd    ON dd.date_key = f.pay_date_key
GROUP BY dof.city_name, dd.year_quarter
ORDER BY dof.city_name, dd.year_quarter;


-- Q3. Company x month pivot from the AGGREGATE table (what a dashboard should hit)
SELECT year_month,
       SUM(total_gross) FILTER (WHERE company_name = 'Acme Corp')  AS acme_gross,
       SUM(total_gross) FILTER (WHERE company_name = 'Globex Inc') AS globex_gross,
       SUM(total_gross)                                            AS total_gross,
       SUM(paychecks)                                              AS paychecks
FROM agg_payroll_monthly
GROUP BY year_month
ORDER BY year_month;


-- Q4. POINT-IN-TIME: everyone's title and pay as of 2026-03-15 (SCD2 BETWEEN)
SELECT full_name, company_name, job_title, pay_type, pay_freq_code, annualized_comp
FROM dim_employee
WHERE DATE '2026-03-15' BETWEEN effective_from AND effective_to
  AND employee_sk <> -1
  AND hire_date <= DATE '2026-03-15'
  AND (termination_date IS NULL OR termination_date >= DATE '2026-03-15')
ORDER BY company_name, annualized_comp DESC;


-- Q5. Month-end headcount and payroll RUN-RATE (committed comp / 12) vs CASH actually paid.
--     Run-rate is smooth; cash spikes in quarter-end months because of quarterly payees.
WITH month_end AS (
    SELECT full_date, year_month FROM dim_date
    WHERE is_month_end AND full_date BETWEEN DATE '2026-01-01' AND DATE '2026-06-30'
),
run_rate AS (
    SELECT m.year_month, de.company_name,
           COUNT(*)                            AS headcount,
           CAST(ROUND(SUM(de.annualized_comp) / 12, 2) AS NUMERIC(14, 2)) AS monthly_run_rate
    FROM month_end m
    JOIN dim_employee de ON m.full_date BETWEEN de.effective_from AND de.effective_to
                        AND de.hire_date <= m.full_date
                        AND (de.termination_date IS NULL OR de.termination_date >= m.full_date)
    WHERE de.employee_sk <> -1
    GROUP BY m.year_month, de.company_name
),
cash AS (
    SELECT year_month, company_name, SUM(total_gross) AS cash_paid
    FROM agg_payroll_monthly
    GROUP BY year_month, company_name
)
SELECT r.year_month, r.company_name, r.headcount, r.monthly_run_rate, c.cash_paid,
       c.cash_paid - r.monthly_run_rate AS cash_minus_run_rate
FROM run_rate r
LEFT JOIN cash c ON c.year_month = r.year_month AND c.company_name = r.company_name
ORDER BY r.company_name, r.year_month;


-- Q6. Full version history of one employee (Irene: frequency change + raise)
SELECT employee_sk, emp_id, job_title, pay_type, pay_freq_code, annual_salary,
       effective_from, effective_to, is_current, change_reason
FROM dim_employee
WHERE emp_id = 202
ORDER BY effective_from;


-- Q7. DQ: RECONCILE warehouse vs source (expect diff = 0 on every line)
SELECT 'paychecks' AS measure,
       (SELECT COUNT(*) FROM paycheck)      AS source_value,
       (SELECT COUNT(*) FROM fact_payroll)  AS dw_value,
       (SELECT COUNT(*) FROM paycheck) - (SELECT COUNT(*) FROM fact_payroll) AS diff
UNION ALL
SELECT 'gross',
       (SELECT SUM(gross_pay) FROM paycheck),
       (SELECT SUM(gross_pay) FROM fact_payroll),
       (SELECT SUM(gross_pay) FROM paycheck) - (SELECT SUM(gross_pay) FROM fact_payroll)
UNION ALL
SELECT 'net',
       (SELECT SUM(net_pay) FROM paycheck),
       (SELECT SUM(net_pay) FROM fact_payroll),
       (SELECT SUM(net_pay) FROM paycheck) - (SELECT SUM(net_pay) FROM fact_payroll)
UNION ALL
SELECT 'agg gross',
       (SELECT SUM(gross_pay) FROM fact_payroll),
       (SELECT SUM(total_gross) FROM agg_payroll_monthly),
       (SELECT SUM(gross_pay) FROM fact_payroll) - (SELECT SUM(total_gross) FROM agg_payroll_monthly);


-- Q8. DQ: GRAIN check - more than one fact row per employee per pay period (expect 0 rows)
SELECT emp_id, period_end_key, COUNT(*) AS rows_at_grain
FROM fact_payroll
GROUP BY emp_id, period_end_key
HAVING COUNT(*) > 1;


-- Q9. DQ: SCD2 integrity - overlapping or gapped versions, or not exactly one current row (expect 0 rows)
WITH v AS (
    SELECT emp_id, employee_sk, effective_from, effective_to, is_current,
           LEAD(effective_from) OVER (PARTITION BY emp_id ORDER BY effective_from) AS next_from
    FROM dim_employee
    WHERE employee_sk <> -1
)
SELECT emp_id, employee_sk, effective_from, effective_to, next_from, 'overlap or gap' AS problem
FROM v
WHERE next_from IS NOT NULL AND next_from <> effective_to + 1
UNION ALL
SELECT emp_id, NULL, NULL, NULL, NULL, 'current rows = ' || CAST(COUNT(*) FILTER (WHERE is_current) AS VARCHAR)
FROM dim_employee
WHERE employee_sk <> -1
GROUP BY emp_id
HAVING COUNT(*) FILTER (WHERE is_current) <> 1;


-- Q10. DQ: facts that fell into the UNKNOWN member (late-arriving dimension). Expect 0.
SELECT COUNT(*) AS facts_with_unknown_employee
FROM fact_payroll
WHERE employee_sk = -1;


-- Q11. SCD2 INCREMENTAL LOAD demo (wrapped in BEGIN ... ROLLBACK so nothing changes)
--      New source event: Bob is promoted on 2026-07-01, 95,000 -> 105,000.
BEGIN;

-- (a) source system writes the change: close the open row, add the new one
UPDATE compensation SET effective_to = DATE '2026-06-30' WHERE emp_id = 102 AND effective_to IS NULL;
INSERT INTO compensation VALUES
    (22, 102, 'Senior Software Engineer', 'SALARY', 'MONTHLY', 105000, NULL, DATE '2026-07-01', NULL, 'PROMOTION');

-- (b) EXTRACT: source versions not yet in the dimension (CDC / watermark in real life)
CREATE TEMP TABLE stg_comp_changes AS
SELECT c.*
FROM compensation c
WHERE NOT EXISTS (SELECT 1 FROM dim_employee d
                  WHERE d.emp_id = c.emp_id AND d.effective_from = c.effective_from);

-- (c) EXPIRE the current version
UPDATE dim_employee
SET effective_to = s.effective_from - 1,
    is_current   = FALSE
FROM stg_comp_changes s
WHERE dim_employee.emp_id = s.emp_id
  AND dim_employee.is_current;

-- (d) INSERT the new version with the next surrogate key
INSERT INTO dim_employee
SELECT (SELECT MAX(employee_sk) FROM dim_employee) + ROW_NUMBER() OVER (ORDER BY s.emp_id, s.effective_from),
       v.emp_id, v.full_name, v.company_name, v.city_name, v.dept_name, v.manager_id,
       v.hire_date, v.termination_date,
       s.job_title, s.pay_type, s.pay_freq_code, s.annual_salary, s.hourly_rate,
       CASE s.pay_type WHEN 'SALARY' THEN s.annual_salary ELSE s.hourly_rate * 2080 END,
       s.change_reason,
       s.effective_from,
       COALESCE(s.effective_to, DATE '9999-12-31'),
       s.effective_to IS NULL
FROM stg_comp_changes s
JOIN v_employee_current v ON v.emp_id = s.emp_id;

-- Q12. Bob's history after the incremental SCD2 load: old row expired, new row current
SELECT employee_sk, full_name, job_title, annual_salary, effective_from, effective_to, is_current
FROM dim_employee
WHERE emp_id = 102
ORDER BY effective_from;

ROLLBACK;
