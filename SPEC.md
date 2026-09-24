# Project Specification: Mini-KV (Append-Only Key-Value Storage Engine)

- **Target Language**: Java 21 (Pure Core + JUnit 5)
- **Location**: `projects/mini-kv/`
- **Timeline**: W03 Weekend Scaffolding → W04/W05 Mastery (6 Incremental Milestones)
- **Pedagogical Goal**: Synthesize Java Core (Maven, Collections, File I/O, NIO, Concurrency, Design Patterns, Networking, JDBC) into a real storage engine you build entirely yourself.
- **Tutoring Model**: Strict Zone of Proximal Development (ZPD). See Section 3.

---

## 1. Architectural Mental Model: The Bitcask Pattern

High-performance storage engines (Bitcask, RocksDB, Redis AOF, Kafka) use an **Append-Only Write-Ahead Log (WAL)** + in-memory index instead of in-place overwrites. This eliminates write amplification and enables sequential disk throughput:

```text
┌──────────────────────────────────────────────────────────────────┐
│ CLIENT LAYER                                                     │
│  CLI: "SET user Alice"  ──or──  TCP socket: "SET user Alice\r\n" │
│  → Parsed into a Command object (Command Pattern)                │
├──────────────────────────────────────────────────────────────────┤
│ CONCURRENCY LAYER                                                │
│  ReentrantReadWriteLock: N concurrent readers / 1 exclusive      │
│  writer at a time — prevents data races on the index + log       │
├──────────────────────────────────────────────────────────────────┤
│ WRITE PIPELINE                                                   │
│  1. Append entry to disk: data/kv.log → "SET:user:Alice\n"      │
│  2. Record byte offset in index: Map<String, Long> index         │
│     index.put("user", currentFileOffset)                         │
├──────────────────────────────────────────────────────────────────┤
│ READ PIPELINE                                                    │
│  1. index.get(key)  → byteOffset (O(1) memory lookup)           │
│  2. LRU cache hit?  → return cached value                        │
│  3. Cache miss      → FileChannel.read(buf, byteOffset)          │
│     → parse value → populate LRU cache → return                  │
└──────────────────────────────────────────────────────────────────┘
```

> **Why build this?** After this project you will understand exactly how Redis, Kafka, and RocksDB work at the disk I/O level — not just how to use them.

---

## 2. The 6 Incremental Milestones

Each milestone has a **single responsibility** — one new concept applied. Do not jump ahead.

---

### 📍 Milestone 1: Maven Setup + CLI REPL + Command Pattern
**Est. time**: 35–45 min  
**Core Java topics applied**: Build tools, OOP, interfaces, design patterns

**What you will build**:  
An interactive REPL where you type `SET name Alice`, `GET name`, `DEL name`, `EXIT` and the engine responds. Data persists to disk and survives a restart.

**Concepts to apply**:
- **Maven**: Configure `pom.xml` — enforce Java 21 via `maven.compiler.release` (not `source`/`target`), add JUnit 5 with correct `<scope>test</scope>`, add `maven-shade-plugin` for executable fat JAR with `Main-Class`.
- **Command Pattern**: Design a `Command` interface with a single method. Implement one concrete class per command verb. A `CommandParser` maps input tokens to Command objects.
- **Storage**: `KvStore` interface (already exists). Implement `InMemoryKvStore` (HashMap only) first, then `FileKvStore` (serialize the whole HashMap to disk on every write using `Files.writeString` / `Files.readString`).

**Hands-on target**: `mvn package` produces a runnable JAR. `java -jar target/mini-kv.jar` launches the REPL. `mvn test` passes.

**⬆️ Stretch (optional, adds Reflection + Annotation concepts)**:  
Define a custom `@CommandName("SET")` annotation. Write a `CommandRegistry` that scans for classes annotated with it using `Class.getAnnotation()` to dynamically register handlers — instead of a hard-coded `if/else` chain.

---

### 📍 Milestone 2: Append-Only WAL + Decorator Pattern + Crash Recovery
**Est. time**: 30–40 min  
**Core Java topics applied**: NIO file I/O, streams, design patterns (Decorator)

**What you will build**:  
Replace the naive "rewrite whole file" approach with a sequential append log. On startup, replay the log to rebuild the in-memory state. Deletion writes a tombstone instead of removing entries.

**Concepts to apply**:
- **WAL append**: Open the log file in append mode (look up `StandardOpenOption.APPEND` and `StandardOpenOption.CREATE`). Write each mutation as a formatted line.
- **Tombstone delete**: Writing `DEL:key` marks a key as deleted without touching earlier log entries.
- **Crash recovery**: On startup, read the log line by line using `Files.lines()`. Replay SET entries into the index; replay DEL entries to remove them. Use `try-with-resources` — the stream holds a file handle.
- **Decorator Pattern**: Wrap your store with a second class that *delegates* all method calls to an inner `KvStore` and adds a cross-cutting concern (e.g. operation count, timing, or audit logging). The wrapper must implement `KvStore` itself.

**Hands-on target**: Kill the process mid-write. Restart. Data is fully recovered. `mvn test` passes including a crash-recovery test.

---

### 📍 Milestone 3: Byte-Offset Index + NIO FileChannel Random Access
**Est. time**: 35–45 min  
**Core Java topics applied**: NIO channels, ByteBuffer state machine

**What you will build**:  
Real Bitcask architecture. Instead of scanning the whole log for each GET, record the byte offset of each SET entry in an in-memory index. Reads jump directly to that offset using `FileChannel`.

**Concepts to apply**:
- **Index**: Replace `Map<String, String>` with `Map<String, Long>` — the Long is the byte offset in `kv.log` where the value starts.
- **Random seek**: `FileChannel.open(path, READ)` + `ByteBuffer.allocate(N)` + `channel.read(buf, offset)` + `buf.flip()` → decode bytes to String.
- **ByteBuffer state machine**: Understand position/limit/capacity before and after `flip()`, `clear()`, `compact()`. Getting this wrong means reading garbage.
- **Benchmark**: Write a JUnit test that generates a 50MB log, then compares time-to-read for: (a) sequential `BufferedReader` scan vs. (b) `FileChannel` random seek to the last entry. Measure with `System.nanoTime()`.

**Hands-on target**: `GET` for any key reads directly from disk at its byte offset without scanning. Benchmark numbers printed to stdout.

---

### 📍 Milestone 4: Custom LRU Cache + Thread-Safe Concurrency
**Est. time**: 40–50 min (split into two sub-sessions if needed)  
**Core Java topics applied**: Data structures, concurrency, design patterns (Strategy)

**What you will build**:  
A bounded in-memory cache to avoid repeated disk reads, and a lock layer so multiple threads can read concurrently without corrupting the index.

**Sub-session A — LRU Cache (implement first, test before moving on)**:
- **Data structure**: Doubly-Linked List + `HashMap<String, Node>`. No `LinkedHashMap` — you must write the pointer manipulation yourself (`node.prev`, `node.next`, `head`, `tail`).
- **Eviction**: When cache is full (`size == capacity`), evict the least-recently-used entry (the node just before `tail`).
- **Strategy Pattern**: Define an `EvictionStrategy` interface with a single method. Implement `LruEvictionStrategy` and `FifoEvictionStrategy`. The cache takes an `EvictionStrategy` at construction — no `if/else` inside the cache class itself.

**Sub-session B — Thread Safety (add after LRU tests pass)**:
- **`ReentrantReadWriteLock`**: Wrap index reads with `readLock` (multiple threads hold simultaneously). Wrap index writes + log appends with `writeLock` (exclusive).
- **Canonical lock pattern**: `lock.lock()` → `try { ... } finally { lock.unlock() }`. Never leave a lock acquired in a failure path.
- **Stress test**: Write a JUnit test that launches 20 concurrent threads (use `ExecutorService` + `CountDownLatch`) — half doing SET, half doing GET on the same keys. Assert no `ConcurrentModificationException` and final state is consistent.

**Hands-on target**: All stress tests pass under `mvn test`. No data races detectable.

---

### 📍 Milestone 5: TCP Socket Server (Networked Mini-KV)
**Est. time**: 40–50 min  
**Core Java topics applied**: Java networking, virtual threads, wire protocol design

**What you will build**:  
Expose Mini-KV over TCP. Any client — including `nc` or `telnet` from a terminal — can send commands and receive responses.

**Concepts to apply**:
- **`ServerSocket`**: Open a server socket on port `6379`. Accept client connections in a loop: `socket = serverSocket.accept()`.
- **Virtual Threads**: Spawn one virtual thread per accepted connection: `Thread.ofVirtual().start(() -> handleClient(socket))`. Virtual threads are cheap enough that you don't need a thread pool.
- **Wire protocol**: Define a minimal text line protocol. Each request is one line: `SET key value\r\n`. Each response is one line: `+OK\r\n` or `-ERR unknown command\r\n`. Read with `BufferedReader`, write with `PrintWriter`.
- **Reuse your engine**: The `ClientHandler` receives a `KvStore` reference (your thread-safe M4 engine). All concurrency is already handled.

**Interactive test**: Run `java -jar target/mini-kv.jar --server`. In a second terminal: `nc localhost 6379`, then type `SET foo bar`. Verify `+OK`. Then `GET foo`. Verify `+bar`.

**Hands-on target**: Two simultaneous `nc` sessions both operate correctly without interfering.

---

### 📍 Milestone 6: Raw JDBC Benchmark (Bridge to Database Track)
**Est. time**: 30 min  
**Core Java topics applied**: `java.sql`, connection management, prepared statements

**What you will build**:  
Connect Java directly to SQLite (no ORM, no Spring) using raw `java.sql` APIs and benchmark write/read throughput against your append-only log.

**Concepts to apply**:
- **`DriverManager.getConnection(url)`**: Establish a JDBC connection to a local SQLite file.
- **`PreparedStatement`**: Always use `?` placeholders — never string concatenation (SQL injection). `stmt.setString(1, key)`.
- **`ResultSet`**: Iterate with `rs.next()`, extract columns with `rs.getString()`.
- **Transactions**: Wrap bulk inserts in `conn.setAutoCommit(false)` + `conn.commit()` — observe the 10–100x throughput difference vs. auto-commit.
- **Benchmark**: Insert 10,000 key-value pairs via JDBC. Insert 10,000 via your WAL append. Compare wall-clock time and explain *why* the numbers differ (WAL sequential writes vs. B-tree random writes).

**Hands-on target**: Benchmark results printed as a table. You can explain the trade-offs between the two storage models.

---

## 3. Mandatory AI Tutoring Protocol (ZPD Rules)

> [!IMPORTANT]
> **Instructions for the AI Tutor — read before every milestone session:**
>
> ### What you MUST do:
> 1. **Deliver exactly one of the 5 Ladder Steps per turn** — do not rush through all 5 in one response.
>    - **Step 1**: Mental model + ASCII diagram (what problem this milestone solves, 2–3 sentences max).
>    - **Step 2**: The JUnit 5 test contract or interface signatures — this is *what must pass*, not how to pass it.
>    - **Step 3**: 2–3 JDK class/method names as hints (e.g. `StandardOpenOption.APPEND`, `ByteBuffer.flip()`). No implementation logic. No code that the user pastes in.
>    - **Step 4**: Ask the user to implement. Wait. Do not continue until they paste their attempt.
>    - **Step 5**: Critique the user's code specifically — resource safety (`try-with-resources`), race hazards, exception leakage, edge cases. Suggest what to investigate, never rewrite for them.
>
> ### What you MUST NOT do:
> - Never write a completed method body.
> - Never write a complete class for the user to copy.
> - Never give "here is the full solution" even when the user is stuck — give a smaller hint instead and ask them to try again.
> - Never skip Step 4 (user attempt) and go straight to Step 5.
>
> ### What the USER must do:
> - Write every line of production code themselves.
> - Run `mvn test` and paste failures before asking for help.
> - Commit after every milestone that passes: `feat(mini-kv): milestone N - <short description>`.
> - Never paste AI-generated code into their editor without understanding every line.
>
> ### Critique checklist (Step 5):
> - [ ] Is every opened resource closed in a `finally` block or `try-with-resources`?
> - [ ] Is every lock `unlock()`-ed in a `finally` block?
> - [ ] Does `DEL` on a missing key return the correct value (no exception)?
> - [ ] Is `ByteBuffer.flip()` called before every `channel.write()` or String decode?
> - [ ] Are `PreparedStatement` placeholders used (never string concatenation)?
> - [ ] Does the concurrent stress test actually assert consistency (not just "no exception")?

---

## 4. What This Project Covers (Topic → Milestone Map)

| Java Topic | Applied In | How |
| :--- | :--- | :--- |
| `tooling.maven` | M1 | `pom.xml` scopes, `maven.compiler.release`, shade plugin |
| Design Patterns: Command | M1 | `Command` interface + verb implementations + `CommandParser` |
| Design Patterns: Decorator | M2 | Wrapping `KvStore` with cross-cutting concerns |
| Design Patterns: Strategy | M4 | Pluggable `EvictionStrategy` on the LRU cache |
| `java.nio` file I/O | M2, M3 | WAL append, `FileChannel` random seek, `ByteBuffer` state machine |
| `java.concurrency` | M4 | `ReentrantReadWriteLock`, `ExecutorService`, `CountDownLatch` |
| Java networking | M5 | `ServerSocket`, virtual threads, wire protocol |
| `java.sql` / JDBC | M6 | `PreparedStatement`, `ResultSet`, transaction control |
| `java.reflection_basic` | M1 stretch | `@CommandName` annotation + reflective registry |
