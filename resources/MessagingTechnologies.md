# Messaging Technologies Cheat Sheet - Kafka, MQ, gRPC

## Quick Comparison Table

| Aspect | Kafka | MQ (RabbitMQ, ActiveMQ) | gRPC |
|--------|-------|-------------------------|------|
| **Type** | Event streaming broker | Message broker (queue) | RPC framework |
| **Pattern** | Pub/sub, async | Publish/subscribe, work queues | Synchronous RPC |
| **Latency** | Low (milliseconds) | Very low (microseconds) | Very low (sub-millisecond) |
| **Throughput** | Very high (millions msg/sec) | High (thousands/sec) | High (request/response) |
| **Durability** | Persists to disk | Optional persistence | No persistence (RPC) |
| **Replay** | Can replay events | Messages deleted after consumption | N/A |
| **Coupling** | Loose (async) | Loose (async) | Tight (sync) |
| **Use Case** | Event streaming, log aggregation | Task queues, notifications | Real-time APIs, microservices |

---

## Part 1: KAFKA

### What is Kafka?

Kafka is a **distributed event streaming platform**. Think of it as an infinite log of events that multiple services can read from.

```
Service A publishes event → Kafka topic (log)
                          ↙    ↓    ↘
                    Service B  Service C  Service D
                    (reads independently, at own pace)
```

### Core Concepts

**1. Topic**
```
A Kafka topic is like a channel or feed

Topic: "fraud_detected"
  ├─ Partition 0: [Event1, Event2, Event3, ...]
  ├─ Partition 1: [Event1, Event2, Event3, ...]
  └─ Partition 2: [Event1, Event2, Event3, ...]

Multiple partitions = parallel processing
Each partition is a queue, messages processed in order per partition
```

**2. Partitions**
```
Why partitions?
- Scalability: Each partition can be read by different consumers
- Ordering: Messages in same partition maintain order
- Parallelism: Process multiple partitions simultaneously

Partition assignment:
- Messages with same key go to same partition (consistent)
- Without key, round-robin across partitions

Example: Transactions by user_id
- User 123 transactions always → Partition 0
- User 456 transactions always → Partition 1
- User 789 transactions always → Partition 2

This ensures all one user's transactions processed in order
```

**3. Producer**
```
Service that sends messages to Kafka

Example (pseudocode):
producer = KafkaProducer(bootstrap_servers=['kafka:9092'])

event = {
  'transaction_id': '12345',
  'user_id': '456',
  'amount': 100.00,
  'timestamp': now()
}

producer.send('fraud_detection_events', 
              key='456',  # User ID (goes to same partition)
              value=event)

producer.flush()  # Ensure message is sent
```

**4. Consumer**
```
Service that reads messages from Kafka

Example (pseudocode):
consumer = KafkaConsumer(
  'fraud_detection_events',
  bootstrap_servers=['kafka:9092'],
  group_id='fraud_analytics_group'  # Consumer group
)

for message in consumer:
  process(message.value)
  consumer.commit_offset()  # Mark as consumed
```

**5. Consumer Groups**
```
Multiple consumers reading same topic = load balancing

Topic: fraud_detection_events
  Partitions: [0, 1, 2, 3]

Consumer Group: "fraud_analytics"
  - Consumer 1 reads Partition 0
  - Consumer 2 reads Partition 1
  - Consumer 3 reads Partition 2
  - Consumer 4 reads Partition 3

Message is only read once per consumer group
Different groups can read same message independently

Why:
- Real-time analytics reads all events
- Compliance logging reads all events
- They don't interfere with each other
```

### Kafka in Fraud Detection (MasterCard Context)

```
Transaction submitted
  ↓
Your Service A: Fraud Detection Engine
  ↓
Publishes: "transaction.submitted" event to Kafka
  ├─ Consumer 1: Real-time Rules Engine (gRPC back to transaction processor)
  ├─ Consumer 2: Async Enrichment Service (adds merchant data)
  ├─ Consumer 3: Analytics (logs for reporting)
  ├─ Consumer 4: Machine Learning Pipeline (trains models)
  └─ Consumer 5: Compliance Logging

Each consumer:
- Works independently
- At own pace
- Can be scaled separately
- Can be updated without affecting others
```

### Key Kafka Features

**1. Retention**
```sql
Topic configuration: retention.ms = 7 days

Messages persisted for 7 days
- If Service B is down for 3 days, it catches up on startup
- If Service C wants to restart fresh, it can rewind to day 1
- Great for debugging ("replay events from yesterday")
```

**2. Exactly-Once Semantics**
```
At-least-once: message delivered ≥ 1 time (duplicates possible)
At-most-once: message delivered ≤ 1 time (loss possible)
Exactly-once: message delivered 1 time (guarantee, harder to implement)

For payments: Use exactly-once or at-least-once with idempotent processing
```

**3. Offset Management**
```
Offset = position in partition (like bookmark)

Consumer remembers: "I've read up to message 10,000"
On restart: Resume from 10,001, don't reprocess 1-10,000

Auto-commit: automatically save offset (risky, message lost if crash)
Manual-commit: commit after processing (safer, what you want)
```

### When to Use Kafka

✅ Event streaming, multiple consumers
✅ High throughput (millions of events/sec)
✅ Need event replay or replay capability
✅ Services should be decoupled
✅ Processing can be asynchronous
✅ Long-term event history needed

❌ Real-time request/response needed
❌ Services tightly coupled
❌ Low latency critical (though Kafka is pretty fast)
❌ Complex routing rules

---

## Part 2: MESSAGE QUEUES (MQ) - RabbitMQ, ActiveMQ

### What is a Message Queue?

A message broker that delivers messages point-to-point or publish/subscribe.

```
Simpler than Kafka, more flexible routing
Less durable, focused on delivery guarantees
```

### Core Concepts

**1. Queue**
```
Named queue that holds messages

Producer sends to queue
Consumer(s) pull from queue
Message delivered once, then removed

Example:
Queue: "email_notifications"
  Producer 1: User service sends "send welcome email"
  Producer 2: Fraud service sends "send fraud alert"
  
  Consumer: Email service reads and sends email
```

**2. Exchange (RabbitMQ)**
```
Routing mechanism

Types:
- Direct: Route to specific queue based on routing key
- Fanout: Broadcast to all queues
- Topic: Pattern-based routing (like pubsub with wildcards)
- Headers: Route based on message headers
```

**3. Publish/Subscribe Pattern**
```
Topic: "transaction.completed"
  Subscribers:
  - Analytics service
  - Compliance service
  - Reporting service

All receive same message (unlike queue where one gets it)
```

**4. Work Queue Pattern**
```
Queue: "fraud_review_tasks"
  
  Producer: Fraud engine publishes "review_transaction_123"
  
  Multiple workers:
  - Worker 1 processes task (if fast)
  - Worker 2 processes task (if Worker 1 slow)
  - Worker 3 processes task
  
  Load balancing across workers
```

### RabbitMQ Example

```python
# Producer
connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()

# Create queue
channel.queue_declare(queue='fraud_review_tasks', durable=True)

# Send message
channel.basic_publish(
  exchange='',
  routing_key='fraud_review_tasks',
  body='Review transaction 12345',
  properties=pika.BasicProperties(delivery_mode=2)  # Persistent
)

connection.close()

# Consumer
connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()

channel.queue_declare(queue='fraud_review_tasks', durable=True)

def callback(ch, method, properties, body):
  print(f"Processing: {body}")
  # Do work
  ch.basic_ack(delivery_tag=method.delivery_tag)

channel.basic_consume(queue='fraud_review_tasks', on_message_callback=callback)
channel.start_consuming()
```

### When to Use Message Queues

✅ Task queues (background jobs)
✅ Notifications (emails, SMS)
✅ Load balancing across workers
✅ Complex routing rules
✅ Need acknowledgment/retry

❌ High volume streaming
❌ Event replay needed
❌ Loose coupling preferred (use Kafka)
❌ Real-time RPC (use gRPC)

---

## Part 3: gRPC

### What is gRPC?

A **high-performance RPC (Remote Procedure Call) framework** built on HTTP/2.

```
Client calls: service.CheckFraudScore(transaction)
Server responds: FraudScoreResponse { risk: 0.85 }
Client waits for response
```

### Core Concepts

**1. Protocol Buffers (protobuf)**
```
Language-neutral, efficient serialization format

Define service:
syntax = "proto3";

message Transaction {
  string transaction_id = 1;
  string user_id = 2;
  double amount = 3;
  int64 timestamp = 4;
}

message FraudScoreResponse {
  double risk_score = 1;
  bool is_suspicious = 2;
  string reason = 3;
}

service FraudDetectionService {
  rpc CheckFraudScore(Transaction) returns (FraudScoreResponse);
}
```

**2. Service Definition**
```protobuf
// Four types of RPC:

// 1. Unary (simple request/response)
rpc CheckFraudScore(Transaction) returns (FraudScoreResponse);

// 2. Server streaming (client request, server sends multiple)
rpc GetTransactionHistory(UserId) returns (stream Transaction);

// 3. Client streaming (client sends multiple, server responds once)
rpc SubmitBatchTransactions(stream Transaction) returns (Response);

// 4. Bidirectional streaming (both send multiple)
rpc ChatWithFraudAnalyst(stream Message) returns (stream Message);
```

**3. Implementation**

```python
# Server
from concurrent import futures
import grpc
import fraud_pb2
import fraud_pb2_grpc

class FraudDetectionService(fraud_pb2_grpc.FraudDetectionServiceServicer):
  def CheckFraudScore(self, request, context):
    # request is Transaction protobuf
    risk_score = self.calculate_risk(request)
    return fraud_pb2.FraudScoreResponse(
      risk_score=risk_score,
      is_suspicious=risk_score > 0.7
    )

server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
fraud_pb2_grpc.add_FraudDetectionServiceServicer_to_server(
  FraudDetectionService(), server
)
server.add_insecure_port('[::]:50051')
server.start()

# Client
stub = fraud_pb2_grpc.FraudDetectionServiceStub(channel)
transaction = fraud_pb2.Transaction(
  transaction_id='123',
  amount=100.00
)
response = stub.CheckFraudScore(transaction)
print(response.risk_score)
```

**4. HTTP/2 Advantages**
```
- Multiplexing: Multiple requests on same connection
- Server push: Server can send data without request
- Binary framing: More efficient than JSON
- Header compression: Reduces overhead

Result: Lower latency, higher throughput than REST
```

### gRPC vs REST

```
REST (HTTP/1.1):
GET /api/v1/fraud/check?transactionId=123
Response: JSON { risk_score: 0.85 }

gRPC (HTTP/2):
CheckFraudScore(Transaction)
Response: FraudScoreResponse { risk_score: 0.85 }

Advantages of gRPC:
- Faster (binary vs text)
- Typed (protobuf contract)
- Bidirectional streaming
- Better for microservices
```

### When to Use gRPC

✅ Service-to-service communication
✅ Need low latency
✅ High throughput RPC
✅ Strongly typed APIs (protobuf)
✅ Bidirectional communication needed
✅ Internal APIs (microservices)

❌ Public APIs (REST better for discovery)
❌ Browser clients (use REST + JSON)
❌ Loose coupling needed (use Kafka)
❌ Simple request/response not critical

---

## Part 4: How They Work Together (Architecture)

### Fraud Detection System at MasterCard

```
Transaction submitted
  ↓
Transaction Service (REST API)
  ├─ Calls gRPC: Rules Service (synchronous, real-time)
  │   └─ CheckFraudScore() → Immediate risk score
  │
  ├─ Publishes to Kafka: "transaction.scored"
  │   ├─ Consumer 1: Analytics (async, eventual)
  │   ├─ Consumer 2: Enrichment (async, eventual)
  │   └─ Consumer 3: Compliance logging (async, eventual)
  │
  └─ Returns to client: "Transaction processed"
```

**Why this architecture?**

```
gRPC for Rules Service:
- Must have answer NOW (decides approve/decline)
- Synchronous, tight coupling acceptable
- Low latency critical
- Circuit breaker protects if it fails

Kafka for downstream:
- Analytics, enrichment, logging don't need immediate answer
- Asynchronous, loose coupling important
- Can be down without blocking transaction
- Multiple consumers for different purposes
```

---

## Part 4: Quick Reference Commands

### Kafka
```bash
# Start consuming from topic
kafka-console-consumer --bootstrap-server localhost:9092 --topic fraud_events

# Produce message
kafka-console-producer --broker-list localhost:9092 --topic fraud_events

# View consumer group info
kafka-consumer-groups --bootstrap-server localhost:9092 --group fraud_analytics --describe

# View topic details
kafka-topics --bootstrap-server localhost:9092 --topic fraud_events --describe
```

### gRPC
```bash
# Generate code from proto
protoc --python_out=. --grpc_python_out=. fraud.proto

# Test gRPC service
grpcurl -plaintext localhost:50051 list fraud.FraudDetectionService
```