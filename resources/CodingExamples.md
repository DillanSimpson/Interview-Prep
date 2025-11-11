# **Potential Coding Questions**

## 1) Java Stream Groups Employees age ranges

```java
import java.util.*;
import java.util.stream.*;

class Employee {
  private String name;
  private int age;

  public Employee(String name, int age) {
    this.name = name;
    this.age = age;
  }

  public String getName() { return name; }
  public int getAge() { return age; }

  @Override
  public String toString() {
    return "(" + name + ", " + age + ")";
  }
}

  public static void main(String[] args) {
      List<Employee> employees = Arrays.asList(
      new Employee("Ankit", 32),
      new Employee("Mayank", 40),
      new Employee("Rahul", 32),
      new Employee("Niky", 24));

        // Generic function to group by range (you can change range size)
      int rangeSize = 10;
      Map<String, List<Employee>> grouped = employees.stream()
              .collect(Collectors.groupingBy(e -> getRangeKey(e.getAge(), rangeSize)));

      grouped.forEach((k, v) -> System.out.println(k + " -> " + v));
    }

    // Generic helper: computes key like "20-30", "30-40", etc.
    private static String getRangeKey(int age, int rangeSize) {
        int lower = (age / rangeSize) * rangeSize;
        int upper = lower + rangeSize;
        return lower + "-" + upper;
    }
```

---

## 2) Process Transactions

```text
[deposit, i, amount]
[withdraw, i, amount]
[transfer, i, j, amount]
```

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

## 3) **Concurrent workflow**

1. API 1 is called **asynchronously**,
2. API 2 is called **asynchronously**,
3. API 3 is called **synchronously**,
4. and finally you **wait for API 1 and 2** to complete before processing everything together.

We can do this *cleanly* using **`CompletableFuture`** (no frameworks, just JDK).

```java
import java.util.concurrent.*;
public class AsyncWorkflowExample {
    public void mainProcessing() {
        // Start API 1 and API 2 asynchronously
        CompletableFuture<String> api1Future = CompletableFuture.supplyAsync(() -> callApi1());
        CompletableFuture<String> api2Future = CompletableFuture.supplyAsync(() -> callApi2());
        // Call API 3 synchronously (blocking)
        String api3Response = callApi3();
        // Wait for API 1 and API 2 to complete
        CompletableFuture.allOf(api1Future, api2Future).join();
        try {
            String api1Response = api1Future.get();
            String api2Response = api2Future.get();
            // Now combine results
            processResults(api1Response, api2Response, api3Response);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private String callApi1() {
        simulateDelay(2000);
        return "Result from API 1";
    }
    private String callApi2() {
        simulateDelay(3000);
        return "Result from API 2";
    }
    private String callApi3() {
        simulateDelay(1000);
        return "Result from API 3";
    }
    private void processResults(String api1, String api2, String api3) {
        System.out.println("Processing combined results:");
        System.out.println(api1 + " | " + api2 + " | " + api3);
    }
    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### What’s happening

* **`CompletableFuture.supplyAsync()`** runs API 1 and 2 calls in parallel, using the **common ForkJoinPool**.
* **API 3** is called **synchronously** (blocks the main thread).
* **`CompletableFuture.allOf(...).join()`** waits for both async tasks to complete.
* Finally, `.get()` retrieves their results (you could also use `.thenCombine()` if you want non-blocking composition).

### Key ideas

* This pattern scales well with IO-bound tasks (like HTTP calls).
* If you need *custom thread pools*, you can pass an `ExecutorService` to `supplyAsync()`.
* Avoid mixing blocking I/O inside async methods in production—use non-blocking HTTP clients if possible.

---

## 3) Find max from array using stream

> ### *Example primitive array*

```java
import java.util.Arrays;
public class MaxFromArray {
    public static void main(String[] args) {
        int[] numbers = {5, 9, 1, 12, 7};

        int max = Arrays.stream(numbers)
                .max()
                .orElseThrow(() -> new RuntimeException("Array is empty"));
    }
}
```

`Arrays.stream(numbers)` creates an `IntStream`, and `.max()` finds the largest element, returning an `OptionalInt`. Using `.orElseThrow()` ensures you handle the empty case safely.

> ### *Example with custom objects*

If you’ve got an array of objects (e.g., `Employee[]`), you can use a **comparator**:

```java
import java.util.*;
import java.util.stream.*;
class Employee {
    String name;
    int age;
    Employee(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
public class MaxEmployee {
    public static void main(String[] args) {
        Employee[] employees = {
            new Employee("Ankit", 32),
            new Employee("Mayank", 40),
            new Employee("Rahul", 35)
        };
        Employee oldest = Arrays.stream(employees)
                                .max(Comparator.comparingInt(e -> e.age))
                                .orElseThrow();
    }
}
```



---

## 4) Find second largest number in array

Suppose your array were `{23, 55, 67, 76, 76, 98, 98}` — without handling duplicates, the “second largest” could incorrectly come out as 98 again.

Here’s a **duplicate-safe** version:

```java
public class SecondLargest {
    public static void main(String[] args) {
        int[] arr = {23, 55, 67, 45, 76, 14, 52, 98, 29, 59, 40, 36, 98};

        Integer first = null;
        Integer second = null;

        for (int num : arr) {
            if (first == null || num > first) {
                second = first;
                first = num;
            } else if (num != first && (second == null || num > second)) {
                second = num;
            }
        }

        if (second != null) {
            System.out.println("Second largest: " + second);
        } else {
            System.out.println("No second largest value (all elements equal)");
        }
    }
}
```

```java
int secondLargest = Arrays.stream(arr).distinct()
                          .boxed()
                          .sorted(Comparator.reverseOrder())
                          .skip(1)
                          .findFirst()
                          .orElseThrow(() -> new IllegalArgumentException("No second largest value"));
```

That uses Java Streams to deduplicate and grab the second element in descending order.

Both are efficient; the stream approach is clearer but does a full sort (`O(n log n)`), while the loop is faster (`O(n)`).


---

## 5) Find numbers from array which are starting with '1' using stream

Convert numbers to **strings** first, then filter by whether they start with `'1'`.

> ### *Example with integers*

```java
import java.util.*;
import java.util.stream.*;
public class NumbersStartingWith1 {
    public static void main(String[] args) {
        int[] numbers = {10, 12, 13, 22, 31, 145, 7, 19};

        List<Integer> result = Arrays.stream(numbers)
            .boxed() // convert int -> Integer (for easy string handling)
            .filter(n -> String.valueOf(n).startsWith("1"))
            .collect(Collectors.toList());
    }
}
```

> ### *Example with primitives*

```java
int[] result = Arrays.stream(numbers)
    .filter(n -> String.valueOf(n).startsWith("1"))
    .toArray();
```

## 6.a) Shift and modify array values

Here’s a clean, in-place **O(n) / O(1)** solution. It clamps, compacts non-zeros left, then fills the tail with `-1`—all in a single pass plus a tail fill.

```java
public class TransactionScoreNormalizer {
    /**
     * Normalize transaction scores in place.
     * Steps:
     *  1) Clamp: <0 -> 0, >100 -> 100
     *  2) Remove zeros by shifting non-zero values left
     *  3) Fill remaining tail with -1
     *
     * Time:  O(n)
     * Space: O(1)
     */
    public static void normalize(int[] scores) {
        if (scores == null || scores.length == 0) return;

        int write = 0; // next position to write a kept (non-zero) value

        // Single pass: clamp and compact non-zeros
        for (int i = 0; i < scores.length; i++) {
            int v = scores[i];
            if (v < 0) {
                v = 0;
            } else if (v > 100) {
                v = 100;
            }

            if (v != 0) {
                scores[write++] = v;
            }
        }

        // Fill the remainder with -1
        while (write < scores.length) {
            scores[write++] = -1;
        }
    }
}
```

This matches the example:

* Input:  `[120, -5, 90, 0, 45, 110, 0, -1]`
* Output: `[100, 90, 45, 100, -1, -1, -1, -1]`

Notes:

* No auxiliary arrays, no streams, no extra allocations.
* Thread-safe as long as each thread works on its own array instance (no shared mutable state).

---

## 6.b) In-place dedup for a sorted array

```java
public class TokenDedupInPlace {

    /**
     * In-place dedup for a sorted array.
     * Keep first occurrence of each value, shift left, fill tail with -1.
     * Time: O(n), Space: O(1)
     */
    public static void dedup(int[] tokens) {
        if (tokens == null || tokens.length == 0) return;

        int write = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (i == 0 || tokens[i] != tokens[i - 1]) {
                tokens[write++] = tokens[i];
            }
        }
        while (write < tokens.length) {
            tokens[write++] = -1;
        }
    }
}
```

---

## 6.c) Reorder risk levels in-place

```java
public class RiskPartitionInPlace {
    /**
     * Reorder risk levels in-place so: all 1s, then 0s, then 2s.
     * Dutch National Flag with custom rank: rank(1)=0, rank(0)=1, rank(2)=2.
     * Time: O(n), Space: O(1)
     */
    public static void partition(int[] risk) {
        if (risk == null || risk.length <= 1) return;

        int low = 0, mid = 0, high = risk.length - 1;

        while (mid <= high) {
            int r = rank(risk[mid]);
            if (r == 0) {               // target group "1"
                swap(risk, low, mid);
                low++; mid++;
            } else if (r == 1) {        // target group "0"
                mid++;
            } else {                    // r == 2, target group "2"
                swap(risk, mid, high);
                high--;
            }
        }
    }

    // Map values to desired order blocks: [1s][0s][2s]
    private static int rank(int v) {
        if (v == 1) return 0;
        if (v == 0) return 1;
        if (v == 2) return 2;
        throw new IllegalArgumentException("Invalid risk value: " + v);
    }

    private static void swap(int[] a, int i, int j) {
        if (i == j) return;
        int t = a[i]; a[i] = a[j]; a[j] = t;
    }
}
```

---

## 7) **Reverse a stack**

text A **stack** is LIFO (Last In, First Out).

> ### ✅ Method 1: Using another stack (iterative)

```java
import java.util.Stack;
public class ReverseStack {
    public static void main(String[] args) {
        Stack<Integer> stack = new Stack<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.push(4);
        Stack<Integer> reversed = reverseUsingExtraStack(stack);
    }

    private static Stack<Integer> reverseUsingExtraStack(Stack<Integer> original) {
        Stack<Integer> temp = new Stack<>();
        while (!original.isEmpty()) {
            temp.push(original.pop());
        }
        return temp;
    }
}
```

> ### 🌀 Method 2: Recursive reversal (no extra stack)

* Pop all elements recursively until the stack is empty,
* then insert each popped element **at the bottom** of the stack.

```java
import java.util.Stack;
public class ReverseStackRecursion {
    public static void main(String[] args) {
        Stack<Integer> stack = new Stack<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.push(4);
        
        reverse(stack);
    }
    // Reverse function using recursion
    private static void reverse(Stack<Integer> stack) {
        if (stack.isEmpty()) {
            return;
        }
        int top = stack.pop();
        reverse(stack);             // Reverse remaining stack
        insertAtBottom(stack, top); // Insert top at bottom
    }
    // Helper: Insert an element at the bottom of the stack
    private static void insertAtBottom(Stack<Integer> stack, int value) {
        if (stack.isEmpty()) {
            stack.push(value);
            return;
        }
        int top = stack.pop();
        insertAtBottom(stack, value);
        stack.push(top);
    }
}
```

---

## 8) When is it OK to create object of a class inside a method of that class?

* A: OK: when the method’s purpose is to produce new instances (factory, clone, immutable transform).

Creating an object of a class *inside one of its own methods* is **not inherently wrong**, but it’s only justified in **specific, intentional design scenarios**. Most of the time, it’s either unnecessary or points to a design smell.

### 🧩 When it *is OK*

#### 1. **Factory or cloning pattern**

If the class method’s purpose is to create *new instances* — like a **factory**, **copy**, or **builder** — then it makes perfect sense.

Example:

```java
class Employee {
    private String name;
    private int age;

    public Employee(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Factory method inside the same class
    public static Employee create(String name, int age) {
        return new Employee(name, age);
    }

    // Clone or duplicate method
    public Employee copy() {
        return new Employee(this.name, this.age);
    }
}
```

#### 2. **Immutable object transformations**

When your class is immutable, you can’t modify its fields.
Instead, you return a **new object** each time you "change" it.

Example:

```java
record Point(int x, int y) {
    public Point moveRight() {
        return new Point(x + 1, y);
    }
}
```

You’re not mutating the current instance; you’re *describing how to create a new one.*

#### 3. **Nested helper logic (temporary instance)**

Sometimes, you just need a quick temporary instance to delegate computation to, without sharing state.

Example:

```java
class MathHelper {
    public int complexCalculation(int x) {
        MathHelper temp = new MathHelper();  // Temporary use
        return temp.helperMethod(x);
    }

    private int helperMethod(int val) {
        return val * val;
    }
}
```

It’s legal, though not ideal — better if `helperMethod` were static or reused instead of creating a new instance.

### 🚫 When it’s *not* OK

1. **If it causes recursion or loops unintentionally**

   ```java
   class Foo {
       Foo() {
           Foo f = new Foo();  // infinite recursion!
       }
   }
   ```

   Each new `Foo()` calls another `Foo()`, causing a `StackOverflowError`.

2. **If you meant to use `this`**
   Sometimes people do:

   ```java
   MyClass obj = new MyClass();
   obj.someMethod();
   ```

   inside an instance method, when they could’ve just written:

   ```java
   this.someMethod();
   ```

   That creates an unnecessary second object, completely separate from the current one.

3. **If it breaks encapsulation or expected state**
   For example, creating a new instance inside a method when your class manages important data or resources — you’ll lose continuity and violate object consistency.

### 🧠 Rule of thumb

* ✅ OK when the method’s *purpose* is to produce new instances (`factory`, `clone`, `immutable transform`).
* 🚫 Avoid it when it’s *incidental* or introduces new, unmanaged state.

In short:
Creating an object of a class inside one of its own methods is **fine when the class is explicitly designed to produce or transform instances of itself** — otherwise, you’re probably walking into a logical loop or a wasteful allocation.

---

## 9) Can we call a static method with a null object in Java? If so, how?

You **can** call a static method with a `null` reference, **because static methods belong to the class, not the instance**.

Static methods are resolved **at compile time** — they’re **bound to the class**, not the object.
So when you write this:

```java
obj.staticMethod();
```

the compiler doesn’t look at what `obj` *is at runtime*. It simply rewrites it to:

```java
ClassName.staticMethod();
```

### ✅ Example

```java
class Demo {
    static void greet() {
        System.out.println("Hello from static method!");
    }
}

public class Test {
    public static void main(String[] args) {
        Demo d = null;
        d.greet();  // perfectly valid!
    }
}
```

Even though `d` is `null`, this works fine — because the compiler replaces `d.greet()` with `Demo.greet()`.

If the method were **non-static**, you’d get a `NullPointerException`:

```java
class Demo {
    void sayHi() {
        System.out.println("Hi from instance method!");
    }
}

Demo d = null;
d.sayHi();  // ❌ NullPointerException at runtime
```

### 💡 Why this works

Static methods:

* Do **not** depend on any instance data (`this` is not available).
* Are stored in the **method area** (class-level), not in heap objects.
* Are **resolved at compile time** (early binding).

So `null` is irrelevant — there’s no instance involved at all.

### 🧩 Rule of thumb

✅ You *can* call static methods using a `null` reference — **but you shouldn’t**.
It’s legal, but misleading. It implies you’re using an instance when you’re not.

Always prefer the class name:

```java
Demo.greet();  // clearer and idiomatic
```

---

## 10) Palindrome problem

### ✅ 1. Check if a string is palindrome

**Algorithm (2-pointer approach):**

1. Set one pointer at the start (`i = 0`) and one at the end (`j = length - 1`).
2. Compare characters at both ends.
3. If any mismatch → not palindrome.
4. Move inward (`i++`, `j--`) until they cross.

```java
public class PalindromeCheck {
    public static boolean isPalindrome(String s) {
        if (s == null) return false;
        s = s.toLowerCase().replaceAll("[^a-z0-9]", ""); // normalize
        int i = 0, j = s.length() - 1;
        while (i < j) {
            if (s.charAt(i) != s.charAt(j))
                return false;
            i++;
            j--;
        }
        return true;
    }
}
```

#### 🌀 2. Make a string *into* a palindrome (by adding characters)

Sometimes interviewers ask:

> “Given a string, make it a palindrome by adding the minimum number of characters.”

For example:
`"ab"` → `"aba"` (add `'a'` at the end).
`"abcd"` → `"abcdcba"`.

**Approach (simplified):**

* Start checking from the end for the **longest prefix that is already a palindrome**.
* Add the reverse of the remaining suffix to the front.

```java
public class MakePalindrome {
    public static String makePalindrome(String str) {
        if (str == null || str.isEmpty()) return str;

        int end = str.length();
        while (end > 0) {
            if (isPalindrome(str.substring(0, end))) break;
            end--;
        }

        String suffix = str.substring(end);
        String reversed = new StringBuilder(suffix).reverse().toString();
        return str + reversed;
    }

    private static boolean isPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            if (s.charAt(i++) != s.charAt(j--)) return false;
        }
        return true;
    }
}
```

---

### 11) Area of a rectangle using Fuctional interface

#### *1. Create your Functional Interface*

A **Functional Interface** is an interface with exactly **one abstract method** (so it can be used with a lambda expression).

```java
@FunctionalInterface
interface Rectangle {
    double area(double length, double breadth);
}
```

#### *2. Implement in Lambda Expression*

```java
public class AreaOfRectangle {
    public static void main(String[] args) {
        // Lambda expression implementing the functional interface
        Rectangle rect = (length, breadth) -> length * breadth;

        double length = 10.5;
        double breadth = 6.2;
        double area = rect.area(length, breadth);
    }
}
```
