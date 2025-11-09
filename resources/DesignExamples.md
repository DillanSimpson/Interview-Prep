# Design Examples

## 1) Employee CRUD REST API design

## ⚙️ Employee CRUD REST API (Spring Boot)

### Endpoints

| Action | Method| Endpoint| Notes |
| :--------- | :------- | :----- | :---- |
| **Create** | `POST`  | `/v1/employees` | Returns `201 Created`, `Location` header |
| **Read** | `GET` | `/v1/employees/{id}` | `200 OK`, `404 Not Found` |
| **List** | `GET` | `/v1/employees?dept=HR&page=0&size=20&sort=lastName,asc` | Supports filters + pagination |
| **Update** | `PUT` | `/v1/employees/{id}` | Full replace, use `If-Match` (ETag) |
| **Patch**  | `PATCH`  | `/v1/employees/{id}`| Partial update |
| **Delete** | `DELETE` | `/v1/employees/{id}  | `204 No Content`, `If-Match` for safety |

### Entity (JPA)

```java
@Entity
class Employee {
  @Id @GeneratedValue UUID id;
  String firstName, lastName, email, title, dept;
  Integer age;
  @Version long version;
  Instant createdAt, updatedAt;
  @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
  @PreUpdate  void onUpdate() { updatedAt = Instant.now(); }
}
```

### DTOs

```java
record EmployeeCreate(String firstName, String lastName, String email,
                      String title, String dept, Integer age) {}
record EmployeeUpdate(String firstName, String lastName, String email,
                      String title, String dept, Integer age) {}
record EmployeeResponse(UUID id, String firstName, String lastName, String email,
                        String dept, Integer age, Instant createdAt, Instant updatedAt, String etag) {}
```

---

### Controller (essence)

```java
@RestController @RequestMapping("/v1/employees")
class EmployeeController {
  private final EmployeeService svc;
  @PostMapping ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeCreate req);
  @GetMapping("/{id}") ResponseEntity<EmployeeResponse> get(@PathVariable UUID id);
  @GetMapping Page<EmployeeResponse> list(...filters...);
  @PutMapping("/{id}") ResponseEntity<EmployeeResponse> update(@PathVariable UUID id,
      @RequestHeader("If-Match") String etag, @Valid @RequestBody EmployeeUpdate req);
  @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable UUID id,
      @RequestHeader("If-Match") String etag);
}
```

### Key design highlights

* **Versioning:** `/v1/`
* **UUID IDs** for global uniqueness
* **ETag / If-Match** for optimistic locking
* **DTOs** separate API from entity
* **Pagination & sorting** with Spring Data
* **Consistent JSON errors** via `@RestControllerAdvice`
* **Validation**: `@NotBlank`, `@Email`, `@Min/@Max`
* **Optional filters** for search (`department`, `age` range, etc.)


### Sample JSON

```json
{
  "id": "c45b...e9",
  "firstName": "Ankit",
  "lastName": "Sharma",
  "email": "ankit@example.com",
  "department": "IT",
  "age": 32
}
```

## 2) **design a distributed, scalable application**

Think **stateless services + smart data layer + async + observability + resilience**.

### 1) Core principles

* **Scale horizontally:** Make services **stateless**; keep session/state in cookies, Redis, or DB.
* **Partition early:** Shard/partition by user/tenant/region; avoid “one big table”.
* **Prefer async:** Use queues/streams for heavy/long-running or fan-out work.
* **Idempotency everywhere:** Safe retries for APIs, consumers, and jobs.
* **Backpressure & limits:** Rate limit, throttle, shed load when needed.
* **Automate everything:** Infra-as-code, autoscaling, health checks, self-healing.

### 2) Reference architecture (mental diagram)

```less
[Client/Web/Mobile/CDN] 
        |
   [API Gateway / LB]  ← auth, rate limiting, request shaping
        |
   ┌─────────────── Microservices (stateless) ───────────────┐
   |  UserSvc   OrderSvc   PaymentSvc   NotifSvc   SearchSvc |
   └───────────────|──────────|──────────|──────────|────────┘
        |           |          |          |         (async)
   [Cache: Redis]  [RDBMS]  [Ledger/Store]   [Queue/Stream: Kafka/SQS]
        |                          |                        |
     (read-through)           (strong constraints)     Workers/Consumers
        |
   [Analytics Lake] ← CDC/streaming → [OLAP / Search (Elasticsearch)]
```

### 3) API design & service layer

* **REST/GraphQL/gRPC**; version your APIs.
* **Stateless handlers**; externalize config; 12-factor.
* **Contracts:** OpenAPI/Protobuf; backward compatible changes.
* **Resilience:** timeouts, retries with jitter, **circuit breakers**, bulkheads; **idempotency keys** on writes.
* **Security:** OAuth2/JWT, mTLS between services, least privilege, secrets manager.

### 4) Data layer patterns

* **Choose per workload:**

  * OLTP: Postgres/MySQL with **read replicas**, **partitioning**.
  * Cache: Redis/Memcached (TTL, eviction policy).
  * Search: Elasticsearch/OpenSearch.
  * Events: Kafka/Pulsar/Kinesis (ordered partitions).
* **CQRS + Event-driven:** Command side is transactional; query side denormalized and **eventually consistent**.
* **Transactions:** Prefer **sagas** over 2PC for cross-service workflows.
* **Migrations:** Online schema changes, feature flags, dual-write → flip.

### 5) Asynchrony & workflows

* **Queues for spikes:** Smooth bursty traffic; workers scale independently.
* **At-least-once processing:** Pair with idempotent handlers; dedupe by key.
* **Fan-out:** Publish domain events; subscribers build projections.
* **Batch vs stream:** Use streaming for near-real-time, batch for big crunch.

### 6) Caching strategy

* **Layers:** CDN (static), edge cache, service-side Redis (read-through), client hints/ETags.
* **Keys & TTLs:** Namespace keys; explicit invalidation; negative caching for 404s.
* **Hot keys:** Precompute; use sharded caches or local cache with TTL.

### 7) Observability & ops

* **Metrics:** RED/USE (Rate, Errors, Duration / Utilization, Saturation, Errors).
* **Logs & traces:** Correlate with trace IDs; distributed tracing (OpenTelemetry).
* **SLOs & alerts:** Error budget policies; on-call runbooks.
* **Chaos & load tests:** Gremlin/chaos monkey; k6/JMeter; canary + auto-rollback.

### 8) Delivery platform

* **Containers + Orchestrator:** Docker + **Kubernetes** (HPA, readiness/liveness probes).
* **Service mesh (optional):** mTLS, retries, traffic shaping (Istio/Linkerd).
* **CI/CD:** Blue/green or canary deploys; progressive delivery; config via GitOps.
* **Autoscaling:** Scale on CPU/RAM/QPS/queue lag; scheduled scaling for diurnal load.

### 9) Reliability & multi-region

* **Understand CAP trade-offs:** Choose **AP** (eventual consistency) for feeds, **CP** for ledgers.
* **Multi-AZ first, then multi-region:**
  * Active-active for read-heavy; active-passive for write-serialized domains.
  * **Global IDs**, clock tolerance, conflict resolution (last-write-wins or CRDTs).
* **Backups & DR:** RPO/RTO targets; regular restore drills.

### 10) Security & compliance

* **Zero trust:** IAM per service; rotated tokens; short-lived creds.
* **Data protections:** Encrypt in transit (TLS) and at rest (KMS); PII tokenization.
* **Least privilege:** Scoped DB users, network policies.
* **Audit trails:** Immutable logs; tamper-evident storage.

### 11) Scalability playbook (how I’d execute)

1. **Make services stateless**, put sessions and cache in Redis.
2. **Introduce a message bus**; move slow ops to async workers.
3. **Partition big tables**; add read replicas; denormalize hot reads.
4. **Add CDN & request shaping** at the gateway; rate limits & quotas.
5. **Instrument**: metrics, traces, error budgets; load test; tune autoscaling.
6. **Plan for failure**: timeouts, retries, circuit breakers, bulkheads; chaos drills.
7. **Iterate**: watch p95/p99 latency, queue lag, DB CPU/IO, cache hit rate.

### Sound-bite (to close in interviews)

> “I design for horizontal scale with stateless services behind an API gateway, keep state in well-partitioned data stores, push heavy work to async queues with idempotent consumers, and bake in resilience (timeouts, retries, circuit breakers) and observability (metrics, logs, tracing). I deploy on Kubernetes with autoscaling and progressive delivery, and I choose consistency vs availability per domain—often CQRS with eventual consistency—backed by SLOs and regular chaos/load testing.”

---

## 2) Given a rest api, consume it and print result in specific format

You have a **REST API** (probably returning JSON), and you need to **consume it from Java** — then print the result in a **specific format**.

Example using **Spring Boot + RestTemplate**

## ⚙️ Implementation (Java + RestTemplate)

```java
import org.springframework.web.client.RestTemplate;
import java.util.*;
class Employee {
    private int id;
    private String employee_name;
    private int employee_salary;
    private int employee_age;
    // Getters & setters
    public String getEmployee_name() { return employee_name; }
    public int getEmployee_salary() { return employee_salary; }
    public int getEmployee_age() { return employee_age; }
}

class ApiResponse {
    private String status;
    private List<Employee> data;
    public List<Employee> getData() { return data; }
}

public class ConsumeApiExample {
    public static void main(String[] args) {
        String url = "https://dummy.restapiexample.com/api/v1/employees";
        RestTemplate restTemplate = new RestTemplate();

        ApiResponse response = restTemplate.getForObject(url, ApiResponse.class);

        if (response != null && response.getData() != null) {
            response.getData().forEach(emp ->
                System.out.printf("Name: %s | Age: %d | Salary: %d%n",
                        emp.getEmployee_name(), emp.getEmployee_age(), emp.getEmployee_salary())
            );
        } else {
            System.out.println("No data received from API.");
        }
    }
}
```

### 🧠 Notes for interviews

* `RestTemplate` is simple and still widely used (though replaced by `WebClient` in reactive setups).
* For modern, non-blocking I/O: use `WebClient` from `spring-webflux`.
* Always handle errors (`HttpClientErrorException`, timeouts) and consider mapping JSON using `Jackson`.

## ## ⚙️ Implementation without Spring (pure Java 11+)

```java
import java.net.http.*;
import java.net.URI;
import com.fasterxml.jackson.databind.*;
public class SimpleHttpClientExample {
    public static void main(String[] args) throws Exception {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create("https://dummy.restapiexample.com/api/v1/employees"))
                .GET().build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var json = response.body();

        // Parse and print using Jackson
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        for (JsonNode emp : root.path("data")) {
            System.out.printf("Name: %s | Age: %s | Salary: %s%n",
                    emp.get("employee_name").asText(),
                    emp.get("employee_age").asText(),
                    emp.get("employee_salary").asText());
        }
    }
}
```

### ✨ Key takeaway

For a quick **API consumer pattern**:

1. Choose **`RestTemplate`** (Spring) or **`HttpClient`** (Java 11+).
2. Map JSON → POJOs (via `Jackson` or `record`).
3. Loop + format output with `printf`.
