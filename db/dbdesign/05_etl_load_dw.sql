-- =====================================================================
-- DB DESIGN CASE STUDY: PAYROLL  (part 5 of 7: ETL - load the star schema from OLTP)
--
-- REMEMBER (ELT order and patterns):
--   1. Load DIMENSIONS first, then FACTS (facts look up surrogate keys from dims).
--   2. dim_date is generated, not extracted.
--   3. SCD2 initial load: one dim row per compensation version, effective_to NULL -> '9999-12-31'.
--      (Incremental SCD2 "expire + insert" is demonstrated in 07_reports_dw.sql.)
--   4. Fact surrogate-key lookup is POINT-IN-TIME:
--        dim.emp_id = src.emp_id AND src.period_end BETWEEN dim.effective_from AND dim.effective_to
--      LEFT JOIN + COALESCE(..., -1) -> unknown member instead of silently losing rows.
--   5. IDEMPOTENT fact load: DELETE the load window, then INSERT it (= "overwrite partition"
--      in Spark / Hive / Delta). Re-running the job gives the same result, never duplicates.
-- =====================================================================


-- ---------- staging view: current employee attributes (Type 1 columns) ----------
CREATE VIEW v_employee_current AS
SELECT e.emp_id,
       e.first_name || ' ' || e.last_name AS full_name,
       e.office_id, e.dept_id, e.manager_id, e.hire_date, e.termination_date,
       co.company_name, ci.city_name, ci.state, o.office_name, d.dept_name
FROM employee e
JOIN office     o  ON o.office_id = e.office_id
JOIN company    co ON co.company_id = o.company_id
JOIN city       ci ON ci.city_id = o.city_id
JOIN department d  ON d.dept_id = e.dept_id;


-- ---------- dim_date (generated) ----------
INSERT INTO dim_date
SELECT (EXTRACT(YEAR FROM d) * 10000 + EXTRACT(MONTH FROM d) * 100 + EXTRACT(DAY FROM d))::INTEGER,
       d,
       EXTRACT(YEAR FROM d)::INTEGER,
       EXTRACT(QUARTER FROM d)::INTEGER,
       EXTRACT(MONTH FROM d)::INTEGER,
       EXTRACT(DAY FROM d)::INTEGER,
       (EXTRACT(YEAR FROM d) * 100 + EXTRACT(MONTH FROM d))::INTEGER,
       CAST(EXTRACT(YEAR FROM d)::INTEGER AS VARCHAR) || '-Q' || CAST(EXTRACT(QUARTER FROM d)::INTEGER AS VARCHAR),
       EXTRACT(DAY FROM d + 1) = 1,
       EXTRACT(DAY FROM d + 1) = 1 AND EXTRACT(MONTH FROM d) IN (3, 6, 9, 12)
FROM (SELECT g.d::DATE AS d
      FROM generate_series(DATE '2018-01-01', DATE '2026-12-31', INTERVAL '1 day') AS g(d)) s;


-- ---------- small Type 1 dimensions ----------
INSERT INTO dim_office
SELECT ROW_NUMBER() OVER (ORDER BY o.office_id), o.office_id, o.office_name, co.company_name, ci.city_name, ci.state
FROM office o
JOIN company co ON co.company_id = o.company_id
JOIN city    ci ON ci.city_id = o.city_id;

INSERT INTO dim_department
SELECT ROW_NUMBER() OVER (ORDER BY d.dept_id), d.dept_id, d.dept_name, co.company_name
FROM department d
JOIN company co ON co.company_id = d.company_id;

INSERT INTO dim_pay_frequency
SELECT ROW_NUMBER() OVER (ORDER BY freq_code), freq_code, description, periods_per_year
FROM pay_frequency;


-- ---------- dim_employee (SCD2 initial load) ----------
INSERT INTO dim_employee (employee_sk, emp_id, full_name, effective_from, effective_to, is_current)
VALUES (-1, -1, 'Unknown', DATE '1900-01-01', DATE '9999-12-31', TRUE);

INSERT INTO dim_employee
SELECT ROW_NUMBER() OVER (ORDER BY c.emp_id, c.effective_from) AS employee_sk,
       v.emp_id, v.full_name, v.company_name, v.city_name, v.dept_name, v.manager_id,
       v.hire_date, v.termination_date,
       c.job_title, c.pay_type, c.pay_freq_code, c.annual_salary, c.hourly_rate,
       CASE c.pay_type WHEN 'SALARY' THEN c.annual_salary ELSE c.hourly_rate * 2080 END,
       c.change_reason,
       c.effective_from,
       COALESCE(c.effective_to, DATE '9999-12-31'),
       c.effective_to IS NULL
FROM compensation c
JOIN v_employee_current v ON v.emp_id = c.emp_id;


-- ---------- fact_payroll (idempotent window load) ----------
DELETE FROM fact_payroll WHERE pay_date_key BETWEEN 20260101 AND 20260630;

INSERT INTO fact_payroll
SELECT pc.paycheck_id,
       COALESCE(de.employee_sk, -1),
       pc.emp_id,
       dof.office_key,
       ddp.dept_key,
       dpf.pay_freq_key,
       d_pay.date_key,
       d_start.date_key,
       d_end.date_key,
       pc.pay_type,
       pc.regular_hours, pc.overtime_hours,
       pc.base_pay, pc.overtime_pay, pc.bonus_pay, pc.gross_pay, pc.tax_amount, pc.net_pay,
       CASE pc.pay_type WHEN 'SALARY' THEN pc.annual_salary ELSE pc.hourly_rate * 2080 END
FROM paycheck pc
JOIN pay_period p             ON p.period_id = pc.period_id
JOIN employee e               ON e.emp_id = pc.emp_id
LEFT JOIN dim_employee de     ON de.emp_id = pc.emp_id
                             AND p.period_end BETWEEN de.effective_from AND de.effective_to
JOIN dim_office dof           ON dof.office_id = e.office_id
JOIN dim_department ddp       ON ddp.dept_id = e.dept_id
JOIN dim_pay_frequency dpf    ON dpf.freq_code = pc.pay_freq_code
JOIN dim_date d_pay           ON d_pay.full_date = pc.pay_date
JOIN dim_date d_start         ON d_start.full_date = p.period_start
JOIN dim_date d_end           ON d_end.full_date = p.period_end
WHERE pc.pay_date BETWEEN DATE '2026-01-01' AND DATE '2026-06-30';


-- ---------- fact_comp_change (derived with LAG over the SCD2 versions) ----------
INSERT INTO fact_comp_change
SELECT employee_sk, prev_employee_sk, emp_id, effective_date_key, change_reason,
       CASE WHEN prev_annualized IS NULL          THEN 'NEW_HIRE'
            WHEN new_annualized > prev_annualized THEN 'RAISE'
            WHEN new_annualized < prev_annualized THEN 'CUT'
            ELSE 'NO_CHANGE' END,
       prev_annualized,
       new_annualized,
       new_annualized - prev_annualized,
       ROUND(100.0 * (new_annualized - prev_annualized) / prev_annualized, 2)
FROM (SELECT de.employee_sk,
             LAG(de.employee_sk)     OVER w AS prev_employee_sk,
             de.emp_id,
             dd.date_key             AS effective_date_key,
             de.change_reason,
             LAG(de.annualized_comp) OVER w AS prev_annualized,
             de.annualized_comp      AS new_annualized
      FROM dim_employee de
      JOIN dim_date dd ON dd.full_date = de.effective_from
      WHERE de.employee_sk <> -1
      WINDOW w AS (PARTITION BY de.emp_id ORDER BY de.effective_from)) x;


-- ---------- agg_payroll_monthly (rebuilt from the fact) ----------
DELETE FROM agg_payroll_monthly;

INSERT INTO agg_payroll_monthly
SELECT dd.year_month, dof.company_name, dof.city_name, ddp.dept_name, dpf.freq_code,
       COUNT(DISTINCT f.emp_id), COUNT(*),
       SUM(f.gross_pay), SUM(f.overtime_pay), SUM(f.tax_amount), SUM(f.net_pay)
FROM fact_payroll f
JOIN dim_date dd           ON dd.date_key = f.pay_date_key
JOIN dim_office dof        ON dof.office_key = f.office_key
JOIN dim_department ddp    ON ddp.dept_key = f.dept_key
JOIN dim_pay_frequency dpf ON dpf.pay_freq_key = f.pay_freq_key
GROUP BY dd.year_month, dof.company_name, dof.city_name, ddp.dept_name, dpf.freq_code;


-- Q1. Row counts after the load (93 facts, 21 + 1 unknown employee versions)
SELECT 'dim_date' AS table_name, COUNT(*) AS row_count FROM dim_date
UNION ALL SELECT 'dim_office',          COUNT(*) FROM dim_office
UNION ALL SELECT 'dim_department',      COUNT(*) FROM dim_department
UNION ALL SELECT 'dim_pay_frequency',   COUNT(*) FROM dim_pay_frequency
UNION ALL SELECT 'dim_employee',        COUNT(*) FROM dim_employee
UNION ALL SELECT 'fact_payroll',        COUNT(*) FROM fact_payroll
UNION ALL SELECT 'fact_comp_change',    COUNT(*) FROM fact_comp_change
UNION ALL SELECT 'agg_payroll_monthly', COUNT(*) FROM agg_payroll_monthly;
