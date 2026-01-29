# PostgreSQL Cheat Sheet - Senior Software Engineer

## 1. Core SQL Fundamentals (Know These Cold)

### SELECT with WHERE
```sql
-- Basic filtering
SELECT id, amount, status FROM transactions 
WHERE amount > 1000 AND status = 'PENDING';

-- IN operator (common for filtering by multiple values)
SELECT * FROM transactions 
WHERE status IN ('APPROVED', 'PENDING', 'REVIEW');

-- BETWEEN (inclusive on both ends)
SELECT * FROM transactions 
WHERE created_at BETWEEN '2024-01-01' AND '2024-12-31';

-- LIKE for pattern matching
SELECT * FROM users 
WHERE email LIKE '%@mastercard.com';
```

### Basic JOINs
```sql
-- INNER JOIN (only matching rows)
SELECT t.id, t.amount, u.name, u.email
FROM transactions t
INNER JOIN users u ON t.user_id = u.id;

-- LEFT JOIN (all from left table, matching from right)
SELECT u.id, u.name, COUNT(t.id) as transaction_count
FROM users u
LEFT JOIN transactions t ON u.id = t.user_id
GROUP BY u.id, u.name;

-- Multiple JOINs (common in fraud detection)
SELECT t.id, t.amount, u.name, r.rule_name, r.risk_score
FROM transactions t
INNER JOIN users u ON t.user_id = u.id
INNER JOIN rules_applied r ON t.id = r.transaction_id
WHERE r.risk_score > 0.7;

-- Self JOIN (join table to itself)
SELECT a.id as account_1, b.id as account_2, a.email
FROM accounts a
INNER JOIN accounts b ON a.email = b.email AND a.id != b.id;
```

### Aggregations & GROUP BY
```sql
-- COUNT, SUM, AVG, MAX, MIN
SELECT 
  user_id,
  COUNT(*) as transaction_count,
  SUM(amount) as total_spent,
  AVG(amount) as avg_transaction,
  MAX(amount) as largest_transaction
FROM transactions
WHERE status = 'COMPLETED'
GROUP BY user_id;

-- HAVING (filter groups, not individual rows)
SELECT 
  user_id,
  SUM(amount) as total_spent
FROM transactions
GROUP BY user_id
HAVING SUM(amount) > 10000;  -- Only users with >$10k spent

-- Multiple aggregations with different conditions
SELECT 
  user_id,
  COUNT(*) as all_transactions,
  COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved_count,
  SUM(CASE WHEN status = 'APPROVED' THEN amount ELSE 0 END) as approved_amount
FROM transactions
GROUP BY user_id;
```

---

## 2. Advanced SQL (Senior-Level)

### Common Table Expressions (CTEs / WITH clause)
```sql
-- CTE makes complex queries readable
WITH recent_transactions AS (
  SELECT user_id, amount, created_at
  FROM transactions
  WHERE created_at > NOW() - INTERVAL '7 days'
),
user_spending_stats AS (
  SELECT 
    user_id,
    COUNT(*) as tx_count,
    SUM(amount) as total_spent,
    AVG(amount) as avg_amount
  FROM recent_transactions
  GROUP BY user_id
)
SELECT 
  u.id, 
  u.name, 
  s.tx_count,
  s.total_spent,
  s.avg_amount
FROM users u
LEFT JOIN user_spending_stats s ON u.id = s.user_id
WHERE s.total_spent > 5000;

-- Recursive CTE (hierarchical data)
WITH RECURSIVE category_hierarchy AS (
  SELECT id, name, parent_id, 1 as level
  FROM categories
  WHERE parent_id IS NULL
  
  UNION ALL
  
  SELECT c.id, c.name, c.parent_id, ch.level + 1
  FROM categories c
  INNER JOIN category_hierarchy ch ON c.parent_id = ch.id
)
SELECT * FROM category_hierarchy;
```

### Window Functions (PARTITION BY, ROW_NUMBER, etc.)
```sql
-- ROW_NUMBER: assign sequential number within partition
SELECT 
  user_id,
  amount,
  created_at,
  ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC) as recency_rank
FROM transactions
WHERE ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC) <= 5;
-- Get 5 most recent transactions per user

-- RANK vs ROW_NUMBER (RANK has ties, ROW_NUMBER doesn't)
SELECT 
  user_id,
  amount,
  RANK() OVER (ORDER BY amount DESC) as rank,
  ROW_NUMBER() OVER (ORDER BY amount DESC) as row_num
FROM transactions;

-- LAG/LEAD: access previous/next row
SELECT 
  user_id,
  created_at,
  amount,
  LAG(amount) OVER (PARTITION BY user_id ORDER BY created_at) as previous_amount,
  amount - LAG(amount) OVER (PARTITION BY user_id ORDER BY created_at) as amount_change
FROM transactions;

-- Running total (cumulative sum)
SELECT 
  user_id,
  created_at,
  amount,
  SUM(amount) OVER (PARTITION BY user_id ORDER BY created_at) as running_total
FROM transactions
ORDER BY user_id, created_at;
```

### Subqueries
```sql
-- Subquery in WHERE clause
SELECT * FROM transactions
WHERE user_id IN (
  SELECT user_id FROM fraud_cases
  WHERE status = 'CONFIRMED'
);

-- Subquery with correlation (references outer query)
SELECT user_id, name
FROM users u
WHERE EXISTS (
  SELECT 1 FROM transactions t
  WHERE t.user_id = u.id
  AND t.amount > 10000
);

-- NOT EXISTS (efficient)
SELECT user_id
FROM users
WHERE NOT EXISTS (
  SELECT 1 FROM transactions
  WHERE transactions.user_id = users.id
);
```

---

## 3. PostgreSQL-Specific Features

### Data Types
```sql
-- JSON/JSONB (common for metadata)
CREATE TABLE transactions (
  id SERIAL PRIMARY KEY,
  metadata JSONB  -- Use JSONB, not JSON (more efficient)
);

-- Query JSON
SELECT 
  id,
  metadata->>'merchant_name' as merchant,
  metadata->'amount' as amount
FROM transactions;

-- UUID (primary keys in distributed systems)
CREATE TABLE accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT
);

-- ARRAY
CREATE TABLE fraud_patterns (
  pattern_ids INTEGER[],
  matched_rules TEXT[]
);

-- ENUM (for status fields)
CREATE TYPE transaction_status AS ENUM ('PENDING', 'APPROVED', 'DECLINED', 'REVIEW');
CREATE TABLE transactions (
  id SERIAL PRIMARY KEY,
  status transaction_status
);

-- TIMESTAMPTZ (timezone-aware, use this, not TIMESTAMP)
CREATE TABLE events (
  id SERIAL PRIMARY KEY,
  occurred_at TIMESTAMPTZ DEFAULT NOW()
);
```

### UPSERT (INSERT ... ON CONFLICT)
```sql
-- Update if exists, insert if not
INSERT INTO user_stats (user_id, total_spent, last_updated)
VALUES (123, 5000, NOW())
ON CONFLICT (user_id)
DO UPDATE SET 
  total_spent = user_stats.total_spent + 5000,
  last_updated = NOW();
```

### EXPLAIN and Query Planning
```sql
-- Understand query performance
EXPLAIN SELECT * FROM transactions WHERE user_id = 123;
-- Look for: Seq Scan (bad if big table), Index Scan (good)

EXPLAIN ANALYZE SELECT * FROM transactions WHERE user_id = 123;
-- Actually runs query and shows real execution time
```

---

## 4. Indexes (Critical for Performance)

### When to Index
```sql
-- Index on columns you filter by (WHERE clause)
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);

-- Composite index (multiple columns)
-- Good if you often filter by both
CREATE INDEX idx_tx_user_status ON transactions(user_id, status);

-- Partial index (only index rows that match condition)
CREATE INDEX idx_pending_transactions ON transactions(user_id) 
WHERE status = 'PENDING';
-- Much smaller, faster queries if most are completed

-- Index on JSON field
CREATE INDEX idx_metadata_merchant ON transactions USING GIN (metadata jsonb_path_ops);

-- UNIQUE index (prevents duplicates + improves performance)
CREATE UNIQUE INDEX idx_email_unique ON users(email);
```

### Index Rules for Interviews
- ✅ Add indexes on columns in WHERE clauses
- ✅ Add indexes on columns in JOIN conditions
- ✅ Add indexes on ORDER BY if result set is large
- ❌ Don't over-index (every index slows INSERT/UPDATE/DELETE)
- ❌ Don't index low-cardinality columns (status: APPROVED/DECLINED)
- ✅ Use EXPLAIN to verify index is actually used

---

## 5. Transactions & ACID

### Basic Transaction
```sql
BEGIN;
  UPDATE accounts SET balance = balance - 100 WHERE id = 1;
  UPDATE accounts SET balance = balance + 100 WHERE id = 2;
COMMIT;  -- Both succeed or both fail

-- Or rollback
BEGIN;
  UPDATE accounts SET balance = balance - 100 WHERE id = 1;
  UPDATE accounts SET balance = balance + 100 WHERE id = 2;
ROLLBACK;  -- Undo everything
```

### Isolation Levels (Payment Systems)
```sql
-- READ COMMITTED (default, good for most cases)
-- Problem: Dirty reads prevented, but phantom reads possible

-- REPEATABLE READ (safer for complex operations)
-- Problem: Phantom reads still possible

-- SERIALIZABLE (safest, slowest)
-- Guarantees complete isolation
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- For payment systems:
-- Use REPEATABLE READ or SERIALIZABLE when moving money
-- Use READ COMMITTED for reads/analytics
```

### Deadlock Awareness
```sql
-- In code, you need to handle this exception:
-- "ERROR: deadlock detected"

-- Bad pattern (deadlock):
Thread 1: UPDATE accounts SET balance = ... WHERE id = 1
          UPDATE accounts SET balance = ... WHERE id = 2

Thread 2: UPDATE accounts SET balance = ... WHERE id = 2
          UPDATE accounts SET balance = ... WHERE id = 1
-- Both waiting on each other

-- Good pattern (order matters):
Thread 1: UPDATE accounts SET balance = ... WHERE id = 1
          UPDATE accounts SET balance = ... WHERE id = 2

Thread 2: UPDATE accounts SET balance = ... WHERE id = 1
          UPDATE accounts SET balance = ... WHERE id = 2
-- Both update in same order, no deadlock
```

---

## 6. Payment System Patterns (MasterCard Specific)

### Idempotent Transactions
```sql
-- Track idempotency keys to prevent double-processing
CREATE TABLE transactions (
  id SERIAL PRIMARY KEY,
  idempotency_key UUID UNIQUE NOT NULL,
  user_id INTEGER,
  amount DECIMAL,
  status transaction_status,
  created_at TIMESTAMPTZ
);

-- On retry, check if idempotency_key exists
SELECT * FROM transactions WHERE idempotency_key = $1;
-- If exists, return existing transaction instead of processing again
```

### Event Sourcing (Audit Trail)
```sql
-- Instead of updating balance, append events
CREATE TABLE account_events (
  id SERIAL PRIMARY KEY,
  account_id INTEGER,
  event_type TEXT,  -- 'DEPOSIT', 'WITHDRAWAL', 'TRANSFER'
  amount DECIMAL,
  occurred_at TIMESTAMPTZ,
  metadata JSONB
);

-- Current balance = SUM of all events
SELECT account_id, SUM(amount) as current_balance
FROM account_events
WHERE event_type IN ('DEPOSIT', 'TRANSFER_IN')
GROUP BY account_id;
```

### Temporal Data (audit history)
```sql
-- Track when data changes
CREATE TABLE transactions_audit (
  id SERIAL PRIMARY KEY,
  transaction_id INTEGER,
  old_status transaction_status,
  new_status transaction_status,
  changed_at TIMESTAMPTZ,
  changed_by TEXT
);

-- Who changed what when?
SELECT * FROM transactions_audit
WHERE transaction_id = 123
ORDER BY changed_at DESC;
```

### Dispute Resolution (Common in payments)
```sql
-- Query to find related transactions for a dispute
SELECT 
  t.id,
  t.amount,
  t.created_at,
  u.name,
  d.dispute_reason,
  d.status as dispute_status
FROM transactions t
INNER JOIN users u ON t.user_id = u.id
LEFT JOIN disputes d ON t.id = d.transaction_id
WHERE u.id = $1
  AND t.created_at BETWEEN $2 AND $3
ORDER BY t.created_at DESC;
```

---

## 7. Common Gotchas & Interview Tips

### NULL Handling
```sql
-- NULL != anything (even NULL)
SELECT * FROM transactions WHERE amount = NULL;  -- Returns nothing!

-- Use IS NULL / IS NOT NULL
SELECT * FROM transactions WHERE amount IS NOT NULL;

-- COUNT(*) includes NULLs, COUNT(column) doesn't
SELECT COUNT(*) as total_rows, COUNT(resolved_at) as resolved_count
FROM transactions;  -- Counts unresolved transactions
```

### Type Casting
```sql
-- Explicit casting
SELECT CAST(amount AS INTEGER) FROM transactions;
SELECT amount::INTEGER FROM transactions;  -- PostgreSQL syntax

-- String to number
SELECT '123'::INTEGER + 1;  -- Returns 124
```

### Date/Time Operations
```sql
-- Current time
SELECT NOW();  -- Returns timestamp with timezone
SELECT CURRENT_DATE;  -- Just date

-- Intervals
SELECT NOW() - INTERVAL '7 days';  -- One week ago
SELECT NOW() - INTERVAL '1 month';
SELECT created_at + INTERVAL '30 days' as due_date FROM transactions;

-- Extract parts
SELECT 
  EXTRACT(YEAR FROM created_at) as year,
  EXTRACT(MONTH FROM created_at) as month,
  EXTRACT(DAY FROM created_at) as day
FROM transactions;
```

### String Operations
```sql
-- Concatenation
SELECT CONCAT(first_name, ' ', last_name) FROM users;
SELECT first_name || ' ' || last_name FROM users;

-- Length, uppercase, lowercase
SELECT LENGTH(email), UPPER(name), LOWER(email) FROM users;

-- Substring
SELECT SUBSTRING(email FROM 1 FOR 5) FROM users;  -- First 5 chars
```

---

## 8. Performance Tips (What Senior Engineers Know)

### N+1 Problem
```sql
-- BAD (N+1):
SELECT * FROM users;  -- 1 query
-- Then in code: for each user, query transactions
// for user in users:
//   SELECT * FROM transactions WHERE user_id = user.id;  // N queries

-- GOOD (single query with JOIN):
SELECT u.*, COUNT(t.id) as tx_count
FROM users u
LEFT JOIN transactions t ON u.id = t.user_id
GROUP BY u.id;
```

### Avoid SELECT *
```sql
-- BAD
SELECT * FROM transactions;  -- Pulls all columns, unnecessary data

-- GOOD
SELECT id, user_id, amount, status FROM transactions;
```

### Use LIMIT with OFFSET carefully
```sql
-- For pagination, LIMIT is efficient
SELECT * FROM transactions ORDER BY created_at DESC LIMIT 10;

-- But OFFSET is slow on large tables
SELECT * FROM transactions ORDER BY created_at DESC LIMIT 10 OFFSET 1000000;
-- ^ Has to read first 1M rows, then skip them

-- Better: use keyset pagination
SELECT * FROM transactions 
WHERE created_at < $last_row_created_at
ORDER BY created_at DESC 
LIMIT 10;
```

### Connection Pooling (Application Level)
```
PostgreSQL connections are expensive
Don't create new connection per request
Use PgBouncer or connection pool in your app
At MasterCard scale: critical for performance
```

---

## 9. SQL Query Writing Exercise

### Scenario: Fraud Detection Query
```sql
-- Find high-risk transactions in the last 24 hours
-- that need immediate review

SELECT 
  t.id,
  t.user_id,
  u.name,
  u.email,
  t.amount,
  t.merchant_id,
  m.name as merchant_name,
  t.created_at,
  r.risk_score,
  COUNT(DISTINCT f.id) as matching_fraud_rules
FROM transactions t
INNER JOIN users u ON t.user_id = u.id
INNER JOIN merchants m ON t.merchant_id = m.id
LEFT JOIN transaction_risk_scores r ON t.id = r.transaction_id
LEFT JOIN fraud_rules_matched f ON t.id = f.transaction_id
WHERE 
  t.created_at > NOW() - INTERVAL '24 hours'
  AND r.risk_score > 0.75
  AND t.status = 'PENDING'
GROUP BY 
  t.id, u.id, u.name, u.email, m.id, m.name, r.risk_score
HAVING COUNT(DISTINCT f.id) > 2
ORDER BY r.risk_score DESC, t.created_at DESC
LIMIT 100;
```

**What this demonstrates:**
- Multiple JOINs (user, merchant, risk scores, fraud rules)
- LEFT JOINs for optional data
- WHERE filtering
- GROUP BY with HAVING
- Aggregation (COUNT)
- ORDER BY with priority
- LIMIT for pagination

---

## 10. Interview Questions They Might Ask

**"How would you optimize a slow query?"**
- Run EXPLAIN ANALYZE to see what's slow
- Add indexes on WHERE/JOIN columns
- Check for N+1 problems
- Reduce columns selected if not needed
- Check for missing JOINs (subquery instead of join)

**"Explain the difference between INNER JOIN and LEFT JOIN"**
- INNER: only rows that match both tables
- LEFT: all rows from left table, matching from right (NULLs if no match)

**"How do you handle duplicate data?"**
- Use DISTINCT keyword
- Use GROUP BY to deduplicate
- Use UNIQUE indexes to prevent them

**"What's an idempotency key and why do we need it?"**
- UUID that identifies a request
- If same request retried, returns same result
- Prevents duplicate transactions in payments

**"How do you think about indexing?"**
- Index WHERE columns, JOIN columns, ORDER BY columns
- Don't over-index (slows writes)
- Use EXPLAIN to verify index is used
- Monitor index bloat in production

---

## 11. Quick Reference Commands

```sql
-- Schema inspection
\d transactions;  -- Describe table
\d+ transactions;  -- More detail
\di;  -- List indexes

-- Performance
EXPLAIN ANALYZE SELECT ...;
VACUUM ANALYZE;  -- Cleanup and update statistics

-- Transactions
BEGIN; COMMIT; ROLLBACK;

-- Locks (for debugging deadlocks)
SELECT * FROM pg_locks;
SELECT * FROM pg_stat_activity;
```

---

## Key Takeaways for Interview

✅ Comfortable writing JOINs (inner, left, multiple)
✅ Comfortable with GROUP BY / HAVING / aggregations
✅ Know window functions (RANK, ROW_NUMBER, LAG/LEAD)
✅ Understand CTEs (WITH clause) for readability
✅ Know basic indexing strategy
✅ Understand transactions and isolation levels
✅ Think about performance (EXPLAIN, N+1, indexes)
✅ Know PostgreSQL-specific features (JSON, UUID, UPSERT)
✅ Can write payment/fraud detection queries

**Practice:** Write queries to answer common questions:
- Top 10 customers by spending
- Monthly transaction trends
- Customers with high fraud risk
- Transaction reconciliation
- Duplicate detection