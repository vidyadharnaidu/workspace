# SQL Interview Questions

170 common SQL interview questions with solutions (150 runnable queries and 20 theory answers), grouped by topic. Every question runs against one small shared dataset, so you can check the answers by eye.

**Dialect:** PostgreSQL. The same files also run on DuckDB. Where MySQL differs, the file's header or a nearby comment gives the MySQL version.

## Folder structure

| Folder | What's inside |
|---|---|
| `00_setup/` | `01_schema.sql` (11 tables) and `02_sample_data.sql`. **Run these first.** |
| `01_basics/` | SELECT, WHERE, BETWEEN/IN/LIKE, IS NULL, ORDER BY, LIMIT/OFFSET pagination, CASE, COALESCE, logical execution order |
| `02_aggregation/` | GROUP BY, HAVING vs WHERE, COUNT(*) vs COUNT(col), conditional aggregation (FILTER / SUM CASE), % of total, median, ROLLUP |
| `03_joins/` | INNER/LEFT/FULL/CROSS, anti-join (never ordered, never sold), self-join (manager, earns more than manager), LEFT JOIN + WHERE trap, non-equi join |
| `04_subqueries/` | Scalar, correlated (above dept average), IN vs EXISTS, **NOT IN + NULL trap**, derived tables, ANY/ALL |
| `05_window_functions/` | `01`: ROW_NUMBER/RANK/DENSE_RANK, **2nd/Nth highest salary (4 ways)**, top N per department, latest row per group, NTILE, median. `02`: running balance, moving average, month-over-month growth with LAG, rising temperature trap, FIRST/LAST_VALUE frame trap, Pareto |
| `06_cte_recursive/` | CTEs, chained CTEs, **org chart levels and paths**, all subordinates, management chain, team size, fill missing dates, Fibonacci |
| `07_duplicates_nulls/` | Find/show/delete duplicates (LeetCode 182/196), case-insensitive duplicates, NULL logic, NULLIF safe division, `<>` dropping NULLs |
| `08_dates/` | Per month/quarter, last N days, tenure, days to first order, day of week, churn, period-over-period, index-friendly date filters |
| `09_gaps_islands/` | **Consecutive login streaks** (date minus ROW_NUMBER), 3+ consecutive days, longest streak, missing days/ids, id ranges |
| `10_pivot_strings/` | Pivot/unpivot with CASE, STRING_AGG, SPLIT_PART, capitalization, string functions, regex |
| `11_business_analytics/` | Top customers, best seller per category, AOV, cancellation rate, repeat rate, bought A and B / A but not B, frequently bought together, new vs active customers, retention, DAU |
| `12_dml_ddl_transactions/` | Constraints, INSERT…SELECT, **UPSERT**, bank-transfer transaction, UPDATE with CASE / JOIN, swap values, ALTER, views, RETURNING. All wrapped in `BEGIN … ROLLBACK`, so the data never changes |
| `13_concepts_performance/` | Theory Q&A (normalization, ACID, isolation levels, indexes, deadlocks, locking, OLTP vs OLAP, sharding, SQL vs NoSQL), EXPLAIN, index-friendly rewrites, keyset pagination |

Each file starts with a **REMEMBER** block listing the key tricks. Each question is marked `-- Qn.`, and many have a `TRICK` or `TRAP` comment.

## Sample data at a glance

- **Employees:** a 4-level org chart. Heidi earns more than her manager. Bob and Carol have the same salary. Oscar has no department and no email.
- **Departments:** Legal has no employees.
- **Customers:** Hooli never ordered.
- **Products:** Monitor was never sold.
- **Orders:** statuses are completed, cancelled and pending.
- **Logins:** includes a 3-day and a 4-day streak, plus one duplicate login.
- **Contacts:** includes duplicates that differ only by letter case.
- **Weather:** one day is missing, which makes the LAG trap visible.

## How to run

**Option 1: no database server (DuckDB)**

```bash
pip install duckdb
python db/run_with_duckdb.py                       # everything
python db/run_with_duckdb.py 05_window_functions   # setup + one topic
```

**Option 2: PostgreSQL**

```bash
createdb interview
psql -d interview -f db/00_setup/01_schema.sql -f db/00_setup/02_sample_data.sql
psql -d interview -f db/05_window_functions/01_ranking_nth_highest.sql
# or run everything in order:
find db -name '*.sql' | sort | xargs -I{} psql -d interview -f {}
```

Or with Docker:

```bash
docker run -d --name pg -e POSTGRES_PASSWORD=pg -p 5432:5432 postgres:16
```

## The 10 to know cold

1. 2nd / Nth highest salary: `DENSE_RANK() OVER (ORDER BY salary DESC)`, then `WHERE rnk = N`
2. Top N per group: `DENSE_RANK() OVER (PARTITION BY dept ORDER BY salary DESC) <= N`
3. Employees earning more than their manager: self-join on `e.manager_id = m.emp_id`
4. Rows with no match (never ordered): `LEFT JOIN … WHERE right.id IS NULL`, or `NOT EXISTS`
5. Duplicates: `GROUP BY key HAVING COUNT(*) > 1`; delete where `id NOT IN (SELECT MIN(id) … GROUP BY key)`
6. Running total: `SUM(x) OVER (PARTITION BY … ORDER BY date)`
7. Change from the previous row: `x - LAG(x) OVER (ORDER BY date)`
8. Consecutive days: `date - ROW_NUMBER()` is constant within a streak, so group by it
9. Hierarchy: `WITH RECURSIVE` anchor `UNION ALL` step
10. Pivot: `MAX(CASE WHEN col = 'X' THEN val END)` per output column
