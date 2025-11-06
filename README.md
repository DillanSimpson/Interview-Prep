# **Interview Cheat Sheet**

## 🧠 **Core Java**

### Concurrency & Memory

- **Thread lifecycle:** `new → runnable → running → waiting/blocking → terminated`.
- **ExecutorService:** Use `Callable`/`Future` for managed concurrency.
- **Locks vs synchronized:** Prefer `ReentrantLock` for fairness and interruptible waits.
- **Volatile:** Ensures visibility but not atomicity.
- **Atomic classes:** (`AtomicInteger`, `AtomicReference`) for lock-free counters.
- **CompletableFuture:** Build async pipelines with `thenCompose`/`exceptionally`.
- **ForkJoinPool:** Efficient for recursive parallel tasks.
- **Semaphore** for throttling.
- **GC Tuning:** Use `-Xlog:gc*` to analyze allocation and pause patterns.

```java
CompletableFuture.supplyAsync(() -> fetchData())
   .thenCombine(fetchUser(), (data, user) -> merge(data, user))
   .exceptionally(ex -> fallback());
```

### Functional & Stream APIs

- `Function`, `Predicate`, `Supplier`, `Consumer` — core functional interfaces.
- Use `map`, `filter`, `flatMap`, `reduce`. Prefer parallel streams only when stateless and data-heavy.
- Combine with `Collectors.groupingBy`, `partitioningBy`.

```java
list.stream()
    .filter(u -> u.isActive())
    .collect(Collectors.groupingBy(User::getRole));
```

### Reflection & Classloading

- Use `Class.forName()`, `getDeclaredFields()`, `Method.invoke()` for dynamic inspection.
- Keep reflection minimal — it breaks type safety and impacts performance.
- Custom ClassLoaders can isolate plugin modules or tenants.

### Exception & Immutability

- Use **custom exceptions** for API-level granularity.
- Favor **immutable objects** (`final` fields, no setters) to avoid race conditions.

---

## 🧩 **Data Structures & Use Cases**

| Data Structure           | Time Complexity (Avg)                        | Pros                                    | Cons                                    | Ideal Use Cases                              |
| ------------------------ | -------------------------------------------- | --------------------------------------- | --------------------------------------- | -------------------------------------------- |
| **Array**                | Access: O(1) <br> Insert/Delete: O(n)        | Fast random access, cache friendly.     | Fixed size, costly resize.              | Static datasets, caching numeric data.       |
| **ArrayList**            | Access: O(1) <br> Insert/Delete: O(n)        | Dynamic resizing, easy iteration.       | Slow insertion/removal mid-list.        | Ordered data, frequent reads.                |
| **LinkedList**           | Access: O(n) <br> Insert/Delete: O(1) (ends) | Fast insert/delete at ends.             | High memory overhead, no random access. | Queues, job schedulers, undo stacks.         |
| **HashMap**              | Access: O(1) <br> Worst: O(n)                | Fast key lookup, null keys supported.   | Unordered, not thread-safe.             | Caching, lookups, symbol tables.             |
| **ConcurrentHashMap**    | Access: O(1)                                 | Thread-safe, concurrent reads/writes.   | Slightly higher memory use.             | Caches, rate-limiters, metrics tracking.     |
| **TreeMap**              | Access: O(log n)                             | Sorted keys, navigable map features.    | Slower than HashMap.                    | Leaderboards, range queries, sorted configs. |
| **HashSet**              | Access: O(1)                                 | Ensures unique elements.                | No ordering, not indexed.               | Membership checks, uniqueness enforcement.   |
| **TreeSet**              | Access: O(log n)                             | Sorted unique set.                      | Slower insert/remove.                   | Sorted collections (tags, ranks).            |
| **PriorityQueue (Heap)** | Insert/Delete: O(log n) <br> Peek: O(1)      | Efficient min/max retrieval.            | Not thread-safe.                        | Task scheduling, Dijkstra, throttling.       |
| **Stack (Deque)**        | Push/Pop: O(1)                               | Simple LIFO.                            | Overuse → readability issues.           | Expression parsing, recursion emulation.     |
| **Queue / Deque**        | Offer/Poll: O(1)                             | Thread-safe variants (`BlockingQueue`). | None major.                             | Producer-consumer, thread pools.             |
| **LinkedHashMap**        | Access: O(1)                                 | Predictable order, easy LRU cache.      | Slightly slower than HashMap.           | LRU caching, recently-used history.          |
| **CopyOnWriteArrayList** | Read: O(1), Write: O(n)                      | Safe concurrent reads.                  | Expensive writes.                       | Config lists, small read-mostly data.        |
| **BlockingQueue**        | Offer/Poll: O(1)                             | Thread-safe, blocking behavior.         | Can block indefinitely.                 | Task queues, async pipelines.                |
| **WeakHashMap**          | Access: O(1)                                 | Auto-clears when keys GC’d.             | Weak refs may vanish.                   | Cache for non-critical metadata.             |
| **EnumMap**              | Access: O(1)                                 | Compact & fast for enum keys.           | Works only with enums.                  | Feature flags, enum-based lookup.            |
| **BitSet**               | Access: O(1)                                 | Space-efficient booleans.               | Fixed size.                             | Flags, bitmap operations.                    |

---

# ⚙️ **Design Patterns (Security & System Context)**

- **Singleton:** Central `SecurityConfig`, connection pool.
- **Factory:** Crypto algorithms, JWT parser.
- **Strategy:** Switch authentication/hash strategies (BCrypt ↔ Argon2).
- **Decorator:** Add logging, metrics, or auditing layers.
- **Observer:** Event notification to SIEM/monitoring.
- **Proxy:** API gateway enforcing auth & throttling.
- **Chain of Responsibility:** Servlet filters → validation → authorization.

---

## 🌐 **Spring Boot Mastery**

### Architecture Layers

- **Controller:** REST entry point (`@RestController`, `@RequestMapping`).
- **Service:** Business logic.
- **Repository:** Data layer.
- **DTO:** Transfer objects, decoupled from persistence.
- **Validation:** `@Valid`, `@NotNull`, `@Pattern` via `spring-boot-starter-validation`.

### **Spring Boot Annotation Reference**

| Annotation                            | Definition                                                                                     | Use Case                                                        | Notes                                                                                                                                          |
| ------------------------------------- | ---------------------------------------------------------------------------------------------- | --------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| **@RestController**                   | Combines `@Controller` + `@ResponseBody`; marks a class as a REST endpoint returning JSON/XML. | Used for API endpoints.                                         | Automatically serializes return objects to HTTP response.                                                                                      |
| **@Repository**                       | Indicates a persistence/DAO layer component.                                                   | Database access logic (JPA, JDBC, etc.).                        | Enables exception translation (e.g., converts SQL exceptions to `DataAccessException`).                                                        |
| **@Value("\${property}")**            | Injects property values from configuration files (`application.yml` or `.properties`).         | Inject config variables like URLs, secrets.                     | Supports SpEL (Spring Expression Language).                                                                                                    |
| **@DependsOn**                        | Forces bean initialization order                                                               | Static initializers/singletons that depend on global resources  | Ensuring Lifecycle Hooks Execute in Correct Order. Use when beans depend on other **@PostConstruct** methods, **@DependsOn** guarantees order. |
| **@Transactional**                    | Defines transaction boundaries on methods or classes.                                          | Used for database operations that need rollback on failure.     | Supports propagation/isolation levels.                                                                                                         |
| **@ControllerAdvice**                 | Global handler for exceptions or cross-cutting controller logic.                               | Centralizes error handling.                                     | Combine with `@ExceptionHandler`.                                                                                                              |
| **@RestControllerAdvice**             | Same as above but automatically adds `@ResponseBody`.                                          | Consistent API error responses.                                 | Ideal for JSON error output.                                                                                                                   |
| **@Cacheable("name")**                | Caches method return values based on parameters.                                               | Used to improve performance for read-heavy APIs.                | Combine with `@CacheEvict`, `@CachePut` for cache control.                                                                                     |
| **@CacheEvict**                       | Removes entries from cache.                                                                    | After updates/deletes.                                          | Can clear all cache (`allEntries=true`).                                                                                                       |
| **@Async**                            | Marks a method for asynchronous execution.                                                     | Offload long-running tasks (email, logs).                       | Requires `@EnableAsync`.                                                                                                                       |
| **@Scheduled(cron="...")**            | Runs scheduled tasks periodically.                                                             | Background jobs, maintenance tasks.                             | Requires `@EnableScheduling`.                                                                                                                  |
| **@Lazy**                             | Delays bean creation until first requested.                                                    | Improves startup time for heavy beans.                          | Use cautiously for dependencies.                                                                                                               |
| **@Scope("prototype")**               | Creates a new bean instance on each injection.                                                 | For stateful or request-specific beans.                         | Default is singleton.                                                                                                                          |
| **@ConditionalOnProperty**            | Enables a bean only if a property in config matches a condition.                               | Feature toggles (e.g., enable Redis caching only if flag=true). | Example: `@ConditionalOnProperty(name="feature.x", havingValue="true")`.                                                                       |
| **@ConditionalOnMissingBean**         | Loads bean only if another bean of same type is not present.                                   | Override auto-config behavior.                                  | Common in library development.                                                                                                                 |
| **@EnableConfigurationProperties**    | Enables property binding to POJOs with `@ConfigurationProperties`.                             | Bind YAML configs to objects.                                   | Simplifies config injection.                                                                                                                   |
| **@RateLimiter** _(custom/3rd-party)_ | Limits number of method executions per time window.                                            | API rate limiting / abuse prevention.                           | Often from Resilience4j: `@RateLimiter(name="apiLimiter").                                                                                     |

---

### Error Handling

```java
@ControllerAdvice
class GlobalHandler {
  @ExceptionHandler(Exception.class)
  ResponseEntity<?> handle(Exception ex) {
     return ResponseEntity.status(500)
       .body(Map.of("error", ex.getClass().getSimpleName(), "message", ex.getMessage()));
  }
}
```

### Caching

```java
@Cacheable("users")
public User findById(Long id) { return repo.findById(id).orElseThrow(); }
```

### Validation

Use `@Valid`, `@NotNull`, `@Pattern`.
Prefer custom validators for complex inputs.

### Performance & Resilience

- **Resilience4j:** circuit breakers, retries, bulkheads.
- **@Async + TaskExecutor:** async workloads.
- **Actuator:** expose `/health`, `/metrics`.
- **Profiles:** environment isolation via `@Profile`.

### Spring Data JPA

- `@RepositoryRestResource` for automatic REST endpoints.
- `PagingAndSortingRepository` for pagination.
- Custom finder methods (`findByEmail`, `findTop10ByStatusOrderByDateDesc`).
- Projections for lightweight DTOs.

---

## 🧩 **Testing — TDD, BDD & Frameworks**

### Unit Testing (TDD)

- Write tests **before implementation** to define behavior.
- Use **JUnit 5** annotations:

  - `@Test`, `@BeforeEach`, `@AfterEach`, `@DisplayName`.
  - Assertions: `assertEquals`, `assertThrows`.

- Isolate business logic — avoid Spring context unless required.

```java
@Test
void shouldCalculateTax() {
    double result = calculator.calculateTax(100);
    assertEquals(10, result);
}
```

### Mocking (Mockito)

- Replace dependencies to isolate unit logic.
- `@Mock`, `@InjectMocks`, `when(...).thenReturn(...)`.

```java
when(service.fetchUser()).thenReturn(new User("Alice"));
```

### Integration Testing (Spring Boot)

- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)`.
- `@TestConfiguration` for local overrides.
- `@DataJpaTest` for repository layers with H2 in-memory DB.

### Behavior-Driven Development (BDD)

- Use **Cucumber**, **JBehave**, or **RestAssured** for BDD.
- Scenarios written as “Given / When / Then”.

```gherkin
Given a valid token
When a request is sent to /users
Then response status is 200
```

## 🧰 **REST API & Microservices Design**

- **Resiliency:** Circuit breakers, bulkheads, fallback methods.
- **Observability:** Tracing (Zipkin), metrics (Prometheus), logs (ELK).
- **Communication:**
  - Synchronous: REST/gRPC.
  - Asynchronous: Kafka/RabbitMQ.
- **Security:**
  - JWT across services.
  - mTLS for internal communication.
  - Rate limiting at API Gateway.
- **Stateless design:** No HTTP sessions.
- **Versioning:** `/api/v1/...`.
- **Idempotency:** Use request keys for POSTs.
- **Caching:** Use `ETag` & `Cache-Control`.
- **Security:** JWT scopes, rate limits, gateway policies.

**Microservices Patterns:**

- **API Gateway → Service → Queue → DB.**
- Service discovery via Eureka/Consul.
- Config via Spring Cloud Config.

---

## 🔐 **Spring Security**

### Authentication & Authorization

- Use JWT or OAuth2 Resource Server (`spring-boot-starter-oauth2-resource-server`).
- Stateless APIs: `SessionCreationPolicy.STATELESS`.
- Passwords: `BCryptPasswordEncoder`.
- Role-based access via `@PreAuthorize("hasRole('ADMIN')")`.

```java
@Bean
SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.csrf().disable()
             .authorizeHttpRequests(a -> a.anyRequest().authenticated())
             .oauth2ResourceServer(o -> o.jwt())
             .build();
}
```

### Secure Headers

`X-Frame-Options: DENY`,
`X-Content-Type-Options: nosniff`,
`Content-Security-Policy`,
CORS whitelisting per environment.

---

## ☸️ **Kubernetes & Cloud Security**

- **Pods & Deployments:** Smallest deployable units; define replicas and rolling updates.
- **Services:** Expose pods internally or externally (`ClusterIP`, `NodePort`, `LoadBalancer`).
- **ConfigMaps & Secrets:** Store environment configs and sensitive keys separately.
- **Autoscaling:**
  - **HPA (Horizontal Pod Autoscaler):** Scales pods based on CPU/memory or custom metrics.
  - **VPA (Vertical Pod Autoscaler):** Adjusts resource limits dynamically.
  - **Cluster Autoscaler:** Adds/removes nodes based on pending pods.
- **Probes:** `livenessProbe`, `readinessProbe`, `startupProbe`.
- **Observability:** Prometheus + Grafana.
- **Security:**
  - Use RBAC for service accounts.
  - Run pods as non-root.
  - Network Policies to isolate namespaces.

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

## 🧱 **JPA / ORM (Object-Relational Mapping)**

### Entity Basics

- Annotate with `@Entity`, `@Id`, `@GeneratedValue`.
- `@Column(nullable=false, unique=true)` for constraints.
- `@Enumerated(EnumType.STRING)` to persist enums safely.
- **Relationships:**

  - `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@JoinColumn`.
  - Use `cascade = CascadeType.ALL` carefully.

```java
@Entity
class User {
  @Id @GeneratedValue
  Long id;
  @OneToMany(mappedBy="user", fetch=FetchType.LAZY)
  List<Order> orders;
}
```

### Lazy vs Eager Loading

- Default: `@OneToMany` is **LAZY**, `@ManyToOne` is **EAGER**.
- Lazy is preferred — prevents loading large graphs unintentionally.

### Query Optimization

- **JPQL** or **CriteriaBuilder** for dynamic queries.
- Use `@Query` with native SQL when complex joins are required.
- Batch fetching: `@BatchSize(size=20)`.
- Cache results with **Spring Cache** or second-level cache (EHCache, Redis).

---

## 🧮 **SQL & NoSQL Advanced Concepts**

### SQL (Relational)

- **Normalization:** reduce redundancy.
- **Indexes:** B-tree (default), hash indexes for equality queries.
- **Transactions:** ACID compliance; use `@Transactional(isolation=READ_COMMITTED)` in Spring.
- **Joins:** Inner, left, self, cross — optimize with proper indexes.
- **Query optimization:** Analyze using `EXPLAIN` plan.

```sql
SELECT u.name, o.amount
FROM users u JOIN orders o ON u.id = o.user_id
WHERE o.amount > 100;
```

### NoSQL (Document / Key-Value)

- **MongoDB:** flexible schema, great for nested documents.
- **Cassandra / DynamoDB:** wide-column stores for scale and throughput.
- **Redis:** caching, pub/sub, rate limiting.
- **Elasticsearch:** full-text search and log analytics.

**Design Trade-Offs:**

| Factor      | SQL           | NoSQL                   |
| ----------- | ------------- | ----------------------- |
| Schema      | Rigid         | Flexible                |
| Scale       | Vertical      | Horizontal              |
| Consistency | Strong (ACID) | Eventual                |
| Queries     | Rich joins    | Limited aggregation     |
| Use Case    | FinTech, ERP  | IoT, analytics, caching |

---

## 🛡️ **Secure SDLC & Threat Modeling**

### Phases

1. **Requirements:** Define controls early.
2. **Design:** Apply STRIDE threat modeling.
3. **Implementation:** Embed SAST/SCA.
4. **Testing:** DAST + fuzzing.
5. **Deployment:** Runtime protection, vulnerability scanning.

---

## 🧪 **Security Testing & Vulnerability Management**

| Type     | Tools                             | Detects                     |
| -------- | --------------------------------- | --------------------------- |
| SAST     | Checkmarx, Fortify, SonarQube     | Code-level flaws            |
| SCA      | Snyk, Black Duck, OWASP Dep-Check | Library CVEs                |
| DAST     | ZAP, Burp Suite                   | Runtime flaws               |
| IaC Scan | Checkov, KICS                     | Insecure Terraform/K8s YAML |

**Vulnerability Lifecycle:** Discover → Validate → Prioritize (CVSS) → Patch → Retest → Report.

---

## 🌩️ **Network & Event Security**

### Network Scanning

- **Nmap**, **Nessus**, **Qualys** for open ports and SSL weakness.
- Use **WAF** and segmented VPCs.

### SIEM / SOAR

- **Splunk**, **QRadar**, **ELK**, **Azure Sentinel** for log correlation.
- Build alerts for anomaly detection, privilege escalation, data exfiltration.

### DLP

- Block sensitive data transfers (PCI, PII).
- Encrypt at rest & in transit (TLS 1.3).

---

## 🧠 **Security Use Cases (Interview Ready)**

| Scenario                | Solution                                             |
| ----------------------- | ---------------------------------------------------- |
| Race condition in API   | Use synchronized block or distributed lock via Redis |
| API abuse / brute force | Apply Redis rate limiting + CAPTCHA                  |
| Sensitive logs          | Redact PII via logging filter                        |
| DDoS attack             | Rate limiting + CDN caching                          |
| Outdated dependency     | Automate SCA → ticket + patch                        |
| File upload risk        | Validate MIME + antivirus (ClamAV)                   |
| GC CPU spike            | Enable GC logs; analyze via GCeasy                   |
