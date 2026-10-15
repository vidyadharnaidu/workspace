-- =====================================================================
-- 13 CONCEPTS, INDEXES AND PERFORMANCE
-- Theory questions are answered in comments; the SQL below them is runnable.
-- =====================================================================


-- ---------------------------------------------------------------------
-- THEORY Q&A (short answers to memorize)
-- ---------------------------------------------------------------------
--
-- Q1. What is normalization? 1NF / 2NF / 3NF?
--   Organizing tables to remove duplicate data and update anomalies.
--   1NF: atomic values, no repeating groups (no "phone1, phone2" or comma lists in a column).
--   2NF: 1NF + every non-key column depends on the WHOLE primary key (matters for composite keys).
--   3NF: 2NF + no column depends on another non-key column (no transitive dependency:
--        employees(dept_id, dept_name) -> move dept_name to departments).
--   Denormalization = deliberately duplicating data to make reads faster (reporting, OLAP).
--
-- Q2. ACID?
--   Atomicity   - all statements in a transaction succeed or none do.
--   Consistency - constraints hold before and after.
--   Isolation   - concurrent transactions don't see each other's partial work.
--   Durability  - once committed, survives a crash (write-ahead log).
--
-- Q3. Isolation levels and anomalies (weakest -> strongest)
--   READ UNCOMMITTED - dirty reads possible (see uncommitted data)
--   READ COMMITTED   - no dirty reads; non-repeatable reads possible   (PostgreSQL default)
--   REPEATABLE READ  - same row reads the same; phantoms possible       (MySQL InnoDB default)
--   SERIALIZABLE     - as if transactions ran one at a time
--   Dirty read: read uncommitted data. Non-repeatable read: same row, different value on re-read.
--   Phantom read: same WHERE returns new rows on re-read.
--
-- Q4. What is an index? Clustered vs non-clustered?
--   A separate sorted structure (usually a B-tree) that finds rows without scanning the table.
--   Clustered: the table rows themselves are stored in index order (one per table; InnoDB primary key).
--   Non-clustered / secondary: a separate structure pointing to the rows (many per table).
--   Cost: faster reads, slower INSERT/UPDATE/DELETE, extra storage.
--
-- Q5. When is an index NOT used?
--   * function or math on the column:   WHERE LOWER(email) = ...   WHERE salary * 12 > ...
--   * leading wildcard:                 WHERE name LIKE '%son'
--   * implicit type cast:               WHERE varchar_col = 123
--   * low selectivity (e.g. gender) or small tables -> full scan is cheaper
--   * composite index (a, b) but filtering only on b (LEFTMOST PREFIX rule)
--   * OR across different columns (sometimes)
--   Fix: rewrite to be SARGABLE (range on the raw column) or create an expression index.
--
-- Q6. Composite index column order?
--   Equality columns first, then range/sort columns. Index (customer_id, order_date) serves
--   "WHERE customer_id = ? ORDER BY order_date" and "WHERE customer_id = ?" but not "WHERE order_date = ?".
--   Covering index: includes all columns the query needs -> index-only scan.
--
-- Q7. WHERE vs HAVING?  WHERE filters rows before GROUP BY; HAVING filters groups after aggregation.
--
-- Q8. UNION vs UNION ALL?  UNION removes duplicates (extra sort/hash); UNION ALL keeps all rows and is faster.
--
-- Q9. PRIMARY KEY vs UNIQUE?  PK: one per table, NOT NULL, identifies the row.
--     UNIQUE: many per table, allows NULL (one or many NULLs depending on the database).
--
-- Q10. DELETE vs TRUNCATE vs DROP?  see 12_dml_ddl_transactions header.
--
-- Q11. What is a deadlock and how do you avoid it?
--   Two transactions each hold a lock the other needs. The database kills one of them.
--   Avoid: always lock rows/tables in the SAME ORDER, keep transactions short, add proper indexes.
--
-- Q12. Optimistic vs pessimistic locking?
--   Pessimistic: lock when reading  -> SELECT ... FOR UPDATE (others wait).
--   Optimistic: no lock; UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?
--               0 rows updated = someone else changed it -> retry.
--
-- Q13. View vs materialized view?  View = saved query, always fresh, no storage.
--      Materialized view = stored result, fast to read, must be refreshed.
--
-- Q14. Stored procedure vs function?  Function returns a value and can be used inside SELECT.
--      Procedure is CALLed, can manage transactions, may return nothing.
--
-- Q15. OLTP vs OLAP?  OLTP: many small reads/writes, normalized (orders app).
--      OLAP: big analytical scans, denormalized star schema (fact table + dimension tables), columnar storage.
--
-- Q16. Partitioning vs sharding vs replication?
--   Partitioning: split one table into parts inside ONE database (e.g., orders by month).
--   Sharding: split data across MANY database servers (e.g., by customer_id hash).
--   Replication: copies of the same data on several servers (read replicas, failover).
--
-- Q17. SQL vs NoSQL?  SQL: schema, joins, ACID transactions, vertical scaling first.
--      NoSQL (document, key-value, wide-column, graph): flexible schema, horizontal scaling,
--      often eventual consistency. CAP theorem: during a network Partition choose Consistency or Availability.
--
-- Q18. How do you find a slow query and speed it up?
--   1. EXPLAIN / EXPLAIN ANALYZE -> look for sequential scans on big tables, bad row estimates, nested loops over many rows.
--   2. Add/adjust indexes for WHERE, JOIN and ORDER BY columns.
--   3. Make predicates sargable, select only needed columns (no SELECT *), avoid N+1 queries from the app.
--   4. Update statistics (ANALYZE), paginate with keyset (WHERE id > last_id) instead of big OFFSETs.
--
-- Q19. Correlated subquery vs JOIN performance?  Modern optimizers often rewrite them the same way;
--      EXISTS is usually best for "has any", JOIN + GROUP BY for aggregates. Check with EXPLAIN.
--
-- Q20. What does NULL mean in SQL?  Unknown/missing. Three-valued logic: TRUE, FALSE, UNKNOWN.
--      See 07_duplicates_nulls for the traps.
-- ---------------------------------------------------------------------


-- Q21. Create indexes (inside a rolled-back transaction so nothing is left behind)
BEGIN;

CREATE INDEX idx_orders_customer_date ON orders (customer_id, order_date);   -- composite
CREATE INDEX idx_employees_dept       ON employees (dept_id);                 -- foreign-key lookups
CREATE UNIQUE INDEX idx_contacts_id   ON contacts (id);                       -- unique index

-- Q22. Look at a query plan
EXPLAIN
SELECT order_id, order_date
FROM orders
WHERE customer_id = 1
ORDER BY order_date;

ROLLBACK;


-- Q23. NON-SARGABLE vs SARGABLE rewrites (same results, but only the second form can use an index)

-- non-sargable: function on the column
SELECT COUNT(*) AS orders_2025 FROM orders WHERE EXTRACT(YEAR FROM order_date) = 2025;
-- sargable
SELECT COUNT(*) AS orders_2025 FROM orders WHERE order_date >= DATE '2025-01-01' AND order_date < DATE '2026-01-01';

-- non-sargable: math on the column
SELECT name FROM employees WHERE salary * 12 > 1800000;
-- sargable
SELECT name FROM employees WHERE salary > 1800000 / 12;


-- Q24. KEYSET PAGINATION (fast for deep pages) vs OFFSET (reads and throws away skipped rows)
-- OFFSET version, page 3 of size 3:
SELECT emp_id, name FROM employees ORDER BY emp_id LIMIT 3 OFFSET 6;
-- Keyset version: remember the last id of the previous page (6) and continue after it
SELECT emp_id, name FROM employees WHERE emp_id > 6 ORDER BY emp_id LIMIT 3;


-- Q25. EXISTS vs COUNT for "does any row exist?" (EXISTS stops at the first match)
SELECT EXISTS (SELECT 1 FROM orders WHERE status = 'pending') AS has_pending;
