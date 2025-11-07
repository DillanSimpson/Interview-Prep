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

## 🏗️ **Data Structures & Use Cases**

### 🧩 Core Collections

| Data Structure | Avg. Time Complexity              | Typical Use / Notes             |
| -------------- | --------------------------------- | ------------------------------- |
| **Array**      | Access: O(1), Insert/Delete: O(n) | Fixed-size, fast random access. |
| **ArrayList**  | Access: O(1), Insert/Delete: O(n) | Dynamic resize, good for reads. |
| **LinkedList** | Access: O(n), Insert/Delete: O(1) | Fast at ends, high overhead.    |

---

### 🔑 Maps

| Data Structure        | Avg. Time Complexity      | Typical Use / Notes                     |
| --------------------- | ------------------------- | --------------------------------------- |
| **HashMap**           | Access: O(1), Worst: O(n) | Fast lookup, unordered.                 |
| **ConcurrentHashMap** | Access: O(1)              | Thread-safe, low contention.            |
| **TreeMap**           | Access: O(log n)          | Sorted keys, slower than HashMap.       |
| **LinkedHashMap**     | Access: O(1)              | Predictable order, great for LRU cache. |
| **WeakHashMap**       | Access: O(1)              | Auto-clears when keys are GC’d.         |
| **EnumMap**           | Access: O(1)              | Optimized for enum keys.                |

---

### 🔁 Sets

| Data Structure | Avg. Time Complexity | Typical Use / Notes         |
| -------------- | -------------------- | --------------------------- |
| **HashSet**    | Access: O(1)         | Unique, unordered elements. |
| **TreeSet**    | Access: O(log n)     | Sorted unique elements.     |
| **BitSet**     | Access: O(1)         | Space-efficient flags.      |

---

### 🧵 Queues & Stacks

| Data Structure           | Avg. Time Complexity    | Typical Use / Notes         |
| ------------------------ | ----------------------- | --------------------------- |
| **Stack / Deque**        | Push/Pop: O(1)          | LIFO operations.            |
| **Queue / Deque**        | Offer/Poll: O(1)        | FIFO or double-ended.       |
| **BlockingQueue**        | Offer/Poll: O(1)        | Thread-safe blocking tasks. |
| **PriorityQueue**        | Insert/Delete: O(log n) | Min/max retrieval.          |
| **CopyOnWriteArrayList** | Read: O(1), Write: O(n) | Safe concurrent reads.      |


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

| Annotation                                  | Purpose / Use Case                                                     | Notes                                              |
| ------------------------------------------- | ---------------------------------------------------------------------- | -------------------------------------------------- |
| **@RestController**                         | Combines `@Controller` + `@ResponseBody` to expose REST APIs.          | Auto-serializes return objects to JSON/XML.        |
| **@Controller**                             | Handles web requests, typically returning views (MVC).                 | Use with `ModelAndView`.                           |
| **@Service**                                | Marks business logic layer beans.                                      | Spring-managed singleton.                          |
| **@Repository**                             | Persistence/DAO layer; translates SQL exceptions.                      | Used for JPA, JDBC, etc.                           |
| **@Configuration**                          | Declares bean definitions.                                             | Equivalent to XML config, type-safe.               |
| **@Bean**                                   | Defines a Spring-managed bean inside config classes.                   | Lifecycle-aware, injectable.                       |
| **@Value("${property}")**                   | Injects values from config files or env vars.                          | Supports SpEL (`#{}`) for expressions.             |
| **@Transactional**                          | Defines DB transaction boundaries.                                     | Supports rollback, isolation, propagation.         |
| **@Profile("prod")**                        | Loads beans only under a given environment profile.                    | Common for `dev`, `test`, `prod`.                  |
| **@DependsOn("beanName")**                  | Enforces bean initialization order.                                    | Useful for implicit dependencies.                  |
| **@ConditionalOnProperty**                  | Loads bean if config flag matches.                                     | Feature toggles, modular features.                 |
| **@ConditionalOnMissingBean**               | Loads bean only if not already defined.                                | Prevents duplicate beans.                          |
| **@Lazy**                                   | Delays bean creation until first needed.                               | Reduces startup time.                              |
| **@Scope("prototype")**                     | Creates new instance per injection.                                    | Default is singleton.                              |
| **@Cacheable("cacheName")**                 | Caches method results by key.                                          | Combine with `@CacheEvict`, `@CachePut`.           |
| **@CacheEvict**                             | Clears cached entries.                                                 | Often after update/delete ops.                     |
| **@Retryable**                              | Retries failed method calls automatically.                             | Requires `@EnableRetry`.                           |
| **@Recover**                                | Fallback for `@Retryable` failures.                                    | Must match retry method signature.                 |
| **@RateLimiter** *(Resilience4j)*           | Limits API execution rate per time window.                             | `@RateLimiter(name="apiLimiter")`.                 |
| **@CircuitBreaker** *(Resilience4j)*        | Stops calling failing services temporarily.                            | Requires `@EnableCircuitBreaker`.                  |
| **@Bulkhead** *(Resilience4j)*              | Isolates service calls via limited threads/semaphores.                 | Prevents cascading failures.                       |
| **@Async**                                  | Executes methods asynchronously in background.                         | Needs `@EnableAsync`.                              |
| **@Scheduled(cron="...")**                  | Runs periodic background tasks.                                        | Needs `@EnableScheduling`.                         |
| **@RestControllerAdvice**                   | Global JSON-based exception handler for REST APIs.                     | Combines `@ControllerAdvice` + `@ResponseBody`.    |
| **@ControllerAdvice**                       | Global handler for exceptions or cross-cutting logic.                  | Works with `@ExceptionHandler`.                    |
| **@ExceptionHandler(Exception.class)**      | Handles specific exceptions locally or globally.                       | Returns custom response.                           |
| **@ResponseStatus(HttpStatus.BAD_REQUEST)** | Defines HTTP status for custom exceptions.                             | Common for API validation errors.                  |
| **@EnableConfigurationProperties**          | Binds YAML configs to POJOs annotated with `@ConfigurationProperties`. | Simplifies typed configuration.                    |
| **@ConfigurationProperties(prefix="app")**  | Binds YAML properties to fields.                                       | Example: `app.url`, `app.timeout`.                 |
| **@RestClientTest**                         | Auto-configures mock REST clients for tests.                           | For RestTemplate or WebClient testing.             |
| **@DataJpaTest**                            | Configures in-memory DB for JPA testing.                               | Auto-rollbacks after tests.                        |
| **@WebMvcTest**                             | Loads only web layer (controllers, filters).                           | Fast, isolated MVC tests.                          |
| **@SpringBootTest**                         | Boots entire context for integration testing.                          | Heavier, full-system tests.                        |
| **@MockBean**                               | Replaces a bean in the Spring context with a Mockito mock.             | Used in integration tests.                         |
| **@TestConfiguration**                      | Test-specific bean overrides/config.                                   | Used inside test packages.                         |
| **@ExtendWith(SpringExtension.class)**      | Integrates JUnit 5 with Spring testing.                                | Required for context injection.                    |
| **@DisplayName("...")**                     | Describes test methods in JUnit 5 reports.                             | Improves readability.                              |
| **@Import(ClassName.class)**                | Imports external configuration or bean definitions.                    | Useful in modular systems.                         |
| **@EnableAutoConfiguration**                | Enables auto-config for Spring Boot apps.                              | Automatically configures beans based on classpath. |
| **@EnableAspectJAutoProxy**                 | Enables AOP proxying for aspects.                                      | Required for `@Aspect`-based logging/security.     |
| **@Aspect**                                 | Defines an Aspect for cross-cutting concerns.                          | Used with `@Before`, `@After`, `@Around`.          |

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

### Recovery Example

```java
@Retryable(value = IOException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public String callExternalApi() { ... }

@Recover
public String recover(IOException ex) {
    return "Fallback response";
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

Here’s a **compact, engineer-friendly cheat sheet** for **Axon Framework + Kafka**, focused on how events, commands, and messages flow through the system — plus the most important configs like `group.id`, partitions, and replay behaviors.

---

## ⚙️ Axon + Kafka Overview

**Axon Framework** is a CQRS (Command Query Responsibility Segregation) + Event Sourcing framework.
**Kafka** is often used as the *Event Bus / Event Store* for Axon distributed setups.

Axon can use **Kafka as a distributed event message broker**, so event processors across microservices can subscribe to the same event stream and handle events in a scalable, decoupled way.

---

## 🧩 Core Concepts Mapping


| Concept             | Axon                            | Kafka Equivalent               | Purpose                                       |
| ------------------- | ------------------------------- | ------------------------------ | --------------------------------------------- |
| **Command Bus**     | Direct point-to-point           | Not Kafka                      | Executes intent (creates/updates aggregate)   |
| **Event Bus**       | Publish-subscribe               | Kafka topic                    | Distributes events to all interested handlers |
| **Query Bus**       | Point-to-point / scatter-gather | Not Kafka                      | Fetches read model data                       |
| **Event Processor** | Handles events                  | Kafka consumer group           | Subscribes to Kafka topics                    |
| **Aggregate**       | Domain object root              | —                             | Source of truth that applies events           |
| **Event Store**     | Axon Server / Kafka             | Kafka topic per aggregate type | Persists and replays events                   |

---

## ⚡ Kafka Configuration Cheat Sheet

### 1. **Basic Properties**

```yaml
axon:
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      client-id: axon-producer
    consumer:
      bootstrap-servers: localhost:9092
      group-id: billing-service   # Key for parallelism & scaling
      auto-offset-reset: earliest
      enable-auto-commit: false
    properties:
      max.poll.records: 100
```

### 2. **`group.id` Explained**

* Identifies a **consumer group** (like a logical Axon event processor).
* Kafka guarantees:

  * **One message per group partition at a time**.
  * If two Axon nodes share the same `group.id`, they load balance event handling.
  * If you want *multiple services* to process the same event (e.g., Notifications + Payments), give them **different group IDs**.

Example:


| Service                | Group ID                 | Effect                          |
| ---------------------- | ------------------------ | ------------------------------- |
| `payment-service`      | `payment-processor`      | Independent consumer            |
| `notification-service` | `notification-processor` | Also receives same event stream |

---

## 🧠 Key Topics & Event Flow


| Topic Name         | Description                                                                                          |
| ------------------ | ---------------------------------------------------------------------------------------------------- |
| `axon.events`      | Default topic for domain events                                                                      |
| `axon.commands`    | Optional if using Kafka as a distributed command bus                                                 |
| `axon.dead-letter` | For failed event processing                                                                          |
| Custom topics      | You can map aggregates or bounded contexts to dedicated topics (`customer.events`, `billing.events`) |

---

## 🔁 Event Processors

Axon defines two main types:


| Type                     | Processing                 | Use Case                          |
| ------------------------ | -------------------------- | --------------------------------- |
| **SubscribingProcessor** | Real-time (like in-memory) | Single-node or local event replay |
| **TrackingProcessor**    | Pulls from Kafka offset    | Distributed & replayable          |

When using Kafka, Axon automatically configures **`TrackingEventProcessor`**, backed by Kafka offsets.

**Offsets** are stored per `group.id` → partition mapping.

---

## 🪄 Common Configs and Gotchas


| Config                       | Meaning                                 | Tip                            |
| ---------------------------- | --------------------------------------- | ------------------------------ |
| `auto.offset.reset=earliest` | Start from beginning if no offset found | Use this for replay or testing |
| `enable.auto.commit=false`   | Let Axon control commit                 | Prevents message loss          |
| `max.poll.interval.ms`       | Max time between polls                  | Tune if handlers are slow      |
| `max.poll.records`           | Number of records per batch             | Control processing throughput  |
| `acks=all`                   | Producer waits for all replicas         | Ensures durability             |

---

## 🧰 Code Patterns

### 1. Event Producer (Publishing to Kafka)

```java
@EventHandler
public void on(OrderCreatedEvent event) {
    kafkaTemplate.send("axon.events", event.getOrderId(), event);
}
```

### 2. Kafka Event Consumer in Axon

```java
@ProcessingGroup("order-events")
public class OrderEventHandler {

    @EventHandler
    public void on(OrderCreatedEvent event) {
        // update read model, trigger saga, etc.
    }
}
```

> Each `@ProcessingGroup` maps to a Kafka `group.id`.
> Multiple instances of the same service share load if they use the same group.

---

## 🧮 Parallelism and Scaling


| Concept            | Kafka Behavior                                                   |
| ------------------ | ---------------------------------------------------------------- |
| **Partitions**     | Parallelism unit; one partition = one thread per consumer group  |
| **Replicas**       | Fault tolerance                                                  |
| **group.id**       | Controls load distribution and consumer uniqueness               |
| **Keyed messages** | Events for same aggregate go to same partition (preserves order) |

**Best practice:** use the aggregate ID as Kafka key → ensures ordered event stream per entity.

---

## 🧱 Axon Kafka Integration Setup

Add dependency:

```xml
<dependency>
  <groupId>org.axonframework.extensions.kafka</groupId>
  <artifactId>axon-kafka-spring-boot-starter</artifactId>
  <version>4.9.3</version>
</dependency>
```

Then configure your event processors:

```yaml
axon:
  eventhandling:
    processors:
      billing-processor:
        mode: tracking
```

Axon automatically binds these processors to Kafka topics using the extension’s configuration.

---

## 🧨 Error Handling

* Axon retries transient errors automatically.
* Permanent failures → Dead Letter Queue topic (e.g., `axon.dead-letter`).
* You can implement custom DLQ handlers or retry policies.

---

## 🧭 Typical Architecture

```
[Command -> Aggregate -> Event]
          ↓
     Kafka Topic ("axon.events")
          ↓
   [Event Processors / Sagas]
          ↓
     Read Models / External Systems
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

## 🔐 Encryption — Reversible Protection

**Purpose:**
Keep data *confidential* while allowing authorized users to recover it.

**How it works:**

* Uses a **key** to transform plaintext into unreadable ciphertext.
* With the right key, you can **decrypt** it back to the original message.

**Example:**

```text
Input: "Secret123"
Key: "A1B2C3"
Output: "F81@^q0Z"  ← Encrypted
```

Decrypt with the same key → get back "Secret123".

**Types:**

1. **Symmetric encryption:** Same key for encryption and decryption.

   * Algorithms: AES, DES, ChaCha20
   * Example: Used for file encryption or database fields.
2. **Asymmetric encryption:** Public key encrypts, private key decrypts.

   * Algorithms: RSA, ECC
   * Example: HTTPS (TLS), email encryption, digital signatures.

**Use cases:**

* Protecting data at rest (databases, files)
* Secure data in transit (SSL/TLS)
* Reversible protection when data must be retrieved later

---

## 🧮 Hashing — Irreversible Integrity Check

**Purpose:**
Ensure *integrity* and *verification* of data, not secrecy.

**How it works:**

* Takes input of any length → outputs fixed-size digest (hash).
* Cannot be reversed to get original input.
* Any small change in input gives completely different hash.

**Example:**

```text
Input: "Secret123"
Hash (SHA-256): 4ffba5659d30...
```

**Properties:**

* **Deterministic:** Same input always → same hash.
* **Irreversible:** You can’t get input back from hash.
* **Avalanche effect:** Tiny input change → drastically different output.
* **Collision-resistant:** Two different inputs shouldn’t produce same hash.

**Use cases:**

* Password storage (`bcrypt`, `scrypt`, `Argon2`)
* File integrity verification (e.g., checksums)
* Blockchain (each block’s hash links to previous one)
* Digital signatures (hashed before signing)

---

## ⚖️ Quick Comparison


| Feature            | Encryption                            | Hashing                     |
| ------------------ | ------------------------------------- | --------------------------- |
| Reversible         | ✅ Yes                                | ❌ No                       |
| Uses a Key         | ✅ Yes                                | ❌ No                       |
| Primary Goal       | Confidentiality                       | Integrity / Authentication  |
| Example Algorithms | AES, RSA                              | SHA-256, bcrypt             |
| Output Size        | Variable                              | Fixed                       |
| Typical Use        | Storing data securely but retrievable | Verifying data or passwords |

---

## 🧠 Practical Security Rules

* **Never encrypt passwords.**
  Store password *hashes* using a **slow hashing algorithm** (`bcrypt`, `Argon2`) + salt.
* **Encrypt sensitive data you must read back**, like:
  credit card numbers, personal info, or API keys.
* **Use both together:**

  * Encrypt communication channels (TLS).
  * Hash passwords and tokens before storage.
  * Optionally hash ciphertext for tamper detection.

---

## 🧠 **Security Use Cases (Interview Ready)**


| Scenario                | Solution                                             |
| ----------------------- | ---------------------------------------------------- |
| Race condition in API   | Use synchronized block or distributed lock via Redis |
| API abuse / brute force | Apply Redis rate limiting + CAPTCHA                  |
| Sensitive logs          | Redact PII via logging filter                        |
| DDoS attack             | Rate limiting + CDN caching                          |
| Outdated dependency     | Automate SCA → ticket + patch                       |
| File upload risk        | Validate MIME + antivirus (ClamAV)                   |
| GC CPU spike            | Enable GC logs; analyze via GCeasy                   |

---

## Potential Code Problem

```java
  public int[] process(int[] balances, String[] transactions) {
      int[] updated = balances.clone();

      for (String txn : transactions) {
          String[] parts = txn.split(" ");
          String type = parts[0].toLowerCase();

          switch (type) {
              case "deposit": {
                  int account = Integer.parseInt(parts[1]) - 1;
                  int amount = Integer.parseInt(parts[2]);
                  updated[account] += amount;
                  break;
              }
              case "withdraw": {
                  int account = Integer.parseInt(parts[1]) - 1;
                  int amount = Integer.parseInt(parts[2]);
                  if (updated[account] < amount) {
                      return new int[]{account + 1, -1};
                  }
                  updated[account] -= amount;
                  break;
              }
              case "transfer": {
                  int from = Integer.parseInt(parts[1]) - 1;
                  int to = Integer.parseInt(parts[2]) - 1;
                  int amount = Integer.parseInt(parts[3]);
                  if (updated[from] < amount) {
                      return new int[]{from + 1, -1};
                  }
                  updated[from] -= amount;
                  updated[to] += amount;
                  break;
              }
              default:
                  System.out.println("Invalid transaction type: " + type);
          }
      }

      return updated;
  }
```
