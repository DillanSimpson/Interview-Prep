# **Performance Engineering Interview Cheat Sheet**

---

## 1) Testing Types

| Type | Purpose | Key Metric |
|------|---------|------------|
| **Load Testing** | Verify behavior under expected load | Throughput, latency |
| **Stress Testing** | Find breaking point (beyond expected load) | Max TPS, error rate |
| **Soak / Endurance** | Detect degradation over time (hours/days) | Memory growth, GC frequency |
| **Spike Testing** | Sudden burst of traffic | Recovery time, error spikes |
| **Volume Testing** | Large data sets impact on DB/IO | Query time, disk IO |
| **Scalability Testing** | Measure how system scales horizontally/vertically | Requests/sec per node |
| **Smoke Testing** | Quick sanity check before heavy tests | Basic pass/fail |
| **Baseline Testing** | Establish a reference for comparison | All metrics — captured first |

### Soak Testing — Key Points
- Run for **8–72 hours** at ~75–80% expected load.
- Looking for: **memory leaks**, connection pool exhaustion, unclosed streams, log file bloat, GC pressure accumulation.
- Metrics to watch: heap growth over time, GC pause frequency/duration, thread count drift, DB connection pool usage.
- Common causes of soak failures: `static` collections growing unbounded, missing `finally`/`try-with-resources`, Hibernate session not closed, thread-local leaks.

---

## 2) JMeter

### Core Concepts
- **Test Plan** → Thread Groups → Samplers → Listeners.
- **Thread Group:** Simulates users — configure number of threads, ramp-up time, loop count.
- **Samplers:** HTTP Request, JDBC Request, JMS, etc.
- **Listeners:** View Results Tree, Aggregate Report, Summary Report, Response Time Graph.
- **Assertions:** Response Assertion (status/body), Duration Assertion (SLA enforcement).
- **Timers:** Constant Timer, Gaussian Random Timer — simulate think time between requests.
- **CSV Data Set Config:** Parameterize requests with user data.

### Key Metrics from JMeter
| Metric | What it Means |
|--------|--------------|
| **Throughput** | Requests/second the server handles |
| **Average / P90 / P99 / P999** | Latency distribution — P99 is critical for SLAs |
| **Error %** | % of failed requests |
| **Latency** | Time to first byte |
| **Connect Time** | TCP handshake time |

### Tips
- Always run JMeter in **non-GUI mode** for actual load tests: `jmeter -n -t test.jmx -l results.jtl`
- Use **Distributed Testing** (controller + remote agents) for high load.
- Set `Think Time` to simulate real users — pure hammering inflates results.
- Enable **GZip compression** and **Keep-Alive** to match production conditions.
- Warm up the JVM before recording results (first 30–60 seconds = discard).

---

## 3) JProfiler

### Core Views
| View | Used For |
|------|---------|
| **Heap Walker** | Snapshot of objects on heap — find memory leaks |
| **CPU Views** | Hot spots — method-level time breakdown |
| **Thread View** | Thread states, deadlocks, blocked threads |
| **Monitor & Locks** | Contention — which locks are blocking threads |
| **JDBC / JPA Probes** | Slow queries, N+1 detection, connection pool hits |
| **Memory Allocation** | Where objects are being allocated (call tree) |

### Workflow for Memory Leak
1. Take **heap snapshot** after steady state.
2. Run load for X minutes.
3. Take **second snapshot**.
4. Use **Heap Comparison** — look for classes growing unboundedly.
5. Drill into **References** to find what is holding the object alive (GC root path).

### Workflow for CPU Bottleneck
1. Start **CPU recording** (sampling or instrumentation).
2. Run load.
3. Stop recording → open **Hot Spots** view.
4. Find methods with highest **self time** (not cumulative).
5. Trace call tree upward to find entry point.

### Sampling vs Instrumentation
- **Sampling:** Low overhead, good for production-like profiling. Less precise.
- **Instrumentation:** Injects bytecode — exact call counts/timing but higher overhead. Use in dev/staging only.

---

## 4) Benchmarking

### JMH (Java Microbenchmark Harness) — Gold Standard
```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
@State(Scope.Thread)
public class MyBenchmark {

    @Benchmark
    public String testStringConcat() {
        return "Hello" + " " + "World";  // naive
    }

    @Benchmark
    public String testStringBuilder() {
        return new StringBuilder().append("Hello").append(" ").append("World").toString();
    }
}
```

### Benchmark Principles
- **Warm up the JVM** — JIT compilation skews early results. Always use warmup iterations.
- **Avoid dead code elimination** — JIT may optimize away results never used. Use `Blackhole.consume()`.
- **Measure P50/P95/P99** not just average — averages hide tail latency.
- **Isolate what you measure** — DB, network, and filesystem IO should be mocked or stubbed in microbenchmarks.
- **Repeat and compare** — single runs are noise. Use statistical significance.
- **Baseline first** — always measure current behavior before optimizing.
- **Don't prematurely optimize** — profile first, optimize the hotspot, measure again.

### System-Level Benchmarking
- **wrk / ab / hey** — HTTP load generators for quick throughput checks.
- **async-profiler** — Low overhead, flame graphs, CPU + allocation profiling.
- **Flame Graphs** — Wide bars = time spent. Self time at top = actual bottleneck.
- **perf / VisualVM** — JVM thread and heap analysis.

---

## 5) Garbage Collection (GC)

### Heap Structure
```
Heap
├── Young Generation (short-lived objects)
│   ├── Eden Space       ← new objects allocated here
│   ├── Survivor S0      ← objects surviving 1st GC
│   └── Survivor S1      ← objects surviving 2nd GC
└── Old Generation (Tenured) ← long-lived objects promoted from Young Gen
```

- **Minor GC:** Cleans Young Gen — fast, frequent, usually < 10ms.
- **Major / Full GC:** Cleans Old Gen (and sometimes Metaspace) — slow, causes "Stop The World" (STW) pauses.
- **Metaspace (Java 8+):** Stores class metadata — not in heap, uses native memory.

### GC Algorithms
| GC | Use Case | Pause Behavior |
|----|---------|----------------|
| **Serial GC** | Single-threaded, small apps | Long STW pauses |
| **Parallel GC** | Throughput-focused (batch jobs) | STW but multi-threaded |
| **G1 GC** (default Java 9+) | Balanced throughput + latency | Predictable pause targets |
| **ZGC** | Ultra-low latency (< 1ms) | Concurrent, minimal STW |
| **Shenandoah** | Low latency, OpenJDK | Concurrent compaction |

### Key GC Flags
```bash
-Xms512m -Xmx2g                  # initial/max heap
-XX:+UseG1GC                     # enable G1
-XX:MaxGCPauseMillis=200         # G1 pause target
-XX:+PrintGCDetails              # verbose GC logs
-XX:+PrintGCDateStamps           # timestamp GC events
-Xlog:gc*:file=gc.log            # Java 9+ logging
-XX:+HeapDumpOnOutOfMemoryError  # capture OOM heap dump
-XX:HeapDumpPath=/tmp/heapdump.hprof
```

### GC Tuning Signals
- **High GC frequency:** Objects promoted too fast — increase Young Gen or tune allocation rate.
- **Long Full GC pauses:** Old Gen filling up — check for memory leaks or increase heap.
- **GC overhead limit exceeded:** > 98% time in GC — severe leak or under-sized heap.
- **Metaspace OOM:** Class loader leak (common in hot-deploy or OSGi apps).

---

## 6) Heap vs Stack

| Aspect | Stack | Heap |
|--------|-------|------|
| Stores | Method frames, local variables, references | Objects, instance variables |
| Size | Small (512KB–1MB per thread default) | Large (configured with -Xmx) |
| Lifecycle | Tied to method call — auto freed on return | Managed by GC |
| Thread Safety | Thread-private | Shared across threads |
| Access Speed | Very fast (LIFO, CPU cache-friendly) | Slower (pointer-based lookup) |
| Errors | `StackOverflowError` (deep recursion) | `OutOfMemoryError` (heap exhaustion) |

### Memory Areas (JVM)
- **Stack:** One per thread. Stores frames (local vars, operand stack, return address).
- **Heap:** All objects. Divided into Young/Old Gen.
- **Metaspace:** Class definitions, method metadata (native memory).
- **Code Cache:** JIT-compiled native code.
- **Direct Memory:** Off-heap buffers (NIO `ByteBuffer.allocateDirect`). Not GC'd — must be explicitly released.

### Common OOM Scenarios
| Error | Cause |
|-------|-------|
| `java.lang.OutOfMemoryError: Java heap space` | Too many live objects, memory leak |
| `java.lang.OutOfMemoryError: Metaspace` | Class loader leak |
| `java.lang.OutOfMemoryError: Direct buffer memory` | Off-heap NIO leak |
| `java.lang.StackOverflowError` | Infinite/deep recursion |

---

## 7) Spring Boot — Performance Code Review Checklist

### Database / JPA
- [ ] **N+1 Query Problem** — `@OneToMany` without `fetch = FetchType.LAZY` or missing `JOIN FETCH` in JPQL. Use `@EntityGraph` or batch fetching.
- [ ] **Missing indexes** — Check `@Column` fields used in `WHERE`, `JOIN ON`, `ORDER BY`.
- [ ] **Eager loading by default** — `@ManyToOne` defaults to `EAGER`. Flip to `LAZY` unless always needed.
- [ ] **Transaction scope too wide** — `@Transactional` on controller or large service method keeps DB connection open too long. Push down to repository layer.
- [ ] **`findAll()` on large tables** — Always paginate: `Pageable` + `Page<T>`.
- [ ] **Unclosed `EntityManager`** — In non-Spring-managed contexts, ensure `em.close()` in `finally`.
- [ ] **Hibernate `open-in-view`** — `spring.jpa.open-in-view=true` (default) keeps session open through HTTP rendering. Set to `false` in production.
- [ ] **Missing `@Transactional(readOnly = true)`** — Read-only queries should declare it — enables Hibernate optimizations and can route to read replicas.

### Connection Pool (HikariCP)
- [ ] **Pool size too small** — Default is 10. Under load this exhausts quickly. Formula: `(cores * 2) + effective_spindle_count`.
- [ ] **Connection leak** — Not returning connection to pool. Look for unclosed `DataSource.getConnection()` without try-with-resources.
- [ ] **`connectionTimeout` too long** — Default 30s. If pool exhausted, requests queue for 30s before failing. Lower to fail fast.
- [ ] **`maximumPoolSize` vs `minimumIdle`** — Keep `minimumIdle = maximumPoolSize` in high-throughput apps to avoid cold-start latency.

### Caching
- [ ] **No caching on expensive reads** — Use `@Cacheable` (Spring Cache + Caffeine/Redis) for stable data.
- [ ] **Missing cache eviction** — `@CacheEvict` on writes — stale data is a correctness bug.
- [ ] **Cache stampede** — Many threads miss cache simultaneously → DB overload. Use `@Cacheable` with `sync = true`.

### HTTP / REST Layer
- [ ] **Synchronous blocking IO in reactive path** — Mixing `RestTemplate` in WebFlux context blocks the event loop. Use `WebClient`.
- [ ] **Large payloads without streaming** — Loading entire result set in memory before writing response. Use streaming / chunked responses.
- [ ] **Missing compression** — Enable `server.compression.enabled=true` for JSON/text responses.
- [ ] **No HTTP keep-alive / connection reuse** — RestTemplate default creates new connections. Configure `HttpComponentsClientHttpRequestFactory` with pooled connections.
- [ ] **No timeout on outbound calls** — RestTemplate/WebClient without connect/read timeout can block threads indefinitely.

### Threading & Async
- [ ] **Blocking in `@Async` thread pool** — Default pool is unbounded `SimpleAsyncTaskExecutor`. Configure a `ThreadPoolTaskExecutor` with bounded queue.
- [ ] **Synchronized on service bean** — Spring beans are singletons. `synchronized` on service method is a global lock — massive contention under load.
- [ ] **Thread-local leaks** — `ThreadLocal` not cleared in thread pool environments causes data bleed between requests.

### Logging
- [ ] **Logging in tight loops** — `log.debug(...)` inside loops materializes strings even at INFO level if not guarded with `log.isDebugEnabled()`.
- [ ] **Synchronous appenders** — Logback's default `ConsoleAppender` and `FileAppender` are synchronous. Use `AsyncAppender` in production.
- [ ] **Logging large objects** — `log.info("Response: {}", largeObject.toString())` serializes on every call. Guard with level checks.

### General Code Patterns
- [ ] **`String` concatenation in loops** — Use `StringBuilder`. `+=` in loop creates O(n²) allocations.
- [ ] **`new` object creation in hot paths** — Prefer object pooling or reuse (e.g., `ObjectMapper` is thread-safe — inject as singleton, don't `new` per request).
- [ ] **`SimpleDateFormat` shared** — Not thread-safe. Use `DateTimeFormatter` (thread-safe) or `ThreadLocal<SimpleDateFormat>`.
- [ ] **Unoptimized collections** — Using `LinkedList` where `ArrayList` fits, or `HashMap` without initial capacity when size is known.
- [ ] **Reflection in hot paths** — Expensive. Cache `Method`/`Field` references or use code generation alternatives.

---

## 8) Quick Reference — Performance Metrics

| Metric | Good Target (REST API) |
|--------|----------------------|
| P50 latency | < 50ms |
| P99 latency | < 500ms |
| Error rate | < 0.1% |
| CPU utilization | 60–70% under peak load |
| GC pause (G1) | < 200ms |
| Heap usage | < 75% of max heap |
| Thread pool queue depth | Near 0 at steady state |
| DB connection pool usage | < 80% |

---

## 9) Common Interview Questions — Quick Answers

**Q: How do you diagnose a memory leak in production?**
Enable `-XX:+HeapDumpOnOutOfMemoryError`, capture heap dump, analyze with JProfiler or Eclipse MAT. Look for classes with unexpectedly large retained heap. Trace GC root references.

**Q: What is the difference between throughput and latency?**
Throughput = requests processed per second. Latency = time for a single request to complete. Optimizing one can hurt the other — batching improves throughput, increases latency.

**Q: What causes GC pressure?**
High object allocation rate, large objects bypassing Young Gen, long-lived temporary objects being promoted to Old Gen, or memory leaks preventing GC from reclaiming objects.

**Q: What is a thread pool thread starvation?**
All threads in a pool are blocked (waiting on IO, locks, or slow downstream). New requests queue up and eventually timeout. Fix: async IO, increase pool size, or circuit breaker on slow dependencies.

**Q: What is the N+1 problem?**
1 query to fetch a list, then N queries (one per item) to fetch related data. Fix with `JOIN FETCH`, `@EntityGraph`, or Hibernate batch fetching (`@BatchSize`).

**Q: How does G1 GC achieve predictable pauses?**
Divides heap into equal-sized regions (not fixed Young/Old areas). Collects regions with most garbage first (Garbage First). Pause time target (`MaxGCPauseMillis`) guides how many regions to collect per cycle.
