-- =====================================================================
-- DB DESIGN CASE STUDY: PAYROLL  (part 6 of 7: reports on the OLTP tables / v_paycheck)
--
-- REMEMBER:
--   * "Who got a raise since the previous pay?" -> LAG over the employee's paychecks,
--     but compare the ANNUALIZED BASE RATE, not gross pay:
--       - gross moves with hours (hourly), bonuses and overtime  -> false raises
--       - a frequency change (monthly -> semi-monthly) halves the check -> false pay cut
--       - hourly -> salary conversion: compare rate * 2080 with the salary
--   * The compensation table is the source of truth for raises (also shows changes not yet paid).
--   * Quarterly payees make month-by-month CASH totals spiky (March, June). Say "cash basis"
--     vs "accrual / run-rate" in an interview.
--   * Expected-vs-actual (anti-join) finds missing paychecks.
-- =====================================================================


-- Q1. TRAP - naive "raise" = gross went up vs the previous paycheck. Count the false positives.
WITH x AS (
    SELECT emp_id, emp_name, pay_date, gross_pay, annualized_rate,
           LAG(gross_pay)       OVER (PARTITION BY emp_id ORDER BY pay_date) AS prev_gross,
           LAG(annualized_rate) OVER (PARTITION BY emp_id ORDER BY pay_date) AS prev_rate
    FROM v_paycheck
)
SELECT emp_name,
       COUNT(*) FILTER (WHERE gross_pay > prev_gross)                         AS naive_raises,
       COUNT(*) FILTER (WHERE annualized_rate > prev_rate)                    AS real_raises,
       COUNT(*) FILTER (WHERE gross_pay < prev_gross AND annualized_rate > prev_rate) AS raise_looks_like_cut
FROM x
GROUP BY emp_id, emp_name
HAVING COUNT(*) FILTER (WHERE gross_pay > prev_gross) > 0
    OR COUNT(*) FILTER (WHERE annualized_rate > prev_rate) > 0
ORDER BY emp_name;


-- Q2. WHO GOT A RAISE FROM THE PREVIOUS PAY (correct: annualized base rate vs previous paycheck)
WITH x AS (
    SELECT emp_id, emp_name, company_name, pay_date, pay_type, pay_freq_code, annualized_rate,
           LAG(pay_date)        OVER w AS prev_pay_date,
           LAG(pay_type)        OVER w AS prev_pay_type,
           LAG(pay_freq_code)   OVER w AS prev_freq,
           LAG(annualized_rate) OVER w AS prev_rate
    FROM v_paycheck
    WINDOW w AS (PARTITION BY emp_id ORDER BY pay_date)
)
SELECT emp_name, company_name, prev_pay_date, pay_date AS first_raised_pay_date,
       prev_pay_type || '/' || prev_freq AS prev_basis,
       pay_type || '/' || pay_freq_code  AS new_basis,
       prev_rate, annualized_rate        AS new_rate,
       annualized_rate - prev_rate       AS raise_per_year,
       ROUND(100.0 * (annualized_rate - prev_rate) / prev_rate, 2) AS raise_pct
FROM x
WHERE annualized_rate > prev_rate
ORDER BY pay_date, emp_name;


-- Q3. Every rate change (raise OR cut) next to the per-check gross - shows why you annualize
WITH x AS (
    SELECT emp_id, emp_name, pay_date, pay_freq_code, gross_pay, annualized_rate,
           LAG(gross_pay)       OVER w AS prev_gross,
           LAG(annualized_rate) OVER w AS prev_rate
    FROM v_paycheck
    WINDOW w AS (PARTITION BY emp_id ORDER BY pay_date)
)
SELECT emp_name, pay_date, pay_freq_code,
       prev_gross, gross_pay, gross_pay - prev_gross AS gross_change,
       prev_rate, annualized_rate,
       ROUND(100.0 * (annualized_rate - prev_rate) / prev_rate, 2) AS rate_change_pct,
       CASE WHEN annualized_rate > prev_rate THEN 'RAISE' ELSE 'CUT' END AS change_type
FROM x
WHERE annualized_rate <> prev_rate
ORDER BY pay_date, emp_name;


-- Q4. Full raise history from the SOURCE OF TRUTH (compensation), incl. pre-2026 and title changes
WITH c AS (
    SELECT c.emp_id, c.job_title, c.change_reason, c.effective_from,
           CASE c.pay_type WHEN 'SALARY' THEN c.annual_salary ELSE c.hourly_rate * 2080 END AS annualized
    FROM compensation c
),
x AS (
    SELECT c.*,
           LAG(annualized) OVER (PARTITION BY emp_id ORDER BY effective_from) AS prev_annualized,
           LAG(job_title)  OVER (PARTITION BY emp_id ORDER BY effective_from) AS prev_title
    FROM c
)
SELECT e.first_name || ' ' || e.last_name AS emp_name, x.effective_from, x.change_reason,
       x.prev_title, x.job_title, x.prev_annualized, x.annualized,
       ROUND(100.0 * (x.annualized - x.prev_annualized) / x.prev_annualized, 2) AS pct,
       CASE WHEN x.prev_annualized IS NULL         THEN 'NEW HIRE'
            WHEN x.annualized > x.prev_annualized THEN 'RAISE'
            WHEN x.annualized < x.prev_annualized THEN 'CUT'
            ELSE 'NO CHANGE' END AS change_type
FROM x
JOIN employee e ON e.emp_id = x.emp_id
ORDER BY x.emp_id, x.effective_from;


-- Q5. Last raise per ACTIVE employee and who is due for a review (no raise in 12 months as of 2026-06-30)
WITH c AS (
    SELECT emp_id, effective_from,
           CASE pay_type WHEN 'SALARY' THEN annual_salary ELSE hourly_rate * 2080 END AS annualized
    FROM compensation
),
x AS (
    SELECT c.*, LAG(annualized) OVER (PARTITION BY emp_id ORDER BY effective_from) AS prev_annualized
    FROM c
),
last_raise AS (
    SELECT emp_id, MAX(effective_from) AS last_raise_date
    FROM x
    WHERE annualized > prev_annualized
    GROUP BY emp_id
)
SELECT e.first_name || ' ' || e.last_name AS emp_name, e.hire_date, lr.last_raise_date,
       DATE '2026-06-30' - COALESCE(lr.last_raise_date, e.hire_date) AS days_since_raise_or_hire,
       CASE WHEN DATE '2026-06-30' - COALESCE(lr.last_raise_date, e.hire_date) > 365
            THEN 'DUE FOR REVIEW' ELSE 'ok' END AS status
FROM employee e
LEFT JOIN last_raise lr ON lr.emp_id = e.emp_id
WHERE e.termination_date IS NULL
ORDER BY days_since_raise_or_hire DESC;


-- Q6. Total salary H1 2026 by COMPANY and CITY with subtotals (ROLLUP)
SELECT CASE WHEN GROUPING(company_name) = 1 THEN '** ALL **' ELSE company_name END AS company,
       CASE WHEN GROUPING(company_name) = 1 THEN ''
            WHEN GROUPING(city_name)    = 1 THEN '** company total **'
            ELSE city_name END                  AS city,
       COUNT(DISTINCT emp_id) AS employees,
       COUNT(*)               AS paychecks,
       SUM(gross_pay)         AS total_gross,
       SUM(tax_amount)        AS total_tax,
       SUM(net_pay)           AS total_net
FROM v_paycheck
GROUP BY ROLLUP (company_name, city_name)
ORDER BY GROUPING(company_name), company_name, GROUPING(city_name), city_name;


-- Q7. Payroll by CITY across companies (Austin hosts both companies)
SELECT city_name,
       COUNT(DISTINCT company_name)                                   AS companies,
       STRING_AGG(DISTINCT company_name, ', ' ORDER BY company_name)  AS company_list,
       COUNT(DISTINCT emp_id)                                         AS employees,
       SUM(gross_pay)                                                 AS total_gross,
       ROUND(100.0 * SUM(gross_pay) / SUM(SUM(gross_pay)) OVER (), 2) AS pct_of_total
FROM v_paycheck
GROUP BY city_name
ORDER BY total_gross DESC;


-- Q8. Monthly payroll pivoted by pay frequency (cash basis: by pay_date month). Note Mar/Jun quarterly spikes.
SELECT DATE_TRUNC('month', pay_date)::DATE                           AS pay_month,
       SUM(gross_pay) FILTER (WHERE pay_freq_code = 'MONTHLY')       AS monthly_gross,
       SUM(gross_pay) FILTER (WHERE pay_freq_code = 'SEMI_MONTHLY')  AS semi_monthly_gross,
       SUM(gross_pay) FILTER (WHERE pay_freq_code = 'QUARTERLY')     AS quarterly_gross,
       SUM(gross_pay)                                                AS total_gross,
       COUNT(*)                                                      AS paychecks
FROM v_paycheck
GROUP BY DATE_TRUNC('month', pay_date)::DATE
ORDER BY pay_month;
-- MySQL: SUM(CASE WHEN pay_freq_code = 'MONTHLY' THEN gross_pay END) instead of FILTER


-- Q9. Quarterly payroll per company with quarter-over-quarter growth
WITH q AS (
    SELECT company_name,
           EXTRACT(YEAR FROM pay_date)::INTEGER    AS yr,
           EXTRACT(QUARTER FROM pay_date)::INTEGER AS qtr,
           SUM(gross_pay)                          AS gross
    FROM v_paycheck
    GROUP BY company_name, EXTRACT(YEAR FROM pay_date)::INTEGER, EXTRACT(QUARTER FROM pay_date)::INTEGER
)
SELECT company_name, yr, qtr, gross,
       LAG(gross) OVER (PARTITION BY company_name ORDER BY yr, qtr) AS prev_qtr_gross,
       ROUND(100.0 * (gross - LAG(gross) OVER (PARTITION BY company_name ORDER BY yr, qtr))
             / LAG(gross) OVER (PARTITION BY company_name ORDER BY yr, qtr), 2) AS qoq_growth_pct
FROM q
ORDER BY company_name, yr, qtr;


-- Q10. Running YTD gross and net for one hourly employee (Dan) - pay stub "YTD" columns
SELECT pay_date, hourly_rate, regular_hours, overtime_hours, gross_pay, net_pay,
       SUM(gross_pay) OVER (PARTITION BY emp_id, EXTRACT(YEAR FROM pay_date) ORDER BY pay_date
                            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS ytd_gross,
       SUM(net_pay)   OVER (PARTITION BY emp_id, EXTRACT(YEAR FROM pay_date) ORDER BY pay_date
                            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS ytd_net
FROM v_paycheck
WHERE emp_id = 104
ORDER BY pay_date;


-- Q11. YTD earnings summary per employee (W-2 style), ranked inside each company
SELECT company_name, emp_name,
       COUNT(*)          AS paychecks,
       SUM(base_pay)     AS base,
       SUM(overtime_pay) AS overtime,
       SUM(bonus_pay)    AS bonus,
       SUM(gross_pay)    AS ytd_gross,
       SUM(tax_amount)   AS ytd_tax,
       SUM(net_pay)      AS ytd_net,
       DENSE_RANK() OVER (PARTITION BY company_name ORDER BY SUM(gross_pay) DESC) AS rank_in_company
FROM v_paycheck
WHERE pay_date BETWEEN DATE '2026-01-01' AND DATE '2026-12-31'
GROUP BY company_name, emp_id, emp_name
ORDER BY company_name, rank_in_company;


-- Q12. Overtime report for hourly employees
SELECT emp_name,
       SUM(regular_hours)                                         AS regular_hours,
       SUM(overtime_hours)                                        AS overtime_hours,
       SUM(overtime_pay)                                          AS overtime_pay,
       ROUND(100.0 * SUM(overtime_pay) / SUM(gross_pay), 2)       AS ot_pct_of_gross,
       COUNT(*) FILTER (WHERE overtime_hours >= 5)                AS periods_with_5plus_ot
FROM v_paycheck
WHERE pay_type = 'HOURLY'
GROUP BY emp_id, emp_name
ORDER BY overtime_hours DESC;


-- Q13. MISSING PAYCHECKS: expected (employed + on that pay calendar) vs actual (anti-join)
WITH expected AS (
    SELECT e.emp_id, e.first_name || ' ' || e.last_name AS emp_name, c.pay_type, p.period_id, p.period_start, p.period_end
    FROM employee e
    JOIN compensation c ON c.emp_id = e.emp_id
    JOIN pay_period   p ON p.freq_code = c.pay_freq_code
                       AND p.period_end BETWEEN c.effective_from AND COALESCE(c.effective_to, DATE '9999-12-31')
    WHERE e.hire_date <= p.period_end
      AND (e.termination_date IS NULL OR e.termination_date >= p.period_start)
)
SELECT ex.emp_name, ex.pay_type, ex.period_start, ex.period_end,
       CASE WHEN ex.pay_type = 'HOURLY' AND t.emp_id IS NULL THEN 'no approved timesheet'
            ELSE 'payroll did not run' END AS likely_reason
FROM expected ex
LEFT JOIN paycheck  pc ON pc.emp_id = ex.emp_id AND pc.period_id = ex.period_id
LEFT JOIN timesheet t  ON t.emp_id = ex.emp_id  AND t.period_id = ex.period_id
WHERE pc.paycheck_id IS NULL
ORDER BY ex.period_start, ex.emp_name;


-- Q14. Top earner per city by H1 gross (ties kept with DENSE_RANK)
SELECT city_name, emp_name, company_name, h1_gross
FROM (SELECT city_name, emp_name, company_name, SUM(gross_pay) AS h1_gross,
             DENSE_RANK() OVER (PARTITION BY city_name ORDER BY SUM(gross_pay) DESC) AS rnk
      FROM v_paycheck
      GROUP BY city_name, emp_id, emp_name, company_name) t
WHERE rnk = 1
ORDER BY city_name;


-- Q15. Current pay normalized across bases: annual, monthly equivalent and per-check amount
SELECT e.first_name || ' ' || e.last_name AS emp_name, c.job_title, c.pay_type, c.pay_freq_code,
       c.annual_salary, c.hourly_rate,
       CASE c.pay_type WHEN 'SALARY' THEN c.annual_salary ELSE c.hourly_rate * 2080 END                AS annualized,
       CAST(CASE c.pay_type WHEN 'SALARY' THEN c.annual_salary ELSE c.hourly_rate * 2080 END / 12
            AS NUMERIC(12, 2))                                                                          AS monthly_equivalent,
       CAST(CASE c.pay_type WHEN 'SALARY' THEN c.annual_salary ELSE c.hourly_rate * 2080 END / f.periods_per_year
            AS NUMERIC(12, 2))                                                                          AS per_check_base
FROM employee e
JOIN compensation  c ON c.emp_id = e.emp_id AND c.effective_to IS NULL
JOIN pay_frequency f ON f.freq_code = c.pay_freq_code
WHERE e.termination_date IS NULL
ORDER BY annualized DESC;


-- Q16. Raise budget impact in 2026 by company and quarter (comp changes with a higher rate)
WITH c AS (
    SELECT c.emp_id, c.effective_from,
           CASE c.pay_type WHEN 'SALARY' THEN c.annual_salary ELSE c.hourly_rate * 2080 END AS annualized
    FROM compensation c
),
x AS (
    SELECT c.*, LAG(annualized) OVER (PARTITION BY emp_id ORDER BY effective_from) AS prev_annualized
    FROM c
)
SELECT co.company_name,
       EXTRACT(QUARTER FROM x.effective_from)::INTEGER                               AS qtr,
       COUNT(*)                                                                      AS raises,
       ROUND(AVG(100.0 * (x.annualized - x.prev_annualized) / x.prev_annualized), 2) AS avg_raise_pct,
       SUM(x.annualized - x.prev_annualized)                                         AS added_annual_cost
FROM x
JOIN employee e ON e.emp_id = x.emp_id
JOIN office   o ON o.office_id = e.office_id
JOIN company co ON co.company_id = o.company_id
WHERE x.annualized > x.prev_annualized
  AND x.effective_from >= DATE '2026-01-01'
GROUP BY co.company_name, EXTRACT(QUARTER FROM x.effective_from)::INTEGER
ORDER BY co.company_name, qtr;


-- Q17. Employees who currently earn more than their manager (self-join, annualized so bases compare)
WITH cur AS (
    SELECT e.emp_id, e.first_name || ' ' || e.last_name AS emp_name, e.manager_id,
           CASE c.pay_type WHEN 'SALARY' THEN c.annual_salary ELSE c.hourly_rate * 2080 END AS annualized
    FROM employee e
    JOIN compensation c ON c.emp_id = e.emp_id AND c.effective_to IS NULL
    WHERE e.termination_date IS NULL
)
SELECT w.emp_name, w.annualized, m.emp_name AS manager, m.annualized AS manager_annualized
FROM cur w
JOIN cur m ON m.emp_id = w.manager_id
WHERE w.annualized > m.annualized;
