# Accertify Past Technical Questions

---

## Java: What's the difference between a List and a Set?

**List**
- Ordered collection — elements maintain insertion order.
- Allows duplicates.
- Access by index: `list.get(2)`.
- Common implementations: `ArrayList` (backed by dynamic array, O(1) random access), `LinkedList` (doubly-linked, O(1) insert/delete at ends).

**Set**
- Unordered collection (except `LinkedHashSet` and `TreeSet`).
- No duplicates — adding an element that already exists is a no-op.
- No index-based access.
- Common implementations: `HashSet` (O(1) add/contains), `LinkedHashSet` (insertion order preserved), `TreeSet` (sorted, O(log n)).

**When to use which:**
- Use a `List` when order matters or you need duplicates (e.g., a transaction history).
- Use a `Set` when you care about membership / uniqueness (e.g., a set of seen fraud rule IDs).

---

## Data Structures: List, Set, Map, HashMap, HashSet, Tree, Graph

### List
An ordered sequence. `ArrayList` uses a resizable array; `LinkedList` uses nodes with next/prev pointers. ArrayList is better for random access; LinkedList is better for frequent insertions/deletions at arbitrary positions.

### Set
A collection of unique elements. Backed by a hash table (`HashSet`) or a balanced BST (`TreeSet`). Primary use: membership testing and deduplication.

### Map
A key→value store. Keys are unique; values can repeat. `HashMap`, `TreeMap`, `LinkedHashMap` are common implementations. `TreeMap` keeps keys sorted.

### HashMap
Backed by an array of "buckets" (see next question for deep dive). O(1) average for get/put. Does not preserve insertion order. Allows one `null` key and multiple `null` values. Not thread-safe.

### HashSet
Implemented internally as a `HashMap<E, PRESENT>` — the set element is the key and a dummy constant is the value. All the same characteristics as HashMap (O(1), unordered, allows `null`).

### Tree (Binary Search Tree / Balanced BST)
Nodes where each node has at most two children, left < node < right. Searching, insertion, deletion are O(log n) on a balanced tree. `TreeMap` and `TreeSet` use a Red-Black Tree (self-balancing BST) to guarantee O(log n) worst case.

### Graph
A set of **vertices** (nodes) connected by **edges**. Can be:
- Directed or undirected
- Weighted or unweighted
- Cyclic or acyclic (DAG)

Represented as adjacency lists (space-efficient for sparse graphs) or adjacency matrices (fast edge lookup). Traversal algorithms: BFS (shortest path in unweighted), DFS (cycle detection, topological sort).

---

## Java: What is a HashMap? How is it implemented? What are the particularities?

### High-Level
A `HashMap` maps keys to values. Average O(1) get/put/remove.

### Internal Implementation
1. **Array of buckets** — internally a `Node<K,V>[] table`. Default initial capacity is **16**, load factor is **0.75**.
2. **Hashing** — when you call `put(key, value)`, Java calls `key.hashCode()`, then applies a secondary hash spread (`(h >>> 16) ^ h`) to reduce collisions from poor hash functions.
3. **Bucket index** — `index = hash & (capacity - 1)` (bitwise AND works because capacity is always a power of 2).
4. **Collision handling** — multiple entries that hash to the same bucket are stored as a linked list. Since **Java 8**, when a bucket's linked list exceeds **8 entries** it is converted to a **red-black tree** (O(log n) per bucket instead of O(n)).
5. **Resize/rehash** — when `size > capacity * loadFactor` (default: when 75% full), the table doubles in capacity and all entries are rehashed. This is O(n) but amortized O(1).

### Key Particularities
- **Not thread-safe** — use `ConcurrentHashMap` for concurrent access. `Collections.synchronizedMap` is an option but coarser.
- **Null keys allowed** — exactly one `null` key (mapped to bucket 0). `Hashtable` does not allow null keys.
- **No ordering guarantee** — iteration order is undefined (use `LinkedHashMap` for insertion order, `TreeMap` for sorted order).
- **equals/hashCode contract** — if you override `equals()` you MUST override `hashCode()`. Two objects that are equal must have the same hash code, or the map will break (you'd insert a key but never find it again).
- **Fail-fast iterators** — modifying the map while iterating (outside of the iterator's own `remove()`) throws `ConcurrentModificationException`.

---

## DNS: How does a domain name get resolved on the internet?

DNS (Domain Name System) translates human-readable names like `accertify.com` into IP addresses.

### Resolution Steps (recursive lookup)

1. **Browser / OS cache** — the OS checks its local DNS cache first. If found and TTL not expired, done.
2. **Recursive Resolver (your ISP or 8.8.8.8)** — if not cached, the OS sends a query to a configured recursive resolver (usually your router → ISP resolver). The resolver does the heavy lifting on your behalf.
3. **Root Name Servers** — the resolver asks a Root server: "who handles `.com`?" The root server responds with the address of the `.com` TLD name servers. (There are 13 root server addresses, operated by various organizations, globally anycast.)
4. **TLD Name Servers** — the resolver asks the `.com` TLD server: "who handles `accertify.com`?" It responds with Accertify's **authoritative name server**.
5. **Authoritative Name Server** — the resolver asks Accertify's authoritative NS: "what is the IP for `accertify.com`?" It returns the A record (IPv4) or AAAA record (IPv6).
6. **Response cached** — the resolver caches the result for the duration of the record's TTL and returns the IP to the client.
7. **TCP/IP connection** — the browser opens a TCP connection (then TLS handshake for HTTPS) to the resolved IP.

### Key Terms
- **A record** — maps hostname → IPv4 address.
- **CNAME** — canonical name alias (e.g., `www.accertify.com → accertify.com`).
- **TTL** — time-to-live; controls how long the answer can be cached.
- **Authoritative NS** — the source of truth for a domain's records.
- **Recursive resolver** — the middleman that walks the hierarchy for you.

---

## Concurrency and Multithreading

### Core Concepts

**Thread** — a lightweight unit of execution within a process. Threads share heap memory but each has its own stack.

**Runnable / Thread** — the two basic ways to define a task:
```java
new Thread(() -> doWork()).start();
ExecutorService pool = Executors.newFixedThreadPool(4);
pool.submit(() -> doWork());
```

**Race condition** — two threads read/write shared data concurrently without synchronization, producing unpredictable results.

**Synchronization (`synchronized`)** — only one thread can hold a monitor lock at a time:
```java
synchronized (this) { count++; }
```

**`volatile`** — ensures reads/writes go directly to main memory, preventing CPU cache inconsistencies. Does NOT make compound actions (check-then-act) atomic.

**Atomic classes** — `AtomicInteger`, `AtomicReference`, etc. Use CAS (compare-and-swap) hardware instructions for lock-free thread safety.

### `java.util.concurrent` Highlights
| Class | Purpose |
|---|---|
| `ExecutorService` | Thread pool management |
| `Future` / `CompletableFuture` | Async results, chaining |
| `ConcurrentHashMap` | Thread-safe map (segment locking) |
| `CountDownLatch` | Wait for N threads to finish |
| `Semaphore` | Limit concurrent access to a resource |
| `ReentrantLock` | More flexible than `synchronized` (tryLock, fairness) |

### Deadlock
Occurs when two threads each hold a lock the other needs. Prevention: always acquire locks in the same order; use `tryLock` with timeout.

### Common Interview Points
- **Happens-before** — the Java Memory Model guarantee that writes in one thread are visible to reads in another after a sync action.
- **Thread pool sizing** — CPU-bound tasks: `# cores`; I/O-bound tasks: `# cores * (1 + wait/compute ratio)`.
- **`synchronized` vs `Lock`** — `Lock` supports `tryLock`, interruptible waits, and multiple conditions; `synchronized` is simpler and sufficient for most cases.

---

## SQL: If you have a table, a column, and an index on that column — what does that imply?

An index is a separate data structure (typically a **B-tree**) maintained by the database alongside the table that maps column values → row locations (page/offset or primary key).

### What it implies:

**Read performance improves** for:
- Point lookups: `WHERE indexed_col = ?` — O(log n) vs O(n) full table scan.
- Range scans: `WHERE indexed_col BETWEEN ? AND ?` — B-tree traversal is efficient.
- `ORDER BY indexed_col` — data is already sorted in the index; no extra sort step.
- `JOIN` conditions on that column — the database can probe the index rather than scanning.

**Write performance degrades slightly** — every `INSERT`, `UPDATE`, or `DELETE` must also update the index. For write-heavy tables, too many indexes can become a bottleneck.

**Storage overhead** — the index consumes additional disk space, proportional to the number of rows and the column's cardinality.

**Low cardinality caveat** — an index on a boolean column (only 2 values) is often not used by the query planner because scanning half the table is still expensive; the optimizer may prefer a full table scan.

**Covering index** — if the query only needs columns present in the index, the DB can answer entirely from the index without touching the actual table rows (very fast).

### B-tree Index internals (briefly)
- Leaf nodes hold the column value + a pointer to the row.
- Internal nodes guide the search down the tree.
- Balanced: O(log n) for point lookups.
- Most RDBMS (PostgreSQL, MySQL InnoDB) default to B-tree indexes. Hash indexes exist for exact-match only.

---

## SQL: What is a JOIN?

A JOIN combines rows from two or more tables based on a related column between them.

### Types

**INNER JOIN** — returns only rows where the condition matches in **both** tables.
```sql
SELECT o.id, c.name
FROM orders o
INNER JOIN customers c ON o.customer_id = c.id;
```
Rows in `orders` with no matching customer are excluded.

**LEFT (OUTER) JOIN** — returns all rows from the left table; matched rows from the right table, or NULLs if no match.
```sql
SELECT c.name, o.id
FROM customers c
LEFT JOIN orders o ON c.id = o.customer_id;
-- Customers with no orders appear with NULL order id
```

**RIGHT (OUTER) JOIN** — mirror of LEFT JOIN; all rows from the right table.

**FULL OUTER JOIN** — all rows from both tables; NULLs where there's no match on either side.

**CROSS JOIN** — Cartesian product; every row in table A paired with every row in table B. Rarely used intentionally; O(n × m) rows.

**SELF JOIN** — a table joined to itself, useful for hierarchical data (e.g., employee → manager, both in the same `employees` table).

### How the DB executes a join (high level)
1. **Nested Loop Join** — for each row in the outer table, scan the inner table for matches. Good when inner table is small or indexed.
2. **Hash Join** — build a hash table from the smaller table; probe it for each row of the larger table. Good for large unsorted datasets.
3. **Merge Join** — both inputs are sorted on the join key; merge them in one pass. Efficient when data is already sorted or an index provides order.

### Key interview points
- Always join on **indexed** columns when possible — prevents full table scans on the inner table.
- `NULL = NULL` is false in SQL — a NULL on the join key will never match.
- Watch for fan-out: a one-to-many join inflates row counts; aggregate after joining or join on the right side.

---

## Insightful Questions to Ask Accertify

- How does the fraud scoring pipeline handle real-time vs. batch transaction evaluation, and what are the latency SLAs?
- What does the data volume look like day-to-day, and how do you approach scaling the platform during peak events (e.g., Black Friday)?
- How does the team balance model iteration speed with the stability requirements of a payments/fraud platform?
- What does the on-call / incident response process look like for the fraud platform?
- What does the onboarding process look like for a new engineer joining the team?
- What are the biggest technical challenges the team is tackling in the next 6–12 months?