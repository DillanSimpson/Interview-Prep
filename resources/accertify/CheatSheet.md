# Java Core Cheat Sheet

---

## Data Structures — Quick Reference

| Data Structure | Access | Search | Insert | Delete | Space | Best Use Case |
|---|---|---|---|---|---|---|
| **Array** | O(1) | O(n) | O(n) | O(n) | O(n) | Fixed-size, index-based lookup |
| **ArrayList** | O(1) | O(n) | O(1) amort. | O(n) | O(n) | Default ordered list, random access |
| **LinkedList** | O(n) | O(n) | O(1) at ends | O(1) at ends | O(n) | Frequent insert/delete at both ends (use as Deque) |
| **ArrayDeque** | O(1) ends | O(n) | O(1) amort. | O(1) | O(n) | Stack or queue; faster than `Stack`/`LinkedList` |
| **HashMap** | O(1) avg | O(1) avg | O(1) avg | O(1) avg | O(n) | Key→value lookup, membership testing |
| **LinkedHashMap** | O(1) avg | O(1) avg | O(1) avg | O(1) avg | O(n) | Key→value with insertion/access order (LRU cache) |
| **TreeMap** | O(log n) | O(log n) | O(log n) | O(log n) | O(n) | Sorted key→value, range queries (`floorKey`, `subMap`) |
| **HashSet** | — | O(1) avg | O(1) avg | O(1) avg | O(n) | Uniqueness check, fast membership test |
| **LinkedHashSet** | — | O(1) avg | O(1) avg | O(1) avg | O(n) | Unique elements with insertion-order iteration |
| **TreeSet** | — | O(log n) | O(log n) | O(log n) | O(n) | Sorted unique elements, range operations |
| **PriorityQueue** | O(1) peek | O(n) | O(log n) | O(log n) | O(n) | Min/max element retrieval (scheduling, Dijkstra) |
| **Stack** | O(1) top | O(n) | O(1) | O(1) | O(n) | LIFO — use `ArrayDeque` instead |
| **BST (balanced)** | O(log n) | O(log n) | O(log n) | O(log n) | O(n) | Sorted data with frequent insert/delete |
| **Graph (adj list)** | O(V+E) | O(V+E) | O(1) | O(E) | O(V+E) | Sparse graphs, BFS/DFS traversal |
| **Graph (adj matrix)** | O(1) | O(1) | O(1) | O(1) | O(V²) | Dense graphs, fast edge existence check |
| **Trie** | O(k) | O(k) | O(k) | O(k) | O(n·k) | Prefix search, autocomplete, dictionary words |

> k = key/word length, V = vertices, E = edges

### Choosing a Data Structure
| Need | Reach for |
|---|---|
| Ordered list, random access | `ArrayList` |
| Fast queue / stack | `ArrayDeque` |
| Key→value, O(1) ops | `HashMap` |
| Key→value, sorted order | `TreeMap` |
| Unique elements, O(1) lookup | `HashSet` |
| Unique elements, sorted | `TreeSet` |
| Repeatedly get min or max | `PriorityQueue` |
| Insertion-order map (or LRU) | `LinkedHashMap` |
| Prefix matching / autocomplete | `Trie` |
| Shortest path (unweighted) | Graph + BFS |
| Shortest path (weighted) | Graph + Dijkstra + `PriorityQueue` |

---

## Advanced Data Structures

### ArrayList vs LinkedList
| | ArrayList | LinkedList |
|---|---|---|
| Backed by | Dynamic array | Doubly-linked nodes |
| Random access `get(i)` | O(1) | O(n) |
| Insert/delete at end | O(1) amortized | O(1) |
| Insert/delete at middle | O(n) (shift) | O(n) to find, O(1) to splice |
| Memory | Compact (contiguous) | Extra pointers per node |

Use `ArrayList` by default. Use `LinkedList` only as a `Deque` (queue/stack operations at both ends).

---

### HashMap Internals
- **Bucket array** (`Node<K,V>[] table`), default capacity **16**, load factor **0.75**.
- Index: `hash & (capacity - 1)` — capacity is always a power of 2.
- Collision resolution: **linked list** per bucket → converted to **red-black tree** when bucket size > **8** (Java 8+).
- **Resize**: when `size > capacity * 0.75`, table doubles and all entries rehash — O(n), amortized O(1).
- Allows **one null key** (bucket 0). Not thread-safe.
- **Always override `hashCode` when you override `equals`** — equal objects must have equal hash codes.

```
HashMap<String, Integer> map = new HashMap<>();
map.put("key", 1);
map.getOrDefault("missing", 0);
map.computeIfAbsent("k", k -> new ArrayList<>());
map.merge("k", 1, Integer::sum);
```

### LinkedHashMap
- Extends `HashMap`, adds a doubly-linked list through all entries in **insertion order** (or access order if constructed with `accessOrder=true`).
- O(1) operations like HashMap.
- Classic LRU cache: extend `LinkedHashMap` and override `removeEldestEntry`.

```java
Map<K,V> lru = new LinkedHashMap<>(capacity, 0.75f, true) {
    protected boolean removeEldestEntry(Map.Entry<K,V> e) {
        return size() > capacity;
    }
};
```

### TreeMap / TreeSet
- Backed by a **Red-Black Tree** (self-balancing BST).
- O(log n) get/put/remove.
- Keys always **sorted** (natural order or custom `Comparator`).
- Unique methods: `floorKey`, `ceilingKey`, `firstKey`, `lastKey`, `subMap`, `headMap`, `tailMap`.

```java
TreeMap<Integer, String> tm = new TreeMap<>();
tm.floorKey(5);    // largest key <= 5
tm.ceilingKey(5);  // smallest key >= 5
```

### PriorityQueue (Heap)
- Min-heap by default (smallest element at head).
- O(log n) offer/poll, O(1) peek.
- Max-heap: `new PriorityQueue<>(Collections.reverseOrder())`.

```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
minHeap.offer(3); minHeap.poll(); // removes smallest
```

### ArrayDeque
- Double-ended queue backed by a resizable array. No capacity restriction, no null elements.
- Faster than `Stack` for stack operations; faster than `LinkedList` for queue operations.

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1); stack.pop();

Deque<Integer> queue = new ArrayDeque<>();
queue.offer(1); queue.poll();
```

### Stack vs Queue vs Deque
| ADT | LIFO/FIFO | Preferred class |
|---|---|---|
| Stack | LIFO | `ArrayDeque` (not `Stack`) |
| Queue | FIFO | `ArrayDeque` or `LinkedList` |
| Deque | Both ends | `ArrayDeque` |

### Graph Representations
```java
// Adjacency list (sparse graphs — most real-world)
Map<Integer, List<Integer>> graph = new HashMap<>();

// Adjacency matrix (dense graphs, quick edge lookup)
boolean[][] adj = new boolean[n][n];
```

**BFS** — shortest path (unweighted), level-order traversal.
```java
Queue<Integer> q = new ArrayDeque<>();
Set<Integer> visited = new HashSet<>();
q.offer(start); visited.add(start);
while (!q.isEmpty()) {
    int node = q.poll();
    for (int neighbor : graph.get(node)) {
        if (visited.add(neighbor)) q.offer(neighbor);
    }
}
```

**DFS** — cycle detection, topological sort, connected components.
```java
void dfs(int node, Set<Integer> visited) {
    visited.add(node);
    for (int neighbor : graph.get(node)) {
        if (!visited.contains(neighbor)) dfs(neighbor, visited);
    }
}
```

---

## Concurrency & Multithreading

### Thread Basics
```java
// Runnable via lambda
Thread t = new Thread(() -> System.out.println("running"));
t.start();

// ExecutorService (preferred)
ExecutorService pool = Executors.newFixedThreadPool(4);
pool.submit(() -> doWork());
pool.shutdown(); // initiates graceful shutdown
```

### synchronized
Only one thread holds the monitor at a time. Can lock on `this`, a class, or any object.
```java
synchronized (this) { count++; }                  // instance lock
synchronized (MyClass.class) { sharedCount++; }   // class-level lock
```

### volatile
Guarantees visibility — reads/writes go directly to main memory, not a CPU cache. Does **not** make compound operations atomic (read-modify-write still needs synchronization).
```java
private volatile boolean running = true;
```

### Atomic Classes (`java.util.concurrent.atomic`)
Lock-free, hardware CAS (compare-and-swap) operations.
```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();
counter.compareAndSet(expected, update);
```

### ReentrantLock
More flexible than `synchronized` — supports `tryLock`, timed lock, interruptible lock, and fairness.
```java
ReentrantLock lock = new ReentrantLock();
lock.lock();
try { /* critical section */ }
finally { lock.unlock(); } // always unlock in finally
```

### java.util.concurrent Toolkit
| Class | Purpose |
|---|---|
| `ExecutorService` | Thread pool lifecycle |
| `ScheduledExecutorService` | Delayed / periodic tasks |
| `Future<T>` | Handle to an async result; `get()` blocks |
| `CompletableFuture<T>` | Async pipelines with callbacks |
| `CountDownLatch` | Wait for N events (one-shot) |
| `CyclicBarrier` | Repeatedly sync N threads at a point |
| `Semaphore` | Limit concurrent access to a resource |
| `ConcurrentHashMap` | Thread-safe map (segment/stripe locking) |
| `BlockingQueue` | Producer-consumer handoff |
| `ReadWriteLock` | Multiple concurrent readers OR one writer |

### CompletableFuture
```java
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(data -> transform(data))
    .thenAccept(result -> save(result))
    .exceptionally(ex -> { log(ex); return null; });
```

### Common Concurrency Problems
| Problem | Cause | Fix |
|---|---|---|
| Race condition | Unsynchronized shared state | `synchronized`, `Atomic*`, `Lock` |
| Deadlock | Circular lock acquisition | Consistent lock ordering, `tryLock` |
| Livelock | Threads keep yielding to each other | Add randomness or backoff |
| Starvation | Low-priority thread never runs | Fair locks (`new ReentrantLock(true)`) |

### Thread Pool Sizing Rules of Thumb
- **CPU-bound tasks**: `# of CPU cores`
- **I/O-bound tasks**: `# cores × (1 + avg wait time / avg compute time)`

### Java Memory Model
- **Happens-before**: a write in thread A _happens-before_ a read in thread B if they share a sync action (lock release, `volatile` write, thread start/join).
- Without happens-before, the JVM/CPU may reorder instructions and cache reads — always synchronize shared mutable state.

---

## Streams API

### Stream Pipeline
```
source → intermediate operations (lazy) → terminal operation (triggers execution)
```

```java
List<String> result = list.stream()          // source
    .filter(s -> s.startsWith("A"))          // intermediate
    .map(String::toUpperCase)                // intermediate
    .sorted()                                // intermediate
    .collect(Collectors.toList());           // terminal
```

### Creating Streams
```java
Stream.of("a", "b", "c")
Arrays.stream(arr)
list.stream()
IntStream.range(0, 10)           // 0..9
IntStream.rangeClosed(1, 10)     // 1..10
Stream.iterate(0, n -> n + 2)   // infinite: 0, 2, 4, ...
Stream.generate(Math::random)   // infinite random
```

### Key Intermediate Operations (lazy)
| Method | Description |
|---|---|
| `filter(Predicate)` | Keep elements matching predicate |
| `map(Function)` | Transform each element |
| `flatMap(Function)` | Flatten stream of streams into one stream |
| `distinct()` | Remove duplicates (uses `equals`) |
| `sorted()` / `sorted(Comparator)` | Sort elements |
| `peek(Consumer)` | Debug side-effect without consuming |
| `limit(n)` | Keep first n elements |
| `skip(n)` | Skip first n elements |

### Key Terminal Operations (trigger execution)
| Method | Returns | Description |
|---|---|---|
| `collect(Collector)` | varies | Accumulate into collection/map/string |
| `forEach(Consumer)` | void | Side-effect on each element |
| `count()` | long | Number of elements |
| `findFirst()` | `Optional<T>` | First element |
| `findAny()` | `Optional<T>` | Any element (faster for parallel) |
| `anyMatch(Predicate)` | boolean | Short-circuits on first true |
| `allMatch(Predicate)` | boolean | Short-circuits on first false |
| `noneMatch(Predicate)` | boolean | Short-circuits on first true |
| `min(Comparator)` | `Optional<T>` | Minimum element |
| `max(Comparator)` | `Optional<T>` | Maximum element |
| `reduce(identity, BinaryOp)` | T | Fold elements into one value |
| `toArray()` | `Object[]` | Collect into array |

### Collectors
```java
// to collections
Collectors.toList()
Collectors.toSet()
Collectors.toUnmodifiableList()

// grouping
Map<Dept, List<Employee>> byDept =
    employees.stream().collect(Collectors.groupingBy(Employee::getDept));

// counting per group
Map<Dept, Long> countByDept =
    employees.stream().collect(Collectors.groupingBy(Employee::getDept, Collectors.counting()));

// joining strings
String csv = stream.collect(Collectors.joining(", ", "[", "]"));

// partitioning (two groups: true/false)
Map<Boolean, List<Integer>> parts =
    numbers.stream().collect(Collectors.partitioningBy(n -> n % 2 == 0));

// to map
Map<Integer, String> idToName =
    list.stream().collect(Collectors.toMap(Person::getId, Person::getName));
```

### flatMap vs map
```java
// map: List<List<String>> → Stream<List<String>>
list.stream().map(l -> l.stream())

// flatMap: List<List<String>> → Stream<String>  (flattened)
list.stream().flatMap(Collection::stream)
```

### Optional
```java
Optional<String> opt = Optional.of("hello");
opt.isPresent();
opt.get();                          // throws if empty
opt.orElse("default");
opt.orElseGet(() -> compute());
opt.orElseThrow(IllegalStateException::new);
opt.map(String::toUpperCase);
opt.filter(s -> s.length() > 3);
opt.ifPresent(System.out::println);
```
Never use `Optional` as a field type or method parameter — it is designed for return values only.

### Parallel Streams
```java
list.parallelStream().filter(...).map(...).collect(toList());
// or
stream.parallel()
```
- Uses the common ForkJoinPool (default: `# cores - 1` threads).
- Worth it only for CPU-bound, large datasets, and stateless operations.
- Avoid with I/O, ordered operations, or shared mutable state.

### Primitive Streams (avoid boxing overhead)
```java
IntStream, LongStream, DoubleStream
intStream.sum(); intStream.average(); intStream.summaryStatistics();
// Box to object stream
intStream.boxed()  // → Stream<Integer>
// Unbox
stream.mapToInt(Integer::intValue)
```

---

## Functional Interfaces (java.util.function)
| Interface | Method | Use |
|---|---|---|
| `Predicate<T>` | `test(T) → boolean` | Filter |
| `Function<T,R>` | `apply(T) → R` | Map/transform |
| `BiFunction<T,U,R>` | `apply(T,U) → R` | Two-arg transform |
| `Consumer<T>` | `accept(T) → void` | Side-effects |
| `Supplier<T>` | `get() → T` | Lazy value production |
| `UnaryOperator<T>` | `apply(T) → T` | Transform same type |
| `BinaryOperator<T>` | `apply(T,T) → T` | Combine same type |

Compose with `andThen`, `compose` (Function), `and`/`or`/`negate` (Predicate).

---

## OOP Pillars

### Encapsulation
Hide internal state behind a public API. Fields are `private`; access is controlled through getters/setters (or no setters for immutability).

```java
public class BankAccount {
    private double balance;                      // hidden
    public double getBalance() { return balance; }
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException();
        balance += amount;
    }
}
```

**Why it matters**: you can change the internal representation without breaking callers. You enforce invariants (e.g. balance can't go negative) in one place.

**Immutability** is the strongest form of encapsulation — make fields `final`, return defensive copies of mutable fields, and provide no setters. `String`, `Integer`, `LocalDate` are all immutable. Immutable objects are inherently thread-safe.

---

### Interface vs Abstract Class

| | Interface | Abstract Class |
|---|---|---|
| Instantiate directly? | No | No |
| Multiple inheritance | Yes (a class implements many) | No (single `extends`) |
| Fields | `public static final` constants only | Any access modifier, instance fields |
| Constructors | None | Yes |
| Default methods | Yes (since Java 8) | Yes |
| When to use | Define a **capability/contract** across unrelated types | Share **common implementation** across related types |

```java
// Interface — defines what something CAN DO
interface Payable {
    void pay(double amount);
    default String currency() { return "USD"; }  // default method
}

// Abstract class — defines what something IS, with partial implementation
abstract class Shape {
    private String color;
    Shape(String color) { this.color = color; }
    abstract double area();                       // subclass must implement
    void describe() { System.out.println(color + " shape, area=" + area()); }
}

class Circle extends Shape implements Payable {
    private double radius;
    Circle(double r) { super("red"); this.radius = r; }
    public double area() { return Math.PI * radius * radius; }
    public void pay(double amount) { /* ... */ }
}
```

**Rule of thumb**: prefer interfaces — they allow multiple inheritance and keep types decoupled. Use abstract classes when you need shared state or a common constructor.

#### Functional Interfaces & Lambdas (interface with one abstract method)
```java
@FunctionalInterface
interface Transformer<T> {
    T transform(T input);
}
Transformer<String> upper = s -> s.toUpperCase();
```

---

### Polymorphism

**Runtime polymorphism (overriding)** — the JVM dispatches to the most-derived implementation at runtime via the **vtable**. The reference type is the interface/superclass; the object type determines the method called.

```java
Shape s = new Circle(5.0);
s.area();          // calls Circle.area(), not Shape.area() — dynamic dispatch
```

**Compile-time polymorphism (overloading)** — same method name, different parameter types. Resolved by the compiler based on static types.

```java
int add(int a, int b) { return a + b; }
double add(double a, double b) { return a + b; }
```

**Covariant return types** — an overriding method may return a subtype of the parent's return type (valid since Java 5).

**`instanceof` and pattern matching (Java 16+)**
```java
if (shape instanceof Circle c) {
    System.out.println(c.radius);   // c is already cast
}
```

**Key rules for overriding (`@Override`)**
- Same method signature.
- Return type must be the same or a covariant subtype.
- Cannot throw broader checked exceptions than the parent.
- Cannot reduce visibility (e.g. `public` → `private` is illegal).

---

## Generics — Advanced

### Wildcards & PECS
```java
// Upper-bounded wildcard — read from it (Producer Extends)
void printAll(List<? extends Number> list) {
    for (Number n : list) System.out.println(n);  // safe to read as Number
}

// Lower-bounded wildcard — write to it (Consumer Super)
void addNumbers(List<? super Integer> list) {
    list.add(42);   // safe to add Integer (or subtypes)
}

// PECS in action: copy src into dst
<T> void copy(List<? extends T> src, List<? super T> dst) {
    src.forEach(dst::add);
}
```

**Why unbounded `List<?>` exists**: use it when you only call methods that don't depend on the type parameter (`size()`, `clear()`, `equals()`).

### Multiple Bounds
```java
// T must be both Comparable and Serializable
<T extends Comparable<T> & Serializable> T findMax(List<T> list) { ... }
// Class bound must come first if mixed with interface bounds
<T extends AbstractBase & Runnable & Serializable> void process(T t) { ... }
```

### Generic Methods vs Generic Classes
```java
// Generic class — type fixed at instantiation
class Box<T> {
    private T value;
    Box(T value) { this.value = value; }
    T get() { return value; }
}

// Generic method — type inferred per call-site
class Utils {
    static <T> List<T> listOf(T... items) { return Arrays.asList(items); }
}
List<String> ss = Utils.listOf("a", "b");  // T inferred as String
```

### Type Erasure & Heap Pollution
Generics exist only at compile time. At runtime `List<String>` and `List<Integer>` are both just `List`. Consequences:
- Cannot do `new T()` or `new T[]` — the JVM doesn't know what T is.
- Cannot do `instanceof List<String>` — only `instanceof List<?>`.
- `@SuppressWarnings("unchecked")` is a signal that you're bypassing the type system — use sparingly and document why.

```java
// Common pattern: pass Class<T> to work around erasure
<T> T deserialize(String json, Class<T> type) {
    return objectMapper.readValue(json, type);
}
```

### Bounded Type Parameters in Practice
```java
// Restrict to Comparable so we can call compareTo
<T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
}

// Restrict to Number so we can call .doubleValue()
<T extends Number> double sum(List<T> list) {
    return list.stream().mapToDouble(Number::doubleValue).sum();
}
```

### Invariance, Covariance, Contravariance
| | Java mechanism | Read | Write |
|---|---|---|---|
| Invariant | `List<T>` | T | T |
| Covariant (producer) | `List<? extends T>` | T | ✗ |
| Contravariant (consumer) | `List<? super T>` | Object | T |

`List<Dog>` is **not** a subtype of `List<Animal>` even though `Dog extends Animal` — this is invariance. Arrays are covariant (`Dog[]` IS-A `Animal[]`) and that's why arrays can cause `ArrayStoreException` at runtime; generics avoid this.

---

## equals / hashCode Contract
1. `a.equals(a)` — reflexive.
2. `a.equals(b) == b.equals(a)` — symmetric.
3. Transitive.
4. `a.equals(b)` → `a.hashCode() == b.hashCode()` — **mandatory**.
5. `hashCode` must be consistent across calls.

Break rule 4 and `HashMap`/`HashSet` silently stops working.

---

## Comparable vs Comparator
```java
// Comparable — natural ordering (implement in the class)
class Person implements Comparable<Person> {
    public int compareTo(Person o) { return this.age - o.age; }
}

// Comparator — external/multiple orderings
Comparator<Person> byName = Comparator.comparing(Person::getName);
Comparator<Person> byAgeThenName = Comparator.comparingInt(Person::getAge)
                                              .thenComparing(Person::getName);
list.sort(byAgeThenName);
```