# Accertify Tech Stack — Quick Reference

> Covers every technology in the JD. GemFire callouts show direct experience bridges
> from the Grid team at Mastercard.

---

## Spring Boot

### Core Annotations
| Annotation | Purpose |
|---|---|
| `@SpringBootApplication` | Combines `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan` |
| `@RestController` | `@Controller` + `@ResponseBody` — returns JSON directly |
| `@RequestMapping` / `@GetMapping` etc. | Map HTTP verbs to handler methods |
| `@Service` | Business logic bean |
| `@Repository` | Data access bean — also translates exceptions to `DataAccessException` |
| `@Component` | Generic Spring-managed bean |
| `@Autowired` | Inject dependency (prefer constructor injection) |
| `@Value("${prop}")` | Inject property from `application.properties` / `application.yml` |
| `@ConfigurationProperties` | Bind a group of properties to a typed POJO |
| `@Profile` | Activate bean only for a specific environment |
| `@Transactional` | Wrap method in a DB transaction (commit on success, rollback on exception) |

### Application Lifecycle
```
main() → SpringApplication.run()
  → creates ApplicationContext
  → scans components
  → auto-configures (DataSource, JPA, etc.)
  → starts embedded Tomcat (default port 8080)
```

### Constructor Injection (preferred over @Autowired on field)
```java
@Service
public class FraudService {
    private final RuleEngine ruleEngine;
    private final TransactionRepository repo;

    public FraudService(RuleEngine ruleEngine, TransactionRepository repo) {
        this.ruleEngine = ruleEngine;
        this.repo = repo;
    }
}
```

### application.yml — common config
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fraud
    username: ${DB_USER}
    password: ${DB_PASS}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
server:
  port: 8080
```

### Actuator (health / metrics endpoint)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

> **GemFire bridge**: On the Grid team, Spring Boot apps used `spring-data-gemfire` for region-backed repositories — same `@Repository` / `@Service` layering, same `@Transactional` semantics, just targeting GemFire regions instead of a relational DB.

---

## SQL & JDBC

### JDBC Template (Spring)
```java
@Repository
public class TransactionDao {
    private final JdbcTemplate jdbc;

    TransactionDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Transaction findById(long id) {
        return jdbc.queryForObject(
            "SELECT * FROM transactions WHERE id = ?",
            (rs, row) -> new Transaction(rs.getLong("id"), rs.getString("status")),
            id
        );
    }

    public int updateStatus(long id, String status) {
        return jdbc.update("UPDATE transactions SET status = ? WHERE id = ?", status, id);
    }
}
```

### SQL Performance Quick Reference
| Scenario | Fix |
|---|---|
| Full table scan on large table | Add index on `WHERE` / `JOIN` column |
| `SELECT *` in prod code | List only needed columns — reduces I/O and network |
| N+1 queries | Use `JOIN` or `IN (?)` to batch; in JPA use `@EntityGraph` or `JOIN FETCH` |
| Slow `LIKE '%term%'` | Full-text search index, or move to Elasticsearch / Solr |
| Missing `LIMIT` on admin queries | Always paginate large result sets |
| Lock contention | Use `SELECT ... FOR UPDATE SKIP LOCKED` for queue-style patterns |

### Key SQL Concepts
```sql
-- Indexes
CREATE INDEX idx_txn_card ON transactions(card_number);
CREATE INDEX idx_txn_status_created ON transactions(status, created_at); -- composite

-- Explain plan (Postgres)
EXPLAIN ANALYZE SELECT * FROM transactions WHERE card_number = '4111...';

-- Window functions (useful for fraud: rank events per card)
SELECT card_number,
       amount,
       ROW_NUMBER() OVER (PARTITION BY card_number ORDER BY created_at DESC) AS rn
FROM transactions;

-- CTE
WITH recent AS (
    SELECT * FROM transactions WHERE created_at > NOW() - INTERVAL '1 hour'
)
SELECT card_number, COUNT(*) FROM recent GROUP BY card_number HAVING COUNT(*) > 5;
```

> **GemFire bridge**: GemFire's OQL (Object Query Language) maps 1:1 to SQL intuition.
> `SELECT * FROM /TransactionRegion t WHERE t.cardNumber = $1` is the GemFire equivalent of
> a SQL point lookup. Index creation (`QueryService.createIndex`) mirrors SQL index strategy —
> same trade-off: faster reads, slower writes.

---

## Hibernate & Spring Data JPA

### Entity Mapping
```java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", nullable = false, length = 20)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)      // LAZY = don't load merchant until accessed
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @CreationTimestamp
    private Instant createdAt;
}
```

### Spring Data Repository
```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCardNumberAndStatus(String cardNumber, TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt > :since AND t.amount > :threshold")
    List<Transaction> findHighValueRecent(@Param("since") Instant since,
                                          @Param("threshold") BigDecimal threshold);

    @Modifying
    @Query("UPDATE Transaction t SET t.status = :status WHERE t.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") TransactionStatus status);
}
```

### FetchType & N+1 Problem
```java
// BAD — triggers N selects for merchants when iterating transactions
List<Transaction> txns = repo.findAll();
txns.forEach(t -> t.getMerchant().getName()); // each getMerchant() = 1 extra query

// GOOD — single JOIN FETCH
@Query("SELECT t FROM Transaction t JOIN FETCH t.merchant WHERE t.status = 'PENDING'")
List<Transaction> findPendingWithMerchant();
```

### `@Transactional` Behaviour
| Propagation | Meaning |
|---|---|
| `REQUIRED` (default) | Join existing tx; create new one if none |
| `REQUIRES_NEW` | Always start a new tx; suspend the current |
| `MANDATORY` | Must be called within an existing tx |
| `NOT_SUPPORTED` | Suspend tx if one exists, run without |

Rollback happens automatically on `RuntimeException`. For checked exceptions add `@Transactional(rollbackFor = MyCheckedException.class)`.

> **GemFire bridge**: GemFire supports distributed transactions via `CacheTransactionManager` —
> same commit/rollback semantics, but across in-memory regions. The Spring `@Transactional`
> annotation worked identically; the platform underneath was GemFire instead of Postgres.

---

## Redis

### Core Data Types
| Type | Commands | Use case |
|---|---|---|
| String | `GET`, `SET`, `INCR`, `EXPIRE` | Session tokens, counters, feature flags |
| Hash | `HGET`, `HSET`, `HMGET` | Object fields (user profile, cart) |
| List | `LPUSH`, `RPOP`, `LRANGE` | Activity feed, message queue |
| Set | `SADD`, `SISMEMBER`, `SMEMBERS` | Unique visitors, tags |
| Sorted Set | `ZADD`, `ZRANGE`, `ZRANGEBYSCORE` | Leaderboards, rate-limiting windows |
| Stream | `XADD`, `XREAD`, `XACK` | Event log (lightweight Kafka alternative) |

### Spring Data Redis
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(factory);
        t.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return t;
    }
}

// Usage
redisTemplate.opsForValue().set("fraud:session:" + sessionId, payload, Duration.ofMinutes(30));
Object cached = redisTemplate.opsForValue().get("fraud:session:" + sessionId);

// @Cacheable — declarative caching on any Spring bean method
@Cacheable(value = "merchants", key = "#merchantId")
public Merchant getMerchant(Long merchantId) { return repo.findById(merchantId).orElseThrow(); }

@CacheEvict(value = "merchants", key = "#merchant.id")
public void updateMerchant(Merchant merchant) { repo.save(merchant); }
```

### Redis vs GemFire — Direct Comparison
| Feature | Redis | GemFire (Mastercard) |
|---|---|---|
| Data model | Key/value + rich types | Regions (partitioned or replicated) storing Java objects |
| Query | RediSearch module / SCAN | OQL — full `SELECT`/`WHERE` support |
| Server-side compute | Lua scripts | Entry Processors, Function Execution |
| Replication | Master/replica, Cluster | WAN Gateway + redundant copies |
| Persistence | RDB snapshots / AOF log | Disk store + overflow |
| Event-driven | Pub/Sub, Keyspace notifications | Continuous Queries (CQ), AsyncEventQueue |
| Spring integration | Spring Data Redis | Spring Data GemFire (same repository API) |
| Typical latency | <1 ms | <1 ms |

> **Talking point**: "At Mastercard we used GemFire as the in-memory data grid for payment
> authorization — essentially the same role Redis plays at Accertify: sub-millisecond reads,
> distributed caching, and event-driven invalidation. The Spring Data abstraction was identical;
> I'd swap `RedisTemplate` for `GemFireTemplate` and be immediately productive."

---

## Apache Kafka

### Core Concepts
| Concept | Description |
|---|---|
| **Topic** | Named stream of records, split into partitions |
| **Partition** | Ordered, immutable log; unit of parallelism |
| **Offset** | Each record's position within a partition |
| **Producer** | Writes records to a topic |
| **Consumer** | Reads records; tracks offset per partition |
| **Consumer Group** | Multiple consumers sharing a topic's partitions (each partition → one consumer) |
| **Broker** | A Kafka server holding partition replicas |
| **Retention** | Records kept for a configured time/size regardless of consumption |

### Spring Kafka
```java
// Producer
@Service
public class FraudEventProducer {
    private final KafkaTemplate<String, FraudEvent> kafka;

    public void send(FraudEvent event) {
        kafka.send("fraud-events", event.getTransactionId(), event)
             .whenComplete((result, ex) -> {
                 if (ex != null) log.error("Send failed", ex);
             });
    }
}

// Consumer
@Component
public class FraudEventConsumer {
    @KafkaListener(topics = "fraud-events", groupId = "fraud-processor")
    public void handle(FraudEvent event, Acknowledgment ack) {
        process(event);
        ack.acknowledge();       // manual commit — guarantees at-least-once
    }
}
```

### application.yml — Kafka config
```yaml
spring:
  kafka:
    bootstrap-servers: kafka-broker:9092
    consumer:
      group-id: fraud-processor
      auto-offset-reset: earliest
      enable-auto-commit: false       # manual ack for reliability
    producer:
      acks: all                       # wait for all replicas to confirm
      retries: 3
```

### Delivery Guarantees
| Setting | Guarantee |
|---|---|
| `acks=0` | Fire and forget — possible loss |
| `acks=1` | Leader confirms — loss if leader fails before replication |
| `acks=all` | All in-sync replicas confirm — strongest durability |
| `enable.idempotence=true` | Exactly-once producer (deduplicates retries) |
| Manual consumer offset commit | At-least-once consumption |

> **GemFire bridge**: GemFire's `AsyncEventQueue` (AEQ) is architecturally similar to a
> Kafka topic — it buffers events from region mutations and delivers them asynchronously to
> listeners. The ordering and at-least-once delivery guarantees are the same conceptual model.
> GemFire CQ (Continuous Query) is analogous to a Kafka consumer that filters on server-side:
> only events matching a `WHERE` predicate are pushed to the client.

---

## Cassandra / ScyllaDB / AWS Keyspaces

### Data Model Fundamentals
```
Keyspace → Namespace (like a database/schema)
  Table   → Column families
    Partition Key → determines which node holds the data (hash-based)
    Clustering Key → sort order within a partition
```

```sql
-- Table design: always model around your query
CREATE TABLE transactions_by_card (
    card_number   TEXT,
    created_at    TIMESTAMP,
    txn_id        UUID,
    amount        DECIMAL,
    status        TEXT,
    PRIMARY KEY ((card_number), created_at, txn_id)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- Query (must include partition key)
SELECT * FROM transactions_by_card
WHERE card_number = '4111...'
  AND created_at > '2024-01-01';
```

### CAP Theorem & Tunable Consistency
| Setting | Meaning | Use case |
|---|---|---|
| `ONE` | Fastest, weakest consistency | Read cache, analytics |
| `QUORUM` | Majority of replicas agree | Balanced fraud reads |
| `ALL` | Every replica must respond | Critical writes where loss = unacceptable |
| `LOCAL_QUORUM` | Quorum within local DC | Multi-region fraud platform |

### Spring Data Cassandra
```java
@Table("transactions_by_card")
public class TransactionByCard {
    @PrimaryKeyColumn(type = PARTITIONED) private String cardNumber;
    @PrimaryKeyColumn(type = CLUSTERED, ordering = DESCENDING) private Instant createdAt;
    private UUID txnId;
    private BigDecimal amount;
}

public interface TransactionByCardRepository
        extends CassandraRepository<TransactionByCard, TransactionByCardKey> {
    List<TransactionByCard> findByCardNumber(String cardNumber);
}
```

### ScyllaDB vs Cassandra vs AWS Keyspaces
| | Cassandra | ScyllaDB | AWS Keyspaces |
|---|---|---|---|
| Engine | JVM | C++ (Seastar) | Managed Cassandra-compatible |
| Latency | ms | sub-ms | ms (managed overhead) |
| Ops burden | Self-managed | Self-managed | Zero (serverless) |
| CQL compatible | Yes | Yes | Mostly (some limits) |

> **GemFire bridge**: GemFire's partitioned regions are the conceptual equivalent of Cassandra
> partitions — data is distributed by a partition key (routing key in GemFire), and each node
> owns a subset of buckets. Replication factor in Cassandra = redundant copies in GemFire.
> Both achieve horizontal scale by adding nodes and rebalancing partitions automatically.

---

## AWS S3

### Common Patterns in a Fraud Platform
| Use case | S3 pattern |
|---|---|
| Cold storage for raw events | Append-only objects with date-partitioned prefixes |
| ML training data | Large Parquet / CSV exports from Cassandra / Kafka |
| Audit logs | Write-once objects with Object Lock (WORM) |
| Batch job input/output | Read input prefix → process → write output prefix |

### Spring Cloud AWS / SDK v2
```java
@Service
public class S3ReportService {
    private final S3Client s3;

    // Upload
    public void upload(String bucket, String key, byte[] data) {
        s3.putObject(PutObjectRequest.builder()
                .bucket(bucket).key(key)
                .contentType("application/json")
                .build(),
            RequestBody.fromBytes(data));
    }

    // Download
    public byte[] download(String bucket, String key) {
        return s3.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket).key(key).build())
            .asByteArray();
    }

    // List with prefix (date-partitioned)
    public List<String> listKeys(String bucket, String prefix) {
        return s3.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucket).prefix(prefix).build())
            .contents().stream()
            .map(S3Object::key)
            .collect(Collectors.toList());
    }
}
```

---

## Drools (Rule Engine)

### What it is
Drools is a **Business Rule Management System (BRMS)**. Rules live in `.drl` files (or a decision table), are loaded into a `KieSession`, and fire when working-memory facts match their conditions. Fraud platforms use it to encode risk rules outside of Java code so analysts can update them without a deploy.

### Rule anatomy
```drl
// fraud-rules.drl
package com.accertify.rules;

rule "High Value International Transaction"
    when
        $t : Transaction(amount > 5000, country != "US", status == "PENDING")
    then
        $t.setRiskScore($t.getRiskScore() + 50);
        $t.addFlag("HIGH_VALUE_INTERNATIONAL");
        update($t);
end

rule "Velocity Check — 3+ txns in 10 minutes"
    when
        $t : Transaction($card : cardNumber, status == "PENDING")
        accumulate(
            Transaction(cardNumber == $card,
                        createdAt > (System.currentTimeMillis() - 600_000)),
            $count : count(1);
            $count >= 3
        )
    then
        $t.setRiskScore($t.getRiskScore() + 75);
        $t.addFlag("HIGH_VELOCITY");
        update($t);
end
```

### Spring Boot Integration
```java
@Configuration
public class DroolsConfig {
    @Bean
    public KieContainer kieContainer() {
        return KieServices.Factory.get().getKieClasspathContainer();
    }
}

@Service
public class RuleEngineService {
    private final KieContainer kieContainer;

    public Transaction evaluate(Transaction txn) {
        KieSession session = kieContainer.newKieSession("fraud-session");
        try {
            session.insert(txn);
            session.fireAllRules();
        } finally {
            session.dispose();
        }
        return txn;
    }
}
```

---

## Cryptography (Java)

### Common Use Cases in Fraud/Payments
| Need | Algorithm | Java class |
|---|---|---|
| Hash a password | BCrypt / Argon2 | Spring Security `PasswordEncoder` |
| Hash for integrity check | SHA-256 | `MessageDigest` |
| Symmetric encryption | AES-256-GCM | `javax.crypto.Cipher` |
| Asymmetric encryption / signing | RSA-2048 / ECDSA | `java.security.KeyPair` |
| HMAC (request signing) | HMAC-SHA256 | `javax.crypto.Mac` |
| Tokenization (PAN → token) | AES + secure random token store | Custom + Redis/DB |

```java
// SHA-256 hash
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
String hex = HexFormat.of().formatHex(hash);

// AES-256-GCM encrypt
SecretKey key = new SecretKeySpec(keyBytes, "AES");
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, iv);
cipher.init(Cipher.ENCRYPT_MODE, key, spec);
byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

// HMAC-SHA256 (API request signing)
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(secret, "HmacSHA256"));
String signature = Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes()));
```

> **GemFire bridge**: At Mastercard, card numbers (PANs) in GemFire regions were stored as
> tokens, not plaintext. The tokenization layer used AES encryption before inserting into the
> region — same pattern Accertify uses for PAN/account data at rest.

---

## Gradle

### build.gradle.kts — Spring Boot project skeleton
```kotlin
plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"           // or java plugin
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.drools:drools-core:8.44.0.Final")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:postgresql")
}
```

### Useful Gradle Tasks
| Task | What it does |
|---|---|
| `./gradlew build` | Compile + test + package JAR |
| `./gradlew test` | Run tests only |
| `./gradlew bootRun` | Start the Spring Boot app locally |
| `./gradlew dependencies` | Print full dependency tree |
| `./gradlew dependencyInsight --dependency log4j` | Find where a transitive dep comes from |
| `./gradlew bootJar` | Build the executable fat JAR |

### Dependency Conflicts
```kotlin
// Force a specific version
configurations.all {
    resolutionStrategy.force("org.yaml:snakeyaml:2.0")
}

// Exclude a transitive dependency
implementation("some.group:some-lib") {
    exclude(group = "commons-logging", module = "commons-logging")
}
```

---

## High-Volume Transactional Systems — Patterns

This is a core requirement of the role. Key patterns used at Mastercard's Grid team that directly apply:

| Pattern | GemFire implementation | Accertify equivalent |
|---|---|---|
| In-memory caching | GemFire Region as L2 cache | Redis `@Cacheable` on merchant/rule lookups |
| Near-cache (client-side) | GemFire client region with local caching | Caffeine / Guava local cache in service layer |
| Async write-behind | GemFire AsyncEventQueue → DB | Kafka consumer persisting events to Cassandra |
| Read-through / write-through | GemFire `CacheLoader` / `CacheWriter` | Spring `@Cacheable` + `@CachePut` |
| Distributed locking | GemFire `DistributedLockService` | Redis `SETNX` / Redisson `RLock` |
| Bulk data load | GemFire `putAll()` | Kafka bulk consumer, JDBC batch update |
| Event-driven invalidation | GemFire CQ fires on data change | Redis keyspace notifications, Kafka compacted topic |
| Partitioned compute | GemFire Function Execution on region subset | Kafka partition-based consumer groups |

### Latency / Throughput Trade-offs
- **Sub-millisecond reads**: serve from in-memory store (Redis/GemFire) — never hit DB on the hot path.
- **Write path**: accept async (Kafka → Cassandra) unless the write is part of an ACID transaction.
- **Batch vs. streaming**: batch for ML feature pipelines (S3 → Spark), streaming for real-time fraud scoring (Kafka → rules engine → Redis).
- **Connection pooling**: HikariCP defaults (max 10) will choke under 10k TPS — tune `maximumPoolSize`, `minimumIdle`, and `connectionTimeout` per environment.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 3000
      idle-timeout: 600000
```
