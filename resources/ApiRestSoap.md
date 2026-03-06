# API / REST / SOAP Cheat Sheet — MasterCard SSE Interview Prep

---

## 1. REST Fundamentals

### Core Constraints (Roy Fielding's 6)

| Constraint | What it means |
|---|---|
| **Client-Server** | Separation of concerns — UI ≠ backend |
| **Stateless** | Every request carries all info needed; server stores no session state |
| **Cacheable** | Responses must declare themselves cacheable or not |
| **Uniform Interface** | Resource-based URIs, standard HTTP verbs, self-descriptive messages |
| **Layered System** | Client can't tell if it's talking to origin server or intermediary |
| **Code on Demand** (optional) | Server can send executable code (e.g., JavaScript) |

### HTTP Methods

| Method | Safe | Idempotent | Body | Use |
|---|---|---|---|---|
| `GET` | Yes | Yes | No | Read resource |
| `HEAD` | Yes | Yes | No | Read headers only |
| `POST` | No | **No** | Yes | Create, non-idempotent action |
| `PUT` | No | Yes | Yes | Full replace |
| `PATCH` | No | No* | Yes | Partial update |
| `DELETE` | No | Yes | Optional | Remove |
| `OPTIONS` | Yes | Yes | No | CORS preflight, list allowed methods |

> **Idempotent** = calling N times gives the same result as calling once.
> POST is not idempotent — submitting a payment twice creates two payments.
> In payments, always use an **idempotency key** header with POST.

### URI Design Best Practices

```
# Resources are nouns, not verbs
GET    /transactions              -- list
GET    /transactions/42           -- single
POST   /transactions              -- create
PUT    /transactions/42           -- full replace
PATCH  /transactions/42           -- partial update
DELETE /transactions/42           -- delete

# Sub-resources (relationships)
GET    /accounts/7/transactions   -- transactions for account 7
POST   /accounts/7/disputes       -- open dispute on account 7

# Actions that don't map to CRUD — use verb as sub-resource
POST   /transactions/42/refund    -- action on a resource
POST   /accounts/7/freeze

# Filtering, sorting, pagination — query params, not path
GET /transactions?status=PENDING&from=2024-01-01&sort=amount,desc&page=0&size=20

# Versioning in path (most common)
GET /v1/transactions
GET /v2/transactions
```

---

## 2. HTTP Status Codes

```
2xx — Success
  200 OK              Standard success
  201 Created         POST created a resource; include Location header
  202 Accepted        Async processing started (not yet complete)
  204 No Content      DELETE / PUT succeeded, no body to return

3xx — Redirection
  301 Moved Permanently   Resource URL changed forever
  304 Not Modified        Cached response still valid (ETag / Last-Modified match)

4xx — Client Error (caller's fault)
  400 Bad Request         Malformed JSON, validation failure
  401 Unauthorized        Missing or invalid authentication token
  403 Forbidden           Authenticated but not authorized
  404 Not Found           Resource doesn't exist
  405 Method Not Allowed  GET on a POST-only endpoint
  409 Conflict            Duplicate, optimistic lock failure
  410 Gone                Resource existed but is permanently deleted
  422 Unprocessable Entity Semantically invalid (passes JSON parse but fails business rules)
  429 Too Many Requests   Rate limit exceeded; include Retry-After header

5xx — Server Error (our fault)
  500 Internal Server Error  Unhandled exception
  502 Bad Gateway            Upstream service returned bad response
  503 Service Unavailable    Overloaded / down; include Retry-After header
  504 Gateway Timeout        Upstream service timed out
```

---

## 3. REST vs SOAP

| Aspect | REST | SOAP |
|---|---|---|
| Protocol | HTTP (also HTTPS, WS) | HTTP, SMTP, TCP |
| Data format | JSON, XML, plain text | XML only |
| Contract | OpenAPI/Swagger (optional) | WSDL (mandatory) |
| State | Stateless | Can be stateful |
| Error handling | HTTP status codes | Fault XML element |
| Security | HTTPS + JWT/OAuth2 | WS-Security (built-in) |
| Overhead | Low | High (XML verbosity) |
| Tooling | Universal | Enterprise Java/.NET |
| Caching | Native HTTP caching | No native caching |
| Use cases | Public APIs, microservices, mobile | Enterprise banking, legacy systems |
| MasterCard relevance | New services | Legacy payment rails (ISO 8583 / SWIFT) |

---

## 4. SOAP Essentials

### SOAP Envelope Structure

```xml
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:pay="http://mastercard.com/payment">

    <soapenv:Header>
        <wsse:Security>
            <wsse:UsernameToken>
                <wsse:Username>svc_account</wsse:Username>
                <wsse:Password>secret</wsse:Password>
            </wsse:UsernameToken>
        </wsse:Security>
    </soapenv:Header>

    <soapenv:Body>
        <pay:ProcessPaymentRequest>
            <pay:amount>150.00</pay:amount>
            <pay:currency>USD</pay:currency>
            <pay:accountId>123456</pay:accountId>
        </pay:ProcessPaymentRequest>
    </soapenv:Body>
</soapenv:Envelope>
```

### SOAP Fault (error response)

```xml
<soapenv:Fault>
    <faultcode>soapenv:Client</faultcode>
    <faultstring>Invalid account number</faultstring>
    <detail>
        <error code="ACC_404">Account 123456 not found</error>
    </detail>
</soapenv:Fault>
```

### Spring Web Services (SOAP server)

```java
@Endpoint
public class PaymentEndpoint {

    @PayloadRoot(namespace = "http://mastercard.com/payment",
                 localPart  = "ProcessPaymentRequest")
    @ResponsePayload
    public ProcessPaymentResponse process(
            @RequestPayload ProcessPaymentRequest request) {
        // handle request
        return new ProcessPaymentResponse("TXN-001", "APPROVED");
    }
}
```

---

## 5. Spring Boot REST

### Controller Basics

```java
@RestController
@RequestMapping("/v1/transactions")
@Validated
public class TransactionController {

    private final TransactionService service;

    // Constructor injection (preferred over @Autowired field injection)
    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status) {
        return ResponseEntity.ok(service.list(page, size, status));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> create(
            @RequestBody @Valid CreateTransactionRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        TransactionDto created = service.create(req, idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TransactionDto> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateStatusRequest req) {
        return ResponseEntity.ok(service.updateStatus(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();   // 204
    }
}
```

### Request Validation

```java
public class CreateTransactionRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    private Long accountId;

    @Future
    private LocalDate expiryDate;

    @Pattern(regexp = "^[A-Z]{2}[0-9]{9}$", message = "Invalid reference format")
    private String reference;
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", errors.toString()));
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateRequestException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Log full stack trace internally, never expose internals to client
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

### Standard Error Response Body

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "amount: must be positive",
  "timestamp": "2024-03-05T14:22:00Z",
  "path": "/v1/transactions",
  "traceId": "abc123def456"
}
```

---

## 6. Caching

### HTTP Cache Headers

```
# Response headers the server sends
Cache-Control: max-age=300, must-revalidate    -- cache for 5 min, then revalidate
Cache-Control: no-cache                         -- always revalidate before serving from cache
Cache-Control: no-store                         -- never cache (sensitive data)
Cache-Control: private                          -- only browser cache, not CDN/proxy
Cache-Control: public, max-age=86400            -- CDN can cache for 1 day
ETag: "abc123def456"                            -- fingerprint of response body
Last-Modified: Tue, 05 Mar 2024 10:00:00 GMT

# Request headers the client sends on revalidation
If-None-Match: "abc123def456"                   -- server returns 304 if ETag matches
If-Modified-Since: Tue, 05 Mar 2024 10:00:00 GMT
```

**HTTP caching decision tree:**
```
Client requests resource
    → Has cached copy with ETag?
        → Send If-None-Match header
        → Server compares ETag
            → Matches: 304 Not Modified (no body, saves bandwidth)
            → Changed: 200 OK with new body + new ETag
    → Cache-Control: max-age still valid?
        → Serve from local cache without hitting server (best performance)
```

### Spring Cache Abstraction

```java
// Enable caching
@SpringBootApplication
@EnableCaching
public class Application { }

// Cache configuration (Caffeine for local, Redis for distributed)
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());              // exposes hit/miss metrics
        return mgr;
    }
}

// Annotations on service methods
@Service
public class AccountService {

    @Cacheable(value = "accounts", key = "#id",
               condition = "#id > 0",        // only cache valid IDs
               unless = "#result == null")   // don't cache null results
    public Account findById(Long id) {
        return accountRepository.findById(id).orElseThrow();
    }

    @CachePut(value = "accounts", key = "#account.id")   // update cache on write
    public Account update(Account account) {
        return accountRepository.save(account);
    }

    @CacheEvict(value = "accounts", key = "#id")          // remove on delete
    public void delete(Long id) {
        accountRepository.deleteById(id);
    }

    @CacheEvict(value = "accounts", allEntries = true)    // clear entire cache
    @Scheduled(fixedRate = 300_000)
    public void evictAll() { }
}
```

### Redis Cache (Distributed — Required at Scale)

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withCacheConfiguration("accounts",
                    config.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("exchange-rates",
                    config.entryTtl(Duration.ofSeconds(30)))
                .build();
    }
}
```

### Cache Patterns & Problems

| Pattern | Description | Use case |
|---|---|---|
| **Cache-Aside (Lazy)** | App checks cache → miss → fetch DB → populate cache | General purpose |
| **Write-Through** | Write to cache and DB simultaneously | Strong consistency |
| **Write-Behind** | Write to cache, async flush to DB | High-write throughput |
| **Read-Through** | Cache fetches from DB on miss automatically | Simplified app code |
| **Refresh-Ahead** | Proactively refresh before expiry | Predictable access patterns |

**Cache Stampede (Thundering Herd)** — many requests hit DB simultaneously when a hot key expires:
```java
// Solution 1: Probabilistic early expiration (PER)
// Solution 2: Mutex lock on cache miss
public Account findByIdWithLock(Long id) {
    String lockKey = "lock:account:" + id;
    if (redis.setIfAbsent(lockKey, "1", Duration.ofSeconds(5))) {
        try {
            Account account = db.findById(id);
            cache.put("account:" + id, account);
            return account;
        } finally {
            redis.delete(lockKey);
        }
    }
    // Wait briefly and retry from cache
    Thread.sleep(50);
    return cache.get("account:" + id);
}
```

**Cache Penetration** — requests for keys that never exist (DDoS vector):
```java
// Solution: Cache null results with short TTL
@Cacheable(value = "accounts", key = "#id", unless = "false")
// Or use a Bloom Filter to reject impossible IDs before hitting cache/DB
```

---

## 7. Rate Limiting & Throttling

### Algorithms

| Algorithm | Pros | Cons | Use case |
|---|---|---|---|
| **Fixed Window** | Simple, low memory | Burst at window boundary | Simple APIs |
| **Sliding Window Log** | Precise | High memory (stores timestamps) | Accurate limiting |
| **Sliding Window Counter** | Low memory, accurate-ish | Slight approximation | Production default |
| **Token Bucket** | Handles bursts gracefully | Slightly complex | APIs with burst allowance |
| **Leaky Bucket** | Smooth output rate | Drops bursts | Rate-shaping |

### Spring + Bucket4j (Token Bucket)

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(100,           // 100 tokens
                    Refill.greedy(100, Duration.ofMinutes(1)))) // refill 100/min
                .build()
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {
        String clientId = req.getHeader("X-Client-ID");
        Bucket bucket = getBucket(clientId);

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setHeader("Retry-After", "60");
            res.setHeader("X-Rate-Limit-Remaining", "0");
            res.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
        }
    }
}
```

### Rate Limit Response Headers

```
X-Rate-Limit-Limit: 100          -- max requests per window
X-Rate-Limit-Remaining: 42       -- remaining in current window
X-Rate-Limit-Reset: 1709640000   -- Unix timestamp when window resets
Retry-After: 60                  -- seconds to wait (on 429)
```

---

## 8. High-Traffic Handling Patterns

### Circuit Breaker (Resilience4j)

Prevents cascading failures when a downstream service is degraded.

```java
// application.yml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        slidingWindowSize: 10
        failureRateThreshold: 50          # open after 50% failure rate
        waitDurationInOpenState: 30s      # stay open 30s before trying half-open
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallDurationThreshold: 2s
        slowCallRateThreshold: 80         # open if 80% of calls are slow

// Usage
@Service
public class PaymentClient {

    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallback")
    @Retry(name = "paymentService")
    @TimeLimiter(name = "paymentService")
    public CompletableFuture<PaymentResponse> processPayment(PaymentRequest req) {
        return CompletableFuture.supplyAsync(() -> externalService.process(req));
    }

    public CompletableFuture<PaymentResponse> fallback(PaymentRequest req, Exception ex) {
        // Queue for async retry, return a pending response
        asyncQueue.enqueue(req);
        return CompletableFuture.completedFuture(
            new PaymentResponse("PENDING", "Queued for retry"));
    }
}
```

**Circuit Breaker States:**
```
CLOSED  → Normal operation; failures tracked in sliding window
   ↓ (failure rate > threshold)
OPEN    → All calls fail fast (no network calls); fallback executed
   ↓ (wait duration elapsed)
HALF-OPEN → Limited calls allowed through as probe
   ↓ (probes succeed)          ↓ (probes fail)
CLOSED                          OPEN
```

### Retry with Exponential Backoff

```java
resilience4j:
  retry:
    instances:
      paymentService:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2      # 500ms, 1s, 2s
        retryExceptions:
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
        ignoreExceptions:
          - com.example.ValidationException  # don't retry business errors

// Or with Spring Retry
@Retryable(
    retryFor   = {ConnectException.class, TimeoutException.class},
    maxAttempts = 3,
    backoff     = @Backoff(delay = 500, multiplier = 2))
public Response callExternalApi(Request req) { ... }

@Recover
public Response recover(ConnectException ex, Request req) {
    return Response.queued();
}
```

### Bulkhead (Isolate Failures)

Limits concurrent calls to a dependency — prevents one slow service from exhausting your thread pool.

```java
resilience4j:
  bulkhead:
    instances:
      fraudService:
        maxConcurrentCalls: 25     # max 25 concurrent calls to fraud service
        maxWaitDuration: 100ms     # reject if can't get slot within 100ms
  thread-pool-bulkhead:
    instances:
      reportingService:
        coreThreadPoolSize: 4
        maxThreadPoolSize: 8
        queueCapacity: 20          # reject with BulkheadFullException after 20 queued
```

### Timeout

```java
resilience4j:
  timelimiter:
    instances:
      externalApi:
        timeoutDuration: 2s
        cancelRunningFuture: true

// RestTemplate (synchronous)
@Bean
public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(1000);        // TCP connect timeout
    factory.setReadTimeout(3000);           // response read timeout
    return new RestTemplate(factory);
}

// WebClient (reactive)
@Bean
public WebClient webClient() {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(
            HttpClient.create().responseTimeout(Duration.ofSeconds(3))))
        .build();
}
```

### Async Processing with CompletableFuture

```java
@Service
public class TransactionService {

    private final Executor taskExecutor;  // bounded thread pool

    public CompletableFuture<TransactionResult> processAsync(TransactionRequest req) {
        CompletableFuture<RiskScore>    risk    = getRiskScoreAsync(req);
        CompletableFuture<AccountInfo>  account = getAccountAsync(req.getAccountId());

        // Fan-out: run both in parallel, then combine
        return risk.thenCombine(account, (r, a) -> {
            if (r.score() > 0.9) throw new HighRiskException();
            return processTransaction(req, a);
        })
        .orTimeout(5, TimeUnit.SECONDS)
        .exceptionally(ex -> TransactionResult.failed(ex.getMessage()));
    }
}
```

### Thread Pool Tuning

```yaml
# application.yml
server:
  tomcat:
    threads:
      min-spare: 10
      max: 200                 # max concurrent requests; size based on CPU + I/O ratio
    max-connections: 10000     # max open TCP connections
    accept-count: 100          # queue size when all threads busy

spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 500
        keep-alive: 60s
```

**Thread pool sizing rule of thumb:**
```
CPU-bound tasks:   pool size ≈ number of CPU cores
I/O-bound tasks:   pool size ≈ cores × (1 + wait_time / compute_time)
                   e.g., 80% I/O wait → cores × 5
```

---

## 9. API Versioning Strategies

| Strategy | Example | Pros | Cons |
|---|---|---|---|
| **URI path** | `/v1/transactions` | Explicit, easy to cache | URL changes, not REST-pure |
| **Query param** | `?version=1` | Non-breaking | Easy to omit accidentally |
| **Header** | `API-Version: 1` | Clean URLs | Less visible, harder to test |
| **Content-Type** | `Accept: application/vnd.mc.v1+json` | REST-pure | Complex, tooling support poor |

**Path versioning** is the industry standard (MasterCard, Stripe, Twilio all use it).

```java
// Version via path
@RequestMapping("/v1/payments")
public class PaymentV1Controller { }

@RequestMapping("/v2/payments")
public class PaymentV2Controller { }

// Or use a custom condition (header-based routing without URL change)
@GetMapping(value = "/payments", headers = "API-Version=2")
public ResponseEntity<PaymentV2Dto> getV2() { }
```

---

## 10. Security

### JWT Authentication Flow

```
Client → POST /auth/login {username, password}
Server → 200 { accessToken: "eyJ...", refreshToken: "eyJ...", expiresIn: 900 }

Client → GET /v1/transactions  Authorization: Bearer eyJ...
Server → validate JWT signature + expiry → proceed or 401
```

```java
// Spring Security JWT config
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())              // REST API — no CSRF tokens
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/transactions/**").hasRole("READ")
                .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### OAuth2 (MasterCard uses this for partner APIs)

```
Authorization Code Flow (user-facing):
  1. Client redirects user → Auth Server /authorize
  2. User logs in, approves scopes
  3. Auth Server redirects back with ?code=...
  4. Client POSTs code + client_secret → Auth Server /token
  5. Auth Server returns access_token + refresh_token
  6. Client uses access_token on API calls

Client Credentials Flow (service-to-service):
  1. Service POSTs client_id + client_secret → Auth Server /token
  2. Auth Server returns access_token
  3. Service uses access_token on API calls
```

### Security Headers

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.headers(headers -> headers
        .frameOptions(fo -> fo.deny())                         // clickjacking
        .xssProtection(xss -> xss.enable())
        .contentSecurityPolicy(csp ->
            csp.policyDirectives("default-src 'self'"))
        .httpStrictTransportSecurity(hsts ->
            hsts.maxAgeInSeconds(31536000).includeSubDomains(true)));
    return http.build();
}
```

### Common Vulnerabilities to Mention

| Vulnerability | Prevention |
|---|---|
| SQL Injection | Parameterized queries / ORM |
| XSS | Output encoding, CSP header |
| CSRF | CSRF token (not needed for stateless APIs) |
| Broken Auth | Short-lived tokens, revocation list, HTTPS only |
| Excessive Data Exposure | Return only needed fields; never log full card numbers |
| Mass Assignment | Use DTOs, never bind entity directly to request body |
| Insecure Direct Object Reference | Verify ownership: `WHERE id = ? AND account_owner = ?` |
| Rate limiting bypass | Limit by IP + user ID, not just one |

---

## 11. API Design Best Practices

### Request/Response Design

```java
// Use DTOs — never expose entities directly
public record TransactionDto(
    Long   id,
    String status,
    BigDecimal amount,
    String currency,
    Instant createdAt
) {}

// Consistent pagination envelope
public record PageResponse<T>(
    List<T>  content,
    int      page,
    int      size,
    long     totalElements,
    int      totalPages,
    boolean  last
) {}

// Consistent error envelope
public record ErrorResponse(
    int    status,
    String error,
    String message,
    String traceId,
    Instant timestamp
) {}
```

### Idempotency (Critical for Payments)

```java
@PostMapping
public ResponseEntity<TransactionDto> create(
        @RequestBody @Valid CreateTransactionRequest req,
        @RequestHeader("Idempotency-Key") String idempotencyKey) {

    // Check if we've already processed this key
    Optional<Transaction> existing = idempotencyStore.find(idempotencyKey);
    if (existing.isPresent()) {
        // Return cached response — same result, no side effects
        return ResponseEntity.ok(mapper.toDto(existing.get()));
    }

    Transaction txn = service.create(req);
    idempotencyStore.save(idempotencyKey, txn, Duration.ofDays(1));

    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(txn));
}
```

### HATEOAS (Hypermedia — Level 3 REST)

```java
// Response includes links to related actions
{
  "id": 42,
  "amount": 150.00,
  "status": "PENDING",
  "_links": {
    "self":    { "href": "/v1/transactions/42" },
    "approve": { "href": "/v1/transactions/42/approve", "method": "POST" },
    "refund":  { "href": "/v1/transactions/42/refund",  "method": "POST" },
    "account": { "href": "/v1/accounts/7" }
  }
}
```

---

## 12. Observability

### Structured Logging with Trace IDs

```java
// MDC (Mapped Diagnostic Context) — propagate trace ID across logs
@Component
public class TraceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {
        String traceId = Optional.ofNullable(req.getHeader("X-Trace-ID"))
                .orElse(UUID.randomUUID().toString());
        MDC.put("traceId", traceId);
        res.setHeader("X-Trace-ID", traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}
```

### Actuator Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus, circuitbreakers
  endpoint:
    health:
      show-details: when-authorized
```

```
GET /actuator/health              -- liveness/readiness (K8s probes)
GET /actuator/metrics             -- JVM, HTTP, DB pool metrics
GET /actuator/prometheus          -- Prometheus-format metrics
GET /actuator/circuitbreakers     -- CB state per instance
```

---

## 13. RestTemplate vs WebClient vs Feign

| | RestTemplate | WebClient | Feign |
|---|---|---|---|
| Style | Synchronous, blocking | Async, reactive (non-blocking) | Declarative interface |
| Thread model | Thread-per-request | Event loop | Thread-per-request |
| Spring Boot 3+ | Deprecated | Preferred | Still supported |
| Streaming | No | Yes | No |
| Code | Verbose | Fluent builder | Minimal |

```java
// Feign (cleanest for service-to-service)
@FeignClient(name = "fraud-service", url = "${fraud.service.url}",
             configuration = FeignConfig.class)
public interface FraudClient {
    @GetMapping("/v1/risk-score/{txnId}")
    RiskScore getRiskScore(@PathVariable Long txnId);

    @PostMapping("/v1/flag")
    void flagTransaction(@RequestBody FlagRequest req);
}

// WebClient (preferred for reactive / high-concurrency)
webClient.get()
    .uri("/v1/accounts/{id}", accountId)
    .header("Authorization", "Bearer " + token)
    .retrieve()
    .onStatus(HttpStatus::is4xxClientError, res ->
        res.bodyToMono(ErrorResponse.class).map(e -> new ClientException(e.message())))
    .bodyToMono(AccountDto.class)
    .timeout(Duration.ofSeconds(3))
    .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
        .filter(ex -> ex instanceof ConnectException));
```

---

## 14. Quick Interview Reference

### Richardson Maturity Model

| Level | Description | Example |
|---|---|---|
| **0** | HTTP as transport only (RPC / SOAP style) | `POST /paymentService` |
| **1** | Resources | `POST /transactions` |
| **2** | HTTP Verbs + Status Codes | `GET /transactions/42` → 200/404 |
| **3** | Hypermedia (HATEOAS) | Response includes `_links` |

Most production REST APIs are Level 2. Level 3 is aspirational.

### Common Interview Questions

**Q: REST vs SOAP — when would you choose each?**
- REST for new microservices, public-facing APIs, mobile clients — lightweight, fast, cacheable.
- SOAP when you need guaranteed delivery (WS-ReliableMessaging), built-in security (WS-Security), formal contracts (WSDL), or integrating with existing enterprise systems like SWIFT or ISO 8583.

**Q: How do you handle high traffic on a single endpoint?**
1. **Cache** aggressively — HTTP caching (CDN/proxy) + application cache (Redis)
2. **Rate limit** — reject or queue excess traffic early (before hitting DB)
3. **Async** — queue requests; return 202 Accepted; process via worker pool
4. **Circuit breaker** — fail fast when downstream is slow; shed load
5. **Horizontal scaling** — stateless services behind a load balancer
6. **DB** — read replicas, connection pooling (HikariCP), query optimization

**Q: How do you make a POST request idempotent?**
Require a client-supplied `Idempotency-Key` header. Server caches (key → response) for N hours. On retry, return the cached response without re-executing. This is how Stripe, Mastercard, and all major payment APIs work.

**Q: What's the difference between 401 and 403?**
- `401 Unauthorized` — who are you? Token missing or invalid. Re-authenticate.
- `403 Forbidden` — I know who you are, but you don't have permission for this resource.

**Q: What's cache stampede and how do you fix it?**
When a hot key expires and many concurrent requests all miss the cache simultaneously, flooding the DB. Fix with: mutex lock on miss (one thread rebuilds, others wait), probabilistic early expiration (refresh before TTL hits), or background refresh threads.

**Q: How does a Circuit Breaker differ from a Retry?**
- **Retry** — keep trying the same call (useful for transient failures: timeouts, network blips).
- **Circuit Breaker** — stop trying entirely after repeated failures (useful for systemic failures: service is down). Fail fast, return fallback immediately. CB reopens automatically after a wait period.
- Use both together: retry a few times, then open the circuit if still failing.

**Q: Explain the difference between sync and async REST.**
- Sync: Client waits, server processes, returns result in same HTTP response. Simple but holds thread.
- Async: Server returns `202 Accepted` with a job ID immediately. Client polls `GET /jobs/{id}` or receives a webhook when done. Necessary for long-running operations (report generation, batch payments).

**Q: What HTTP status code do you return when a resource is being created asynchronously?**
`202 Accepted` — request received, processing has started but is not complete. Include a `Location` header pointing to a status endpoint.

**Q: How do you version an API without breaking existing clients?**
- Never remove or rename existing fields — only add new optional fields.
- Never change the meaning of existing fields.
- When you must break the contract, introduce a new version (`/v2/`) and deprecate `/v1/` with a sunset date in the `Deprecation` and `Sunset` response headers.
- Keep old versions alive for at least one deprecation cycle (typically 6-12 months).
