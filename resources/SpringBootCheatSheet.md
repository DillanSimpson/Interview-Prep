# **Spring Boot Deep Dive — Interview Cheat Sheet**

---

## 1) Spring Boot Core & Auto-Configuration

### How Spring Boot Works
- `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`.
- **Auto-configuration:** Spring Boot reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and conditionally registers beans.
- **Conditions:** `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty` — auto-config only applies when conditions pass.
- **Starter POMs:** Pre-packaged dependency bundles (e.g., `spring-boot-starter-web`) that trigger the matching auto-config.

### Startup Sequence
```
main()
 └─ SpringApplication.run()
     ├─ Create ApplicationContext
     ├─ Load & process @Configuration classes
     ├─ Run auto-configuration
     ├─ Instantiate & inject beans (IoC container)
     ├─ Run ApplicationRunner / CommandLineRunner
     └─ Start embedded server (Tomcat/Jetty/Undertow)
```

### Bean Lifecycle
```
Instantiate → Inject Dependencies → @PostConstruct
     → Bean Ready (in context)
     → @PreDestroy → Destroy
```
- `BeanFactoryPostProcessor` — modifies bean definitions before instantiation.
- `BeanPostProcessor` — wraps/modifies beans after instantiation (how AOP proxies are created).
- `InitializingBean.afterPropertiesSet()` / `DisposableBean.destroy()` — lifecycle hooks (prefer `@PostConstruct`/`@PreDestroy`).

### Bean Scopes
| Scope | Lifecycle |
|-------|----------|
| `singleton` (default) | One instance per ApplicationContext |
| `prototype` | New instance every time requested |
| `request` | One per HTTP request (web only) |
| `session` | One per HTTP session (web only) |
| `application` | One per ServletContext |

> **Scope mismatch trap:** Injecting a `prototype` bean into a `singleton` — the prototype is effectively frozen to one instance. Fix with `ObjectProvider<T>` or `@Lookup`.

---

## 2) AOP — Aspect-Oriented Programming

### Core Concepts
| Term | Meaning |
|------|---------|
| **Aspect** | A class encapsulating cross-cutting concern (`@Aspect`) |
| **Join Point** | A point in execution (always a method call in Spring AOP) |
| **Pointcut** | Expression selecting which join points to intercept |
| **Advice** | Code that runs at a join point |
| **Weaving** | Process of applying aspects (Spring does this at runtime via proxies) |

### Advice Types
```java
@Aspect
@Component
public class AuditAspect {

    @Before("execution(* com.example.service.*.*(..))")
    public void before(JoinPoint jp) { /* runs before method */ }

    @After("execution(* com.example.service.*.*(..))")
    public void after(JoinPoint jp) { /* always runs after */ }

    @AfterReturning(pointcut = "execution(* com.example.service.*.*(..))", returning = "result")
    public void afterReturning(Object result) { /* runs on success */ }

    @AfterThrowing(pointcut = "execution(* com.example.service.*.*(..))", throwing = "ex")
    public void afterThrowing(Exception ex) { /* runs on exception */ }

    @Around("execution(* com.example.service.*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // before
        Object result = pjp.proceed(); // actual method call
        // after
        return result;
    }
}
```

### Pointcut Expressions
```
execution(modifiers? return-type declaring-type? method-name(params) throws?)

execution(* com.example.service.*.*(..))     // all methods in service package
execution(public * *(..))                    // all public methods
@annotation(com.example.Audited)             // methods with @Audited annotation
@within(org.springframework.stereotype.Service) // all @Service classes
bean(paymentService)                         // specific bean name
```

### Common AOP Use Cases
- **Logging / Auditing** — log method entry/exit, args, return values.
- **Security checks** — verify roles before method execution.
- **Transaction management** — `@Transactional` is implemented via AOP.
- **Caching** — `@Cacheable` is AOP advice.
- **Rate limiting / Metrics** — wrap service calls with timers or counters.
- **Exception translation** — convert persistence exceptions to domain exceptions.

### AOP Limitations (important!)
- Only works on **Spring-managed beans** (not `new` instances).
- **Self-invocation bypasses AOP** — calling `this.method()` within same bean skips the proxy. Fix: inject self via `ApplicationContext` or restructure.
- AOP intercepts **public methods only** (with CGLIB, protected too, but avoid relying on it).

---

## 3) Proxying — JDK Dynamic vs CGLIB

### How Spring Creates Proxies
Spring wraps beans in a proxy to apply AOP advice and `@Transactional`. Two strategies:

| | JDK Dynamic Proxy | CGLIB Proxy |
|-|------------------|-------------|
| **Requires** | Bean implements an interface | No interface needed |
| **Mechanism** | `java.lang.reflect.Proxy` + `InvocationHandler` | Generates a subclass at runtime |
| **Limitation** | Only proxies interface methods | Cannot proxy `final` classes/methods |
| **Default** | Used when interface present (pre-Spring Boot 2.x) | Default since Spring Boot 2.x |

### Force CGLIB
```java
@EnableAspectJAutoProxy(proxyTargetClass = true) // on @Configuration
// or in application.properties:
spring.aop.proxy-target-class=true
```

### Self-invocation Problem Explained
```java
@Service
public class OrderService {
    @Transactional
    public void placeOrder() {
        this.validateOrder(); // BAD — calls real method, bypasses @Transactional proxy
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void validateOrder() { ... }
}
```
Fix: extract `validateOrder` to a separate `@Service` bean.

### Transaction Propagation
| Propagation | Behavior |
|-------------|---------|
| `REQUIRED` (default) | Join existing tx or create new one |
| `REQUIRES_NEW` | Always create new tx, suspend existing |
| `NESTED` | Nested tx with savepoint inside existing |
| `SUPPORTS` | Join if exists, non-transactional if not |
| `NOT_SUPPORTED` | Always run non-transactionally, suspend existing |
| `MANDATORY` | Must run inside existing tx, throws if none |
| `NEVER` | Must NOT run in tx, throws if one exists |

### Transaction Isolation Levels
| Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|-------|-----------|-------------------|-------------|
| `READ_UNCOMMITTED` | Yes | Yes | Yes |
| `READ_COMMITTED` | No | Yes | Yes |
| `REPEATABLE_READ` | No | No | Yes |
| `SERIALIZABLE` | No | No | No |

---

## 4) Spring Security

### Filter Chain Architecture
```
HTTP Request
  └─ DelegatingFilterProxy
      └─ FilterChainProxy
          └─ SecurityFilterChain (ordered filters)
              ├─ SecurityContextPersistenceFilter
              ├─ UsernamePasswordAuthenticationFilter
              ├─ BasicAuthenticationFilter
              ├─ BearerTokenAuthenticationFilter (JWT)
              ├─ ExceptionTranslationFilter
              └─ FilterSecurityInterceptor (authorization)
```

### Security Config (Spring Security 6+)
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())                    // stateless APIs
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### JWT Authentication Flow
```
Client → POST /auth/login {credentials}
  → AuthenticationManager.authenticate()
  → UserDetailsService.loadUserByUsername()
  → Validate password (BCrypt)
  → Generate JWT (sign with secret/RSA key)
  → Return JWT to client

Client → GET /api/resource (Authorization: Bearer <token>)
  → BearerTokenAuthenticationFilter
  → JwtDecoder validates signature + expiry
  → SecurityContext populated
  → Request proceeds
```

### Method-Level Security
```java
@EnableMethodSecurity  // replaces @EnableGlobalMethodSecurity
public class SecurityConfig { ... }

@PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
public User getUser(String userId) { ... }

@PostAuthorize("returnObject.ownerId == authentication.name")
public Account getAccount(Long id) { ... }

@Secured("ROLE_ADMIN")
public void deleteUser(Long id) { ... }
```

### Security Headers (important for Mastercard/payment context)
```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .frameOptions(frame -> frame.deny())
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
    .xssProtection(xss -> xss.enable())
);
```

### PCI DSS / Payment Security Considerations
- **TLS 1.2+ mandatory** — disable older protocols in embedded Tomcat.
- **Encrypt PAN data at rest** — never log card numbers; use tokenization.
- **Audit logging** — log all access to sensitive resources (AOP + `@Audited`).
- **Rate limiting** — protect payment endpoints from brute force (Bucket4j + Redis).
- **CSRF** — disable for stateless REST APIs using JWT; enable for session-based UIs.
- **Principle of least privilege** — fine-grained roles (`ROLE_PAYMENT_READ`, `ROLE_PAYMENT_WRITE`).
- **Secrets management** — never hardcode credentials; use Vault or environment variables.

---

## 5) REST APIs & OpenAPI

### REST Controller Pattern
```java
@RestController
@RequestMapping("/api/v1/payments")
@Validated
public class PaymentController {

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable @NotNull Long id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(
            @RequestBody @Valid CreatePaymentRequest request) {
        PaymentDto created = paymentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### API Versioning Strategies
| Strategy | Example | Pros | Cons |
|---------|---------|------|------|
| **URI Path** | `/api/v1/payments` | Simple, cacheable | URL pollution |
| **Request Param** | `?version=1` | Flexible | Less RESTful |
| **Header** | `Accept-Version: v1` | Clean URLs | Less discoverable |
| **Media Type** | `Accept: application/vnd.company.v1+json` | Most RESTful | Complex |

### OpenAPI / Springdoc
```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info().title("Payment API").version("v1"))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
        .components(new Components()
            .addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(HTTP).scheme("bearer").bearerFormat("JWT")));
}

// On controller methods:
@Operation(summary = "Process a payment", description = "Initiates a new payment transaction")
@ApiResponse(responseCode = "201", description = "Payment created")
@ApiResponse(responseCode = "400", description = "Invalid request")
```

### HTTP Status Codes — Quick Reference
| Code | When to Use |
|------|------------|
| 200 OK | Successful GET, PUT, PATCH |
| 201 Created | Successful POST (include Location header) |
| 204 No Content | Successful DELETE |
| 400 Bad Request | Validation failure, malformed request |
| 401 Unauthorized | Not authenticated |
| 403 Forbidden | Authenticated but not authorized |
| 404 Not Found | Resource doesn't exist |
| 409 Conflict | Duplicate / state conflict |
| 422 Unprocessable Entity | Semantic validation error |
| 429 Too Many Requests | Rate limit exceeded |
| 500 Internal Server Error | Unexpected server failure |
| 503 Service Unavailable | Downstream dependency down |

---

## 6) Configuration & Profiles

### Externalized Config Hierarchy (highest to lowest priority)
1. Command-line arguments (`--server.port=9090`)
2. `SPRING_APPLICATION_JSON` env variable
3. OS environment variables
4. `application-{profile}.properties` / `.yml`
5. `application.properties` / `.yml`
6. `@PropertySource` on `@Configuration`
7. Default properties

### Profiles
```yaml
# application.yml
spring:
  profiles:
    active: dev

---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:devdb

---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:postgresql://prod-host/payments
```

```java
@Profile("prod")
@Bean
public DataSource prodDataSource() { ... }

@Profile("!prod")
@Bean
public DataSource devDataSource() { ... }
```

### Type-Safe Configuration
```java
@ConfigurationProperties(prefix = "payment")
@Validated
public class PaymentProperties {
    @NotNull
    private String gatewayUrl;
    @Min(1) @Max(30)
    private int timeoutSeconds = 10;
    private Map<String, String> headers = new HashMap<>();
    // getters/setters
}

// In application.yml:
payment:
  gateway-url: https://api.payment.com
  timeout-seconds: 15
  headers:
    X-API-KEY: ${PAYMENT_API_KEY}
```

### Actuator Endpoints
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```
Key endpoints: `/actuator/health`, `/actuator/metrics`, `/actuator/env`, `/actuator/beans`, `/actuator/mappings`, `/actuator/loggers`

---

## 7) Validation

### Bean Validation (JSR-380)
```java
public class CreatePaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    @Valid                          // cascades validation into nested object
    private CardDetails card;

    @Pattern(regexp = "^\\d{16}$", message = "Card number must be 16 digits")
    private String cardNumber;

    @Future(message = "Expiry must be in the future")
    private LocalDate expiryDate;
}
```

### Controller Validation
```java
// Trigger validation on request body:
public ResponseEntity<?> create(@RequestBody @Valid CreatePaymentRequest req) { ... }

// Trigger validation on path/query params (need @Validated on class):
@Validated
@RestController
public class PaymentController {
    public ResponseEntity<?> get(@PathVariable @Min(1) Long id) { ... }
}
```

### Custom Validator
```java
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = LuhnValidator.class)
public @interface ValidCardNumber {
    String message() default "Invalid card number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class LuhnValidator implements ConstraintValidator<ValidCardNumber, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        return value != null && luhnCheck(value);
    }
}
```

### Validation Groups
```java
public interface OnCreate {}
public interface OnUpdate {}

public class PaymentRequest {
    @Null(groups = OnCreate.class)      // must be null on create
    @NotNull(groups = OnUpdate.class)   // must be set on update
    private Long id;
}

// Controller:
public ResponseEntity<?> create(@RequestBody @Validated(OnCreate.class) PaymentRequest req) { ... }
```

---

## 8) Exception Handling

### Global Exception Handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.toList());
        return new ErrorResponse("VALIDATION_ERROR", errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return new ErrorResponse("FORBIDDEN", "Access denied");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
    }
}
```

### Problem Detail (RFC 7807 — Spring 6+)
```java
// application.properties:
spring.mvc.problemdetails.enabled=true

// Spring auto-generates:
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "amount: must be greater than 0",
  "instance": "/api/v1/payments"
}
```

### Custom Exception Hierarchy
```java
public abstract class BaseApiException extends RuntimeException {
    private final String errorCode;
    // constructor, getter
}

public class PaymentNotFoundException extends BaseApiException {
    public PaymentNotFoundException(Long id) {
        super("PAYMENT_NOT_FOUND", "Payment not found: " + id);
    }
}

public class DuplicatePaymentException extends BaseApiException {
    public DuplicatePaymentException(String ref) {
        super("DUPLICATE_PAYMENT", "Payment already exists: " + ref);
    }
}
```

### Error Response Structure
```java
public record ErrorResponse(
    String code,
    Object message,         // String or List<String>
    Instant timestamp,
    String path
) {
    public ErrorResponse(String code, Object message) {
        this(code, message, Instant.now(), null);
    }
}
```

---

## 9) Spring Data & JPA Deep Dive

### Repository Hierarchy
```
Repository (marker)
 └─ CrudRepository       (save, findById, delete, count)
     └─ PagingAndSortingRepository  (findAll(Pageable))
         └─ JpaRepository           (flush, saveAndFlush, deleteInBatch)
```

### Derived Query Methods
```java
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStatusAndAmountGreaterThan(Status status, BigDecimal amount);
    Optional<Payment> findByReferenceId(String refId);
    Page<Payment> findByMerchantId(Long merchantId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.createdAt > :since AND p.status = :status")
    List<Payment> findRecentByStatus(@Param("since") LocalDateTime since,
                                     @Param("status") Status status);

    @Modifying
    @Transactional
    @Query("UPDATE Payment p SET p.status = :status WHERE p.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") Status status);
}
```

### Entity Design Patterns
```java
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_ref", columnList = "reference_id"),
    @Index(name = "idx_payment_merchant", columnList = "merchant_id, status")
})
public class Payment extends BaseEntity {  // BaseEntity has @Id, createdAt, updatedAt

    @Enumerated(EnumType.STRING)           // always STRING, not ORDINAL
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)    // always LAZY
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentEvent> events = new ArrayList<>();
}

@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version                              // optimistic locking
    private Long version;
}
```

---

## 10) Resilience Patterns (Resilience4j)

```java
@CircuitBreaker(name = "paymentGateway", fallbackMethod = "fallbackPayment")
@Retry(name = "paymentGateway")
@TimeLimiter(name = "paymentGateway")
@RateLimiter(name = "paymentGateway")
public CompletableFuture<PaymentResponse> callGateway(PaymentRequest req) {
    return CompletableFuture.supplyAsync(() -> gatewayClient.process(req));
}

public CompletableFuture<PaymentResponse> fallbackPayment(PaymentRequest req, Exception ex) {
    log.error("Gateway call failed, using fallback", ex);
    return CompletableFuture.completedFuture(PaymentResponse.pending(req.getId()));
}
```

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      paymentGateway:
        maxAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - java.net.ConnectException
          - java.util.concurrent.TimeoutException
```

---

## 11) Messaging (Kafka Integration)

```java
@KafkaListener(topics = "payment-events", groupId = "payment-processor",
               containerFactory = "kafkaListenerContainerFactory")
public void handlePaymentEvent(@Payload PaymentEvent event,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                Acknowledgment ack) {
    try {
        paymentService.process(event);
        ack.acknowledge();          // manual commit after successful processing
    } catch (RetryableException ex) {
        // don't ack — let retry happen
        throw ex;
    } catch (Exception ex) {
        // send to DLQ
        deadLetterTemplate.send("payment-events.DLT", event);
        ack.acknowledge();
    }
}

@Bean
public NewTopic paymentTopic() {
    return TopicBuilder.name("payment-events")
        .partitions(6)
        .replicas(3)
        .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 days
        .build();
}
```

---

## 12) Annotations Reference Table

### Core Stereotypes
| Annotation | Purpose |
|-----------|---------|
| `@Component` | Generic Spring-managed bean |
| `@Service` | Business logic layer — semantic alias for `@Component` |
| `@Repository` | Data access layer — adds exception translation |
| `@Controller` | MVC controller — returns view names |
| `@RestController` | `@Controller` + `@ResponseBody` — returns data directly |
| `@Configuration` | Defines `@Bean` methods; processed early in lifecycle |

### Request Mapping
| Annotation | Purpose |
|-----------|---------|
| `@RequestMapping` | Map URL pattern to class or method |
| `@GetMapping` | HTTP GET shorthand |
| `@PostMapping` | HTTP POST shorthand |
| `@PutMapping` | HTTP PUT shorthand |
| `@PatchMapping` | HTTP PATCH shorthand |
| `@DeleteMapping` | HTTP DELETE shorthand |
| `@PathVariable` | Extract value from URL path segment |
| `@RequestParam` | Extract query parameter |
| `@RequestBody` | Deserialize request body to object |
| `@RequestHeader` | Extract HTTP header value |
| `@ResponseStatus` | Set HTTP status code for response |
| `@ResponseBody` | Write return value directly to response body |

### Dependency Injection
| Annotation | Purpose |
|-----------|---------|
| `@Autowired` | Inject dependency (by type); prefer constructor injection |
| `@Qualifier("name")` | Disambiguate when multiple beans of same type exist |
| `@Primary` | Mark bean as default when multiple candidates exist |
| `@Inject` | JSR-330 equivalent of `@Autowired` |
| `@Resource` | JSR-250 — injects by name first, then type |
| `@Lazy` | Delay bean initialization until first use |
| `@Value("${prop}")` | Inject property value or SpEL expression |

### Configuration
| Annotation | Purpose |
|-----------|---------|
| `@Bean` | Declare a bean in a `@Configuration` class |
| `@Profile("dev")` | Activate bean only for specified profile |
| `@Conditional` | Register bean if condition class returns true |
| `@ConditionalOnProperty` | Register bean if property has specified value |
| `@ConditionalOnClass` | Register bean if class is on classpath |
| `@ConditionalOnMissingBean` | Register bean only if no other bean of that type exists |
| `@PropertySource` | Load properties file into Environment |
| `@ConfigurationProperties` | Bind properties to a typed POJO |
| `@EnableConfigurationProperties` | Activate `@ConfigurationProperties` scanning |

### AOP
| Annotation | Purpose |
|-----------|---------|
| `@Aspect` | Declare class as an aspect |
| `@Before` | Advice runs before join point |
| `@After` | Advice runs after join point (always) |
| `@AfterReturning` | Advice runs after successful return |
| `@AfterThrowing` | Advice runs after exception thrown |
| `@Around` | Advice wraps join point (most powerful) |
| `@Pointcut` | Reusable pointcut expression |
| `@EnableAspectJAutoProxy` | Enable AOP proxy creation |

### Transaction & Data
| Annotation | Purpose |
|-----------|---------|
| `@Transactional` | Demarcate transaction boundary |
| `@Modifying` | Mark `@Query` as write operation (UPDATE/DELETE) |
| `@Query` | Custom JPQL or native SQL query |
| `@EntityGraph` | Define fetch graph to avoid N+1 |
| `@Lock(LockModeType.PESSIMISTIC_WRITE)` | Acquire DB row lock |
| `@Version` | Enable optimistic locking |
| `@CreationTimestamp` | Auto-set on entity creation (Hibernate) |
| `@UpdateTimestamp` | Auto-set on entity update (Hibernate) |
| `@Enumerated(EnumType.STRING)` | Persist enum as string |

### Validation
| Annotation | Purpose |
|-----------|---------|
| `@Valid` | Trigger cascading Bean Validation |
| `@Validated` | Spring's variant — supports validation groups |
| `@NotNull` | Value must not be null |
| `@NotBlank` | String must not be null or blank |
| `@NotEmpty` | Collection/String must not be null or empty |
| `@Size(min, max)` | Size constraint on String/Collection |
| `@Min` / `@Max` | Numeric range |
| `@DecimalMin` / `@DecimalMax` | BigDecimal range |
| `@Pattern(regexp)` | Regex constraint on String |
| `@Email` | Valid email format |
| `@Future` / `@Past` | Date must be in future or past |
| `@Positive` / `@Negative` | Numeric sign constraint |
| `@Digits(integer, fraction)` | Max digits in integer and decimal parts |

### Security
| Annotation | Purpose |
|-----------|---------|
| `@EnableWebSecurity` | Enable Spring Security's web support |
| `@EnableMethodSecurity` | Enable method-level security annotations |
| `@PreAuthorize` | Check expression before method executes |
| `@PostAuthorize` | Check expression using return value after execution |
| `@Secured` | Role-based restriction on method |
| `@RolesAllowed` | JSR-250 role check |
| `@WithMockUser` | Test utility — inject mock security context |

### Caching
| Annotation | Purpose |
|-----------|---------|
| `@EnableCaching` | Activate Spring caching infrastructure |
| `@Cacheable` | Return cached result; execute method on miss |
| `@CachePut` | Always execute method and update cache |
| `@CacheEvict` | Remove entry from cache |
| `@Caching` | Group multiple cache annotations |

### Testing
| Annotation | Purpose |
|-----------|---------|
| `@SpringBootTest` | Full application context integration test |
| `@WebMvcTest` | Slice test — only MVC layer |
| `@DataJpaTest` | Slice test — only JPA/DB layer (in-memory DB) |
| `@MockBean` | Add Mockito mock as Spring bean in context |
| `@SpyBean` | Add Mockito spy as Spring bean |
| `@TestConfiguration` | Additional beans for test context only |
| `@ActiveProfiles("test")` | Activate test profile |
| `@Sql` | Run SQL scripts before/after test |
| `@Transactional` on test | Rollback DB changes after each test |
| `@AutoConfigureMockMvc` | Auto-configure MockMvc without full server |

### Async & Scheduling
| Annotation | Purpose |
|-----------|---------|
| `@EnableAsync` | Enable `@Async` method execution |
| `@Async` | Execute method in separate thread |
| `@EnableScheduling` | Enable `@Scheduled` task execution |
| `@Scheduled(cron="...")` | Run method on cron schedule |

### Actuator / Observability
| Annotation | Purpose |
|-----------|---------|
| `@Endpoint` | Declare custom Actuator endpoint |
| `@ReadOperation` | HTTP GET on Actuator endpoint |
| `@WriteOperation` | HTTP POST on Actuator endpoint |
| `@Timed` | Micrometer — record method execution time |
| `@Counted` | Micrometer — count method invocations |

---

## 13) Mastercard / Enterprise Context — Key Points

### API Gateway & Service Mesh
- **mTLS (mutual TLS)** — both client and server present certificates. Standard in service-to-service calls within Mastercard infrastructure.
- **OAuth 2.0 + PKCE** — standard for external API authorization.
- **API Keys + HMAC signing** — request signing for webhook payloads and callback validation.
- **Idempotency keys** — payment APIs must support idempotent POST requests (`Idempotency-Key` header + DB-stored results) to handle retries safely.

### Idempotency Pattern
```java
@PostMapping("/payments")
public ResponseEntity<PaymentDto> createPayment(
        @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
        @RequestBody @Valid CreatePaymentRequest req) {

    return idempotencyService.getOrExecute(idempotencyKey,
        () -> paymentService.create(req),
        PaymentDto.class);
}

// IdempotencyService: check Redis/DB for existing result by key
// If found → return cached response
// If not   → execute, store result, return
```

### Distributed Tracing
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% in dev, lower in prod
spring:
  application:
    name: payment-service
```
- Trace ID propagated across services via `traceparent` header (W3C standard) or `X-B3-TraceId` (Zipkin).
- Use `@NewSpan` or `Observation` API to create child spans.

### Health Checks & Graceful Shutdown
```yaml
management:
  endpoint:
    health:
      show-details: always
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true

server:
  shutdown: graceful   # drain in-flight requests before stopping

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Configuration Best Practices
- **Never hardcode secrets** — use environment variables, Vault, or AWS Secrets Manager.
- **Separate config per environment** — `application-prod.yml` should only override, not duplicate.
- **Validate config on startup** — `@Validated` on `@ConfigurationProperties` fails fast.
- **Feature flags** — gate new payment flows behind `@ConditionalOnProperty` or LaunchDarkly.

---

## 14) Quick Interview Q&A

**Q: What is the difference between `@Component`, `@Service`, `@Repository`?**
All three are `@Component` aliases — functionally identical for bean registration. `@Repository` adds exception translation (converts persistence exceptions to Spring's `DataAccessException`). `@Service` is semantic — it signals business logic layer to readers and tools.

**Q: How does `@Transactional` work internally?**
Spring wraps the bean in a proxy (CGLIB or JDK). When the proxied method is called, the proxy intercepts it, opens a transaction (or joins existing), invokes the real method, then commits or rolls back depending on the outcome. Self-invocation bypasses this because it calls the real object, not the proxy.

**Q: When does `@Transactional` NOT rollback?**
By default, only rolls back on `RuntimeException` and `Error`. Checked exceptions do NOT trigger rollback. Fix: `@Transactional(rollbackFor = Exception.class)`.

**Q: What is the difference between `@MockBean` and `@Mock`?**
`@Mock` (Mockito) creates a mock outside Spring context. `@MockBean` registers a mock as a Spring bean, replacing any existing bean of that type in the ApplicationContext — used in `@SpringBootTest` or `@WebMvcTest` slice tests.

**Q: How would you prevent duplicate payment submissions?**
Idempotency key + atomic check-and-store in Redis or DB with unique constraint on the key. On duplicate key, return the original response without re-processing.

**Q: What is N+1 and how do you fix it in Spring Data JPA?**
N+1: 1 query fetches parent entities, then N separate queries fetch each child. Fix options: `JOIN FETCH` in JPQL, `@EntityGraph` on repository method, or `@BatchSize(size=25)` on the collection for Hibernate batching.

**Q: How do you secure sensitive fields in logs?**
Use `@JsonIgnore` or `@JsonProperty(access = WRITE_ONLY)` to exclude from serialization. Custom `toString()` that masks PAN/CVV. Logback pattern with custom converters to redact known field names. Never log `HttpServletRequest` body containing card data directly.
