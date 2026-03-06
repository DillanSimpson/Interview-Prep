# **Interview Cheat Sheet**

## 1) 🧠 Core Java

### Concurrency & Memory

* **Thread lifecycle:** `new → runnable → running → waiting/blocking → terminated`.
* **ExecutorService:** Use `Callable`/`Future` for managed concurrency.
* **Locks vs synchronized:** Use `synchronized` for most cases (simplicity); prefer `ReentrantLock` only for advanced needs like fairness or interruptible waits.
* **Volatile:** Ensures visibility but not atomicity.
* **Atomic classes:** (`AtomicInteger`, `AtomicReference`) for lock-free counters.
* **CompletableFuture:** Build async pipelines with `thenCompose`/`exceptionally`.
* **ForkJoinPool:** Efficient for recursive parallel tasks.
* **Semaphore:** Controls concurrent access (permits); useful for throttling or limiting resource usage.

```java
CompletableFuture.supplyAsync(() -> fetchData())
    .thenCombine(
        CompletableFuture.supplyAsync(() -> fetchUser()),
        (data, user) -> merge(data, user)
    )
    .exceptionally(ex -> fallback(ex));
// Example: fetchData and fetchUser are methods fetching data asynchronously; merge combines their results.
```

### Functional & Stream APIs

* `Function`, `Predicate`, `Supplier`, `Consumer` — core functional interfaces.
* Use `map`, `filter`, `flatMap`, `reduce`. Prefer parallel streams only when stateless and data-heavy.
* Combine with `Collectors.groupingBy`, `partitioningBy`.

```java
list.stream()
    .filter(u -> u.isActive())
    .collect(Collectors.groupingBy(User::getRole));
```

### Composition vs Inheritance

**Concepts**
- **Composition:** “has-a” → build with smaller parts.
- **Inheritance:** “is-a” → extend existing type.

| Aspect | Composition | Inheritance |
|--------|--------------|--------------|
| Relationship | Has-a | Is-a |
| Flexibility | High (runtime swap) | Low (compile-time) |
| Coupling | Loose | Tight |
| Encapsulation | Preserved | Often broken |
| Reuse | Delegation | Base-class reuse |
| Testing | Easy to mock | Complex |
| Maintenance | Localized | Fragile |
| Use When | Behavior changes often | Stable hierarchy |

#### Pros / Cons

* **Composition:** ✅ Modular, testable, flexible; ❌ Slightly more wiring, Can cause replication.
* **Inheritance:** ✅ Simple reuse; ❌ Tight coupling, ripple bugs.

**Tip:** Prefer composition unless the *is-a* relationship is clear and stable.

#### Patterns

* Composition → Strategy, Decorator, Adapter, Proxy  
* Inheritance → Template Method, Abstract Base

```java
interface RiskScorer { int score(Transaction t); }
class PaymentService {
  private final RiskScorer scorer;
  PaymentService(RiskScorer scorer) { this.scorer = scorer; }
}
```

### Reflection & Classloading

* Use `Class.forName()`, `getDeclaredFields()`, `Method.invoke()` for dynamic inspection.
* Keep reflection minimal — it breaks type safety and impacts performance.
* Custom ClassLoaders can isolate plugin modules or tenants.

### Exception & Immutability

* Use **custom exceptions** for API-level granularity.
* Favor **immutable objects** (`final` fields, no setters) to avoid race conditions.

### Checked Vs Uncehcked Exceptions

* **Checked exceptions** represent conditions you are *expected to handle* in your code.
* **Unchecked exceptions** (runtime exceptions) represent *programming errors* you are not required to catch.

---

## 🧩 1. Checked Exceptions

**Examples:** `IOException`, `SQLException`, `FileNotFoundException`, `ClassNotFoundException`

* These are **checked at compile time**.
* The compiler **forces** you to handle them using `try-catch` or declare them with `throws`.
* They usually represent **recoverable conditions** — something external went wrong that you can potentially fix or report gracefully.

**Example:**

```java
try {
    FileReader file = new FileReader("data.txt"); // may throw FileNotFoundException
    file.read();
} catch (IOException e) {
    System.out.println("File error: " + e.getMessage());
}
```

### ⚡ 2. Unchecked Exceptions

**Examples:** `NullPointerException`, `ArithmeticException`, `ArrayIndexOutOfBoundsException`, `IllegalArgumentException`

* Subclasses of `RuntimeException`
* **Not checked at compile time** — compiler doesn’t force you to catch or declare them.
* Represent **programming logic errors**, not external issues.
* Usually not recoverable at runtime; you fix your code instead of catching them.

**Example:**

```java
int a = 10 / 0;  // throws ArithmeticException at runtime
```

### 🧠 3. Custom exceptions

You can make your own:

```java
// Checked
class MyCheckedException extends Exception { }

// Unchecked
class MyUncheckedException extends RuntimeException { }
```

Use a **checked** exception if callers *should* handle the issue (like validation or resource unavailability).
Use **unchecked** if it’s a programming misuse (like invalid arguments).

| Type | Superclass | Checked at Compile? | Typical Use | Example |
| :---- | :-- | :- | :-- | :-------- |
| **Checked** | `java.lang.Exception` (not RuntimeException) | ✅ Yes | Recoverable, external | `IOException`, `SQLException` |
| **Unchecked** | `java.lang.RuntimeException` | ❌ No | Logic/programming error | `NullPointerException`, `ArithmeticException` |


---

In interviews, a crisp closing line works:

> “Checked exceptions represent expected, recoverable problems that must be handled or declared. Unchecked exceptions represent programming errors — the compiler ignores them because they usually indicate logic issues, not recoverable states.”


---

## 2) 🏗️ Data Structures & Use Cases

### Core Collections

| Data Structure | Avg. Time Complexity              | Typical Use / Notes             |
| -------------- | --------------------------------- | ------------------------------- |
| **Array**      | Access: O(1), Insert/Delete: O(n) | Fixed-size, fast random access. |
| **ArrayList**  | Access: O(1), Insert/Delete: O(n) | Dynamic resize, good for reads. |
| **LinkedList** | Access: O(n), Insert/Delete: O(1) | Fast at ends, high overhead.    |

### 🗺️ Maps

| Data Structure        | Avg. Time Complexity      | Typical Use / Notes                     |
| --------------------- | ------------------------- | --------------------------------------- |
| **HashMap**           | Access: O(1), Worst: O(n) | Fast lookup, unordered.                 |
| **ConcurrentHashMap** | Access: O(1)              | Thread-safe, low contention.            |
| **TreeMap**           | Access: O(log n)          | Sorted keys, slower than HashMap.       |
| **LinkedHashMap**     | Access: O(1)              | Predictable order, great for LRU cache. |
| **WeakHashMap**       | Access: O(1)              | Auto-clears when keys are GC’d.         |
| **EnumMap**           | Access: O(1)              | Optimized for enum keys.                |

### 🔁 Sets

| Data Structure | Avg. Time Complexity | Typical Use / Notes         |
| -------------- | -------------------- | --------------------------- |
| **HashSet**    | Access: O(1)         | Unique, unordered elements. |
| **TreeSet**    | Access: O(log n)     | Sorted unique elements.     |
| **BitSet**     | Access: O(1)         | Space-efficient flags.      |

### 🧵 Queues & Stacks

| Data Structure           | Avg. Time Complexity    | Typical Use / Notes         |
| ------------------------ | ----------------------- | --------------------------- |
| **Stack / Deque**        | Push/Pop: O(1)          | LIFO operations.            |
| **Queue / Deque**        | Offer/Poll: O(1)        | FIFO or double-ended.       |
| **BlockingQueue**        | Offer/Poll: O(1)        | Thread-safe blocking tasks. |
| **PriorityQueue**        | Insert/Delete: O(log n) | Min/max retrieval.          |
| **CopyOnWriteArrayList** | Read: O(1), Write: O(n) | Safe concurrent reads.      |

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

### 🏦 Interview: Choosing the Right Structure (Payment Context)

| Scenario | Best Choice | Reason |
|---|---|---|
| Dedup idempotency keys | `HashSet` | O(1) lookup |
| LRU cache (rate limit) | `LinkedHashMap` (access-order) | O(1) access + insertion order |
| Priority fraud queue | `PriorityQueue` | O(log n) min/max extraction |
| Thread-safe counter | `AtomicLong` | Lock-free, CAS-based |
| Concurrent config map | `ConcurrentHashMap` | Segment-level locking |
| Message buffer | `ArrayBlockingQueue` | Bounded, thread-safe FIFO |
| Sorted transaction list | `TreeMap<Date, Txn>` | Sorted by timestamp automatically |

### 🏗️ LRU Cache (Classic Interview Pattern)

```java
// Using LinkedHashMap with access-order — O(1) get and put
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    LRUCache(int capacity) {
        super(capacity, 0.75f, true);   // true = access-order
        this.capacity = capacity;
    }
    @Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > capacity;
    }
}
// Or: Spring's @Cacheable with Caffeine maximumSize(n) + expireAfterAccess
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

#### 🧭 General Framework / Bootstrapping

| Annotation | Purpose / Use Case | Notes |
|:-----------|:-------------------|:------|
| **@RestController** | Combines `@Controller` + `@ResponseBody` to expose REST APIs. | Auto-serializes to JSON/XML. |
| **@Controller** | Handles web requests, returns views (MVC). | Use with `ModelAndView`. |
| **@Service** | Marks business logic beans. | Singleton by default. |
| **@Repository** | DAO layer, translates SQL exceptions. | Used for JPA/JDBC. |
| **@Configuration** | Declares bean definitions. | Type-safe alt to XML. |
| **@Bean** | Defines a Spring-managed bean. | Lifecycle-aware, injectable. |
| **@Value("${property}")** | Injects config/env values. | Supports SpEL (`#{}`) syntax. |
| **@Transactional** | Defines DB transaction boundaries. | Supports rollback/isolation. |
| **@Profile("prod")** | Loads beans under specific env profile. | Common: `dev`, `test`, `prod`. |
| **@DependsOn("bean")** | Enforces bean init order. | Handles implicit dependencies. |
| **@ConditionalOnProperty** | Loads bean if config flag matches. | Feature toggles. |
| **@ConditionalOnMissingBean** | Loads bean only if not defined. | Avoids duplicates. |
| **@Lazy** | Delays bean creation. | Improves startup time. |
| **@Scope("prototype")** | Creates new instance per injection. | Default scope: singleton. |
| **@Cacheable**, **@CacheEvict** | Cache method results / clear entries. | Pair with `@CachePut`. |
| **@Retryable**, **@Recover** | Auto retries + fallback. | Needs `@EnableRetry`. |
| **@RateLimiter**, **@CircuitBreaker**, **@Bulkhead** | Limits/trips/isolates calls. | From *Resilience4j*. |
| **@Async** | Runs async methods. | Needs `@EnableAsync`. |
| **@Scheduled(cron="...")** | Periodic background tasks. | Needs `@EnableScheduling`. |
| **@RestControllerAdvice**, **@ControllerAdvice**, **@ExceptionHandler** | Global or targeted exception handling. | JSON responses for REST APIs. |
| **@ResponseStatus** | Maps exceptions to HTTP codes. | Use for validation errors. |
| **@EnableConfigurationProperties**, **@ConfigurationProperties** | Binds YAML props to POJOs. | Example: `prefix="app"`. |
| **@RestClientTest**, **@DataJpaTest**, **@WebMvcTest**, **@SpringBootTest**, **@MockBean**, **@TestConfiguration**, **@ExtendWith**, **@DisplayName** | Test scaffolding annotations. | Scope context for speed. |
| **@EnableAutoConfiguration**, **@EnableAspectJAutoProxy**, **@Aspect** | Bootstrapping / AOP setup. | For cross-cutting concerns. |

### 🧩 Core Dependency Injection Annotations

| Annotation | Purpose / Use Case | Notes |
|:-----------|:-------------------|:------|
| **@Autowired** | Injects a bean automatically by type. | Works on constructors, setters, or fields. |
| **@Qualifier("beanName")** | Specifies which bean to inject when multiple candidates exist. | Often used with `@Autowired`. |
| **@Primary** | Marks a bean as the default when multiple candidates exist. | Overridden by `@Qualifier`. |
| **@Resource(name="beanName")** | JSR-250 standard annotation; injects by name. | Alternative to `@Autowired`. |
| **@Inject** | JSR-330 standard equivalent of `@Autowired`. | Lacks `required=false` option. |
| **@Lookup** | Injects prototype-scoped beans into singletons. | Spring generates method implementations dynamically. |
| **@Component** | Marks a class as a Spring-managed bean. | The base stereotype for `@Service`, `@Repository`, etc. |
| **@ComponentScan(basePackages = "...")** | Tells Spring where to find annotated components. | Used with `@Configuration`. |
| **@Import(ConfigClass.class)** | Brings in other configuration classes. | Useful for modular setups. |
| **@Order(n)** | Sets priority when multiple beans implement the same interface. | Lower values = higher priority. |

### ⚙️ Configuration & Environment Annotations

| Annotation | Purpose / Use Case | Notes |
|:-----------|:-------------------|:------|
| **@ConfigurationProperties(prefix = "app")** | Binds externalized config (YAML/properties) to a POJO. | Requires a getter/setter or record-style object. |
| **@EnableConfigurationProperties(MyProps.class)** | Registers `@ConfigurationProperties` beans explicitly. | Usually unnecessary if class is already a component. |
| **@PropertySource("classpath:custom.properties")** | Loads additional `.properties` files into the Environment. | Works only with `.properties`, not YAML. |
| **@Value("${property.name}")** | Injects a single config value. | Supports SpEL with `#{}` syntax. |
| **@Profile("dev")** | Activates a bean/config only for a specific environment. | Combine with `spring.profiles.active`. |
| **Environment** *(interface)* | Programmatic access to environment variables and property sources. | Inject with `@Autowired` or constructor. |
| **@ConfigurationPropertiesScan** | Automatically scans for `@ConfigurationProperties` classes. | Alternative to manual registration. |
| **@ConditionalOnProperty** | Loads config or beans only if a property matches a value. | Common for feature flags. |
| **@ConditionalOnExpression** | Activates a bean when a SpEL expression evaluates to true. | Example: `@ConditionalOnExpression("'${env}' == 'prod'")`. |
| **@ActiveProfiles("test")** *(in tests)* | Sets profile for integration tests. | Works with JUnit + `@SpringBootTest`. |

### 🔮 Aspect-Oriented Programming (AOP) Annotations

| Annotation | Purpose / Use Case | Notes |
|:-----------|:-------------------|:------|
| **@Aspect** | Marks a class as an Aspect (cross-cutting logic). | Combine with `@EnableAspectJAutoProxy`. |
| **@Before("pointcut")** | Run advice before target method execution. | Often used for logging or validation. |
| **@After("pointcut")** | Run advice after method execution (regardless of outcome). | Similar to `finally` in try-catch. |
| **@AfterReturning(pointcut="...", returning="var")** | Executes after successful completion of method. | Can access return value. |
| **@AfterThrowing(pointcut="...", throwing="ex")** | Executes only if method throws an exception. | Ideal for error logging. |
| **@Around("pointcut")** | Wraps target method; allows custom pre/post logic. | Must return the result of `proceed()`. |
| **@Pointcut("execution(...)")** | Defines reusable join point expressions. | Helps organize advice neatly. |

### AOP Concepts

1. Aspect: An Aspect is a modular unit of cross-cutting concerns. For example, a logging aspect can be applied across various methods in different classes.
2. Advice: This is the action taken by an aspect at a particular join point. There are five types of advice:

* Before: Executed before the method call.
* After: Executed after the method call, regardless of its outcome.
* AfterReturning: Executed after the method returns a result, but not if an exception occurs.
* Around: Surrounds the method execution, allowing you to control the method execution and its result.
* AfterThrowing: Executed if the method throws an exception.

### Error Handling

```java
@ControllerAdvice
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
public String callExternalApi() {
    // Implementation goes here
}
// This method is called by Spring Retry after all retry attempts for callExternalApi() are exhausted.
@Recover
public String recover(IOException ex) {
    return "Fallback response";
}
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
* **@Async + TaskExecutor:** async workloads — always use a bounded `ThreadPoolTaskExecutor`, never the default unbounded one.
* **Actuator:** exposes `/health`, `/metrics`, `/info`, `/env` and more; endpoints can be enabled/disabled or customized via config.
* **Profiles:** environment isolation via `@Profile`.
  Activate a profile by setting `spring.profiles.active=dev` (in `application.properties`, environment variable, or JVM argument).
* **HikariCP** (default Spring Boot connection pool): tune `maximumPoolSize`, `connectionTimeout`, `idleTimeout`. Pool size ≈ cores × 2 for I/O-heavy services.

```java
// Bounded async thread pool — always configure explicitly
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
    // returns future — callers can chain or join
    return CompletableFuture.completedFuture("done");
}
```

```yaml
# HikariCP tuning in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000     # ms — fail fast, don't queue forever
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: HikariPool-Main
```

### @Transactional — Deep Dive

```java
// Propagation controls what happens when a transactional method calls another
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

// Rollback rules — by default only RuntimeException triggers rollback
@Transactional(rollbackFor = Exception.class)           // rollback on checked too
@Transactional(noRollbackFor = NotFoundException.class) // don't rollback on this

// readOnly hint — allows DB/JPA optimizations (no dirty checking, read replica routing)
@Transactional(readOnly = true)
```

**Common @Transactional pitfalls:**
- **Self-invocation bypass:** calling a `@Transactional` method from within the same class skips the proxy — extract to a separate Spring bean.
- **Private methods:** `@Transactional` on private methods is silently ignored — Spring can't proxy them.
- **Exception swallowing:** catching and not rethrowing prevents rollback.

### SOLID principles

The `SOLID` principles are five timeless guidelines for writing clean, maintainable, and extensible object-oriented software. They act as design guardrails—helping you avoid spaghetti code and “god classes.”

| Principle | Key idea | Benefit |
| :--| :- | :- |
| **S** — Single Responsibility | One class = one job | Easier maintenance |
| **O** — Open/Closed | Extend, don’t modify | Flexible & safe changes |
| **L** — Liskov Substitution | Subclasses honor contracts | Reliable polymorphism |
| **I** — Interface Segregation | Small interfaces | Less coupling |
| **D** — Dependency Inversion | Depend on abstractions | Easier testing & decoupling |

---

## 4) 🧰 REST API & Microservices Design

* **Resiliency:** Circuit breakers, bulkheads, fallback methods (use **Resilience4j** for implementation).
* **Observability:** Tracing (Zipkin via Spring Cloud Sleuth), metrics (Prometheus via Micrometer), logs (ELK via Logback/Logstash); all integrate seamlessly with Spring Boot using auto-configured starters.
* **Communication:** REST/gRPC (sync) vs Kafka/RabbitMQ (async).
* **Communication:** REST/gRPC (sync) vs Kafka/RabbitMQ (async).
  * Use **REST/gRPC** for request/response, low-latency, or transactional operations.
  * Use **Kafka/RabbitMQ** for event-driven, decoupled, or high-throughput scenarios where reliability and eventual consistency are needed.
* **Stateless design:** No HTTP sessions.
* **Idempotency:** Use request keys for POSTs.
* **Caching:** Use `ETag` & `Cache-Control`.

### Patterns

* **API Gateway → Service → Queue → DB.**
* Service discovery via Eureka/Consul.
* Config via Spring Cloud Config.

---

## 5) ☸️ Platform: Cloud, Kubernetes

## ☁️ Cloud Ecosystems (Private vs Public)

**Public (AWS/Azure/GCP):** EKS/AKS/GKE, RDS/Dynamo/Cosmos, S3/Blob/GCS, CloudWatch/Monitor/Stackdriver.
**Advantages:** scalability, elasticity, managed services.
**Challenges:** cost, compliance, lock-in.

**Private (VMware/OpenStack/PCF/OpenShift):** self-service infra, SDN/SDS, strict governance.
**Advantages:** control, data residency, security posture.
**Challenges:** hardware scaling, maintenance.

**Hybrid & Multi-Cloud:** private for regulated workloads; public for elastic traffic; Jenkins, Terraform, Vault, and Kubernetes as common layer.

### Spring Boot in Cloud Context

* **Stateless services**, **externalized config** (Config/Vault), **service discovery** (Eureka/Consul/K8s), **Resilience4j**, **Micrometer→Prometheus/Grafana**, **12-factor** principles.

## Cloud Design & Tools

### 1. Pagination — dividing *results for humans or clients*

Pagination is about **presenting data in manageable chunks** to users or client programs.
If you have 10 million transactions in a database, you don’t want to return them all at once. Instead, you serve them in *pages*:

* Page 1: items 1–100
* Page 2: items 101–200
* …and so on.

It’s a client-side or API-level concept — purely about *display* or *data access patterns*, not where data lives.

### 2. Load Balancing — dividing *traffic for fairness and speed*

Load balancing spreads **incoming requests or workloads** across multiple servers or nodes so none gets overwhelmed.
Imagine five identical web servers behind a load balancer. When traffic arrives:

* The balancer sends request 1 to server A,
* request 2 to server B,
* request 3 to server C, etc.

The goal is to prevent bottlenecks and improve reliability — if one node fails, the balancer routes around it.
It doesn’t care which *data* lives where; it just wants to keep all workers busy and responsive.

### 3. Partitioning — dividing *data for scalability and parallelism*

Partitioning (also called *sharding*) splits the **actual dataset** across multiple machines.
Each node holds a subset of the total data — say, transactions by customer ID, or by region.

For example:

* Node 1 stores customers A–M
* Node 2 stores customers N–Z

This makes reads and writes faster and lets you scale horizontally. It’s how distributed databases and in-memory grids (like Geode or Cassandra) handle huge datasets that can’t fit on one machine.

## Quick summary analogy

| Concept | Divides | Why | Example |
|:-|:----|:------|:------|
| Pagination | A *response* | To make results digestible | Show 20 search results per page  |
| Load balancing | *Requests* | To share traffic and avoid overload | Route HTTP requests among servers |
| Partitioning | The *data itself* | To scale storage and processing | Store customers A–M on one node, N–Z on another |

Pagination organizes how you **see** data,
load balancing organizes how you **send** work,
and partitioning organizes how you **store** data.

## Kubernetes & Cloud Security

* **Pods/Deployments**, Services (`ClusterIP/NodePort/LoadBalancer`).
* **ConfigMaps/Secrets**, probes (liveness/readiness/startup).
* **Autoscaling:** HPA / VPA / Cluster Autoscaler.
* **Observability:** Prometheus + Grafana.
* **Security:** RBAC, run as non-root, NetworkPolicies.

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

---

## 6) ⚙️ Kafka & Axon

### Axon + Kafka Overview

**Axon Framework** (CQRS + Event Sourcing) with **Kafka** as distributed event bus/store for scalable processors.

### Core Concepts Mapping

| Concept         | Axon                 | Kafka Equivalent | Purpose             |
| --------------- | -------------------- | ---------------- | ------------------- |
| Command Bus     | Direct P2P           | —                | Executes intent     |
| Event Bus       | Pub-sub              | Topic            | Distributes events  |
| Query Bus       | P2P / scatter-gather | —                | Fetches read models |
| Event Processor | Handler              | Consumer group   | Processes events    |
| Aggregate       | Domain root          | —                | Applies events      |
| Event Store     | Axon Server / Kafka  | Topic per type   | Persist/replay      |

### Kafka Configuration Cheat Sheet

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

**`group.id`**

* One message per partition per group at a time.
* Same `group.id` → load-balance within a service.
* Different `group.id`s → multiple services consume the same events.

Example:

| Service                | Group ID                 | Effect               |
| ---------------------- | ------------------------ | -------------------- |
| `payment-service`      | `payment-processor`      | Independent consumer |
| `notification-service` | `notification-processor` | Also receives events |

### Key Topics & Event Flow

| Topic              | Description                   |
| ------------------ | ----------------------------- |
| `axon.events`      | Domain events                 |
| `axon.commands`    | Optional command bus          |
| `axon.dead-letter` | Failed processing             |
| Custom topics      | Per aggregate/bounded context |

### Event Processors

| Type                 | Processing   | Use Case                 |
| -------------------- | ------------ | ------------------------ |
| SubscribingProcessor | Real-time    | Single-node/local replay |
| TrackingProcessor    | Offset-based | Distributed & replayable |

### Common Configs & Gotchas

| Config                       | Meaning                           | Tip                    |
| ---------------------------- | --------------------------------- | ---------------------- |
| `auto.offset.reset=earliest` | Start from beginning if no offset | For replay/testing     |
| `enable.auto.commit=false`   | Let Axon control commit           | Prevent loss           |
| `max.poll.interval.ms`       | Max time between polls            | Tune for slow handlers |
| `max.poll.records`           | Records per batch                 | Throughput control     |
| `acks=all`                   | Producer durability               | Wait for replicas      |

## Code Patterns

> **Producer**

```java
@EventHandler
public void on(OrderCreatedEvent event) {
    kafkaTemplate.send("axon.events", event.getOrderId(), event);
}
```

> **Consumer**

```java
@ProcessingGroup("order-events")
public class OrderEventHandler {
  @EventHandler
  public void on(OrderCreatedEvent event) {
      // update read model, trigger saga, etc.
  }
}
```

> `@ProcessingGroup` ≈ consumer `group.id`.

### Parallelism & Scaling

* **Partitions** = parallelism unit. Key by aggregate ID to preserve order.
* **Replicas** for fault tolerance.
* Scale consumers **to** (not beyond) partition count.

### Error Handling Kafka

* Automatic retries for transient errors.
* DLQ (e.g., `axon.dead-letter`) for permanent failures; custom retry policies supported.

### Typical Architecture

```vbnet
[Command -> Aggregate -> Event]
          ↓
     Kafka Topic ("axon.events")
          ↓
   [Event Processors / Sagas]
          ↓
     Read Models / External Systems
```

---

## 7) 🗄️ Persistence (Spring Data JPA, JPA/ORM, SQL & NoSQL)

### <u>Spring Data JPA</u>

* `@RepositoryRestResource` for automatic REST endpoints.
* `PagingAndSortingRepository` for pagination.
* Custom finder methods (`findByEmail`, `findTop10ByStatusOrderByDateDesc`).
* Projections for lightweight DTOs.

### <u>JPA / ORM (Entity Modeling)</u>

* `@Entity`, `@Id`, `@GeneratedValue`.
* `@Column(nullable=false, unique=true)`, `@Enumerated(EnumType.STRING)`.
* Relationships: `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@JoinColumn` (use `cascade` carefully).

```java
@Entity
class User {
  @Id @GeneratedValue
  Long id;
  @OneToMany(mappedBy="user", fetch=FetchType.LAZY)
  List<Order> orders;
}
```

* Lazy vs eager:
  * prefer **LAZY** to avoid loading large object graphs unnecessarily.
  * Use `JOIN FETCH` in JPQL to eagerly load only when needed (avoids N+1).
* Optimization:
  * JPQL/Criteria; native SQL for complex joins.
  * `@BatchSize(size=20)` — batch N+1 sub-selects into one.
  * Cache with Spring Cache or 2nd-level cache (EHCache, Redis).

### JPA Deep Dive — N+1, Queries, Locking, Auditing

**N+1 Problem & Fix:**
```java
// BAD — 1 query for accounts + N queries for each account's transactions
List<Account> accounts = accountRepo.findAll();
accounts.forEach(a -> a.getTransactions().size()); // N lazy loads

// GOOD — single JOIN FETCH
@Query("SELECT a FROM Account a JOIN FETCH a.transactions WHERE a.status = :s")
List<Account> findWithTransactions(@Param("s") String status);

// GOOD — DTO projection (no entity needed, skips persistence context overhead)
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
// Optimistic locking — version column prevents lost updates; throws OptimisticLockException
@Entity
class Account {
    @Version Long version;   // JPA manages this automatically
    BigDecimal balance;
}

// Pessimistic locking — DB-level row lock; use for critical sections
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
Account findAndLock(@Param("id") Long id);
```

**Auditing (@EntityListeners):**
```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {}

@Entity
@EntityListeners(AuditingEntityListener.class)
class Transaction {
    @CreatedDate   Instant createdAt;
    @LastModifiedDate Instant updatedAt;
    @CreatedBy     String createdBy;      // requires AuditorAware bean
    @LastModifiedBy String updatedBy;
}

@Bean
public AuditorAware<String> auditorAware() {
    return () -> Optional.ofNullable(SecurityContextHolder.getContext())
        .map(ctx -> ctx.getAuthentication())
        .map(auth -> auth.getName());
}
```

**Pagination with Spring Data:**
```java
Page<Transaction> page = repo.findByStatus("PENDING",
    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

page.getContent();       // items
page.getTotalElements(); // total count
page.getTotalPages();
page.hasNext();
```

## ⚙️ What ACID stands for

| Property | Meaning | Ensures |
| :------- | :------ | :-- |
| **A — Atomicity**   | All or nothing | No partial transactions   |
| **C — Consistency** | Valid state transitions | Data integrity maintained |
| **I — Isolation** | Transactions don’t interfere | Correct results under concurrency |
| **D — Durability**  | Results survive failures | Data safely persisted |

### <u>ACID principles</u>

* Four fundamental properties that guarantee **reliable and consistent transactions** in a database system.
* They ensure that even when something goes wrong — crashes, power failures, concurrent access — the data remains correct and trustworthy.

## 💡 Quick summary analogy

Imagine you’re writing a bank transaction on a whiteboard:

* **Atomicity:** Either you finish both debit & credit, or erase everything.
* **Consistency:** The total money in the system stays the same.
* **Isolation:** Only one person writes at a time, no overlapping edits.
* **Durability:** Once written, it’s copied to permanent storage.

## SQL & NoSQL Advanced Concepts

* **SQL**
  * Normalization
  * indexes (B-tree/hash)
  * ACID (`@Transactional(isolation=READ_COMMITTED)`)
  * joins, 
  * `EXPLAIN`.

```sql
SELECT u.name, o.amount
FROM users u JOIN orders o ON u.id = o.user_id
WHERE o.amount > 100;
```

* **NoSQL**
  * MongoDB (documents)
  * Cassandra/DynamoDB (wide-column)
  * Redis (cache/pub-sub/rate limit)
  * Elasticsearch (search/logs).

### Trade-offs

| Factor      | SQL           | NoSQL                   |
| ----------- | ------------- | ----------------------- |
| Schema      | Rigid         | Flexible                |
| Scale       | Vertical      | Horizontal              |
| Consistency | Strong (ACID) | Eventual                |
| Queries     | Rich joins    | Limited aggregation     |
| Use Case    | FinTech, ERP  | IoT, analytics, caching |

---

## 8) 🧠 Advanced Oracle SQL

### ⚙️ EXPLAIN PLAN — Query Execution Blueprint

#### What it does

`EXPLAIN PLAN` shows how Oracle *plans* to execute a SQL statement — what indexes, joins, and access paths it will use.

```sql
EXPLAIN PLAN FOR
SELECT e.name, d.dept_name
FROM employees e
JOIN departments d ON e.dept_id = d.dept_id
WHERE e.salary > 100000;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
```

#### Key Columns in Plan Output

| Column                       | Meaning                                            |
| ---------------------------- | -------------------------------------------------- |
| **Operation**                | Step performed (TABLE ACCESS, NESTED LOOPS, etc.)  |
| **Options**                  | Details of operation (FULL, INDEX, BY ROWID, etc.) |
| **Object Name**              | Table or index being accessed                      |
| **Rows**                     | Estimated number of rows returned                  |
| **Cost**                     | Oracle’s internal cost estimate (lower = better)   |
| **Cardinality**              | Estimated number of rows output at that stage      |
| **Bytes**                    | Estimated data size                                |
| **Filter/Access Predicates** | Conditions applied at that stage                   |

### Typical Access Paths

| Path                      | Description                    | Use When                              |
| ------------------------- | ------------------------------ | ------------------------------------- |
| **FULL TABLE SCAN**       | Reads every block              | Small tables or no usable index       |
| **INDEX RANGE SCAN**      | Uses index for range/filter    | Index on `WHERE` columns              |
| **INDEX UNIQUE SCAN**     | For unique key lookups         | PK or unique constraint               |
| **INDEX FULL SCAN**       | Reads all entries in index     | Query can be satisfied by index alone |
| **TABLE ACCESS BY ROWID** | Fetch row from table via index | Common after index scan               |

### 🧩 Join Methods in Execution Plans

| Method             | Description                                           | Best For                          |
| ------------------ | ----------------------------------------------------- | --------------------------------- |
| **NESTED LOOPS**   | For each row in outer table, find matches in inner    | Small outer set + indexed inner   |
| **HASH JOIN**      | Build hash table for smaller table, probe with larger | Large joins without indexes       |
| **MERGE JOIN**     | Both inputs sorted; merge results                     | Large, pre-sorted datasets        |
| **CARTESIAN JOIN** | Every row joined with every other                     | Red flag — missing `ON` condition |

### 📊DBMS_XPLAN Views

```sql
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));
```

**`DISPLAY_CURSOR`** shows *actual* runtime stats (requires `GATHER_PLAN_STATISTICS`).

```sql
ALTER SESSION SET statistics_level = ALL;

SELECT /*+ gather_plan_statistics */ ...
FROM orders o JOIN customers c ON o.cid = c.cid;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));
```

Look for:

* **A-Rows** = actual rows processed
* **E-Rows** = estimated rows
  If A ≫ E → poor cardinality estimate (gather stats!).

### 🧮 Optimizer Hints

Hints override Oracle’s cost-based optimizer decisions.

| Hint                             | Description           |
| -------------------------------- | --------------------- |
| `/*+ INDEX(table index_name) */` | Force index usage     |
| `/*+ FULL(table) */`             | Force full table scan |
| `/*+ USE_HASH(t1 t2) */`         | Use hash join         |
| `/*+ USE_NL(t1 t2) */`           | Use nested loop join  |
| `/*+ PARALLEL(table, 4) */`      | Parallelize query     |
| `/*+ LEADING(table) */`          | Force join order      |

***Example***

```sql
SELECT /*+ USE_HASH(e d) PARALLEL(e 4) */ 
  e.emp_id, d.dept_name
FROM employees e, departments d
WHERE e.dept_id = d.dept_id;
```

### 📈 Indexing Strategies

#### Types of Indexes

| Type                           | Description                    | Use Case                                     |
| ------------------------------ | ------------------------------ | -------------------------------------------- |
| **B-Tree**                     | Default balanced tree index    | High-selectivity columns                     |
| **Bitmap**                     | Bit arrays for distinct values | Low-cardinality columns (e.g., gender)       |
| **Function-Based**             | Index on computed value        | `UPPER(name)`, `TRUNC(date)`                 |
| **Composite**                  | Multiple columns               | When queries use left-most prefix            |
| **Reverse Key**                | Reverses index bytes           | Avoids index hot spots for sequential keys   |
| **Global / Local Partitioned** | Index for partitioned tables   | Use local when partitions queried separately |

### 🧮 Partitioning

| Type          | Description                     | Use When                  |
| ------------- | ------------------------------- | ------------------------- |
| **Range**     | Partition by numeric/date range | e.g., orders by month     |
| **List**      | Discrete values                 | Region, country           |
| **Hash**      | Distribute evenly by hash       | Load balancing            |
| **Composite** | Mix range + hash                | Range by date, hash by ID |

***Example***

```sql
CREATE TABLE sales (
  sale_id NUMBER,
  sale_date DATE,
  amount NUMBER
)
PARTITION BY RANGE (sale_date)
(
  PARTITION p_2024 VALUES LESS THAN (TO_DATE('2025-01-01','YYYY-MM-DD'))
);
```

### ⚙️ Query Optimization & Tuning

#### Checklist

1. **Gather statistics**

   ```sql
   EXEC DBMS_STATS.GATHER_TABLE_STATS('SCHEMA','TABLE');
   ```

2. **Eliminate unnecessary DISTINCT / GROUP BY**
3. **Avoid functions on indexed columns**

   ```sql
   -- Bad
   WHERE UPPER(name) = 'JOHN'
   -- Good
   WHERE name = INITCAP('john')
   ```

4. **Use EXISTS instead of IN** for correlated subqueries.
5. **Use bind variables** (`:param`) for reusable execution plans.
6. **Materialize heavy subqueries** with **CTE or temp tables**.
7. **Partition pruning:** filter on partition key.
8. **Use proper join order:** small → large table.

### 🧰 Dynamic Performance Views

| View                       | Description                        |
| -------------------------- | ---------------------------------- |
| `V$SQL`                    | SQL text, parse calls, executions  |
| `V$SQL_PLAN`               | Execution plan for cached SQL      |
| `V$SESSION_LONGOPS`        | Track long-running ops             |
| `V$SEGMENT_STATISTICS`     | Object-level I/O stats             |
| `V$SQLAREA`                | Aggregated SQL stats (shared pool) |
| `V$ACTIVE_SESSION_HISTORY` | Wait events & bottlenecks          |


### 🔍 Profiling Queries

```sql
SET AUTOTRACE ON
SELECT COUNT(*) FROM orders WHERE status='SHIPPED';
SET AUTOTRACE OFF;
```

Shows both result and execution plan.

Or:

```sql
SELECT SQL_ID, ELAPSED_TIME, CPU_TIME, DISK_READS
FROM V$SQL
WHERE SQL_TEXT LIKE '%ORDERS%';
```

### 🧩 Common Bottlenecks & Fixes

| Symptom              | Likely Cause                 | Remedy                                   |
| -------------------- | ---------------------------- | ---------------------------------------- |
| Full table scan      | Missing/unused index         | Add or hint index                        |
| High I/O waits       | Unselective predicates       | Filter earlier, partition                |
| CPU-bound sort       | Unnecessary ORDER BY         | Remove or pre-sort                       |
| Cardinality mismatch | Stale stats                  | Gather fresh stats                       |
| Temp usage spikes    | Large joins or sorts         | Increase `TEMP` tablespace / add indexes |
| Row chaining         | Long rows / small block size | Rebuild table with PCTFREE adjustment    |

### ⚡ Real-World Tip

If your plan looks *good* but performance isn’t —
run with:

```sql
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST +PEEKED_BINDS'));
```

This shows the *actual* bind variables Oracle saw, which can drastically change plan selection.

---

## ⚙️ **Hibernate Cheat Sheet**

**Purpose:** ORM (Object-Relational Mapping) framework that maps Java objects to database tables.

### 🔹 Core Concepts

| Concept                             | Description                                                           | Example                                         |
| :---------------------------------- | :-------------------------------------------------------------------- | :---------------------------------------------- |
| **Entity**                          | Java class mapped to a table.                                         | `@Entity @Table(name="employee")`               |
| **SessionFactory / EntityManager**  | Factory to create sessions; manages persistence context.              | `Session session = factory.openSession();`      |
| **Session / Transaction**           | Used to perform CRUD operations; transactions ensure ACID properties. | `session.beginTransaction();`                   |
| **Persistent, Detached, Transient** | Object states in Hibernate lifecycle.                                 | Persistent → saved in DB                        |
| **Lazy vs Eager Loading**           | Lazy loads only when accessed; Eager loads immediately.               | `@OneToMany(fetch = FetchType.LAZY)`            |
| **Cascade Types**                   | Propagate actions to related entities.                                | `CascadeType.ALL`                               |
| **HQL (Hibernate Query Language)**  | Object-oriented SQL for entities.                                     | `FROM Employee e WHERE e.dept='IT'`             |
| **Criteria API**                    | Type-safe query building.                                             | `criteria.add(Restrictions.eq("name", "John"))` |
| **Caching**                         | First-level (Session) and Second-level (EhCache, Redis).              | `hibernate.cache.use_second_level_cache=true`   |

### 🔹 Interview Highlights

* Difference between **save()**, **persist()**, and **merge()**.
* How Hibernate handles **N+1 queries** (use `JOIN FETCH` or DTO projections).
* How transactions are managed under the hood (`@Transactional`, JTA).
* Pros/Cons: Speeds development but adds overhead vs plain JDBC.

## 🌐 **REST vs SOAP Web Services Cheat Sheet**

### 🔹 REST (Representational State Transfer)

| Feature           | Description                             | Example                    |
| :---------------- | :-------------------------------------- | :------------------------- |
| **Protocol**      | HTTP (stateless).                       | CRUD = POST/GET/PUT/DELETE |
| **Data Format**   | JSON (lightweight).                     | `{"id":1,"name":"John"}`   |
| **Style**         | Resource-oriented (`/api/employees/1`). |                            |
| **Statelessness** | No client context stored on server.     |                            |
| **Caching**       | Supported via HTTP headers.             |                            |
| **Frameworks**    | Spring Boot, Jersey, JAX-RS.            |                            |

### 🔹 SOAP (Simple Object Access Protocol)

| Feature         | Description                                | Example                              |
| :-------------- | :----------------------------------------- | :----------------------------------- |
| **Protocol**    | XML over HTTP, SMTP, etc.                  | Uses **WSDL** for contract.          |
| **Data Format** | Strict XML structure.                      | `<soap:Envelope>...</soap:Envelope>` |
| **Style**       | Operation-oriented.                        | `getCustomerDetails()`               |
| **Security**    | WS-Security (header-based).                | XML Signature, Encryption            |
| **Tooling**     | JAX-WS, Apache CXF, Axis2.                 |                                      |
| **Contract**    | WSDL defines types, operations, endpoints. |                                      |

### 🔹 REST vs SOAP Comparison

| Feature         | REST                               | SOAP                                         |
| :-------------- | :--------------------------------- | :------------------------------------------- |
| **Ease of Use** | Simpler, JSON-based                | Verbose XML                                  |
| **Performance** | Faster, lighter                    | Slower, more secure by default               |
| **Flexibility** | Loose contract                     | Strict contract                              |
| **When to Use** | Public APIs, mobile, microservices | Enterprise transactions, legacy integrations |

## 🧠 **Oracle Database & SQL Cheat Sheet**

**Purpose:** RDBMS with advanced performance, security, and PL/SQL features.

### 🔹 SQL Essentials

| Category                          | Examples / Key Notes                                                                 |
| :-------------------------------- | :----------------------------------------------------------------------------------- |
| **DDL (Data Definition)**         | `CREATE`, `ALTER`, `DROP`, `TRUNCATE`                                                |
| **DML (Data Manipulation)**       | `INSERT`, `UPDATE`, `DELETE`, `MERGE`                                                |
| **DQL (Query)**                   | `SELECT * FROM EMP WHERE SAL > 5000;`                                                |
| **Constraints**                   | `PRIMARY KEY`, `UNIQUE`, `NOT NULL`, `CHECK`, `FOREIGN KEY`                          |
| **Joins**                         | `INNER`, `LEFT`, `RIGHT`, `FULL`, `SELF`, `CROSS`                                    |
| **Functions**                     | String (`SUBSTR`, `INSTR`), Date (`SYSDATE`, `ADD_MONTHS`), Aggregate (`SUM`, `AVG`) |
| **Analytic Functions**            | `ROW_NUMBER() OVER (PARTITION BY DEPT ORDER BY SAL DESC)`                            |
| **Set Operations**                | `UNION`, `INTERSECT`, `MINUS`                                                        |
| **Indexes**                       | Speeds up queries; use carefully to avoid DML slowdown.                              |
| **Views / Materialized Views**    | Logical abstraction / cached snapshots.                                              |
| **Transactions**                  | `COMMIT`, `ROLLBACK`, `SAVEPOINT` — ensure **ACID**.                                 |
| **Explain Plan / Execution Plan** | Use `EXPLAIN PLAN FOR ...` to analyze performance.                                   |

### 🔹 PL/SQL Basics

| Concept                | Description                                     |
| :--------------------- | :---------------------------------------------- |
| **Blocks**             | Anonymous, Procedure, Function, Trigger         |
| **Cursor**             | Used to iterate query results.                  |
| **Exception Handling** | `BEGIN ... EXCEPTION WHEN OTHERS THEN ... END;` |
| **Stored Procedures**  | Precompiled SQL logic for reusability.          |
| **Packages**           | Grouped procedures/functions.                   |

### 🔹 Optimization Tips

* Use **bind variables** to avoid hard parsing.
* Avoid **SELECT *** in production code.
* Create **indexes** only on frequently queried columns.
* Use **partitioning** for large tables.
* Analyze query cost with `AUTOTRACE` or `DBMS_XPLAN`.


## 🚀 **Integration View (Full-Stack / Microservices Context)**

| Layer                      | Role                  | Common Tools / Concepts                |
| :------------------------- | :-------------------- | :------------------------------------- |
| **Controller (REST/SOAP)** | Exposes API endpoints | `@RestController`, `@WebService`, WSDL |
| **Service**                | Business logic        | `@Service`, transactional boundaries   |
| **Persistence**            | ORM to DB             | Hibernate/JPA + Oracle                 |
| **Database**               | Data storage          | SQL tuning, indexes, PL/SQL            |
| **Messaging (Optional)**   | Async integration     | Kafka / MQ / NATS                      |

---

## 8) 🔐 Security

### Spring Security (AuthN/Z)

* JWT or OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`).
* Stateless APIs: `SessionCreationPolicy.STATELESS`.
* Passwords: `BCryptPasswordEncoder`.
* Role-based access: `@PreAuthorize("hasRole('ADMIN')")`.

```java
// Modern Spring Security 6 config
@Bean
SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http
    .csrf(csrf -> csrf.disable())                    // stateless API — no CSRF needed
    .sessionManagement(sm -> sm
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
public Account getAccount(Long accountId) { }  // owner-only check

@PostAuthorize("returnObject.ownerId == authentication.principal.id")
public Account findById(Long id) { }           // filter after method runs

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

**Spring Security Filter Chain Order (important for debugging):**
```
Request → ChannelProcessingFilter
        → SecurityContextPersistenceFilter
        → HeaderWriterFilter
        → CorsFilter
        → CsrfFilter (disabled for stateless)
        → BasicAuthenticationFilter / JwtAuthFilter (custom)
        → ExceptionTranslationFilter
        → FilterSecurityInterceptor (authorization)
        → Your Controller
```

### Secure Headers

* `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy`, `HSTS`, CORS per env.
* Never log sensitive fields — use `@JsonIgnore`, PII redaction filter, or structured log masking.
* Always validate inputs at the controller boundary (`@Valid`) and sanitize before DB writes.

### Secure SDLC & Threat Modeling

**Phases:** Requirements → Design (STRIDE) → Implementation (SAST/SCA) → Testing (DAST/fuzzing) → Deployment (runtime & IaC scans).

### Security Testing & Vulnerability Management

| Type     | Tools                             | Detects                     |
| -------- | --------------------------------- | --------------------------- |
| SAST     | Checkmarx, Fortify, SonarQube     | Code-level flaws            |
| SCA      | Snyk, Black Duck, OWASP Dep-Check | Library CVEs                |
| DAST     | ZAP, Burp Suite                   | Runtime flaws               |
| IaC Scan | Checkov, KICS                     | Insecure Terraform/K8s YAML |

**Lifecycle:** Discover → Validate → Prioritize (CVSS) → Patch → Retest → Report.

### Network & Event Security

* **Scanning:** Nmap, Nessus, Qualys.
* **Controls:** WAF, segmented VPCs.
* **SIEM/SOAR:** Splunk, QRadar, ELK, Azure Sentinel.
* **DLP:** Protect PCI/PII; encrypt at rest & in transit (TLS 1.3).

### Encryption (Reversible) vs Hashing (Irreversible)

#### Encryption

* Symmetric (AES/ChaCha20) and Asymmetric (RSA/ECC).
* Use for data at rest/in transit; recoverable.

#### Hashing

* SHA-256, bcrypt/scrypt/Argon2.
* Integrity, password storage; not reversible.

#### Quick Comparison

| Feature    | Encryption      | Hashing         |
| ---------- | --------------- | --------------- |
| Reversible | ✅               | ❌            |
| Uses a Key | ✅               | ❌            |
| Goal       | Confidentiality | Integrity/Auth  |
| Examples   | AES, RSA        | SHA-256, bcrypt |

#### Practical Rules

* Never encrypt passwords; hash with slow KDF + salt.
* Encrypt sensitive data you must read back (cards, PII, API keys).
* Use both: TLS channels, hashed secrets, optional ciphertext hashing.

### Security Use Cases (Interview Ready)

| Scenario                | Solution                               |
| ----------------------- | -------------------------------------- |
| Race condition in API   | synchronized or Redis distributed lock |
| API abuse / brute force | Redis rate limiting + CAPTCHA          |
| Sensitive logs          | PII redaction filter                   |
| DDoS                    | Rate limiting + CDN caching            |
| Outdated dependency     | Automate SCA → ticket → patch          |
| File upload risk        | MIME validation + antivirus (ClamAV)   |
| GC CPU spike            | Enable GC logs; analyze via GCeasy     |

---

## 10) 🧩 System Design/Patterns

### Design Patterns — Quick Ref (Java/Spring)

| Pattern                     | When to Use                                       | One-liner Example                                                  |
| --------------------------- | ------------------------------------------------- | ------------------------------------------------------------------ |
| **Factory**                 | Hide creation logic; return interface/abstraction | `CryptoFactory.get("AES").encrypt(data)`                           |
| **Abstract Factory**        | Create families of related objects                | `AwsStackFactory.createQueue(); createStore();`                    |
| **Builder**                 | Build complex immutable objects fluently          | `User.builder().id(1).name("Ada").build()`                         |
| **Singleton**               | One instance app-wide (stateless, thread-safe)    | `@Bean public ObjectMapper mapper(){ return new ObjectMapper(); }` |
| **Strategy**                | Swap algorithms at runtime                        | `hasher = new BcryptStrategy(); hasher.hash(pw)`                   |
| **Template Method**         | Fixed steps, customizable hooks                   | `AbstractJob.run() -> pre(); doRun(); post();`                     |
| **Decorator**               | Add behavior without changing core type           | `new LoggingClient(new RetryingClient(http))`                      |
| **Proxy**                   | Control access, lazy load, caching                | Spring AOP proxies around services                                 |
| **Adapter**                 | Make incompatible APIs play nice                  | Wrap legacy SOAP client behind `PaymentPort`                       |
| **Facade**                  | Simple API over complex subsystem                 | `PaymentFacade.authorizeAndCapture()`                              |
| **Observer / Pub-Sub**      | React to events                                   | Spring `ApplicationEventPublisher` / Kafka                         |
| **Chain of Responsibility** | Pipeline of filters/validators                    | Servlet filter chain; custom validation chain                      |
| **Command**                 | Encapsulate requests; queue/retry                 | `PlaceOrderCommand` -> handler -> outbox                           |
| **Repository**              | Encapsulate persistence                           | `UserRepository.findByEmail(...)`                                  |
| **Specification**           | Reusable query predicates                         | JPA Specification for dynamic filters                              |

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
  Response send(Request r){ Timer t=m.timer("http"); return t.record(() -> delegate.send(r)); }
}
```

#### Chain of Responsibility

```java
interface Rule { Optional<String> apply(Request r); }
class CompositeRules implements Rule {
  List<Rule> rules; public Optional<String> apply(Request r){ return rules.stream()
      .map(rule -> rule.apply(r)).filter(Optional::isPresent).findFirst().orElse(Optional.empty()); }
}
```

## ⚙️ Design Patterns (Security & System Context)

* **Singleton:** Central `SecurityConfig`, connection pool.
* **Factory:** Crypto algorithms, JWT parser.
* **Strategy:** Switch authentication/hash strategies (BCrypt ↔ Argon2).
* **Decorator:** Add logging, metrics, or auditing layers.
* **Observer:** Event notification to SIEM/monitoring.
* **Proxy:** API gateway enforcing auth & throttling.
* **Chain of Responsibility:** Servlet filters → validation → authorization.

---

## 🗺️ Microservices System

```less
[ Client / Web / Mobile ]
          |
          v
     [ CDN / WAF ]
          |
          v
 [ API Gateway / Nginx / LB ]
    ↑           |            ↑
    |           |            └─ auth, rate-limit, routing, metrics
    |           v
    |     ┌───────────────────────────────────────────────┐
    |     |            Stateless Microservices            |
    |     |───────────────────────────────────────────────|
    |     |  AuthSvc    OrderSvc    InventorySvc          |
    |     |  PaymentSvc  PricingSvc  NotificationSvc      |
    |     |  QuerySvc (Read API)                          |
    |     └───────────────────────────────────────────────┘
    |             |          |           |         |
    |             |          |           |         |
    |             ↓          ↓           ↓         ↓
    |       ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
    |       | Redis  |  | RDBMS  |  | Kafka  |  | Elastic|
    |       | (Cache)|  |(Stores)|  |(Async) |  | Search |
    |       └────────┘  └────────┘  └────────┘  └────────┘
    |          ↑           ↑            ↑          ↑
    |          |           |            |          |
    |  (read-through) (strong consistency)   (async stream)
    |
[ Analytics / Data Lake ]
       ↑             ↑
       |             |
 (CDC / streaming)   |
       |             |
   [ OLAP / Search (Elasticsearch) ]
```

### 🔄 Flow Semantics

| Symbol  | Meaning                  | Example                           |
| :------ | :----------------------- | :-------------------------------- |
| `→`     | Sync REST call           | Client → Gateway → AuthSvc        |
| `~>`    | Async event              | OrderSvc ~> Kafka ~> InventorySvc |
| `↔`     | Bidirectional stream     | Kafka ↔ QuerySvc                  |
| `↑ / ↓` | Data flow between layers | CDC → Analytics                   |

### 🧠 Core Concepts to Recall During Interviews

* **Gateway Layer:** Handles **auth, throttling, request shaping**, and **routing**.
* **Service Layer:** Each service is **independently deployable**, **stateless**, and owns its data.
* **Async Backbone:** Kafka enables **loose coupling**, **retries**, and **event-driven** communication.
* **Persistence & Cache:**

  * Redis → rate-limits, token blacklists, idempotent keys.
  * Postgres → domain data.
  * Elasticsearch → read models / fast queries.
* **Observability:** Logs + Metrics + Traces (OpenTelemetry).
* **External Integrations:** PaymentSvc → PSP, NotificationSvc → Email/SMS/Push.

### 🎙️ How to Talk Through It (30 Seconds)

* **Edge:** Nginx/API Gateway terminates TLS, validates JWTs, applies rate limits, and routes to stateless services.
* **Write Path:** Commands hit domain services → local DB commit + *outbox* → Kafka events.
* **Read Side:** Kafka feeds Query/Read API (materialized views or Elasticsearch) for low-latency reads.
* **Workflows:** Cross-service coordination via Sagas; Redis holds idempotency keys; Kafka partitioning enforces per-aggregate ordering.
* **Security / PCI:** PaymentSvc isolated inside PCI boundary; tokens and blacklists cached in Redis; tracing and metrics collected via OpenTelemetry + Prometheus.

## ⚙️ **Backpressure Handling**

### 💡 Definition

> **Backpressure** = controlling producer speed so consumers, queues, and downstream systems aren’t overwhelmed.

**Never accept more work than you can process within your latency & memory budget.**


### 🧱 **Layers & Controls**

#### 🧍 Edge (API / Gateway)

| Technique                              | Purpose                                          |
| -------------------------------------- | ------------------------------------------------ |
| **Rate Limiting (Token/Leaky Bucket)** | Reject overload with **HTTP 429 + Retry-After**  |
| **Load Shedding (Fail Fast)**          | Return **503** if inflight > limit or queue full |
| **Timeout Budgets**                    | Parent < child < downstream (no zombie requests) |
| **Circuit Breakers / Bulkheads**       | Isolate hot endpoints, stop cascading failures   |
| **Bounded Thread Pools**               | No unbounded queues—protect CPU & memory         |

#### 🗄️ Service → Database

| Control                         | Effect                           |
| ------------------------------- | -------------------------------- |
| **Connection pool caps**        | Limit DB concurrency             |
| **Retry + Backoff (w/ jitter)** | Smooth transient overload        |
| **Queue writes / buffer**       | Drain at fixed rate              |
| **Dead-letter queue (DLQ)**     | Store failed ops for later retry |

---

#### 📨 Messaging Systems (Kafka / SQS / MQ)

| Producer Side                 | Consumer Side                                    |
| ----------------------------- | ------------------------------------------------ |
| `acks=all`, `max.in.flight=1` | **Pull-based** → built-in backpressure           |
| Limit `buffer.memory`         | Use `pause()` / `resume()` when processing slows |
| Throttle by quota             | Scale consumers by **lag/time-to-drain**         |
| Ensure idempotency            | Safe reprocessing & retries                      |


#### 🔁 Reactive / Streaming (Reactor, RxJava, gRPC)

| Operator                                | Behavior                                 |                      |
| --------------------------------------- | ---------------------------------------- | -------------------- |
| `.onBackpressureBuffer(size, dropOldest | error)`                                  | Buffer bursts safely |
| `.onBackpressureDrop()` / `Latest()`    | Keep only most recent                    |                      |
| `.limitRate(n)`                         | Downstream pulls only what it can handle |                      |
| **Credit-based flow control**           | gRPC/HTTP2 manage demand windows         |                      |

---

### 🧩 **Patterns & Controls**

| Pattern                | Purpose                                      |
| ---------------------- | -------------------------------------------- |
| **Admission Control**  | Refuse new work beyond safe thresholds       |
| **Priority Lanes**     | Reserve capacity for high-value traffic      |
| **SLO-aware Shedding** | Drop requests when latency breaches budget   |
| **Retry Discipline**   | Limit attempts, exponential backoff + jitter |


### 📈 **Monitor These Metrics**

* Queue depth / Kafka lag
* P95/P99 latency
* Concurrency per worker
* Rate of 429/503 responses
* DB pool usage, run queue length

### 🧪 **Mini Recipes**

**Gateway Guardrail**

```text
if inflight >= MAX_CONCURRENCY or queueLen >= MAX_QUEUE:
    return 503  // shed load
```

**Reactor Example**

```java
Flux<Event> stream = source
  .onBackpressureLatest()
  .limitRate(256);
```

**Kafka Consumer Control**

```java
if (processingSlow()) consumer.pause(partitions);
if (recovered()) consumer.resume(partitions);
```

**DB Gate**

```java
if (!semaphore.tryAcquire()) throw new ServiceUnavailableException();
```

### 🧠 **Visual Summary**

```cpp
Producer → (bounded queue) → Workers → Downstream
      ↑         |                |
      |         |                └─ slow? reduce intake
      └─────────┴────── backpressure signal ────────┘
```

### 🏁 **TL;DR**

> **Cap concurrency. Bound queues. Prefer pull over push. Autoscale on lag. Shed early, not late.**

---

## ⚙️ **Java Performance Tools**

### 🧭 **Purpose Map**

| Goal                                | Tool Type        | Examples                                                        |
| :---------------------------------- | :--------------- | :-------------------------------------------------------------- |
| **Detect CPU / memory bottlenecks** | Profiler         | VisualVM, JProfiler, YourKit, Async Profiler                    |
| **Measure throughput / latency**    | Benchmarking     | JMH, Caliper                                                    |
| **Simulate load / concurrency**     | Load Testing     | JMeter, Gatling, k6                                             |
| **Observe live metrics**            | Monitoring / APM | JConsole, Java Mission Control, Micrometer, Prometheus, Grafana |
| **Inspect GC behavior**             | GC Analysis      | GC logs, GCViewer, GCeasy, JClarity Censum                      |
| **System-level analysis**           | OS & JVM tools   | `jcmd`, `jstat`, `jmap`, `jstack`, Flight Recorder              |
| **Distributed tracing**             | Observability    | OpenTelemetry, Zipkin, Jaeger                                   |
| **Heap / Leak diagnosis**           | Memory tools     | Eclipse MAT, HeapHero, VisualVM HeapDump                        |

### 🧩 **Key Categories**

#### 🧠 Profilers (Find Hot Spots)

| Tool                                   | Highlights                                                            |
| -------------------------------------- | --------------------------------------------------------------------- |
| **VisualVM** (bundled with JDK)        | Free GUI; CPU, memory, thread profiling, heap dumps.                  |
| **JProfiler** (commercial)             | Deep insight into heap, threads, DB, I/O calls; excellent call graph. |
| **YourKit Java Profiler**              | Low overhead, good for CI integration and production sampling.        |
| **Async Profiler**                     | Native, async-safe, ultra-low overhead—great for flame graphs.        |
| **Eclipse MAT (Memory Analyzer Tool)** | Post-mortem analysis from heap dumps.                                 |

#### ⚡ Load & Stress Testing

| Tool              | Use Case                                                          |
| ----------------- | ----------------------------------------------------------------- |
| **Apache JMeter** | GUI/CLI load testing for REST, SOAP, MQ, JDBC.                    |
| **Gatling**       | Scala-based DSL; strong for HTTP APIs and CI/CD pipelines.        |
| **k6**            | Modern JavaScript-based load testing; integrates with Prometheus. |
| **wrk / hey**     | Lightweight CLI HTTP load tools for quick spikes.                 |


#### 🔍 JVM & GC Diagnostics

| Command                                        | Purpose                                                |
| ---------------------------------------------- | ------------------------------------------------------ |
| `jcmd <pid> GC.heap_info`                      | Print heap and GC info.                                |
| `jstat -gcutil <pid> 1s`                       | Monitor GC usage in real-time.                         |
| `jmap -dump:live,format=b,file=heap.bin <pid>` | Dump heap for MAT.                                     |
| `jstack <pid>`                                 | Capture thread states (detect deadlocks / stalls).     |
| `jfr start/stop`                               | Start/stop Java Flight Recorder (JDK Mission Control). |
| `-Xlog:gc*`                                    | Enable structured GC logging.                          |
| **GCViewer / GCeasy**                          | Parse and visualize GC logs.                           |
| **JClarity Censum**                            | Commercial GC analyzer by ex-Oracle engineers.         |


#### 📊 Monitoring / Observability

| Tool                                                  | Function                                                                |
| ----------------------------------------------------- | ----------------------------------------------------------------------- |
| **Micrometer**                                        | Metrics facade for Spring Boot / Micronaut; feeds Prometheus / Datadog. |
| **Prometheus + Grafana**                              | Time-series metrics + dashboards.                                       |
| **OpenTelemetry**                                     | Tracing, metrics, logs with vendor-neutral API.                         |
| **Java Mission Control (JMC)**                        | Deep JVM introspection and Flight Recorder analysis.                    |
| **Elastic APM / New Relic / Dynatrace / Datadog APM** | Full-stack distributed tracing and service metrics.                     |

#### 🧪 Microbenchmarking

| Tool                                  | Highlights                                                                     |
| ------------------------------------- | ------------------------------------------------------------------------------ |
| **JMH (Java Microbenchmark Harness)** | Official tool for measuring method-level performance; handles warmup, JIT, GC. |
| **Caliper (Google)**                  | Lightweight microbenchmarking (less maintained now).                           |

Example JMH snippet:

```java
@Benchmark
@Fork(1)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
public void testSorting() {
    Arrays.sort(data);
}
```

### 📈 **Performance Workflow**

```
1️⃣ Baseline → 2️⃣ Profile → 3️⃣ Tune → 4️⃣ Verify → 5️⃣ Automate
```

* **Baseline:** Use JMH or JMeter to measure current throughput/latency.
* **Profile:** Attach VisualVM/JProfiler to identify hotspots.
* **Tune:** Adjust thread pools, GC params, indexes, algorithms.
* **Verify:** Load-test again.
* **Automate:** Monitor via Prometheus + alerts.


### 🧠 **Bonus**

* **GC Tuning:** `-Xms`, `-Xmx`, `-XX:+UseG1GC`, `-XX:+PrintGCDetails`
* **CPU Profiling Flame Graphs:** via Async Profiler or `perf + FlameGraph`
* **Memory Leak Detection:** watch heap growth, GC frequency, `MAT` dominator tree.

### 🏁 **TL;DR**

> **Profile → Measure → Tune → Monitor → Repeat.**
> VisualVM for local, JProfiler for deep, JMH for micro, JMeter for macro.

---

## 9) 🧪 Testing — TDD, BDD & Frameworks

## 🧩 **TDD (Test-Driven Development)**

**Core Idea:**
Write **tests before code**. Development follows the **Red → Green → Refactor** cycle:

1. **Red:** Write a failing test (no implementation yet).
2. **Green:** Write minimal code to make it pass.
3. **Refactor:** Improve code structure without changing behavior.

**Benefits:**

* Forces modular design and high test coverage.
* Detects regressions early.
* Encourages clean APIs and loose coupling.

**Example:**

```java
// Step 1: Write test (fails)
@Test
void shouldReturnSum() {
    assertEquals(5, Calculator.add(2, 3));
}

// Step 2: Implement minimal code
public static int add(int a, int b) { return a + b; }

// Step 3: Refactor if needed
```

---

## 🧠 **BDD (Behavior-Driven Development)**

**Core Idea:**
BDD extends TDD — focuses on **user behavior** and business outcomes using natural language scenarios.

**Structure:**

```
Given <initial context>
When <event occurs>
Then <expected outcome>
```

**Tools:**

* **Cucumber / JBehave** (feature files).
* **Spring Boot + TestRestTemplate** for service-level BDD tests.

**Example (Cucumber feature):**

```gherkin
Feature: Login
  Scenario: Valid credentials
    Given a user "john" with password "1234"
    When user logs in
    Then login should be successful
```

**Example (Step Definition):**

```java
@Given("a user {string} with password {string}")
public void createUser(String user, String pass) { ... }

@When("user logs in")
public void login() { ... }

@Then("login should be successful")
public void verifyLogin() { ... }
```

---

## ⚙️ **JUnit (v5) Cheat Sheet**

| Category                | Key Concepts / Annotations                                | Example                                               |
| :---------------------- | :-------------------------------------------------------- | :---------------------------------------------------- |
| **Setup / Teardown**    | `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`    | Initialize mocks, DB connections                      |
| **Test Case**           | `@Test`, `@DisplayName`, `@Tag`                           | `@Test void shouldReturnTrue() { ... }`               |
| **Assertions**          | `assertEquals`, `assertTrue`, `assertThrows`, `assertAll` | `assertThrows(Exception.class, () -> service.call())` |
| **Parameterized Tests** | `@ParameterizedTest`, `@ValueSource`, `@CsvSource`        | Run same test with multiple inputs                    |
| **Nested Tests**        | `@Nested`                                                 | Group related tests logically                         |
| **Timeouts**            | `@Timeout(5)`                                             | Fail if execution exceeds limit                       |

**Best Practices:**

* Name methods descriptively: `shouldReturn404WhenUserNotFound()`.
* Keep one logical assertion per test method.
* Use **assertAll** for grouped checks.

---

## 🧪 **Mockito Cheat Sheet**

**Purpose:** Mock dependencies to isolate unit under test.

| Concept                 | Example                                                 | Description                        |
| :---------------------- | :------------------------------------------------------ | :--------------------------------- |
| **Mock Object**         | `@Mock private UserRepository repo;`                    | Simulate dependency behavior       |
| **Inject Mocks**        | `@InjectMocks private UserService service;`             | Inject mocks into class under test |
| **Setup Behavior**      | `when(repo.findById(1)).thenReturn(Optional.of(user));` | Define mock responses              |
| **Verify Calls**        | `verify(repo, times(1)).save(any(User.class));`         | Ensure interaction occurred        |
| **ArgumentCaptor**      | Capture arguments passed to mocks                       | `captor.getValue()`                |
| **DoThrow / DoNothing** | `doThrow(new Exception()).when(repo).deleteById(1L);`   | Simulate exception behavior        |

**Example:**

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

## 🌱 **Spring Test Cheat Sheet**

**Purpose:** Integration and context-level testing for Spring Boot apps.

| Annotation                | Purpose                                   | Example                             |
| :------------------------ | :---------------------------------------- | :---------------------------------- |
| **@SpringBootTest**       | Loads full application context.           | For integration tests               |
| **@WebMvcTest**           | Loads web layer only (controllers).       | `@WebMvcTest(UserController.class)` |
| **@DataJpaTest**          | Tests JPA repositories with in-memory DB. | Auto-configures H2                  |
| **@MockBean**             | Spring-managed mock.                      | `@MockBean UserRepository repo;`    |
| **@AutoConfigureMockMvc** | Enables MockMvc for HTTP testing.         |                                     |
| **@Sql / @SqlGroup**      | Run SQL scripts before/after tests.       | Useful for DB setup                 |

**Example (MockMvc Test):**

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

---

## 🧩 **TDD/BDD Integration Flow**

| Stage                | Tool / Framework      | Purpose                                          |
| :------------------- | :-------------------- | :----------------------------------------------- |
| **Unit Test**        | JUnit + Mockito       | Test isolated methods, mock dependencies         |
| **Integration Test** | Spring Test / MockMvc | Test REST endpoints, DB integration              |
| **Behavior Test**    | Cucumber (BDD)        | Validate business behavior from user perspective |

---

### Unit Testing (TDD)

* Write tests first; use JUnit 5 (`@Test`, `@BeforeEach`, `@AfterEach`, `@DisplayName`).
* Assertions: `assertEquals`, `assertThrows`.
* Isolate business logic (avoid full Spring context).

```java
@Test
void shouldCalculateTax() {
    double result = calculator.calculateTax(100);
    assertEquals(10, result);
}
```

### Mocking (Mockito)

* `@Mock`, `@InjectMocks`, `when(...).thenReturn(...)`.

```java
when(service.fetchUser()).thenReturn(new User("Alice"));
```

### Integration Testing (Spring Boot)

* `@SpringBootTest(webEnvironment = RANDOM_PORT)`.
* `@TestConfiguration` for overrides.
* `@DataJpaTest` with H2 for repositories.

### Behavior-Driven Development (BDD)

* Cucumber/JBehave/RestAssured; Given/When/Then.

```gherkin
Given a valid token
When a request is sent to /users
Then response status is 200
```

### Best Practice Testing Patterns

**Test Pyramid:**
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

**Testcontainers — Real DB in tests (no H2 surprises):**
```java
@SpringBootTest
@Testcontainers
class TransactionRepositoryTest {

    @Container
    static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",    oracle::getJdbcUrl);
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

**RestAssured — API-level integration tests:**
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
            .header("Location", containsString("/v1/transactions/"))
            .body("status", equalTo("PENDING"))
            .body("amount", equalTo(150.0f));
    }
}
```

**ArgumentCaptor — verify what was saved:**
```java
@Test
void shouldSaveAuditRecordOnStatusChange() {
    service.updateStatus(1L, "SETTLED");
    ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
    verify(auditRepo).save(captor.capture());
    assertThat(captor.getValue().getNewStatus()).isEqualTo("SETTLED");
}
```

**Key Testing Principles:**
- **Arrange / Act / Assert** — clear 3-section structure in every test.
- **One logical assertion per test** — easier to understand failures.
- **Test behavior, not implementation** — don't test private methods directly.
- **Use `@DisplayName`** — readable test names in reports.
- **Avoid `Thread.sleep()`** — use `Awaitility` for async assertions.
- **Flaky test = broken test** — fix or delete; never ignore.

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
| **Image** | Immutable snapshot — built from a Dockerfile |
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
RUN ./mvnw dependency:go-offline -q          # cache deps layer separately
COPY src ./src
RUN ./mvnw package -DskipTests -q

# Stage 2: Extract layered jar (Spring Boot 2.3+ layertools)
FROM builder AS layers
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 3: Runtime — minimal image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser                                  # never run as root

COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \              # respect K8s/Docker CPU/memory limits
  "-XX:MaxRAMPercentage=75.0", \             # don't let JVM OOM the container
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.JarLauncher"]
```

### Docker Compose (Local Development Stack)

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
# Build & run
docker build -t payment-service:latest .
docker run -d -p 8080:8080 --name payment-svc \
  -e SPRING_PROFILES_ACTIVE=prod payment-service:latest

# Inspect
docker logs -f payment-svc
docker exec -it payment-svc sh
docker stats payment-svc                     # live CPU/memory usage

# Resource limits (mirror K8s requests/limits)
docker run --memory="512m" --cpus="1.0" payment-service:latest

# Compose
docker compose up -d
docker compose logs -f app
docker compose down -v                       # remove volumes too
```

### Container Best Practices

- **One process per container** — don't bundle DB + app.
- **Immutable images** — no SSH, no package installs at runtime.
- **Non-root user** — `USER appuser` in Dockerfile.
- **Read-only filesystem** — `docker run --read-only` where possible.
- **Health check** — `HEALTHCHECK CMD curl -f http://localhost:8080/actuator/health`.
- **Layer cache** — copy `pom.xml` before `src/` so deps layer is only rebuilt on pom change.
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
        stage('Build') {
            steps { sh 'mvn package -DskipTests' }
        }
        stage('Unit Tests') {
            steps { sh 'mvn test' }
            post { always { junit 'target/surefire-reports/*.xml' } }
        }
        stage('SonarQube') {
            steps { sh "mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN" }
        }
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
        stage('Deploy Dev') {
            when { branch 'develop' }
            steps { sh "cf push payment-service -f manifest-dev.yml" }
        }
        stage('Deploy Prod') {
            when { branch 'main' }
            input { message 'Deploy to production?' }
            steps { sh "cf push payment-service -f manifest-prod.yml" }
        }
    }
    post {
        failure { slackSend(channel: '#alerts', message: "Build failed: ${env.BUILD_URL}") }
    }
}
```

### Quality Gates (SonarQube)

```
Coverage    ≥ 80%
Duplication ≤ 3%
Bugs        = 0 (blocker/critical)
Security    = 0 (critical/blocker)
Code Smells ≤ Threshold
```

### Branch Strategy

```
main        → production; only merge via PR from release/*
develop     → integration; feature/* merges here
feature/*   → individual features; short-lived
release/*   → release stabilization; hotfixes back to main + develop
hotfix/*    → urgent prod fixes; merge to main + develop
```

---

## 13) ☁️ PCF / Tanzu & Cloud Scaling

### PCF (Pivotal Cloud Foundry / Tanzu Application Service)

PCF is a PaaS that abstracts away infrastructure. MasterCard and many banks use it for internal platform-as-a-service.

```bash
# Login
cf login -a https://api.sys.example.com -u user@mc.com -o MyOrg -s Production

# Push app (uses manifest.yml by default)
cf push payment-service

# Push with Docker image
cf push payment-service --docker-image registry/payment-service:abc123

# Check app status
cf app payment-service
cf logs payment-service --recent
cf logs payment-service          # tail live

# Scale manually
cf scale payment-service -i 4   # 4 instances (horizontal)
cf scale payment-service -m 1G  # memory per instance (vertical)
cf scale payment-service -k 2G  # disk quota

# Environment variables (externalize config — 12-factor)
cf set-env payment-service SPRING_PROFILES_ACTIVE prod
cf restage payment-service       # apply env changes

# Marketplace services (bind DB, Redis, etc.)
cf marketplace
cf create-service p.redis cache-small my-redis
cf bind-service payment-service my-redis
cf restage payment-service
```

### manifest.yml (PCF App Descriptor)

```yaml
applications:
  - name: payment-service
    memory: 1G
    disk_quota: 1G
    instances: 2                            # starting instance count
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
# 1. Push new version as "payment-service-green" (no traffic yet)
cf push payment-service-green -f manifest-green.yml

# 2. Smoke test green
cf app payment-service-green

# 3. Switch traffic (map route from blue to green)
cf map-route   payment-service-green apps.example.com --hostname payment-service
cf unmap-route payment-service       apps.example.com --hostname payment-service

# 4. Verify traffic is fully on green, then remove blue
cf delete payment-service -f

# 5. Rename green → canonical name for next cycle
cf rename payment-service-green payment-service
```

### Auto-Scaling on PCF (App Autoscaler)

```yaml
# autoscaler manifest
instance_limits:
  min: 2
  max: 20
rules:
  - rule_type: http_latency
    rule_sub_type: avg_99th
    threshold:
      min: 0
      max: 200                # scale up if p99 > 200ms
  - rule_type: cpu
    threshold:
      min: 10
      max: 70                 # scale up if CPU > 70%
  - rule_type: memory
    threshold:
      min: 20
      max: 80
scheduled_limit_changes:
  - recurrence: 5             # weekdays
    executes_at: "2024-01-01T07:00:00Z"
    instance_limits: { min: 5, max: 20 }   # ramp up for business hours
  - recurrence: 5
    executes_at: "2024-01-01T19:00:00Z"
    instance_limits: { min: 2, max: 10 }   # scale back overnight
```

### 12-Factor App Principles (PCF/Cloud Native)

| Factor | Principle | Spring Boot Implementation |
|---|---|---|
| **I. Codebase** | One repo, many deploys | Git monorepo or per-service |
| **II. Dependencies** | Explicitly declare | `pom.xml` / `build.gradle` |
| **III. Config** | Store in env, not code | `spring.cloud.config`, env vars |
| **IV. Backing Services** | Treat as attached resources | VCAP_SERVICES in PCF |
| **V. Build/Release/Run** | Strict separation | CI/CD pipeline stages |
| **VI. Processes** | Stateless, share-nothing | No local session, use Redis |
| **VII. Port Binding** | Export via port | `server.port=8080` |
| **VIII. Concurrency** | Scale via process model | `cf scale -i N` |
| **IX. Disposability** | Fast startup, graceful shutdown | `server.shutdown=graceful` |
| **X. Dev/Prod Parity** | Keep environments alike | Testcontainers, same image |
| **XI. Logs** | Treat as event streams | Logback to stdout → Splunk |
| **XII. Admin Processes** | Run as one-off tasks | Spring Batch jobs, `cf run-task` |

```yaml
# Graceful shutdown — drain in-flight requests on scale-down or restart
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Kubernetes Scaling (for comparison)

```yaml
# Horizontal Pod Autoscaler (HPA) — scale on CPU or custom metrics
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
    - type: Pods                          # custom metric (e.g., Kafka consumer lag)
      pods:
        metric: { name: kafka_consumer_lag }
        target: { type: AverageValue, averageValue: "1000" }
```

```yaml
# Deployment with resource limits + liveness/readiness probes
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
            limits:   { cpu: "1000m", memory: "1Gi" }  # never exceed; OOMKilled if over
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8080 }
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8080 }
            initialDelaySeconds: 15
            periodSeconds: 5
```

### Scaling Strategies Summary

| Strategy | Trigger | When to Use |
|---|---|---|
| **Horizontal (scale out)** | More instances | Stateless services — most Spring Boot apps |
| **Vertical (scale up)** | More CPU/RAM | Stateful / memory-heavy (only if can't go horizontal) |
| **Auto-scale on CPU** | CPU > threshold | General-purpose compute-bound services |
| **Auto-scale on latency** | p99 > threshold | User-facing APIs with SLAs |
| **Auto-scale on queue lag** | Kafka lag high | Message consumers falling behind |
| **Scheduled scaling** | Time-based | Known traffic patterns (business hours) |
| **Blue-Green** | Deploy | Zero-downtime rollout + instant rollback |
| **Canary** | Deploy | Gradual rollout to % of users |
