# **Interview Cheat Sheet**

---

## 1) 🧠 Core Java

### JVM Architecture

The JVM has three main subsystems: **ClassLoader**, **Runtime Data Areas**, and **Execution Engine**.

```
+──────────────────── JVM ─────────────────────+
│  ClassLoader Subsystem                        │
│   Bootstrap → Platform → Application          │
├───────────────────────────────────────────────┤
│  Runtime Data Areas (shared across threads)   │
│   ┌────────────────┬──────────────────────┐   │
│   │  Method Area   │        Heap          │   │
│   │  (Metaspace)   │  Young Gen │ Old Gen │   │
│   │  class meta,   │  Eden/S0/S1│ Tenured │   │
│   │  static vars   │            │         │   │
│   └────────────────┴──────────────────────┘   │
│  Runtime Data Areas (per-thread)              │
│   JVM Stack │ PC Register │ Native Stack       │
├───────────────────────────────────────────────┤
│  Execution Engine                             │
│   Interpreter → JIT Compiler → GC             │
+───────────────────────────────────────────────+
```

### Heap Memory

```
Heap
├── Young Generation  (minor GC — fast, frequent)
│   ├── Eden Space      ← new objects allocated here
│   ├── Survivor S0     ← survivors after Eden GC
│   └── Survivor S1     ← objects aged from S0
└── Old Generation     (major GC — expensive, stop-the-world)
    (Tenured Space)     ← long-lived objects promoted from Young Gen

Metaspace (off-heap)   ← class metadata; replaced PermGen in Java 8+
```

**GC Flow:** Eden → (minor GC) → S0/S1 (age++) → (tenure threshold) → Old Gen → (full GC) → reclaimed

| GC Algorithm | Flag | Optimized For |
|---|---|---|
| G1GC (default Java 9+) | `-XX:+UseG1GC` | Balanced latency + throughput, large heaps |
| ZGC | `-XX:+UseZGC` | Ultra-low latency (<10ms pauses) |
| Parallel GC | `-XX:+UseParallelGC` | Max throughput, batch workloads |
| Shenandoah | `-XX:+UseShenandoahGC` | Low pause, concurrent compaction |

```bash
-Xms512m                     # initial heap (set equal to -Xmx in containers)
-Xmx2g                       # max heap
-XX:NewRatio=3               # Old:Young ratio
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200     # G1 pause target
-Xlog:gc*:file=gc.log        # structured GC logging
```

**Memory leak signals:** heap grows each GC cycle; `jmap -histo` shows accumulating object types; GC frequency rises while reclaimed memory shrinks.

### Stack Memory

Each thread has its own **JVM stack** — a LIFO structure of **stack frames** created per method call.

```
Thread Stack
├── Frame N  (active method)
│   ├── Local Variables array   ← primitives + object references
│   ├── Operand Stack           ← intermediate computation values
│   └── Frame Data              ← constant pool ref, return address
├── Frame N-1 (caller)
└── Frame 0   (thread entry)
```

- Frame is **pushed** on method call, **popped** on return or exception.
- Primitives and references live on the stack; **objects themselves live on the heap**.
- `StackOverflowError` → recursion too deep (default ~500–1000 frames per thread).
- `OutOfMemoryError: unable to create native thread` → OS thread limit hit.

```bash
-Xss512k   # per-thread stack size; reduce this when running thousands of threads
```

### ClassLoader Architecture

ClassLoaders load `.class` bytecode into the JVM using **parent-delegation**: always ask the parent first.

```
Bootstrap ClassLoader    ← JDK core (java.lang, java.util) — native code, no parent
        ↑ parent
Platform ClassLoader     ← javax.*, extensions (Java 9+ module system)
        ↑ parent
Application ClassLoader  ← your classpath / JARs
        ↑ parent
Custom ClassLoader       ← plugins, hot-reload, tenant isolation
```

**Delegation model:** child asks parent before attempting self-load → prevents duplicate/conflicting class definitions across JARs.

```java
// Custom ClassLoader — child-first for plugin isolation
public class PluginClassLoader extends URLClassLoader {
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("com.plugin.")) {
            return findClass(name);   // child-first for plugin classes
        }
        return super.loadClass(name, resolve); // parent-first for everything else
    }
}

// Reflection — dynamic inspection (use sparingly; bypasses type safety)
Class<?> clazz = Class.forName("com.mc.payments.PaymentProcessor");
Method method = clazz.getDeclaredMethod("process", Payment.class);
method.setAccessible(true);
method.invoke(instance, payment);
```

**ClassLoader use cases in Grid / NRT context:**
- Hot-deploy rule engines or pricing models without JVM restart
- Isolate per-tenant strategy implementations in the same JVM
- Load versioned processor classes side-by-side (e.g., v1 and v2 fraud rules)

### Concurrency & Memory

* **Thread lifecycle:** `new → runnable → running → waiting/blocking → terminated`
* **ExecutorService:** Use `Callable`/`Future` for managed concurrency; always shut down the executor.
* **synchronized vs ReentrantLock:** Use `synchronized` for simplicity; `ReentrantLock` for fairness, `tryLock`, or interruptible waits.
* **Volatile:** Ensures visibility across threads but not atomicity — use for single-writer flags.
* **Atomic classes:** (`AtomicInteger`, `AtomicLong`, `AtomicReference`) for lock-free CAS operations.
* **CompletableFuture:** Build async pipelines with `thenCompose`, `thenCombine`, `exceptionally`.
* **ForkJoinPool:** Efficient for recursive divide-and-conquer parallel tasks.
* **Semaphore:** Limits concurrent resource access; useful for DB/API throttling.

```java
CompletableFuture.supplyAsync(() -> fetchData())
    .thenCombine(
        CompletableFuture.supplyAsync(() -> fetchUser()),
        (data, user) -> merge(data, user)
    )
    .exceptionally(ex -> fallback(ex));
```

#### Concurrency in Grid / NRT Context

In a **Grid** (Apache Geode / GemFire in-memory data grid) and **NRT** (near-real-time processing) environment, concurrency spans multiple JVMs and partitioned data regions — not just threads within a single process.

**Distributed Region — partition-aware atomic updates:**
```java
// Region<K,V> is the Grid's distributed, partitioned data structure.
// Reads/writes are automatically routed to the owning partition node.
@Autowired Region<String, PaymentEvent> paymentRegion;

// Optimistic compare-and-swap across the cluster — no distributed lock needed
boolean replaced = paymentRegion.replace(
    "TXN-001",
    existingEvent,                           // expected current value
    existingEvent.toBuilder()
        .status("PROCESSING")
        .build()
);
if (!replaced) throw new ConcurrentUpdateException("Stale read on TXN-001 — retry");
```

**Function Execution — co-locate compute with data:**
```java
// Execute logic on the node that owns the data; avoids cross-node network hops.
// withFilter routes the function only to partitions holding those keys.
Execution exec = FunctionService
    .onRegion(paymentRegion)
    .withFilter(Set.of("TXN-001", "TXN-002"))
    .setArguments("SETTLE");

ResultCollector<?, ?> results = exec.execute(new SettlePaymentFunction());
```

**ContinuousQuery — NRT event-driven processing:**
```java
// React to Grid data changes in near-real-time without polling.
// Fires onEvent for every insert or update that matches the query predicate.
CqAttributesFactory factory = new CqAttributesFactory();
factory.addCqListener(new CqListener() {
    @Override
    public void onEvent(CqEvent event) {
        PaymentEvent p = (PaymentEvent) event.getNewValue();
        if (p.getAmount().compareTo(FRAUD_THRESHOLD) > 0) {
            riskEngine.evaluateAsync(p);   // non-blocking NRT fraud check
        }
    }
    @Override public void onError(CqEvent event) { deadLetterQueue.publish(event); }
    @Override public void close() {}
});

CqQuery cq = clientCache.getQueryService().newCq(
    "SELECT * FROM /payments WHERE status = 'PENDING'",
    factory.create()
);
cq.execute();
```

**Bounded Async Pipeline for NRT Throughput:**
```java
// Always bound the thread pool — unbounded queues silently cause OOM under burst load.
@Bean
public ThreadPoolTaskExecutor nrtExecutor() {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
    exec.setMaxPoolSize(50);
    exec.setQueueCapacity(1000);   // CallerRunsPolicy applies backpressure at capacity
    exec.setThreadNamePrefix("nrt-");
    exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    exec.initialize();
    return exec;
}

// Optimistic retry loop — safe for high-frequency NRT position/balance updates
public void updatePosition(String id, BigDecimal delta) {
    for (int attempt = 0; attempt < 3; attempt++) {
        PositionEntry current = region.get(id);
        if (region.replace(id, current, current.add(delta))) return;
    }
    throw new ConcurrentUpdateException("Position update failed after retries: " + id);
}
```

**Key concurrency patterns for Grid / NRT:**

| Pattern | Tool | Use Case |
|---|---|---|
| Distributed lock | `DistributedLockService` / Redis `SETNX` | Single-node processing of a partition key |
| Optimistic update | `region.replace(key, old, new)` | High-frequency balance/position updates |
| Compute co-location | `FunctionService.onRegion().withFilter()` | Avoid cross-node data shuffling |
| NRT change stream | `ContinuousQuery` / Kafka consumer | React to data changes in microseconds |
| Idempotency key | `ConcurrentHashMap` / Redis `SETNX` | Deduplicate retried NRT messages |
| Aggregate mailbox | `CompletableFuture` chain / Axon `@EventHandler` | Serialize per-aggregate writes |

### Functional & Stream APIs

* `Function`, `Predicate`, `Supplier`, `Consumer` — core functional interfaces.
* Use `map`, `filter`, `flatMap`, `reduce`. Prefer parallel streams only when stateless and data-heavy.
* Combine with `Collectors.groupingBy`, `partitioningBy`, `toUnmodifiableList`.

```java
list.stream()
    .filter(User::isActive)
    .collect(Collectors.groupingBy(User::getRole));

// flatMap — flatten nested collections
orders.stream()
    .flatMap(order -> order.getItems().stream())
    .mapToDouble(Item::getPrice)
    .sum();
```

### Composition vs Inheritance

- **Composition:** "has-a" → build with injectable parts (prefer this).
- **Inheritance:** "is-a" → extend existing type (only for stable, clear hierarchies).

| Aspect | Composition | Inheritance |
|---|---|---|
| Relationship | Has-a | Is-a |
| Flexibility | High (runtime swap) | Low (compile-time) |
| Coupling | Loose | Tight |
| Encapsulation | Preserved | Often broken |
| Testing | Easy to mock | Complex |
| Use When | Behavior changes often | Stable hierarchy |

**Patterns:** Composition → Strategy, Decorator, Adapter, Proxy. Inheritance → Template Method, Abstract Base.

```java
interface RiskScorer { int score(Transaction t); }
class PaymentService {
    private final RiskScorer scorer;  // composed — swap strategy at runtime
    PaymentService(RiskScorer scorer) { this.scorer = scorer; }
}
```

### Exception Handling

| Type | Superclass | Compiler Checks? | When to Use |
|---|---|---|---|
| **Checked** | `Exception` | ✅ Yes | Recoverable, external condition (file, DB, network) |
| **Unchecked** | `RuntimeException` | ❌ No | Programming/logic error — caller can't reasonably recover |

```java
// Checked — compiler forces callers to handle or declare
class InsufficientFundsException extends Exception {
    InsufficientFundsException(BigDecimal shortfall) { super("Short by: " + shortfall); }
}

// Unchecked — signals a programming mistake
class PaymentDeclinedException extends RuntimeException {
    PaymentDeclinedException(String reason) { super(reason); }
}
```

> **Interview line:** "Checked = expected, recoverable; compiler enforces a contract. Unchecked = logic error; compiler trusts you to prevent it."

* Favor **immutable objects** (`final` fields, no setters) to avoid shared-state race conditions.
* Use **custom exceptions** for API-level granularity and caller clarity.

### SOLID Principles

Five design guidelines for writing clean, maintainable, extensible OO software.

| Principle | Core Idea | Benefit |
|---|---|---|
| **S** — Single Responsibility | One class = one reason to change | Easier maintenance |
| **O** — Open/Closed | Extend behavior; never modify existing code | Safe, flexible changes |
| **L** — Liskov Substitution | Subclasses must honor the base type's contract | Reliable polymorphism |
| **I** — Interface Segregation | Small, focused interfaces over fat ones | Less coupling |
| **D** — Dependency Inversion | Depend on abstractions, not concrete classes | Testable, swappable |

#### S — Single Responsibility

```java
// BAD — three unrelated jobs in one class
class PaymentService {
    void process(Payment p) { /* DB write, fraud check, email */ }
    void sendEmail(String to) { /* SMTP logic */ }
    void writeAuditLog(Payment p) { /* I/O */ }
}

// GOOD — each class has exactly one reason to change
@Service class PaymentProcessor   { void process(Payment p)  { /* payment logic only */ } }
@Service class NotificationService { void notify(Payment p)  { /* comms only */ } }
@Service class AuditLogger         { void log(Payment p)     { /* audit only */ } }
```

#### O — Open/Closed

```java
// BAD — adding a new discount type requires editing existing code (risk of regression)
class DiscountCalculator {
    double calculate(Order o, String type) {
        if ("SEASONAL".equals(type)) return o.total() * 0.10;
        if ("LOYALTY".equals(type))  return o.total() * 0.15;
        return 0;
    }
}

// GOOD — new discount = new class; zero changes to existing code
interface DiscountStrategy { double calculate(Order o); }

class SeasonalDiscount implements DiscountStrategy {
    public double calculate(Order o) { return o.total() * 0.10; }
}
class LoyaltyDiscount implements DiscountStrategy {
    public double calculate(Order o) { return o.total() * 0.15; }
}
class DiscountCalculator {
    double calculate(Order o, DiscountStrategy strategy) { return strategy.calculate(o); }
}
```

#### L — Liskov Substitution

```java
// BAD — Square breaks Rectangle's contract (width and height are no longer independent)
class Rectangle { protected int width, height; int area() { return width * height; } }
class Square extends Rectangle {
    @Override void setWidth(int w) { super.width = w; super.height = w; } // violates LSP
}

// GOOD — unrelated shapes implement the same interface; no contract is broken
interface Shape { int area(); }
class Rectangle implements Shape { int w, h;   public int area() { return w * h; } }
class Square    implements Shape { int side;   public int area() { return side * side; } }
```

#### I — Interface Segregation

```java
// BAD — BatchJob forced to implement methods it will never use
interface Worker {
    void processPayment();
    void sendNotification();  // batch jobs don't send notifications
    void generateReport();    // not all workers generate reports
}

// GOOD — small, purpose-built interfaces; classes implement only what they need
interface PaymentProcessor { void processPayment(); }
interface Notifier         { void sendNotification(); }
interface Reporter         { void generateReport(); }

class PaymentBatchJob implements PaymentProcessor, Reporter { /* only these two */ }
class AlertService        implements Notifier              { /* only notification */ }
```

#### D — Dependency Inversion

```java
// BAD — tightly coupled to a concrete class; untestable without a real Oracle DB
class PaymentService {
    private final OraclePaymentRepository repo = new OraclePaymentRepository();
    void save(Payment p) { repo.save(p); }
}

// GOOD — depends on abstraction; Spring injects the concrete at runtime
interface PaymentRepository { void save(Payment p); }

@Repository
class OraclePaymentRepository implements PaymentRepository {
    public void save(Payment p) { /* Oracle-specific */ }
}

@Service
class PaymentService {
    private final PaymentRepository repo;
    PaymentService(PaymentRepository repo) { this.repo = repo; } // injected, testable, swappable
    void save(Payment p) { repo.save(p); }
}
```

---

## 2) 🏗️ Data Structures & Use Cases

### Core Collections

| Data Structure | Avg. Time Complexity | Typical Use / Notes |
|---|---|---|
| **Array** | Access: O(1), Insert/Delete: O(n) | Fixed-size, fast random access. |
| **ArrayList** | Access: O(1), Insert/Delete: O(n) | Dynamic resize, good for reads. |
| **LinkedList** | Access: O(n), Insert/Delete: O(1) | Fast at ends, high overhead. |

### 🗺️ Maps

| Data Structure | Avg. Time | Typical Use |
|---|---|---|
| **HashMap** | O(1) | Fast lookup, unordered. |
| **ConcurrentHashMap** | O(1) | Thread-safe, low contention; segment-level locking. |
| **TreeMap** | O(log n) | Sorted keys, slower than HashMap. |
| **LinkedHashMap** | O(1) | Insertion/access order; great for LRU cache. |
| **WeakHashMap** | O(1) | Auto-clears when keys are GC'd. |
| **EnumMap** | O(1) | Optimized for enum keys. |

### 🔁 Sets

| Data Structure | Avg. Time | Typical Use |
|---|---|---|
| **HashSet** | O(1) | Unique, unordered elements. |
| **TreeSet** | O(log n) | Sorted unique elements. |
| **BitSet** | O(1) | Space-efficient boolean flags. |

### 🧵 Queues & Stacks

| Data Structure | Avg. Time | Typical Use |
|---|---|---|
| **Stack / Deque** | Push/Pop: O(1) | LIFO operations. |
| **Queue / Deque** | Offer/Poll: O(1) | FIFO or double-ended. |
| **BlockingQueue** | O(1) | Thread-safe blocking tasks. |
| **PriorityQueue** | Insert/Delete: O(log n) | Min/max retrieval. |
| **CopyOnWriteArrayList** | Read: O(1), Write: O(n) | Safe concurrent reads. |

### 🧠 Algorithm Complexity Quick Reference

| Algorithm / Operation | Best | Average | Worst | Space |
|---|---|---|---|---|
| Binary Search | O(1) | O(log n) | O(log n) | O(1) |
| Linear Search | O(1) | O(n) | O(n) | O(1) |
| QuickSort | O(n log n) | O(n log n) | O(n²) | O(log n) |
| MergeSort | O(n log n) | O(n log n) | O(n log n) | O(n) |
| HeapSort | O(n log n) | O(n log n) | O(n log n) | O(1) |
| HashMap get/put | O(1) | O(1) | O(n) | O(n) |
| TreeMap get/put | O(log n) | O(log n) | O(log n) | O(n) |
| BFS / DFS | — | O(V+E) | O(V+E) | O(V) |

### 🏦 Interview: Choosing the Right Structure

| Scenario | Best Choice | Reason |
|---|---|---|
| Dedup idempotency keys | `HashSet` | O(1) lookup |
| LRU cache (rate limit) | `LinkedHashMap` (access-order) | O(1) access + insertion order |
| Priority fraud queue | `PriorityQueue` | O(log n) min/max extraction |
| Thread-safe counter | `AtomicLong` | Lock-free CAS |
| Concurrent config map | `ConcurrentHashMap` | Segment-level locking |
| Message buffer | `ArrayBlockingQueue` | Bounded, thread-safe FIFO |
| Sorted transaction list | `TreeMap<Date, Txn>` | Sorted by timestamp automatically |

### 🏗️ LRU Cache (Classic Interview Pattern)

```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    LRUCache(int capacity) {
        super(capacity, 0.75f, true);  // true = access-order
        this.capacity = capacity;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
// Or: Spring @Cacheable with Caffeine maximumSize(n) + expireAfterAccess
```

---

## 3) 🍃 Spring Boot Mastery

### Architecture & Validation

* **Controller:** REST entry point (`@RestController`, `@RequestMapping`).
* **Service:** Business logic.
* **Repository:** Data layer.
* **DTO:** Transfer objects, decoupled from persistence.
* **Validation:** `@Valid`, `@NotNull`, `@Pattern` via `spring-boot-starter-validation`.

### Spring Annotation Cheat Sheet

#### 🧭 General / Bootstrapping

| Annotation | Purpose | Notes |
|:---|:---|:---|
| **@RestController** | Combines `@Controller` + `@ResponseBody` | Auto-serializes to JSON/XML |
| **@Controller** | Handles web requests, returns views | Use with `ModelAndView` |
| **@Service** | Business logic beans | Singleton by default |
| **@Repository** | DAO layer, translates SQL exceptions | Used for JPA/JDBC |
| **@Configuration** | Declares bean definitions | Type-safe alt to XML |
| **@Bean** | Defines a Spring-managed bean | Lifecycle-aware, injectable |
| **@Value("${property}")** | Injects config/env values | Supports SpEL (`#{}`) |
| **@Transactional** | DB transaction boundaries | Supports rollback/isolation |
| **@Profile("prod")** | Loads beans under specific profile | Common: `dev`, `test`, `prod` |
| **@DependsOn("bean")** | Enforces bean init order | Handles implicit dependencies |
| **@ConditionalOnProperty** | Loads bean if config flag matches | Feature toggles |
| **@ConditionalOnMissingBean** | Loads bean only if not yet defined | Avoids duplicates |
| **@Lazy** | Delays bean creation | Improves startup time |
| **@Scope("prototype")** | New instance per injection | Default scope: singleton |
| **@Cacheable**, **@CacheEvict** | Cache method results / clear entries | Pair with `@CachePut` |
| **@Retryable**, **@Recover** | Auto retries + fallback | Needs `@EnableRetry` |
| **@RateLimiter**, **@CircuitBreaker**, **@Bulkhead** | Resilience4j controls | Limits/trips/isolates calls |
| **@Async** | Runs method asynchronously | Needs `@EnableAsync` |
| **@Scheduled(cron="...")** | Periodic background tasks | Needs `@EnableScheduling` |
| **@RestControllerAdvice**, **@ExceptionHandler** | Global exception handling | JSON responses for REST APIs |
| **@EnableConfigurationProperties**, **@ConfigurationProperties** | Binds YAML props to POJOs | `prefix="app"` |

#### 🧩 Dependency Injection

| Annotation | Purpose | Notes |
|:---|:---|:---|
| **@Autowired** | Injects bean by type | Constructor, setter, or field |
| **@Qualifier("beanName")** | Specifies which bean when multiple exist | Used with `@Autowired` |
| **@Primary** | Default bean when multiple candidates | Overridden by `@Qualifier` |
| **@Component** | Base stereotype; marks a Spring bean | Base for `@Service`, `@Repository` |
| **@ComponentScan(basePackages)** | Where to scan for components | Used with `@Configuration` |
| **@Import(ConfigClass.class)** | Brings in other configuration classes | Modular setups |
| **@Order(n)** | Priority when multiple beans match interface | Lower value = higher priority |
| **@Lookup** | Injects prototype bean into singleton | Dynamic method generation |

#### ⚙️ Configuration & Environment

| Annotation | Purpose | Notes |
|:---|:---|:---|
| **@ConfigurationProperties(prefix="app")** | Binds YAML to a POJO | Requires getters/setters |
| **@PropertySource("classpath:custom.properties")** | Loads extra `.properties` | Not for YAML files |
| **@Profile("dev")** | Activates bean for specific env | With `spring.profiles.active` |
| **@ConditionalOnExpression** | Activates when SpEL evaluates true | `'${env}' == 'prod'` |
| **@ActiveProfiles("test")** | Sets profile in tests | With `@SpringBootTest` |

#### 🔮 Aspect-Oriented Programming (AOP)

| Annotation | Purpose |
|:---|:---|
| **@Aspect** | Marks class as cross-cutting concern module |
| **@Before("pointcut")** | Runs before method — logging, validation |
| **@After("pointcut")** | Runs after method always — like `finally` |
| **@AfterReturning** | Runs after successful return; can access return value |
| **@AfterThrowing** | Runs only on exception — error logging |
| **@Around("pointcut")** | Full wrap; must call `proceed()`; most powerful |
| **@Pointcut("execution(...)")** | Defines reusable join point expression |

**AOP Concepts:**
- **Aspect** — modular cross-cutting concern (logging, auditing, security, metrics).
- **Advice** — the action: Before, After, Around, AfterReturning, AfterThrowing.
- **Join Point** — execution moment where advice can be applied (method call, field access).
- **Pointcut** — predicate expression selecting which join points to intercept.

### Error Handling

```java
@RestControllerAdvice
class GlobalHandler {
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> handle(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", ex.getClass().getSimpleName(), "message", ex.getMessage()));
    }
}
```

### Retry & Recovery

```java
@Retryable(value = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public String callExternalApi() { /* ... */ }

@Recover
public String recover(IOException ex) { return "Fallback response"; }
```

### Caching

```java
@Cacheable("users")
public User findById(Long id) { return repo.findById(id).orElseThrow(); }
```

### Performance & Resilience

* **Resilience4j:** circuit breakers, retries, bulkheads — isolate and protect downstream calls.
* **@Async + TaskExecutor:** always use a bounded `ThreadPoolTaskExecutor`, never the default unbounded one.
* **Actuator:** exposes `/health`, `/metrics`, `/info`, `/env` — enable/disable via config.
* **HikariCP** (default Spring Boot pool): tune `maximumPoolSize`, `connectionTimeout`, `idleTimeout`. Pool size ≈ cores × 2 for I/O-heavy services.

```java
@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(10);
    exec.setMaxPoolSize(50);
    exec.setQueueCapacity(200);
    exec.setThreadNamePrefix("async-");
    exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    exec.initialize();
    return exec;
}

@Async
public CompletableFuture<String> runAsyncTask() {
    return CompletableFuture.completedFuture("done");
}
```

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: HikariPool-Main
```

### @Transactional — Deep Dive

```java
// Propagation — what happens when a @Transactional method calls another
@Transactional(propagation = Propagation.REQUIRED)      // default — join or create
@Transactional(propagation = Propagation.REQUIRES_NEW)  // always new TX; suspends outer
@Transactional(propagation = Propagation.SUPPORTS)      // join if exists, none if not
@Transactional(propagation = Propagation.NOT_SUPPORTED) // suspend outer, run non-TX
@Transactional(propagation = Propagation.NEVER)         // throw if TX exists
@Transactional(propagation = Propagation.MANDATORY)     // throw if no TX exists

// Isolation levels
@Transactional(isolation = Isolation.READ_COMMITTED)    // default — no dirty reads
@Transactional(isolation = Isolation.REPEATABLE_READ)   // no phantom reads within TX
@Transactional(isolation = Isolation.SERIALIZABLE)      // full isolation — slowest

// Rollback & hints
@Transactional(rollbackFor = Exception.class)           // rollback on checked too
@Transactional(noRollbackFor = NotFoundException.class) // don't rollback on this
@Transactional(readOnly = true)                         // allows DB/JPA optimizations
```

**Common pitfalls:**
- **Self-invocation:** calling a `@Transactional` method from within the same class bypasses the proxy — extract to a separate Spring bean.
- **Private methods:** silently ignored — Spring can't proxy them.
- **Exception swallowing:** catching without rethrowing prevents rollback.

---

## 4) 🧰 REST API & Microservices Design

### REST vs SOAP

| Feature | REST | SOAP |
|:---|:---|:---|
| **Protocol** | HTTP, stateless | XML over HTTP/SMTP/etc., WSDL contract |
| **Data Format** | JSON (lightweight) | Strict XML — `<soap:Envelope>` |
| **Style** | Resource-oriented (`/api/users/1`) | Operation-oriented (`getUser()`) |
| **Security** | OAuth2, JWT | WS-Security (XML Signature, Encryption) |
| **Performance** | Faster, lighter | Slower, more structured |
| **Caching** | Supported via HTTP headers | Not natively supported |
| **Tooling** | Spring Boot, JAX-RS | JAX-WS, Apache CXF, Axis2 |
| **When to Use** | Public APIs, mobile, microservices | Enterprise transactions, legacy systems |

### Microservice Design Principles

* **Resiliency:** Circuit breakers, bulkheads, fallback methods — use **Resilience4j**.
* **Observability:** Tracing (Zipkin/Jaeger via OpenTelemetry), metrics (Prometheus via Micrometer), logs (ELK via Logback/Logstash).
* **Communication:**
  * **REST/gRPC** — sync, request/response, low-latency, transactional operations.
  * **Kafka/RabbitMQ** — async, event-driven, high-throughput, eventual consistency.
* **Stateless design:** No HTTP sessions — use Redis for shared state.
* **Idempotency:** Use idempotency keys for POSTs; store in Redis with TTL.
* **Caching:** `ETag` & `Cache-Control` for read-heavy resources.

### Patterns

* **API Gateway → Service → Queue → DB.**
* Service discovery via Eureka/Consul/K8s DNS.
* Config via Spring Cloud Config / Vault.

### Cloud Design Concepts

| Concept | Divides | Purpose | Example |
|:---|:---|:---|:---|
| **Pagination** | A *response* | Make results digestible for clients | 20 items per page |
| **Load Balancing** | *Requests* | Spread traffic, avoid overload | Round-robin across 5 pods |
| **Partitioning (Sharding)** | The *data itself* | Scale storage and parallel processing | Customers A–M on node 1, N–Z on node 2 |

---

## 5) ⚙️ Kafka & Axon

### Axon + Kafka Overview

**Axon Framework** (CQRS + Event Sourcing) with **Kafka** as distributed event bus for scalable processors.

### Core Concepts Mapping

| Concept | Axon | Kafka Equivalent | Purpose |
|---|---|---|---|
| Command Bus | Direct P2P | — | Executes intent |
| Event Bus | Pub-sub | Topic | Distributes events |
| Query Bus | P2P / scatter-gather | — | Fetches read models |
| Event Processor | Handler | Consumer group | Processes events |
| Aggregate | Domain root | — | Applies events |
| Event Store | Axon Server / Kafka | Topic per type | Persist/replay |

### Kafka Configuration

```yaml
axon:
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      client-id: axon-producer
    consumer:
      bootstrap-servers: localhost:9092
      group-id: billing-service
      auto-offset-reset: earliest
      enable-auto-commit: false
    properties:
      max.poll.records: 100
```

**`group.id`** — Same `group.id` → load-balance within a service. Different `group.id`s → multiple services consume the same events independently.

| Service | Group ID | Effect |
|---|---|---|
| `payment-service` | `payment-processor` | Independent consumer |
| `notification-service` | `notification-processor` | Also receives events |

### Key Topics & Event Flow

| Topic | Description |
|---|---|
| `axon.events` | Domain events |
| `axon.commands` | Optional command bus |
| `axon.dead-letter` | Failed processing |
| Custom topics | Per aggregate/bounded context |

### Event Processors

| Type | Processing | Use Case |
|---|---|---|
| SubscribingProcessor | Real-time | Single-node/local replay |
| TrackingProcessor | Offset-based | Distributed & replayable |

### Common Configs & Gotchas

| Config | Meaning | Tip |
|---|---|---|
| `auto.offset.reset=earliest` | Start from beginning if no offset | For replay/testing |
| `enable.auto.commit=false` | Let Axon control commit | Prevent message loss |
| `max.poll.interval.ms` | Max time between polls | Tune for slow handlers |
| `max.poll.records` | Records per batch | Throughput control |
| `acks=all` | Producer durability | Wait for all replicas |

### Code Patterns

```java
// Producer — publish event to Kafka topic
@EventHandler
public void on(OrderCreatedEvent event) {
    kafkaTemplate.send("axon.events", event.getOrderId(), event);
}

// Consumer — @ProcessingGroup maps to consumer group.id
@ProcessingGroup("order-events")
public class OrderEventHandler {
    @EventHandler
    public void on(OrderCreatedEvent event) {
        // update read model, trigger saga, etc.
    }
}
```

### Parallelism & Scaling

* **Partitions** = unit of parallelism. Key by aggregate ID to preserve per-aggregate ordering.
* **Replicas** for fault tolerance.
* Scale consumers **up to** (not beyond) partition count.

### Error Handling

* Automatic retries for transient errors.
* DLQ (`axon.dead-letter`) for permanent failures; custom retry policies supported.

### Typical Architecture

```
[Command → Aggregate → Event]
          ↓
     Kafka Topic ("axon.events")
          ↓
   [Event Processors / Sagas]
          ↓
     Read Models / External Systems
```

---

## 6) 🔐 Security

### Spring Security (AuthN/Z)

* JWT or OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`).
* Stateless APIs: `SessionCreationPolicy.STATELESS`.
* Passwords: `BCryptPasswordEncoder`.
* Role-based access: `@PreAuthorize("hasRole('ADMIN')")`.

```java
@Bean
SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**", "/actuator/health").permitAll()
            .requestMatchers(HttpMethod.GET, "/v1/**").hasAnyRole("READ", "ADMIN")
            .requestMatchers("/v1/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
        .headers(h -> h
            .frameOptions(fo -> fo.deny())
            .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000)))
        .build();
}

// Method-level security — enable with @EnableMethodSecurity
@PreAuthorize("hasRole('ADMIN')")
public void deleteAccount(Long id) { }

@PreAuthorize("hasRole('USER') and #accountId == authentication.principal.accountId")
public Account getAccount(Long accountId) { }   // owner-only check

@PostAuthorize("returnObject.ownerId == authentication.principal.id")
public Account findById(Long id) { }            // filter after method runs

// CORS configuration
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.mastercard.com"));
    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization","Content-Type","Idempotency-Key"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/v1/**", config);
    return source;
}
```

**Spring Security Filter Chain Order:**
```
Request → ChannelProcessingFilter
        → SecurityContextPersistenceFilter
        → HeaderWriterFilter
        → CorsFilter
        → CsrfFilter (disabled for stateless)
        → BasicAuthenticationFilter / JwtAuthFilter
        → ExceptionTranslationFilter
        → FilterSecurityInterceptor (authorization)
        → Your Controller
```

### Secure Headers & Best Practices

* `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy`, `HSTS`, CORS per env.
* Never log sensitive fields — use `@JsonIgnore`, PII redaction filters, or structured log masking.
* Always validate inputs at the controller boundary (`@Valid`) and sanitize before DB writes.

### Secure SDLC & Threat Modeling

**Phases:** Requirements → Design (STRIDE) → Implementation (SAST/SCA) → Testing (DAST/fuzzing) → Deployment (runtime & IaC scans).

### Security Testing

| Type | Tools | Detects |
|---|---|---|
| SAST | Checkmarx, Fortify, SonarQube | Code-level flaws |
| SCA | Snyk, Black Duck, OWASP Dep-Check | Library CVEs |
| DAST | ZAP, Burp Suite | Runtime flaws |
| IaC Scan | Checkov, KICS | Insecure Terraform/K8s YAML |

**Lifecycle:** Discover → Validate → Prioritize (CVSS) → Patch → Retest → Report.

### Encryption vs Hashing

| Feature | Encryption | Hashing |
|---|---|---|
| Reversible | ✅ | ❌ |
| Uses a Key | ✅ | ❌ |
| Goal | Confidentiality | Integrity/Auth |
| Examples | AES, RSA | SHA-256, bcrypt, Argon2 |

* Never encrypt passwords — hash with slow KDF + salt (bcrypt/Argon2).
* Encrypt sensitive data you must read back (card numbers, PII, API keys).

### Security Use Cases

| Scenario | Solution |
|---|---|
| Race condition in API | `synchronized` or Redis distributed lock |
| API abuse / brute force | Redis rate limiting + CAPTCHA |
| Sensitive logs | PII redaction filter |
| DDoS | Rate limiting + CDN caching |
| Outdated dependency | Automate SCA → ticket → patch |
| File upload risk | MIME validation + antivirus (ClamAV) |
| GC CPU spike | Enable GC logs; analyze via GCeasy |

---

## 7) 🧩 System Design & Patterns

### Design Patterns — Quick Ref (Java/Spring)

| Pattern | When to Use | Example |
|---|---|---|
| **Factory** | Hide creation logic; return interface | `CryptoFactory.get("AES").encrypt(data)` |
| **Abstract Factory** | Create families of related objects | `AwsStackFactory.createQueue(); createStore()` |
| **Builder** | Build complex immutable objects fluently | `User.builder().id(1).name("Ada").build()` |
| **Singleton** | One instance app-wide (stateless, thread-safe) | `@Bean public ObjectMapper mapper()` |
| **Strategy** | Swap algorithms at runtime | `hasher = new BcryptStrategy(); hasher.hash(pw)` |
| **Template Method** | Fixed steps, customizable hooks | `AbstractJob.run() → pre(); doRun(); post()` |
| **Decorator** | Add behavior without changing core type | `new LoggingClient(new RetryingClient(http))` |
| **Proxy** | Control access, lazy load, caching | Spring AOP proxies around services |
| **Adapter** | Make incompatible APIs play nice | Wrap legacy SOAP client behind `PaymentPort` |
| **Facade** | Simple API over complex subsystem | `PaymentFacade.authorizeAndCapture()` |
| **Observer / Pub-Sub** | React to events | Spring `ApplicationEventPublisher` / Kafka |
| **Chain of Responsibility** | Pipeline of filters/validators | Servlet filter chain; custom validation chain |
| **Command** | Encapsulate requests; queue/retry | `PlaceOrderCommand` → handler → outbox |
| **Repository** | Encapsulate persistence | `UserRepository.findByEmail(...)` |
| **Specification** | Reusable query predicates | JPA Specification for dynamic filters |

### Micro-snippets

#### Factory
```java
interface Hasher { String hash(String s); }
class BcryptHasher implements Hasher { /* ... */ }
class Argon2Hasher implements Hasher { /* ... */ }

final class HasherFactory {
    static Hasher of(String alg) {
        return switch (alg) { case "argon2" -> new Argon2Hasher(); default -> new BcryptHasher(); };
    }
}
```

#### Builder
```java
@Getter @Builder @AllArgsConstructor
class Order { Long id; String status; BigDecimal amount; }
```

#### Strategy
```java
class PasswordService {
    private final Hasher hasher; // injected
    String store(String plain) { return hasher.hash(plain); }
}
```

#### Decorator
```java
class MetricsClient implements HttpClient {
    private final HttpClient delegate; private final MeterRegistry m;
    Response send(Request r) { return m.timer("http").record(() -> delegate.send(r)); }
}
```

#### Chain of Responsibility
```java
interface Rule { Optional<String> apply(Request r); }
class CompositeRules implements Rule {
    List<Rule> rules;
    public Optional<String> apply(Request r) {
        return rules.stream()
            .map(rule -> rule.apply(r))
            .filter(Optional::isPresent)
            .findFirst()
            .orElse(Optional.empty());
    }
}
```

### Microservices System Architecture

```
[ Client / Web / Mobile ]
          |
          v
     [ CDN / WAF ]
          |
          v
 [ API Gateway / Nginx / LB ]
    |         auth, rate-limit, routing, metrics
    v
 ┌──────────────────────────────────────┐
 │         Stateless Microservices       │
 │  AuthSvc    OrderSvc    InventorySvc  │
 │  PaymentSvc  PricingSvc  NotifySvc   │
 │  QuerySvc (Read API)                  │
 └──────────────────────────────────────┘
        |         |          |         |
        v         v          v         v
   ┌────────┐ ┌────────┐ ┌──────┐ ┌────────┐
   │ Redis  │ │ RDBMS  │ │Kafka │ │Elastic │
   │(Cache) │ │(Stores)│ │(Async│ │Search  │
   └────────┘ └────────┘ └──────┘ └────────┘
```

**Write Path:** Commands → domain service → local DB commit + outbox → Kafka events.
**Read Side:** Kafka feeds Query/Read API (materialized views or Elasticsearch) for low-latency reads.
**Workflows:** Cross-service coordination via Sagas; Redis holds idempotency keys; Kafka partitioning enforces per-aggregate ordering.

### Design Patterns in Security Context

* **Singleton:** Central `SecurityConfig`, connection pool.
* **Factory:** Crypto algorithms, JWT parser.
* **Strategy:** Switch hash strategies (BCrypt ↔ Argon2).
* **Decorator:** Add logging, metrics, or auditing layers.
* **Observer:** Event notification to SIEM/monitoring.
* **Proxy:** API gateway enforcing auth & throttling.
* **Chain of Responsibility:** Servlet filters → validation → authorization.

---

## 8) ⚙️ Backpressure

> **Backpressure** = controlling producer speed so consumers, queues, and downstream systems aren't overwhelmed.
> **Never accept more work than you can process within your latency & memory budget.**

### Layers & Controls

#### Edge (API / Gateway)

| Technique | Purpose |
|---|---|
| **Rate Limiting (Token/Leaky Bucket)** | Reject overload with HTTP 429 + Retry-After |
| **Load Shedding (Fail Fast)** | Return 503 if inflight > limit or queue full |
| **Timeout Budgets** | Parent < child < downstream (no zombie requests) |
| **Circuit Breakers / Bulkheads** | Isolate hot endpoints, stop cascading failures |
| **Bounded Thread Pools** | No unbounded queues — protect CPU & memory |

#### Service → Database

| Control | Effect |
|---|---|
| **Connection pool caps** | Limit DB concurrency |
| **Retry + Backoff (with jitter)** | Smooth transient overload |
| **Queue writes / buffer** | Drain at fixed rate |
| **Dead-letter queue (DLQ)** | Store failed ops for later retry |

#### Messaging (Kafka / SQS / MQ)

| Producer Side | Consumer Side |
|---|---|
| `acks=all`, `max.in.flight=1` | Pull-based → built-in backpressure |
| Limit `buffer.memory` | `pause()` / `resume()` when processing slows |
| Throttle by quota | Scale consumers by lag/time-to-drain |

#### Reactive / Streaming

| Operator | Behavior |
|---|---|
| `.onBackpressureBuffer(size)` | Buffer bursts safely |
| `.onBackpressureDrop()` | Keep only most recent |
| `.limitRate(n)` | Downstream pulls only what it can handle |

### Patterns

| Pattern | Purpose |
|---|---|
| **Admission Control** | Refuse new work beyond safe thresholds |
| **Priority Lanes** | Reserve capacity for high-value traffic |
| **SLO-aware Shedding** | Drop requests when latency breaches budget |
| **Retry Discipline** | Limit attempts, exponential backoff + jitter |

### Monitor These Metrics

* Queue depth / Kafka lag
* P95/P99 latency
* Concurrency per worker
* Rate of 429/503 responses
* DB pool usage

### Mini Recipes

```java
// Gateway guardrail
if (inflight >= MAX_CONCURRENCY || queueLen >= MAX_QUEUE) return 503;

// Reactor
Flux<Event> stream = source.onBackpressureLatest().limitRate(256);

// Kafka consumer control
if (processingSlow()) consumer.pause(partitions);
if (recovered())     consumer.resume(partitions);

// DB gate
if (!semaphore.tryAcquire()) throw new ServiceUnavailableException();
```

> **TL;DR: Cap concurrency. Bound queues. Prefer pull over push. Autoscale on lag. Shed early, not late.**

---

## 9) ⚙️ Java Performance Tools

### Purpose Map

| Goal | Tool Type | Examples |
|:---|:---|:---|
| **Detect CPU / memory bottlenecks** | Profiler | VisualVM, JProfiler, YourKit, Async Profiler |
| **Measure throughput / latency** | Benchmarking | JMH, Caliper |
| **Simulate load / concurrency** | Load Testing | JMeter, Gatling, k6 |
| **Observe live metrics** | Monitoring / APM | JConsole, JMC, Micrometer, Prometheus, Grafana |
| **Inspect GC behavior** | GC Analysis | GC logs, GCViewer, GCeasy |
| **System-level analysis** | OS & JVM tools | `jcmd`, `jstat`, `jmap`, `jstack`, Flight Recorder |
| **Distributed tracing** | Observability | OpenTelemetry, Zipkin, Jaeger |
| **Heap / Leak diagnosis** | Memory tools | Eclipse MAT, HeapHero, VisualVM HeapDump |

### Profilers

| Tool | Highlights |
|---|---|
| **VisualVM** | Free GUI; CPU, memory, thread profiling, heap dumps |
| **JProfiler** | Deep heap, threads, DB, I/O; excellent call graph |
| **YourKit** | Low overhead; good for CI and production sampling |
| **Async Profiler** | Native, async-safe, ultra-low overhead; flame graphs |
| **Eclipse MAT** | Post-mortem analysis from heap dumps |

### Load & Stress Testing

| Tool | Use Case |
|---|---|
| **Apache JMeter** | GUI/CLI load testing for REST, SOAP, MQ, JDBC |
| **Gatling** | Scala DSL; strong for HTTP APIs and CI/CD |
| **k6** | JavaScript-based; integrates with Prometheus |
| **wrk / hey** | Lightweight CLI HTTP tools for quick spikes |

### JVM & GC Diagnostics

| Command | Purpose |
|---|---|
| `jcmd <pid> GC.heap_info` | Print heap and GC info |
| `jstat -gcutil <pid> 1s` | Monitor GC usage real-time |
| `jmap -dump:live,format=b,file=heap.bin <pid>` | Dump heap for MAT |
| `jstack <pid>` | Capture thread states (detect deadlocks) |
| `jfr start/stop` | Java Flight Recorder |
| `-Xlog:gc*` | Enable structured GC logging |

### Microbenchmarking (JMH)

```java
@Benchmark
@Fork(1)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
public void testSorting() {
    Arrays.sort(data);
}
```

### Performance Workflow

```
1️⃣ Baseline → 2️⃣ Profile → 3️⃣ Tune → 4️⃣ Verify → 5️⃣ Automate
```

* **Baseline:** JMH or JMeter for current throughput/latency.
* **Profile:** VisualVM/JProfiler to identify hotspots.
* **Tune:** Adjust thread pools, GC params, indexes, algorithms.
* **Verify:** Load-test again.
* **Automate:** Monitor via Prometheus + alerts.

**GC Tuning flags:** `-Xms`, `-Xmx`, `-XX:+UseG1GC`, `-XX:MaxGCPauseMillis=200`
**Memory Leak:** watch heap growth per GC cycle; use MAT dominator tree to find retained roots.

> **TL;DR: Profile → Measure → Tune → Monitor → Repeat.**

---

## 10) 🧪 Testing — TDD, BDD & Frameworks

### TDD (Test-Driven Development)

**Red → Green → Refactor cycle:**
1. **Red:** Write a failing test (no implementation yet).
2. **Green:** Write minimal code to make it pass.
3. **Refactor:** Improve structure without changing behavior.

```java
// Step 1: Write test (fails)
@Test
void shouldReturnSum() { assertEquals(5, Calculator.add(2, 3)); }

// Step 2: Implement minimal code
public static int add(int a, int b) { return a + b; }
```

### BDD (Behavior-Driven Development)

```
Given <initial context>
When  <event occurs>
Then  <expected outcome>
```

```gherkin
Feature: Login
  Scenario: Valid credentials
    Given a user "john" with password "1234"
    When  user logs in
    Then  login should be successful
```

```java
@Given("a user {string} with password {string}")
public void createUser(String user, String pass) { ... }

@When("user logs in")
public void login() { ... }

@Then("login should be successful")
public void verifyLogin() { assertTrue(result.isSuccess()); }
```

### JUnit 5 Cheat Sheet

| Category | Key Annotations | Example |
|:---|:---|:---|
| **Setup / Teardown** | `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll` | Init mocks, DB connections |
| **Test Case** | `@Test`, `@DisplayName`, `@Tag` | `@Test void shouldReturnTrue()` |
| **Assertions** | `assertEquals`, `assertTrue`, `assertThrows`, `assertAll` | `assertThrows(Exception.class, () -> ...)` |
| **Parameterized** | `@ParameterizedTest`, `@ValueSource`, `@CsvSource` | Multiple input variations |
| **Nested Tests** | `@Nested` | Group related tests logically |
| **Timeouts** | `@Timeout(5)` | Fail if execution exceeds limit |

### Mockito Cheat Sheet

| Concept | Example |
|:---|:---|
| **Mock Object** | `@Mock private UserRepository repo;` |
| **Inject Mocks** | `@InjectMocks private UserService service;` |
| **Setup Behavior** | `when(repo.findById(1)).thenReturn(Optional.of(user));` |
| **Verify Calls** | `verify(repo, times(1)).save(any(User.class));` |
| **ArgumentCaptor** | Capture arguments passed to mocks |
| **DoThrow** | `doThrow(new Exception()).when(repo).deleteById(1L);` |

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository repo;
    @InjectMocks private UserService service;

    @Test
    void shouldReturnUserWhenExists() {
        when(repo.findById(1)).thenReturn(Optional.of(new User("John")));
        User result = service.getUser(1);
        assertEquals("John", result.getName());
        verify(repo).findById(1);
    }
}
```

### Spring Test

| Annotation | Purpose | Example |
|:---|:---|:---|
| **@SpringBootTest** | Loads full application context | Integration tests |
| **@WebMvcTest** | Loads web layer only | `@WebMvcTest(UserController.class)` |
| **@DataJpaTest** | JPA repositories with in-memory DB | Auto-configures H2 |
| **@MockBean** | Spring-managed mock | `@MockBean UserRepository repo;` |
| **@AutoConfigureMockMvc** | Enables MockMvc for HTTP testing | |
| **@Sql / @SqlGroup** | Run SQL scripts before/after tests | DB setup |

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldReturnOkForGetUser() throws Exception {
        mockMvc.perform(get("/api/users/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("John"));
    }
}
```

### Test Pyramid

```
        /\
       /  \     E2E / Contract Tests (few, slow, expensive)
      /----\
     /      \   Integration Tests — Spring Boot, Testcontainers
    /--------\
   /          \ Unit Tests — JUnit 5 + Mockito (many, fast, cheap)
  /____________\
```

**Coverage targets:** Unit > 80%, Integration > 60% of critical paths, E2E on happy paths only.

### Testcontainers — Real DB in Tests

```java
@SpringBootTest
@Testcontainers
class TransactionRepositoryTest {

    @Container
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withDatabaseName("testdb").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      oracle::getJdbcUrl);
        registry.add("spring.datasource.username", oracle::getUsername);
        registry.add("spring.datasource.password", oracle::getPassword);
    }

    @Autowired TransactionRepository repo;

    @Test
    void shouldPersistAndRetrieve() {
        repo.save(new Transaction(/* ... */));
        assertThat(repo.findByStatus("PENDING")).hasSize(1);
    }
}
```

### RestAssured — API-Level Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionApiTest {
    @LocalServerPort int port;

    @Test
    void shouldReturn201OnCreate() {
        given()
            .port(port)
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .header("Authorization", "Bearer " + getTestToken())
            .body("""{"amount": 150.00, "currency": "USD", "accountId": 1}""")
        .when()
            .post("/v1/transactions")
        .then()
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .body("amount", equalTo(150.0f));
    }
}
```

### ArgumentCaptor

```java
@Test
void shouldSaveAuditRecordOnStatusChange() {
    service.updateStatus(1L, "SETTLED");
    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditRepo).save(captor.capture());
    assertThat(captor.getValue().getNewStatus()).isEqualTo("SETTLED");
}
```

### Key Testing Principles

- **Arrange / Act / Assert** — clear 3-section structure in every test.
- **One logical assertion per test** — easier to diagnose failures.
- **Test behavior, not implementation** — don't test private methods directly.
- **Use `@DisplayName`** — readable test names in reports.
- **Avoid `Thread.sleep()`** — use Awaitility for async assertions.
- **Flaky test = broken test** — fix or delete; never skip with `@Disabled`.

```java
// Awaitility — clean async assertions
await().atMost(5, SECONDS)
       .until(() -> repo.findByStatus("SETTLED").size() == 1);
```

---

## 11) 🐳 Docker

### Core Concepts

| Concept | Description |
|---|---|
| **Image** | Immutable snapshot built from a Dockerfile |
| **Container** | Running instance of an image |
| **Layer** | Each Dockerfile instruction adds a cached layer |
| **Registry** | Stores images (Docker Hub, ECR, Artifactory) |
| **Volume** | Persistent storage outside container lifecycle |
| **Network** | `bridge` (default), `host`, `overlay` (Swarm/K8s) |

### Dockerfile (Spring Boot — Multi-Stage Build)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw package -DskipTests -q

# Stage 2: Extract layered jar
FROM builder AS layers
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 3: Runtime — minimal image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.JarLauncher"]
```

### Docker Compose (Local Dev Stack)

```yaml
version: '3.9'
services:
  app:
    build: .
    ports: ["8080:8080"]
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:oracle:thin:@oracle:1521/XEPDB1
      SPRING_REDIS_HOST: redis
    depends_on:
      oracle: { condition: service_healthy }
      redis:  { condition: service_started }

  oracle:
    image: gvenzl/oracle-xe:21-slim-faststart
    environment: { ORACLE_PASSWORD: secret }
    ports: ["1521:1521"]
    healthcheck:
      test: ["CMD", "healthcheck.sh"]
      interval: 10s
      retries: 10

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    ports: ["9092:9092"]
```

### Essential Docker Commands

```bash
docker build -t payment-service:latest .
docker run -d -p 8080:8080 --name payment-svc \
  -e SPRING_PROFILES_ACTIVE=prod payment-service:latest

docker logs -f payment-svc
docker exec -it payment-svc sh
docker stats payment-svc

docker run --memory="512m" --cpus="1.0" payment-service:latest

docker compose up -d
docker compose logs -f app
docker compose down -v
```

### Container Best Practices

- **One process per container** — don't bundle DB + app.
- **Immutable images** — no SSH, no runtime package installs.
- **Non-root user** — `USER appuser` in Dockerfile.
- **Health check** — `HEALTHCHECK CMD curl -f http://localhost:8080/actuator/health`.
- **Layer cache** — copy `pom.xml` before `src/` so deps layer only rebuilds on pom change.
- **`.dockerignore`** — exclude `target/`, `.git/`, IDE files.

---

## 12) 🔄 CI/CD

### Pipeline Overview

```
Code Push → Build → Test → Static Analysis → Docker Build → Deploy (Dev) → Deploy (Prod)
```

| Stage | Tools | Purpose |
|---|---|---|
| **Source Control** | Git / GitHub / Bitbucket | Branch strategy, PRs |
| **Build** | Maven / Gradle | Compile, package JAR |
| **Unit Tests** | JUnit 5, Mockito | Fast feedback, gates merge |
| **Static Analysis** | SonarQube, Checkmarx | Code quality, CVEs |
| **Integration Tests** | Spring Boot Test, Testcontainers | Real-stack tests |
| **Docker Build** | Docker, Kaniko | Build & push image to registry |
| **Deploy (Dev/QA)** | cf push / kubectl apply | Automated on every merge |
| **Deploy (Prod)** | Blue-Green / Canary | Gated, manual approval or canary |

### GitHub Actions Pipeline

```yaml
name: CI/CD Pipeline
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: 'maven' }

      - name: Build & Unit Test
        run: mvn verify -DskipITs

      - name: SonarQube Scan
        env: { SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }} }
        run: mvn sonar:sonar -Dsonar.host.url=${{ secrets.SONAR_URL }}

      - name: Integration Tests
        run: mvn verify -P integration-tests

      - name: Build Docker Image
        run: docker build -t myregistry/payment-service:${{ github.sha }} .

      - name: Push to Registry
        run: |
          echo ${{ secrets.REGISTRY_PASSWORD }} | docker login -u ${{ secrets.REGISTRY_USER }} --password-stdin
          docker push myregistry/payment-service:${{ github.sha }}

  deploy-dev:
    needs: build-and-test
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to PCF Dev
        env: { CF_PASSWORD: ${{ secrets.CF_PASSWORD }} }
        run: |
          cf login -a $CF_API -u $CF_USER -p $CF_PASSWORD -o $CF_ORG -s dev
          cf push payment-service --docker-image myregistry/payment-service:${{ github.sha }}
```

### Jenkins Pipeline (Declarative)

```groovy
pipeline {
    agent { docker { image 'eclipse-temurin:21' } }
    environment {
        IMAGE_TAG = "${env.GIT_COMMIT[0..7]}"
        SONAR_TOKEN = credentials('sonar-token')
    }
    stages {
        stage('Build')       { steps { sh 'mvn package -DskipTests' } }
        stage('Unit Tests') {
            steps { sh 'mvn test' }
            post { always { junit 'target/surefire-reports/*.xml' } }
        }
        stage('SonarQube')  { steps { sh "mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN" } }
        stage('Docker Build & Push') {
            steps {
                sh "docker build -t registry/payment-service:${IMAGE_TAG} ."
                withCredentials([usernamePassword(credentialsId: 'docker-creds',
                        usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh "echo $PASS | docker login -u $USER --password-stdin"
                    sh "docker push registry/payment-service:${IMAGE_TAG}"
                }
            }
        }
        stage('Deploy Dev')  { when { branch 'develop' } steps { sh "cf push payment-service -f manifest-dev.yml" } }
        stage('Deploy Prod') {
            when { branch 'main' }
            input { message 'Deploy to production?' }
            steps { sh "cf push payment-service -f manifest-prod.yml" }
        }
    }
    post { failure { slackSend(channel: '#alerts', message: "Build failed: ${env.BUILD_URL}") } }
}
```

### Quality Gates (SonarQube)

```
Coverage    ≥ 80%
Duplication ≤ 3%
Bugs        = 0 (blocker/critical)
Security    = 0 (critical/blocker)
```

### Branch Strategy

```
main        → production; only merge via PR from release/*
develop     → integration; feature/* merges here
feature/*   → individual features; short-lived
release/*   → stabilization; hotfixes back to main + develop
hotfix/*    → urgent prod fixes; merge to main + develop
```

---

## 13) ☁️ PCF / Tanzu & Cloud Scaling

### PCF (Pivotal Cloud Foundry / Tanzu Application Service)

PCF is a PaaS that abstracts away infrastructure. Mastercard and many banks use it for internal platform-as-a-service.

```bash
cf login -a https://api.sys.example.com -u user@mc.com -o MyOrg -s Production
cf push payment-service
cf push payment-service --docker-image registry/payment-service:abc123

cf app payment-service
cf logs payment-service --recent
cf logs payment-service          # tail live

cf scale payment-service -i 4   # 4 instances (horizontal)
cf scale payment-service -m 1G  # memory per instance (vertical)
cf scale payment-service -k 2G  # disk quota

cf set-env payment-service SPRING_PROFILES_ACTIVE prod
cf restage payment-service

cf marketplace
cf create-service p.redis cache-small my-redis
cf bind-service payment-service my-redis
cf restage payment-service
```

### manifest.yml

```yaml
applications:
  - name: payment-service
    memory: 1G
    disk_quota: 1G
    instances: 2
    buildpacks:
      - java_buildpack_offline
    path: target/payment-service.jar
    env:
      SPRING_PROFILES_ACTIVE: prod
      JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 21.+ } }'
      JAVA_OPTS: >
        -XX:+UseContainerSupport
        -XX:MaxRAMPercentage=75.0
    health-check-type: http
    health-check-http-endpoint: /actuator/health
    routes:
      - route: payment-service.apps.example.com
    services:
      - my-oracle-db
      - my-redis
      - my-kafka
```

### Blue-Green Deployment (Zero-Downtime)

```bash
cf push payment-service-green -f manifest-green.yml
cf app payment-service-green                              # smoke test
cf map-route   payment-service-green apps.example.com --hostname payment-service
cf unmap-route payment-service       apps.example.com --hostname payment-service
cf delete payment-service -f
cf rename payment-service-green payment-service
```

### Auto-Scaling (App Autoscaler)

```yaml
instance_limits:
  min: 2
  max: 20
rules:
  - rule_type: http_latency
    rule_sub_type: avg_99th
    threshold: { min: 0, max: 200 }   # scale up if p99 > 200ms
  - rule_type: cpu
    threshold: { min: 10, max: 70 }
scheduled_limit_changes:
  - recurrence: 5
    executes_at: "2024-01-01T07:00:00Z"
    instance_limits: { min: 5, max: 20 }   # ramp up for business hours
  - recurrence: 5
    executes_at: "2024-01-01T19:00:00Z"
    instance_limits: { min: 2, max: 10 }   # scale back overnight
```

### 12-Factor App Principles

| Factor | Principle | Spring Boot Implementation |
|---|---|---|
| **I. Codebase** | One repo, many deploys | Git mono/per-service |
| **II. Dependencies** | Explicitly declare | `pom.xml` / `build.gradle` |
| **III. Config** | Store in env, not code | Spring Cloud Config, env vars |
| **IV. Backing Services** | Treat as attached resources | VCAP_SERVICES in PCF |
| **V. Build/Release/Run** | Strict separation | CI/CD pipeline stages |
| **VI. Processes** | Stateless, share-nothing | No local session; use Redis |
| **VII. Port Binding** | Export via port | `server.port=8080` |
| **VIII. Concurrency** | Scale via process model | `cf scale -i N` |
| **IX. Disposability** | Fast startup, graceful shutdown | `server.shutdown=graceful` |
| **X. Dev/Prod Parity** | Keep environments alike | Testcontainers, same image |
| **XI. Logs** | Treat as event streams | Logback to stdout → Splunk |
| **XII. Admin Processes** | Run as one-off tasks | Spring Batch jobs, `cf run-task` |

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Scaling Strategies Summary

| Strategy | Trigger | When to Use |
|---|---|---|
| **Horizontal (scale out)** | More instances | Stateless services — most Spring Boot apps |
| **Vertical (scale up)** | More CPU/RAM | Stateful / memory-heavy |
| **Auto-scale on CPU** | CPU > threshold | Compute-bound services |
| **Auto-scale on latency** | p99 > threshold | User-facing APIs with SLAs |
| **Auto-scale on queue lag** | Kafka lag high | Message consumers falling behind |
| **Scheduled scaling** | Time-based | Known traffic patterns (business hours) |
| **Blue-Green** | Deploy | Zero-downtime rollout + instant rollback |
| **Canary** | Deploy | Gradual rollout to % of users |

---

## 14) 🗄️ Persistence (JPA / ORM)

### Spring Data JPA

* `@RepositoryRestResource` for automatic REST endpoints.
* `PagingAndSortingRepository` for pagination.
* Custom finder methods (`findByEmail`, `findTop10ByStatusOrderByDateDesc`).
* Projections for lightweight DTOs.

### JPA / ORM (Entity Modeling)

* `@Entity`, `@Id`, `@GeneratedValue`.
* `@Column(nullable=false, unique=true)`, `@Enumerated(EnumType.STRING)`.
* Relationships: `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@JoinColumn`.

```java
@Entity
class User {
    @Id @GeneratedValue
    Long id;
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    List<Order> orders;
}
```

* Prefer **LAZY** to avoid loading large object graphs.
* Use `JOIN FETCH` in JPQL to load eagerly only when needed (avoids N+1).
* `@BatchSize(size=20)` — batch N+1 sub-selects into one query.

### JPA Deep Dive — N+1, Queries, Locking, Auditing

**N+1 Problem & Fix:**
```java
// BAD — 1 query for accounts + N queries for each account's transactions
List<Account> accounts = accountRepo.findAll();
accounts.forEach(a -> a.getTransactions().size()); // N lazy loads

// GOOD — single JOIN FETCH
@Query("SELECT a FROM Account a JOIN FETCH a.transactions WHERE a.status = :s")
List<Account> findWithTransactions(@Param("s") String status);

// GOOD — DTO projection (skips persistence context overhead)
@Query("SELECT new com.mc.dto.AccountSummary(a.id, a.balance, COUNT(t)) " +
       "FROM Account a LEFT JOIN a.transactions t GROUP BY a.id, a.balance")
List<AccountSummary> findSummaries();
```

**JPQL & Native Queries:**
```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Derived method query
    List<Transaction> findByStatusAndAmountGreaterThan(String status, BigDecimal amount);

    // JPQL — portable, entity-based
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :id AND t.createdAt >= :from")
    List<Transaction> findByAccountSince(@Param("id") Long id, @Param("from") LocalDate from);

    // Native SQL — for Oracle-specific features
    @Query(value = "SELECT * FROM transactions WHERE ROWNUM <= 10 ORDER BY amount DESC",
           nativeQuery = true)
    List<Transaction> findTop10ByAmount();

    // Modifying query
    @Modifying
    @Transactional
    @Query("UPDATE Transaction t SET t.status = :status WHERE t.id IN :ids")
    int bulkUpdateStatus(@Param("status") String status, @Param("ids") List<Long> ids);
}
```

**JPA Specifications (dynamic filtering):**
```java
public class TransactionSpecs {
    public static Specification<Transaction> hasStatus(String status) {
        return (root, query, cb) ->
            status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }
    public static Specification<Transaction> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> cb.between(root.get("amount"), min, max);
    }
}
// Usage
transactionRepo.findAll(hasStatus("PENDING").and(amountBetween(100, 5000)), pageable);
```

**Optimistic vs Pessimistic Locking:**
```java
// Optimistic — version column prevents lost updates; throws OptimisticLockException
@Entity
class Account {
    @Version Long version;   // JPA manages automatically
    BigDecimal balance;
}

// Pessimistic — DB-level row lock; use for critical sections
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
Account findAndLock(@Param("id") Long id);
```

**Auditing:**
```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {}

@Entity
@EntityListeners(AuditingEntityListener.class)
class Transaction {
    @CreatedDate      Instant createdAt;
    @LastModifiedDate Instant updatedAt;
    @CreatedBy        String createdBy;
    @LastModifiedBy   String updatedBy;
}

@Bean
public AuditorAware<String> auditorAware() {
    return () -> Optional.ofNullable(SecurityContextHolder.getContext())
        .map(ctx -> ctx.getAuthentication())
        .map(auth -> auth.getName());
}
```

**Pagination:**
```java
Page<Transaction> page = repo.findByStatus("PENDING",
    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

page.getContent();       // items on this page
page.getTotalElements(); // total record count
page.getTotalPages();
page.hasNext();
```

### ACID

| Property | Meaning | Ensures |
|:---|:---|:---|
| **A — Atomicity** | All or nothing | No partial transactions |
| **C — Consistency** | Valid state transitions | Data integrity maintained |
| **I — Isolation** | Transactions don't interfere | Correct results under concurrency |
| **D — Durability** | Results survive failures | Data safely persisted |

> **Analogy:** Atomicity = finish both debit + credit or erase both. Consistency = total money unchanged. Isolation = only one writer at a time. Durability = result is copied to permanent storage.

### Hibernate

| Concept | Description | Example |
|:---|:---|:---|
| **Entity** | Java class mapped to a table | `@Entity @Table(name="employee")` |
| **SessionFactory / EntityManager** | Factory for sessions; manages persistence context | `factory.openSession()` |
| **Persistent / Detached / Transient** | Object lifecycle states | Persistent = tracked in DB |
| **HQL** | Object-oriented SQL for entities | `FROM Employee e WHERE e.dept='IT'` |
| **Criteria API** | Type-safe query building | `cb.equal(root.get("name"), "John")` |
| **Caching** | First-level (Session) and Second-level (EhCache/Redis) | `hibernate.cache.use_second_level_cache=true` |

**Interview highlights:** `save()` vs `persist()` vs `merge()`; N+1 queries (`JOIN FETCH` or DTO projections); transaction management under the hood.

### SQL & NoSQL Trade-offs

| Factor | SQL | NoSQL |
|---|---|---|
| Schema | Rigid | Flexible |
| Scale | Vertical | Horizontal |
| Consistency | Strong (ACID) | Eventual |
| Queries | Rich joins | Limited aggregation |
| Use Case | FinTech, ERP | IoT, analytics, caching |

---

## 15) 🧮 Oracle SQL

### SQL Essentials

| Category | Examples |
|:---|:---|
| **DDL** | `CREATE`, `ALTER`, `DROP`, `TRUNCATE` |
| **DML** | `INSERT`, `UPDATE`, `DELETE`, `MERGE` |
| **DQL** | `SELECT * FROM EMP WHERE SAL > 5000` |
| **Constraints** | `PRIMARY KEY`, `UNIQUE`, `NOT NULL`, `CHECK`, `FOREIGN KEY` |
| **Joins** | `INNER`, `LEFT`, `RIGHT`, `FULL`, `SELF`, `CROSS` |
| **Functions** | String (`SUBSTR`, `INSTR`), Date (`SYSDATE`, `ADD_MONTHS`), Aggregate (`SUM`, `AVG`) |
| **Analytic Functions** | `ROW_NUMBER() OVER (PARTITION BY DEPT ORDER BY SAL DESC)` |
| **Set Operations** | `UNION`, `INTERSECT`, `MINUS` |
| **Transactions** | `COMMIT`, `ROLLBACK`, `SAVEPOINT` |

### PL/SQL Basics

| Concept | Description |
|:---|:---|
| **Blocks** | Anonymous, Procedure, Function, Trigger |
| **Cursor** | Iterate query results |
| **Exception Handling** | `BEGIN ... EXCEPTION WHEN OTHERS THEN ... END;` |
| **Stored Procedures** | Precompiled SQL logic for reusability |
| **Packages** | Grouped procedures/functions |

### EXPLAIN PLAN — Query Execution Blueprint

```sql
EXPLAIN PLAN FOR
SELECT e.name, d.dept_name
FROM employees e
JOIN departments d ON e.dept_id = d.dept_id
WHERE e.salary > 100000;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

| Column | Meaning |
|---|---|
| **Operation** | Step performed (TABLE ACCESS, NESTED LOOPS, etc.) |
| **Options** | Details (FULL, INDEX, BY ROWID) |
| **Cost** | Oracle's internal estimate (lower = better) |
| **Cardinality** | Estimated rows at that stage |

### Access Paths

| Path | Description | Use When |
|---|---|---|
| **FULL TABLE SCAN** | Reads every block | Small tables or no usable index |
| **INDEX RANGE SCAN** | Uses index for range/filter | Index on `WHERE` columns |
| **INDEX UNIQUE SCAN** | Unique key lookup | PK or unique constraint |
| **TABLE ACCESS BY ROWID** | Fetch row via index | Common after index scan |

### Join Methods

| Method | Description | Best For |
|---|---|---|
| **NESTED LOOPS** | For each outer row, find matches in inner | Small outer + indexed inner |
| **HASH JOIN** | Build hash table for smaller, probe with larger | Large joins without indexes |
| **MERGE JOIN** | Both inputs sorted; merge | Large, pre-sorted datasets |
| **CARTESIAN JOIN** | Every row × every row | Red flag — missing ON condition |

### DBMS_XPLAN — Actual Stats

```sql
ALTER SESSION SET statistics_level = ALL;

SELECT /*+ gather_plan_statistics */ *
FROM orders o JOIN customers c ON o.cid = c.cid;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));
```

* **A-Rows** = actual rows processed; **E-Rows** = estimated. If A ≫ E → stale stats.

### Optimizer Hints

| Hint | Description |
|---|---|
| `/*+ INDEX(table index_name) */` | Force index usage |
| `/*+ FULL(table) */` | Force full table scan |
| `/*+ USE_HASH(t1 t2) */` | Use hash join |
| `/*+ PARALLEL(table, 4) */` | Parallelize query |
| `/*+ LEADING(table) */` | Force join order |

### Indexing Strategies

| Type | Description | Use Case |
|---|---|---|
| **B-Tree** | Default balanced tree | High-selectivity columns |
| **Bitmap** | Bit arrays for distinct values | Low-cardinality columns (status, gender) |
| **Function-Based** | Index on computed value | `UPPER(name)`, `TRUNC(date)` |
| **Composite** | Multiple columns | Queries using left-most prefix |
| **Reverse Key** | Reverses index bytes | Avoids hot spots for sequential keys |

### Partitioning

| Type | Description | Use When |
|---|---|---|
| **Range** | Partition by numeric/date range | Orders by month |
| **List** | Discrete values | Region, country |
| **Hash** | Distribute evenly | Load balancing |
| **Composite** | Mix range + hash | Range by date, hash by ID |

```sql
CREATE TABLE sales (sale_id NUMBER, sale_date DATE, amount NUMBER)
PARTITION BY RANGE (sale_date)
(PARTITION p_2024 VALUES LESS THAN (TO_DATE('2025-01-01','YYYY-MM-DD')));
```

### Query Optimization Checklist

1. **Gather statistics:** `EXEC DBMS_STATS.GATHER_TABLE_STATS('SCHEMA','TABLE');`
2. **Eliminate unnecessary DISTINCT / GROUP BY**
3. **Avoid functions on indexed columns:** `WHERE name = INITCAP('john')` not `WHERE UPPER(name) = 'JOHN'`
4. **Use EXISTS instead of IN** for correlated subqueries
5. **Use bind variables** (`:param`) for reusable execution plans
6. **Materialize heavy subqueries** with CTE or temp tables
7. **Partition pruning** — filter on partition key
8. **Proper join order:** small → large table

### Dynamic Performance Views

| View | Description |
|---|---|
| `V$SQL` | SQL text, parse calls, executions |
| `V$SQL_PLAN` | Execution plan for cached SQL |
| `V$SESSION_LONGOPS` | Track long-running ops |
| `V$ACTIVE_SESSION_HISTORY` | Wait events & bottlenecks |
| `V$SQLAREA` | Aggregated SQL stats (shared pool) |

### Common Bottlenecks & Fixes

| Symptom | Likely Cause | Remedy |
|---|---|---|
| Full table scan | Missing/unused index | Add or hint index |
| High I/O waits | Unselective predicates | Filter earlier, partition |
| Cardinality mismatch | Stale stats | Gather fresh stats |
| Temp usage spikes | Large joins or sorts | Increase TEMP tablespace / add indexes |
| Row chaining | Long rows / small block size | Rebuild with PCTFREE adjustment |

---

## 16) ☸️ Kubernetes & Cloud Platform

### Cloud Ecosystems

**Public (AWS/Azure/GCP):** EKS/AKS/GKE, RDS/Dynamo/Cosmos, S3/Blob/GCS, CloudWatch/Monitor/Stackdriver.
- Advantages: scalability, elasticity, managed services.
- Challenges: cost, compliance, lock-in.

**Private (VMware/OpenStack/PCF/OpenShift):** self-service infra, SDN/SDS, strict governance.
- Advantages: control, data residency, strong security posture.
- Challenges: hardware scaling, maintenance overhead.

**Hybrid & Multi-Cloud:** private for regulated workloads; public for elastic traffic; Kubernetes, Terraform, and Vault as a common layer.

### Spring Boot in Cloud Context

* **Stateless services**, **externalized config** (Spring Cloud Config / Vault), **service discovery** (Eureka/Consul/K8s DNS).
* **Resilience4j**, **Micrometer → Prometheus/Grafana**, **OpenTelemetry** for full observability.
* Follow **12-factor** principles (see PCF section).

### Pods, Deployments & Services

* **Pods/Deployments** — manage replicated container sets.
* **Services** — `ClusterIP` (internal), `NodePort` (external on node), `LoadBalancer` (cloud LB).
* **ConfigMaps/Secrets** — externalize config and credentials.
* **Probes** — `liveness` (restart on failure), `readiness` (route traffic), `startup` (slow-starting apps).

### Autoscaling

* **HPA** — Horizontal Pod Autoscaler (CPU, memory, custom metrics like Kafka lag).
* **VPA** — Vertical Pod Autoscaler (right-sizes resource requests).
* **Cluster Autoscaler** — adds/removes nodes based on pending pods.

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: payment-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-service
  minReplicas: 2
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target: { type: Utilization, averageUtilization: 70 }
    - type: Resource
      resource:
        name: memory
        target: { type: Utilization, averageUtilization: 80 }
    - type: Pods                           # custom metric — Kafka consumer lag
      pods:
        metric: { name: kafka_consumer_lag }
        target: { type: AverageValue, averageValue: "1000" }
```

### Deployment with Probes & Resource Limits

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: payment-service
          image: registry/payment-service:abc123
          resources:
            requests: { cpu: "500m", memory: "512Mi" }
            limits:   { cpu: "1000m", memory: "1Gi" }  # OOMKilled if exceeded
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
            initialDelaySeconds: 15
            periodSeconds: 5
```

### Security & Observability

* **RBAC** — role-based access control for K8s resources.
* **Run as non-root** — `securityContext.runAsNonRoot: true`.
* **NetworkPolicies** — restrict pod-to-pod communication.
* **Prometheus + Grafana** — metrics scraping and dashboards.
* **Liveness/Readiness** — ensure only healthy pods receive traffic.

---

## 17) 🐧 Linux — Commands & Troubleshooting

### Navigation & File System

```bash
pwd                          # print working directory
ls -lah                      # long list, human-readable sizes, hidden files
ls -lt                       # sort by modification time (newest first)
cd -                         # go back to previous directory
find /var/log -name "*.log" -mtime -1    # logs modified in last 24h
find . -type f -size +100M               # files larger than 100MB
locate payment-service.jar               # fast filename search (uses index)
du -sh /opt/apps/*           # disk usage per directory, human-readable
df -h                        # disk space on all mounted filesystems
```

### File Operations & Text Processing

```bash
cat app.log                              # print entire file
less app.log                             # paginated view (q to quit, /term to search)
tail -f app.log                          # follow live log output
tail -n 200 app.log                      # last 200 lines
head -n 50 app.log                       # first 50 lines

# grep — search patterns
grep "ERROR" app.log                     # lines containing ERROR
grep -i "exception" app.log              # case-insensitive
grep -n "NullPointer" app.log            # show line numbers
grep -C 5 "OutOfMemory" app.log          # 5 lines of context around matches
grep -E "ERROR|WARN" app.log             # multiple patterns (extended regex)
grep -v "DEBUG" app.log                  # exclude DEBUG lines

# Chained log investigation
tail -f app.log | grep --line-buffered "ERROR"
cat app.log | grep "2026-05-28" | grep "FATAL" | wc -l   # count today's fatals

# awk — column extraction and filtering
awk '{print $1, $2, $NF}' app.log                        # print columns 1, 2, last
awk -F',' '{print $3}' data.csv                          # CSV third column
awk '$5 > 1000 {print $0}' metrics.log                   # filter rows by value
awk '{sum += $5} END {print "Total:", sum}' metrics.log  # sum a column

# sed — inline text substitution
sed 's/ERROR/CRITICAL/g' app.log         # replace all in output (no file change)
sed -i 's/localhost/prod-host/g' app.yml # replace in-place in file
sed -n '100,200p' app.log               # print lines 100–200
sed '/DEBUG/d' app.log                  # delete DEBUG lines from output

# cut, sort, uniq
cut -d':' -f1 /etc/passwd               # extract first field (username)
sort -k2 -n results.txt                 # numeric sort on second column
sort app.log | uniq -c | sort -rn       # frequency count, sorted descending
```

### Process Management

```bash
ps aux                                  # all running processes (user, pid, cpu, mem, cmd)
ps aux | grep java                      # find Java processes
ps -ef --forest                         # process tree view

top                                     # live process monitor (q to quit)
htop                                    # interactive process monitor (if installed)

# Kill processes
kill -9 <pid>                           # SIGKILL — force terminate
kill -15 <pid>                          # SIGTERM — graceful shutdown (default)
pkill -f "payment-service"              # kill by process name pattern
killall java                            # kill all java processes

# Background jobs
nohup java -jar app.jar > app.log 2>&1 &   # run detached, redirect stdout+stderr
jobs                                    # list background jobs in current shell
bg %1                                   # resume job 1 in background
fg %1                                   # bring job 1 to foreground
disown %1                               # detach job from shell (survives logout)
```

### System Resources

```bash
# CPU & Memory
free -h                                 # memory usage (total, used, available, swap)
vmstat 1 5                              # CPU/memory/IO stats, 5 samples 1s apart
mpstat -P ALL 1                         # per-CPU utilization
uptime                                  # load averages: 1m, 5m, 15m

# Disk I/O
iostat -xz 1                            # extended disk I/O stats per second
iotop                                   # live disk I/O by process (requires root)
lsblk                                   # list block devices and mount points

# Open files / limits
lsof -p <pid>                           # all files opened by a process
lsof -i :8080                           # what process is using port 8080
ulimit -n                               # max open file descriptors for current shell
cat /proc/<pid>/limits                  # limits for a specific process
```

### Networking

```bash
# Connectivity
ping -c 4 google.com                    # ICMP ping, 4 packets
traceroute google.com                   # hop-by-hop path
curl -I https://api.example.com         # HTTP headers only
curl -v -X POST https://api.example.com/health \
  -H "Content-Type: application/json" \
  -d '{"key":"value"}'                  # verbose POST with body
wget -O output.json https://api/data    # download to file

# Ports & Connections
ss -tulnp                               # listening ports + process (replaces netstat)
ss -tnp state established               # established TCP connections
netstat -tulnp                          # older alternative (may need net-tools)
nc -zv hostname 5432                    # test if port 5432 is open (TCP probe)
nc -zvu hostname 9092                   # UDP probe

# DNS
nslookup payment-service.internal       # DNS lookup
dig +short payment-service.internal     # clean DNS answer
host kafka-broker-1                     # simple hostname lookup

# Firewall (iptables / firewalld)
iptables -L -n -v                       # list all firewall rules
firewall-cmd --list-all                 # firewalld active rules
```

### Permissions & Users

```bash
# Permissions: rwxrwxrwx = owner | group | others
chmod 755 script.sh                     # rwxr-xr-x
chmod +x script.sh                      # add execute for all
chmod -R 644 /opt/config/              # recursive: rw-r--r--
chown appuser:appgroup app.jar         # change owner and group
chown -R appuser /opt/apps/            # recursive ownership change

# Users & Groups
whoami                                  # current user
id                                      # uid, gid, and groups
groups appuser                          # groups a user belongs to
sudo -u appuser java -jar app.jar      # run command as another user
su - appuser                            # switch to user (full login shell)
last                                    # recent login history
who                                     # who is currently logged in
```

### SSH & Remote Operations

```bash
ssh user@hostname                       # connect to remote host
ssh -i ~/.ssh/id_rsa user@hostname     # with specific key
ssh -L 5005:localhost:5005 user@host   # local port forward (remote debug)
ssh -N -f -L 9200:es-host:9200 user@jump  # background tunnel to Elasticsearch

scp app.jar user@host:/opt/apps/       # copy file to remote
scp user@host:/var/log/app.log ./      # copy file from remote
rsync -avz ./target/ user@host:/opt/   # sync directory (only changed files)

# Key management
ssh-keygen -t ed25519 -C "me@work.com" # generate SSH key pair
ssh-copy-id user@hostname              # install public key on remote host
cat ~/.ssh/id_ed25519.pub              # view public key to add to GitHub/servers
```

### Logs & System Journal

```bash
# systemd journal
journalctl -u payment-service          # logs for a specific service unit
journalctl -u payment-service -f       # follow live
journalctl -u payment-service --since "1 hour ago"
journalctl -p err -b                   # errors since last boot
journalctl --disk-usage                # how much space logs use

# System logs
dmesg | tail -50                        # kernel ring buffer (OOM, hardware errors)
dmesg | grep -i "oom\|killed"           # OOM killer events
cat /var/log/syslog | grep payment      # syslog (Debian/Ubuntu)
cat /var/log/messages | grep payment    # syslog (RHEL/CentOS)
```

### Cron & Scheduling

```bash
crontab -e                              # edit current user's crontab
crontab -l                              # list current crontabs
crontab -u appuser -l                  # list another user's crontab

# Cron expression format:
# MIN  HOUR  DOM  MON  DOW  command
#  *    *     *    *    *   /opt/scripts/cleanup.sh
0    2    *    *    *   /opt/scripts/backup.sh >> /var/log/backup.log 2>&1
*/5  *    *    *    *   /opt/scripts/healthcheck.sh   # every 5 minutes
0    9    *    *   1-5  /opt/scripts/report.sh        # 9am Mon-Fri
```

### Environment & Shell

```bash
env                                     # all environment variables
echo $JAVA_HOME                         # print a variable
export JAVA_HOME=/usr/lib/jvm/java-21  # set for current session
printenv PATH                           # print PATH

# Aliases & functions (add to ~/.bashrc or ~/.zshrc)
alias ll='ls -lah'
alias logs='tail -f /var/log/app/app.log | grep -v DEBUG'
alias ports='ss -tulnp'

source ~/.bashrc                        # reload shell config without restarting
```

### Scripting Essentials

```bash
#!/bin/bash
set -euo pipefail                       # exit on error, unset var, pipe failure

# Variables
APP_NAME="payment-service"
LOG_DIR="/var/log/$APP_NAME"

# Conditionals
if [ -f "$LOG_DIR/app.log" ]; then
    echo "Log exists"
elif [ -d "$LOG_DIR" ]; then
    echo "Dir exists but no log"
else
    mkdir -p "$LOG_DIR"
fi

# Loops
for file in /opt/apps/*.jar; do
    echo "Deploying: $file"
done

# Functions
check_port() {
    nc -zv "$1" "$2" 2>/dev/null && echo "OPEN" || echo "CLOSED"
}
check_port kafka-broker 9092

# Capture command output
PID=$(pgrep -f payment-service)
echo "Service PID: $PID"
```

---

### 🔧 Troubleshooting Scenarios

#### High CPU Usage

```bash
# 1. Find top CPU consumers
top -c                                  # press P to sort by CPU
ps aux --sort=-%cpu | head -10

# 2. For a Java process — identify hot threads
jstack <pid> > thread-dump.txt
# Look for RUNNABLE threads; the top CPU thread's hex NID matches `top -H -p <pid>`

# 3. Get thread-level CPU (convert TID decimal→hex to match jstack)
top -H -p <pid>                         # per-thread CPU inside a JVM
printf '%x\n' <tid>                     # convert TID to hex for jstack lookup

# 4. Flame graph (Async Profiler)
./profiler.sh -d 30 -f flame.html <pid>
```

#### Out of Memory / OOM Kill

```bash
# 1. Confirm OOM killer fired
dmesg | grep -i "killed process\|out of memory"
grep -i "oom" /var/log/syslog

# 2. Heap dump for analysis
jmap -dump:live,format=b,file=/tmp/heap.bin <pid>
# Open heap.bin in Eclipse MAT or HeapHero

# 3. Check current heap usage without dump
jcmd <pid> GC.heap_info
jstat -gcutil <pid> 1s 10

# 4. Watch memory growth live
watch -n 2 "ps -p <pid> -o pid,rss,vsz,cmd"   # RSS = resident memory (MB)
```

#### Port Already in Use

```bash
# Find what's holding a port
ss -tulnp | grep :8080
lsof -i :8080

# Kill the occupying process
kill -9 $(lsof -ti :8080)
```

#### Service Won't Start / Crashes

```bash
# 1. Check service status and recent logs
systemctl status payment-service
journalctl -u payment-service -n 100 --no-pager

# 2. Run manually to see startup error directly
sudo -u appuser java -jar /opt/apps/payment-service.jar

# 3. Check file descriptor limits (common cause of "too many open files")
ulimit -n                               # current limit
cat /proc/<pid>/fd | wc -l             # how many FDs the process has open
# Fix: add LimitNOFILE=65536 to the systemd unit file

# 4. Check disk space (full disk causes silent failures)
df -h
du -sh /var/log/* | sort -rh | head -10
```

#### Network / Connectivity Issues

```bash
# 1. Is the service listening?
ss -tulnp | grep 8080

# 2. Can we reach it locally?
curl -v http://localhost:8080/actuator/health

# 3. Can we reach a dependency (DB, Kafka, Redis)?
nc -zv oracle-host 1521
nc -zv kafka-host 9092
nc -zv redis-host 6379

# 4. DNS resolution working?
dig +short kafka-host
nslookup oracle-host

# 5. Trace the route if packets are dropping
traceroute kafka-host
mtr kafka-host                          # real-time traceroute (requires mtr)
```

#### Disk Space Full

```bash
# 1. Find the culprit directories
du -sh /* 2>/dev/null | sort -rh | head -10
du -sh /var/log/* | sort -rh | head -10

# 2. Find and remove large old log files
find /var/log -name "*.log" -mtime +7 -size +100M -exec ls -lh {} \;
find /var/log -name "*.log.gz" -mtime +30 -delete

# 3. Truncate a live log without stopping the process (safe)
> /var/log/app/app.log                  # truncates to 0 bytes, keeps file descriptor

# 4. Check and clear journal logs
journalctl --disk-usage
journalctl --vacuum-time=7d            # keep only last 7 days
journalctl --vacuum-size=500M          # keep only 500MB of logs
```

#### Zombie / Stuck Processes

```bash
# Find zombie processes (state Z in ps)
ps aux | awk '$8 == "Z" {print}'

# Find processes in uninterruptible sleep (D-state — usually I/O wait)
ps aux | awk '$8 == "D" {print}'
# D-state usually means storage/NFS issue — check iostat and dmesg

# Stuck Java thread — check for deadlocks
jstack <pid> | grep -A 20 "deadlock"
jstack <pid> | grep "BLOCKED" | wc -l
```

#### Log Analysis — Common Patterns

```bash
# Count exceptions by type in last hour's logs
grep "Exception" app.log | awk '{print $NF}' | sort | uniq -c | sort -rn

# Find slow requests (>1000ms) in Spring Boot logs
grep "Completed in" app.log | awk '$NF > 1000' | tail -20

# Extract all unique IPs from access log
awk '{print $1}' access.log | sort -u

# Find requests that returned 5xx
grep '" 5[0-9][0-9] ' access.log | wc -l

# Watch error rate live (errors per second)
watch -n 1 "tail -1000 app.log | grep -c ERROR"

# Parse structured JSON logs with jq
tail -f app.log | jq 'select(.level == "ERROR") | {time: .timestamp, msg: .message, trace: .traceId}'
```

### 🧠 Linux Quick Reference Card

| Goal | Command |
|:---|:---|
| Who owns port 8080? | `ss -tulnp \| grep :8080` |
| How much heap is the JVM using? | `jcmd <pid> GC.heap_info` |
| Why did the process die? | `dmesg \| grep -i killed` |
| Which files are open by PID? | `lsof -p <pid>` |
| How much disk does a dir use? | `du -sh /path/` |
| Top CPU threads in JVM | `top -H -p <pid>` |
| Follow logs for a service | `journalctl -u svc -f` |
| Test if a port is reachable | `nc -zv host port` |
| Count errors in log file | `grep -c ERROR app.log` |
| Find largest files | `find / -type f -size +500M` |
| Tail log and filter errors | `tail -f app.log \| grep ERROR` |
| Check memory free | `free -h` |
| Check load average | `uptime` |
| Kill process by name | `pkill -f payment-service` |
