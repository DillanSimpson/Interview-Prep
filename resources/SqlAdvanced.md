# Oracle SQL Cheat Sheet — MasterCard SSE Interview Prep

---

## 1. Core SELECT Fundamentals

```sql
SELECT column1, column2, NVL(column3, 'default')
FROM   table_name t
WHERE  t.status = 'ACTIVE'
  AND  t.created_date >= ADD_MONTHS(SYSDATE, -12)
ORDER  BY t.created_date DESC
FETCH  FIRST 100 ROWS ONLY;   -- Oracle 12c+
```

**Key Oracle Differences vs ANSI / PostgreSQL**

| Feature | Oracle | PostgreSQL/MySQL |
|---|---|---|
| Limit rows | `FETCH FIRST N ROWS ONLY` | `LIMIT N` |
| String concat | `\|\|` or `CONCAT(a,b)` | `\|\|` / `CONCAT(a,b)` |
| Null-safe equality | `NVL(col, val)` | `COALESCE(col, val)` |
| Current timestamp | `SYSDATE` / `SYSTIMESTAMP` | `NOW()` |
| Dual table | `SELECT 1 FROM DUAL` | `SELECT 1` |
| Top-N (legacy) | `WHERE ROWNUM <= N` | — |
| Upsert | `MERGE INTO` | `INSERT ... ON CONFLICT` |
| Auto-increment | `SEQUENCE` / `GENERATED AS IDENTITY` | `SERIAL` / `GENERATED` |

---

## 2. JOINs

```sql
-- INNER JOIN
SELECT o.order_id, c.name
FROM   orders o
JOIN   customers c ON o.customer_id = c.customer_id;

-- LEFT OUTER JOIN (keep all left rows)
SELECT c.name, o.order_id
FROM   customers c
LEFT JOIN orders o ON c.customer_id = o.customer_id;

-- FULL OUTER JOIN
SELECT c.name, o.order_id
FROM   customers c
FULL OUTER JOIN orders o ON c.customer_id = o.customer_id;

-- SELF JOIN (employee-manager hierarchy)
SELECT e.name AS employee, m.name AS manager
FROM   employees e
JOIN   employees m ON e.manager_id = m.employee_id;

-- CROSS JOIN (Cartesian product — use with care)
SELECT a.val, b.val FROM set_a a CROSS JOIN set_b b;
```

---

## 3. Aggregation & Grouping

```sql
SELECT   department_id,
         COUNT(*)                                           AS total_emp,
         SUM(salary)                                        AS total_salary,
         AVG(salary)                                        AS avg_salary,
         MAX(salary)                                        AS max_salary,
         MIN(salary)                                        AS min_salary,
         LISTAGG(last_name, ', ')
             WITHIN GROUP (ORDER BY last_name)             AS names   -- Oracle string agg
FROM     employees
WHERE    status = 'ACTIVE'
GROUP BY department_id
HAVING   COUNT(*) > 5
ORDER BY total_salary DESC;
```

**ROLLUP / CUBE / GROUPING SETS** (subtotals & cross-tabs)

```sql
-- ROLLUP: subtotals + grand total
SELECT   region, product, SUM(sales)
FROM     sales_fact
GROUP BY ROLLUP(region, product);

-- CUBE: all combinations
GROUP BY CUBE(region, product, quarter);

-- GROUPING SETS: specific combinations only
GROUP BY GROUPING SETS ((region, product), (region), ());

-- GROUPING() distinguishes real NULLs from subtotal NULLs
SELECT region, GROUPING(region) AS is_subtotal, SUM(sales)
FROM   sales_fact
GROUP BY ROLLUP(region);
```

---

## 4. Subqueries

```sql
-- Scalar subquery (single value)
SELECT name, salary,
       (SELECT AVG(salary) FROM employees) AS avg_sal
FROM   employees;

-- Correlated subquery (references outer query)
SELECT e.name, e.salary
FROM   employees e
WHERE  e.salary > (SELECT AVG(salary)
                   FROM   employees e2
                   WHERE  e2.department_id = e.department_id);

-- EXISTS (preferred over IN for large sets — short-circuits)
SELECT c.customer_id
FROM   customers c
WHERE  EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id);

-- NOT EXISTS vs NOT IN (NOT IN fails silently if subquery returns NULL!)
SELECT c.customer_id
FROM   customers c
WHERE  NOT EXISTS (SELECT 1 FROM blacklist b WHERE b.customer_id = c.customer_id);
```

---

## 5. Common Table Expressions (CTEs / WITH Clause)

```sql
-- Basic CTE
WITH active_accounts AS (
    SELECT account_id, balance
    FROM   accounts
    WHERE  status = 'ACTIVE'
),
high_value AS (
    SELECT account_id, balance
    FROM   active_accounts
    WHERE  balance > 100000
)
SELECT * FROM high_value ORDER BY balance DESC;

-- Recursive CTE (Oracle 11g+): org hierarchy
WITH org_tree (employee_id, name, manager_id, lvl) AS (
    SELECT employee_id, name, manager_id, 1
    FROM   employees
    WHERE  manager_id IS NULL          -- root
    UNION ALL
    SELECT e.employee_id, e.name, e.manager_id, ot.lvl + 1
    FROM   employees e
    JOIN   org_tree ot ON e.manager_id = ot.employee_id
)
SELECT LPAD(' ', (lvl-1)*2) || name AS org_chart, lvl
FROM   org_tree
ORDER  SIBLINGS BY name;
```

**Oracle-native hierarchy (pre-12c / still widely used):**

```sql
SELECT LEVEL, LPAD(' ', LEVEL*2) || name AS org
FROM   employees
START WITH manager_id IS NULL
CONNECT BY PRIOR employee_id = manager_id
ORDER SIBLINGS BY name;
```

---

## 6. Window (Analytic) Functions

Critical for interviews — MasterCard uses these heavily for financial analytics.

```sql
SELECT
    employee_id,
    department_id,
    salary,

    -- Ranking
    ROW_NUMBER()  OVER (PARTITION BY department_id ORDER BY salary DESC) AS rn,
    RANK()        OVER (PARTITION BY department_id ORDER BY salary DESC) AS rnk,     -- gaps on ties
    DENSE_RANK()  OVER (PARTITION BY department_id ORDER BY salary DESC) AS d_rnk,   -- no gaps
    NTILE(4)      OVER (ORDER BY salary)                                  AS quartile,

    -- Offset
    LAG(salary,  1, 0) OVER (PARTITION BY department_id ORDER BY hire_date) AS prev_salary,
    LEAD(salary, 1, 0) OVER (PARTITION BY department_id ORDER BY hire_date) AS next_salary,
    FIRST_VALUE(salary) OVER (PARTITION BY department_id ORDER BY salary DESC) AS dept_max,
    LAST_VALUE(salary)  OVER (PARTITION BY department_id
                              ORDER BY salary DESC
                              ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS dept_min,

    -- Running totals / moving averages
    SUM(salary)  OVER (PARTITION BY department_id ORDER BY hire_date
                       ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_total,
    AVG(salary)  OVER (ORDER BY hire_date
                       ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)         AS moving_avg_3
FROM employees;
```

**Frame clause quick reference:**

| Frame | Meaning |
|---|---|
| `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` | Running total |
| `ROWS BETWEEN 2 PRECEDING AND CURRENT ROW` | 3-row moving window |
| `ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING` | Entire partition |
| `RANGE BETWEEN INTERVAL '7' DAY PRECEDING AND CURRENT ROW` | Last 7 days |

---

## 7. Oracle-Specific Functions

### String

```sql
SUBSTR('Hello World', 1, 5)            -- 'Hello' (1-indexed, unlike most languages)
INSTR('Hello', 'l')                    -- 3
LENGTH('Hello')                        -- 5
UPPER / LOWER / INITCAP('hello')       -- 'HELLO' / 'hello' / 'Hello'
TRIM('  hi  ')  LTRIM  RTRIM
REPLACE('abc', 'b', 'X')              -- 'aXc'
REGEXP_REPLACE('abc123', '[0-9]', '')  -- 'abc'
REGEXP_LIKE(phone, '^\d{10}$')         -- boolean match in WHERE clause
REGEXP_SUBSTR(email, '[^@]+', 1, 1)    -- local part of email
TO_CHAR(12345.67, '$99,999.99')        -- '$12,345.67'
```

### Date / Time

```sql
SYSDATE                               -- server date+time (no timezone)
SYSTIMESTAMP                          -- with timezone
TRUNC(SYSDATE, 'MM')                  -- first day of current month
TRUNC(SYSDATE, 'YYYY')               -- first day of current year
ADD_MONTHS(SYSDATE, 3)               -- 3 months forward
MONTHS_BETWEEN(date1, date2)         -- fractional months between two dates
LAST_DAY(SYSDATE)                    -- last day of current month
NEXT_DAY(SYSDATE, 'MONDAY')          -- next occurrence of Monday
TO_DATE('2024-01-15', 'YYYY-MM-DD')
TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS')
EXTRACT(YEAR FROM SYSDATE)           -- 2024
```

### NULL Handling

```sql
NVL(col, 'default')                   -- replace NULL with default
NVL2(col, 'not null val', 'null val') -- ternary on NULL check
NULLIF(a, b)                          -- returns NULL if a = b, else a
COALESCE(a, b, c)                     -- first non-NULL (ANSI standard, preferred)
```

### Conversion

```sql
CAST(salary AS VARCHAR2(20))
TO_NUMBER('12345.67', '99999.99')
TO_CHAR(sysdate, 'Day')              -- 'Monday   '
```

---

## 8. DML — INSERT / UPDATE / DELETE / MERGE

```sql
-- Multi-row INSERT (Oracle)
INSERT ALL
    INTO orders(id, amount) VALUES (1, 100)
    INTO orders(id, amount) VALUES (2, 200)
SELECT 1 FROM DUAL;

-- UPDATE with subquery
UPDATE accounts a
SET    a.balance = a.balance * 1.05
WHERE  a.account_id IN (SELECT account_id FROM vip_customers);

-- DELETE with EXISTS
DELETE FROM transactions t
WHERE NOT EXISTS (SELECT 1 FROM accounts a WHERE a.id = t.account_id);

-- MERGE (atomic upsert — very common in data pipelines and ETL)
MERGE INTO target_table tgt
USING source_table src
   ON (tgt.id = src.id)
WHEN MATCHED THEN
    UPDATE SET tgt.amount     = src.amount,
               tgt.updated_at = SYSDATE
    WHERE tgt.amount != src.amount        -- optional update filter
WHEN NOT MATCHED THEN
    INSERT (id, amount, created_at)
    VALUES (src.id, src.amount, SYSDATE);
```

---

## 9. DDL — Tables, Constraints, Sequences

```sql
-- Table creation
CREATE TABLE transactions (
    txn_id       NUMBER(18)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,  -- 12c+
    account_id   NUMBER(18)    NOT NULL,
    amount       NUMBER(15,2)  NOT NULL,
    currency     CHAR(3)       DEFAULT 'USD',
    status       VARCHAR2(20)  CHECK (status IN ('PENDING','SETTLED','FAILED')),
    txn_date     DATE          DEFAULT SYSDATE NOT NULL,
    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

-- Sequence (pre-12c identity or when you need more control)
CREATE SEQUENCE seq_txn_id START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
INSERT INTO transactions(txn_id, ...) VALUES (seq_txn_id.NEXTVAL, ...);
SELECT seq_txn_id.CURRVAL FROM DUAL;

-- Alter
ALTER TABLE transactions ADD     (merchant_id NUMBER(18));
ALTER TABLE transactions MODIFY  (currency VARCHAR2(5));
ALTER TABLE transactions DROP    COLUMN merchant_id;
ALTER TABLE transactions RENAME  COLUMN txn_date TO transaction_date;
```

---

## 10. Indexes

```sql
-- B-tree (default, best for high-cardinality equality/range queries)
CREATE INDEX idx_txn_account ON transactions(account_id);

-- Composite — column order matters (leftmost prefix rule)
CREATE INDEX idx_txn_status_date ON transactions(status, txn_date DESC);

-- Unique
CREATE UNIQUE INDEX idx_txn_ref ON transactions(reference_number);

-- Function-based (when WHERE clause transforms the column)
CREATE INDEX idx_upper_email ON customers(UPPER(email));
-- Enables: WHERE UPPER(email) = 'USER@EXAMPLE.COM'

-- Bitmap (low-cardinality in DW/reporting; BAD for OLTP — causes lock contention)
CREATE BITMAP INDEX idx_status_bmp ON transactions(status);

-- Invisible index (test performance impact without affecting optimizer)
CREATE INDEX idx_test ON transactions(amount) INVISIBLE;
ALTER INDEX idx_test VISIBLE;

-- Force index with hint
SELECT /*+ INDEX(t idx_txn_account) */ * FROM transactions t WHERE account_id = 123;
```

**When indexes hurt performance:**
- High DML tables (every INSERT/UPDATE/DELETE must update the index)
- Low-cardinality columns in OLTP (use bitmap only in DW)
- Small tables (full scan is cheaper than index lookup overhead)
- Leading column not used in WHERE clause

---

## 11. Query Optimization & Execution Plans

```sql
-- View execution plan (no execution — estimated stats)
EXPLAIN PLAN FOR
SELECT * FROM transactions WHERE account_id = 123;
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);

-- Real plan with actual runtime stats (executes the query)
SELECT /*+ GATHER_PLAN_STATISTICS */ * FROM transactions WHERE account_id = 123;
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));
```

**Reading the plan — key operations:**

| Operation | Good/Bad | Notes |
|---|---|---|
| `INDEX RANGE SCAN` | Good | Index used, selective predicate |
| `TABLE ACCESS BY INDEX ROWID` | Good | Row fetch via index lookup |
| `TABLE ACCESS FULL` | Depends | Bad on large tables; fine on small |
| `HASH JOIN` | Good (large sets) | Optimizer default for large joins |
| `NESTED LOOPS` | Good (small sets) | Best when inner table has selective index |
| `SORT MERGE JOIN` | Good (pre-sorted) | Range join predicates |
| `FILTER` | Warning | Correlated subquery evaluated per outer row |
| `CARTESIAN JOIN` | Bad | Missing join condition |

**Common optimizer hints:**

```sql
/*+ FULL(t) */           -- force full table scan
/*+ INDEX(t idx_name) */ -- force specific index
/*+ USE_NL(a b) */       -- nested loops join
/*+ USE_HASH(a b) */     -- hash join
/*+ PARALLEL(t 4) */     -- 4 parallel query slaves
/*+ NO_MERGE */          -- prevent view/subquery merging
/*+ LEADING(a b c) */    -- control join order
```

**Refresh stale optimizer statistics:**

```sql
EXEC DBMS_STATS.GATHER_TABLE_STATS('SCHEMA', 'TRANSACTIONS', cascade => TRUE);
```

---

## 12. Partitioning

Partitioning is critical at MasterCard scale — billions of transactions.

```sql
-- Range partitioning (most common for time-series data)
CREATE TABLE transactions (
    txn_id   NUMBER,
    txn_date DATE,
    amount   NUMBER
)
PARTITION BY RANGE (txn_date) (
    PARTITION p_2023   VALUES LESS THAN (DATE '2024-01-01'),
    PARTITION p_2024   VALUES LESS THAN (DATE '2025-01-01'),
    PARTITION p_future VALUES LESS THAN (MAXVALUE)
);

-- Interval partitioning (auto-creates monthly partitions — no manual ADD PARTITION)
PARTITION BY RANGE (txn_date)
INTERVAL (NUMTOYMINTERVAL(1, 'MONTH')) (
    PARTITION p_initial VALUES LESS THAN (DATE '2024-01-01')
);

-- Hash partitioning (even distribution, eliminate data skew)
PARTITION BY HASH (account_id) PARTITIONS 16;

-- List partitioning (categorical values)
PARTITION BY LIST (region) (
    PARTITION p_us    VALUES ('US', 'CA'),
    PARTITION p_eu    VALUES ('UK', 'DE', 'FR'),
    PARTITION p_other VALUES (DEFAULT)
);

-- Composite: range-hash (range by time, hash by ID within each range)
PARTITION BY RANGE (txn_date)
SUBPARTITION BY HASH (account_id) SUBPARTITIONS 8 (
    PARTITION p_2024 VALUES LESS THAN (DATE '2025-01-01')
);
```

**Partition pruning** — optimizer skips irrelevant partitions:
```sql
-- Optimizer only scans p_2024 partition
SELECT * FROM transactions WHERE txn_date >= DATE '2024-06-01';
```

**Partition maintenance:**
```sql
ALTER TABLE transactions DROP PARTITION p_2023;
ALTER TABLE transactions TRUNCATE PARTITION p_2024;
ALTER TABLE transactions SPLIT PARTITION p_future AT (DATE '2026-01-01')
    INTO (PARTITION p_2025, PARTITION p_future);

-- Zero-copy partition swap (archive data instantly)
ALTER TABLE transactions EXCHANGE PARTITION p_2023
    WITH TABLE transactions_2023_archive;
```

---

## 13. Views & Materialized Views

```sql
-- Standard view (virtual — always reads from base tables)
CREATE OR REPLACE VIEW vw_active_accounts AS
SELECT account_id, customer_id, balance
FROM   accounts
WHERE  status = 'ACTIVE';

-- Materialized View (physically stored snapshot for heavy aggregations)
CREATE MATERIALIZED VIEW mv_daily_totals
BUILD IMMEDIATE                  -- populate immediately (DEFERRED = on first refresh)
REFRESH FAST ON COMMIT           -- incremental refresh; requires MV log on base table
ENABLE QUERY REWRITE             -- optimizer can transparently rewrite queries to use this MV
AS
SELECT txn_date, SUM(amount) AS total, COUNT(*) AS cnt
FROM   transactions
GROUP BY txn_date;

-- MV Log (required for FAST refresh — tracks changes to base table)
CREATE MATERIALIZED VIEW LOG ON transactions
WITH ROWID, SEQUENCE (txn_date, amount) INCLUDING NEW VALUES;

-- Manual refresh
EXEC DBMS_MVIEW.REFRESH('MV_DAILY_TOTALS', 'C');  -- C=complete, F=fast
```

---

## 14. PL/SQL Essentials

### Blocks & Variables

```sql
DECLARE
    v_count    NUMBER;
    v_name     employees.last_name%TYPE;   -- anchored to column type
    v_rec      employees%ROWTYPE;           -- entire row as a record
    c_limit    CONSTANT NUMBER := 100;
BEGIN
    SELECT COUNT(*), last_name
    INTO   v_count, v_name
    FROM   employees
    WHERE  department_id = 10;

    IF v_count > c_limit THEN
        DBMS_OUTPUT.PUT_LINE('Over limit: ' || v_count);
    ELSIF v_count > 50 THEN
        DBMS_OUTPUT.PUT_LINE('Medium: ' || v_count);
    ELSE
        DBMS_OUTPUT.PUT_LINE('Under: ' || v_count);
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND  THEN DBMS_OUTPUT.PUT_LINE('No rows found');
    WHEN TOO_MANY_ROWS  THEN DBMS_OUTPUT.PUT_LINE('Multiple rows returned');
    WHEN OTHERS         THEN DBMS_OUTPUT.PUT_LINE('Error: ' || SQLERRM);
END;
/
```

### Cursors

```sql
-- Implicit cursor FOR loop (simplest, auto-managed)
FOR rec IN (SELECT * FROM employees WHERE dept_id = 10) LOOP
    DBMS_OUTPUT.PUT_LINE(rec.last_name);
END LOOP;

-- Explicit cursor (when you need FETCH control or FOR UPDATE)
DECLARE
    CURSOR c_emp IS SELECT employee_id, salary FROM employees;
BEGIN
    FOR r IN c_emp LOOP
        -- process r.employee_id, r.salary
        NULL;
    END LOOP;
END;

-- FOR UPDATE — acquires row-level lock
CURSOR c_lock IS SELECT * FROM accounts WHERE balance < 0 FOR UPDATE NOWAIT;
```

### Stored Procedure & Function

```sql
-- Procedure (no return value; uses OUT parameters)
CREATE OR REPLACE PROCEDURE update_account_status (
    p_account_id IN  accounts.account_id%TYPE,
    p_status     IN  VARCHAR2,
    p_rows_upd   OUT NUMBER
) AS
BEGIN
    UPDATE accounts SET status = p_status WHERE account_id = p_account_id;
    p_rows_upd := SQL%ROWCOUNT;
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN ROLLBACK; RAISE;
END update_account_status;
/

-- Function (returns a value; callable from SQL)
CREATE OR REPLACE FUNCTION get_account_balance (
    p_account_id IN NUMBER
) RETURN NUMBER AS
    v_balance NUMBER;
BEGIN
    SELECT balance INTO v_balance FROM accounts WHERE account_id = p_account_id;
    RETURN v_balance;
EXCEPTION
    WHEN NO_DATA_FOUND THEN RETURN NULL;
END;
/

-- Usage in SQL
SELECT get_account_balance(12345) FROM DUAL;
```

### Packages

```sql
-- Package spec (public interface)
CREATE OR REPLACE PACKAGE pkg_payment AS
    c_max_amount CONSTANT NUMBER := 1000000;
    PROCEDURE process_payment(p_txn_id IN NUMBER);
    FUNCTION  validate_account(p_id IN NUMBER) RETURN BOOLEAN;
END pkg_payment;
/

-- Package body (implementation — can have private procedures not in spec)
CREATE OR REPLACE PACKAGE BODY pkg_payment AS
    PROCEDURE process_payment(p_txn_id IN NUMBER) AS BEGIN NULL; END;
    FUNCTION  validate_account(p_id IN NUMBER) RETURN BOOLEAN AS BEGIN RETURN TRUE; END;
END pkg_payment;
/
```

### Triggers

```sql
-- Audit trigger
CREATE OR REPLACE TRIGGER trg_account_audit
AFTER UPDATE OF balance ON accounts
FOR EACH ROW
BEGIN
    INSERT INTO account_audit(account_id, old_balance, new_balance, changed_at, changed_by)
    VALUES (:OLD.account_id, :OLD.balance, :NEW.balance, SYSDATE, USER);
END;
/

-- Before insert — set defaults / sequence values
CREATE OR REPLACE TRIGGER trg_txn_before_insert
BEFORE INSERT ON transactions
FOR EACH ROW
BEGIN
    :NEW.txn_id     := seq_txn_id.NEXTVAL;
    :NEW.created_at := SYSDATE;
END;
/
```

### Bulk Operations (performance-critical)

```sql
-- BULK COLLECT + FORALL avoids row-by-row context switching (slow-by-slow)
DECLARE
    TYPE t_id_list IS TABLE OF employees.employee_id%TYPE;
    v_ids t_id_list;
BEGIN
    SELECT employee_id BULK COLLECT INTO v_ids
    FROM   employees
    WHERE  dept_id = 10;

    FORALL i IN v_ids.FIRST..v_ids.LAST
        UPDATE employees SET bonus = 500 WHERE employee_id = v_ids(i);

    COMMIT;
END;
```

---

## 15. Transactions & Locking

```sql
-- Savepoints for partial rollback
BEGIN
    SAVEPOINT sp_before_transfer;
    UPDATE accounts SET balance = balance - 500 WHERE id = 1;
    UPDATE accounts SET balance = balance + 500 WHERE id = 2;
    -- If something fails:
    ROLLBACK TO sp_before_transfer;
    -- On success:
    COMMIT;
END;

-- Row-level locking modes
SELECT * FROM accounts WHERE id = 1 FOR UPDATE;              -- exclusive, wait forever
SELECT * FROM accounts WHERE id = 1 FOR UPDATE NOWAIT;       -- fail immediately if locked
SELECT * FROM accounts WHERE id = 1 FOR UPDATE WAIT 5;       -- wait up to 5 seconds
SELECT * FROM accounts WHERE id = 1 FOR UPDATE SKIP LOCKED;  -- skip locked rows (queue processing)

-- View current locks
SELECT s.sid, s.serial#, l.type, l.lmode, o.object_name
FROM   v$lock l
JOIN   v$session  s ON l.sid = s.sid
JOIN   dba_objects o ON l.id1 = o.object_id
WHERE  l.block = 1;
```

**ACID in Oracle:**

| Property | Oracle Mechanism |
|---|---|
| Atomicity | Rollback segments / undo tablespace |
| Consistency | Constraints + triggers enforced at commit |
| Isolation | MVCC — readers never block writers |
| Durability | Redo log (WAL) flushed to disk before commit returns |

**Isolation levels:**
```sql
-- Default: READ COMMITTED (each statement sees committed data at statement start)
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- Serializable (consistent snapshot from transaction start; may get ORA-08177 on conflict)
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- Read-only (consistent snapshot, no DML allowed)
SET TRANSACTION READ ONLY;
```

---

## 16. Performance Tuning Patterns

### Top-N Queries

```sql
-- Modern (Oracle 12c+) — preferred
SELECT * FROM transactions ORDER BY amount DESC FETCH FIRST 10 ROWS ONLY;
SELECT * FROM transactions ORDER BY amount DESC FETCH FIRST 10 ROWS WITH TIES;

-- Keyset pagination (fast at any offset — recommended for large tables)
SELECT * FROM transactions
WHERE (txn_date, txn_id) < (:last_date, :last_id)
ORDER BY txn_date DESC, txn_id DESC
FETCH FIRST 20 ROWS ONLY;

-- Legacy Top-N (pre-12c)
SELECT * FROM (
    SELECT t.*, ROWNUM AS rn FROM (
        SELECT * FROM transactions ORDER BY amount DESC
    ) t WHERE ROWNUM <= 30
) WHERE rn > 20;
```

### Avoiding Common Anti-Patterns

```sql
-- BAD: function on indexed column prevents index use
WHERE TRUNC(txn_date) = TRUNC(SYSDATE)

-- GOOD: range predicate allows index range scan
WHERE txn_date >= TRUNC(SYSDATE) AND txn_date < TRUNC(SYSDATE) + 1

-- BAD: implicit type conversion breaks index (account_id is NUMBER, but passing string)
WHERE account_id = '12345'

-- GOOD: match data types
WHERE account_id = 12345

-- BAD: leading wildcard kills index — full scan required
WHERE name LIKE '%Smith%'

-- GOOD: trailing wildcard uses index
WHERE name LIKE 'Smith%'

-- BAD: NOT IN with nullable subquery (returns 0 rows if any NULL exists in subquery!)
WHERE id NOT IN (SELECT manager_id FROM employees)  -- manager_id can be NULL!

-- GOOD: NOT EXISTS handles NULLs safely
WHERE NOT EXISTS (SELECT 1 FROM employees e2 WHERE e2.manager_id = e.employee_id)
```

### Bind Variables (critical for shared pool / scalability)

```sql
-- BAD: literal values — each query is unique, floods shared pool with hard parses
WHERE account_id = 12345
WHERE account_id = 67890

-- GOOD: bind variables reuse parsed cursor
WHERE account_id = :p_account_id    -- SQL*Plus / SQL Developer
-- In JDBC: preparedStatement.setLong(1, accountId);  -- auto-uses bind variables
```

---

## 17. Advanced Patterns

### PIVOT / UNPIVOT

```sql
-- PIVOT: rows to columns
SELECT * FROM (
    SELECT quarter, region, sales FROM sales_data
)
PIVOT (SUM(sales) FOR quarter IN ('Q1' AS q1, 'Q2' AS q2, 'Q3' AS q3, 'Q4' AS q4));

-- UNPIVOT: columns to rows
SELECT region, quarter, sales FROM quarterly_sales
UNPIVOT (sales FOR quarter IN (q1, q2, q3, q4));
```

### Conditional Aggregation

```sql
SELECT
    department_id,
    COUNT(CASE WHEN status = 'ACTIVE'   THEN 1 END)                AS active_count,
    COUNT(CASE WHEN status = 'INACTIVE' THEN 1 END)                AS inactive_count,
    SUM(CASE WHEN salary > 100000       THEN salary ELSE 0 END)    AS high_earner_total
FROM employees
GROUP BY department_id;
```

### Multi-table Conditional INSERT

```sql
-- Conditional INSERT ALL — route rows to different tables based on data
INSERT ALL
    WHEN amount > 10000   THEN INTO large_transactions
    WHEN amount >= 1000   THEN INTO medium_transactions
    ELSE                       INTO small_transactions
SELECT txn_id, amount, txn_date FROM transactions_staging;
```

### Idempotent Upsert (Payment Safety)

```sql
-- Use MERGE to safely re-process without duplicates
MERGE INTO processed_payments tgt
USING (SELECT :idempotency_key AS ikey, :amount AS amt FROM DUAL) src
   ON (tgt.idempotency_key = src.ikey)
WHEN NOT MATCHED THEN
    INSERT (idempotency_key, amount, created_at)
    VALUES (src.ikey, src.amt, SYSDATE);
-- If key exists, nothing happens — natural idempotency
```

---

## 18. Data Dictionary Queries

```sql
-- Tables and columns
SELECT table_name FROM user_tables ORDER BY 1;
SELECT column_name, data_type, data_length, nullable
FROM   user_tab_columns WHERE table_name = 'TRANSACTIONS';

-- Indexes
SELECT index_name, index_type, uniqueness, status
FROM   user_indexes WHERE table_name = 'TRANSACTIONS';

SELECT index_name, column_name, column_position
FROM   user_ind_columns WHERE table_name = 'TRANSACTIONS' ORDER BY index_name, column_position;

-- Constraints
SELECT constraint_name, constraint_type, search_condition
FROM   user_constraints WHERE table_name = 'TRANSACTIONS';

-- Invalid objects (recompile after changes)
SELECT object_name, object_type FROM user_objects WHERE status = 'INVALID';
ALTER PROCEDURE my_proc COMPILE;

-- Session & query monitoring
SELECT sid, serial#, username, status, sql_id, event
FROM   v$session WHERE username IS NOT NULL;

-- Top SQL by elapsed time
SELECT sql_id, ROUND(elapsed_time/1e6, 2) AS elapsed_sec, executions, sql_text
FROM   v$sql ORDER BY elapsed_time DESC FETCH FIRST 10 ROWS ONLY;

-- Table size
SELECT segment_name, ROUND(bytes/1024/1024, 2) AS size_mb
FROM   user_segments WHERE segment_name = 'TRANSACTIONS';
```

---

## 19. Security

```sql
-- Object-level privileges
GRANT SELECT, INSERT ON transactions TO app_user;
REVOKE INSERT ON transactions FROM app_user;
GRANT SELECT ON transactions TO reporting_role WITH GRANT OPTION;

-- Row-Level Security (Virtual Private Database — VPD)
-- Transparently appends WHERE clause to every query
CREATE OR REPLACE FUNCTION sec_policy(schema_name IN VARCHAR2, table_name IN VARCHAR2)
RETURN VARCHAR2 AS
BEGIN
    RETURN 'region = SYS_CONTEXT(''USERENV'', ''CLIENT_INFO'')';
END;

EXEC DBMS_RLS.ADD_POLICY('HR', 'EMPLOYEES', 'region_policy', 'HR', 'sec_policy');

-- Unified Auditing (12c+)
AUDIT SELECT ON transactions BY ACCESS WHENEVER SUCCESSFUL;

-- Session context (useful for application-level security)
SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') AS db_user,
       SYS_CONTEXT('USERENV', 'IP_ADDRESS')   AS ip,
       SYS_CONTEXT('USERENV', 'OS_USER')      AS os_user
FROM   DUAL;
```

---

## 20. Payment System Patterns (MasterCard Specific)

### Event Sourcing / Append-Only Ledger

```sql
-- Instead of mutating balance, append immutable events
CREATE TABLE account_ledger (
    event_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id   NUMBER       NOT NULL,
    event_type   VARCHAR2(20) CHECK (event_type IN ('CREDIT','DEBIT','FEE','REVERSAL')),
    amount       NUMBER(15,2) NOT NULL,
    reference    VARCHAR2(100) UNIQUE,   -- idempotency key
    occurred_at  TIMESTAMP    DEFAULT SYSTIMESTAMP
);

-- Current balance = aggregate of all events
SELECT account_id,
       SUM(CASE WHEN event_type IN ('CREDIT','REVERSAL') THEN amount ELSE -amount END) AS balance
FROM   account_ledger
WHERE  account_id = :aid
GROUP BY account_id;
```

### Fraud Detection Query

```sql
-- High-risk transactions needing review in the last 24 hours
SELECT t.txn_id, t.amount, u.name, m.merchant_name,
       r.risk_score,
       COUNT(f.rule_id) AS matched_fraud_rules,
       LISTAGG(f.rule_name, ', ') WITHIN GROUP (ORDER BY f.rule_name) AS rules_matched
FROM   transactions t
JOIN   customers  u ON t.customer_id = u.customer_id
JOIN   merchants  m ON t.merchant_id = m.merchant_id
JOIN   risk_scores r ON t.txn_id = r.txn_id
LEFT JOIN fraud_rules_matched f ON t.txn_id = f.txn_id
WHERE  t.txn_date >= SYSDATE - 1
  AND  r.risk_score > 0.75
  AND  t.status = 'PENDING'
GROUP BY t.txn_id, t.amount, u.name, m.merchant_name, r.risk_score
HAVING COUNT(f.rule_id) > 2
ORDER BY r.risk_score DESC, t.txn_date DESC
FETCH FIRST 100 ROWS ONLY;
```

### Dispute / Chargeback Query

```sql
SELECT t.txn_id, t.amount, t.txn_date,
       c.name AS customer, m.merchant_name,
       d.dispute_reason, d.status AS dispute_status,
       d.filed_at, d.resolved_at
FROM   transactions t
JOIN   customers c ON t.customer_id = c.customer_id
JOIN   merchants  m ON t.merchant_id = m.merchant_id
LEFT JOIN disputes d ON t.txn_id = d.txn_id
WHERE  c.customer_id = :cust_id
  AND  t.txn_date BETWEEN :start_date AND :end_date
ORDER BY t.txn_date DESC;
```

---

## 21. Quick Interview Reference

### Must-Know Concepts

| Topic | Key Point |
|---|---|
| **ROWNUM vs ROW_NUMBER()** | ROWNUM assigned before ORDER BY runs; ROW_NUMBER() is a window function evaluated after ORDER BY |
| **ROWID** | Physical row address (fastest access) but not stable across exports/imports |
| **NULL arithmetic** | Any operation with NULL returns NULL; use NVL/COALESCE |
| **MVCC** | Readers never block writers; Oracle reconstructs old versions from undo tablespace using SCN |
| **Redo vs Undo** | Redo = replay for durability (crash recovery); Undo = rollback + consistent reads |
| **Full scan vs Index** | Optimizer picks index when < ~5-15% of rows qualify; depends on statistics |
| **Bind variables** | Prevents hard parse on every execution; critical for shared pool at scale |
| **Partition pruning** | Optimizer eliminates irrelevant partitions only when partition key is in WHERE clause |
| **MERGE** | Atomic upsert — prevents lost update race condition; essential for idempotent processing |
| **Bitmap vs B-tree** | Bitmap: low-cardinality DW (read-mostly); B-tree: high-cardinality OLTP |

### Common Interview Questions

**Q: What's the difference between DELETE, TRUNCATE, and DROP?**
- `DELETE`: DML, logged, rollbackable, fires row triggers, supports WHERE clause
- `TRUNCATE`: DDL, not rollbackable (implicit commit), resets high-water mark, no row triggers, much faster
- `DROP`: removes the table structure entirely from the schema

**Q: How do you find and delete duplicate rows?**
```sql
-- Find duplicates
SELECT email, COUNT(*) FROM customers GROUP BY email HAVING COUNT(*) > 1;

-- Delete keeping lowest ROWID (fastest Oracle approach)
DELETE FROM customers c
WHERE ROWID > (SELECT MIN(c2.ROWID) FROM customers c2 WHERE c2.email = c.email);
```

**Q: What is a covering index?**
An index containing all columns referenced in a query — the optimizer satisfies the query from the index alone, avoiding a table access entirely.
```sql
-- Index covers everything needed; no TABLE ACCESS BY INDEX ROWID
CREATE INDEX idx_covering ON transactions(account_id, txn_date, amount);
SELECT txn_date, amount FROM transactions WHERE account_id = 123;
```

**Q: Explain read consistency in Oracle.**
Oracle uses MVCC with SCN (System Change Number). When a query starts, it records the current SCN. If a block has been modified after that SCN, Oracle reconstructs the old version from the undo tablespace. Writers never block readers — a fundamental difference from lock-based databases.

**Q: How do you handle the N+1 query problem in Oracle?**
Use `JOIN` or `BULK COLLECT` instead of querying inside a loop. With ORMs, use eager-loading / fetch joins. In PL/SQL, use `FORALL` with `BULK COLLECT` to batch all DML in one context switch.

**Q: UNION vs UNION ALL?**
- `UNION`: eliminates duplicates (requires sort/hash) — slower
- `UNION ALL`: keeps all rows including duplicates — always prefer when duplicates are acceptable

**Q: Why does NOT IN fail with NULL values?**
```sql
-- manager_id can be NULL. NOT IN uses <> for each value.
-- NULL <> anything = NULL (not TRUE), so no rows qualify.
WHERE id NOT IN (SELECT manager_id FROM employees)  -- dangerous!

-- NOT EXISTS correctly handles NULLs
WHERE NOT EXISTS (SELECT 1 FROM employees e2 WHERE e2.manager_id = e.employee_id)
```

**Q: What causes a deadlock and how do you prevent it?**
Deadlock occurs when two sessions each hold a lock the other needs. Prevention: always acquire locks in the same order across all transactions. Detection: Oracle automatically detects deadlocks and rolls back one statement (ORA-00060); handle this exception and retry.
