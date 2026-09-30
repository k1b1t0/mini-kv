# Mini-KV 🗄️

A lightweight, high-performance Key-Value storage engine built from scratch in pure **Java 21**, implementing the **Bitcask append-only storage architecture** and a networked Redis-like line protocol.

---

## ⚡ Architecture Overview

```text
CLIENT LAYER
 ├── Interactive CLI REPL
 └── TCP Socket Server (Java 21 Virtual Threads / Loom)
       │
COMMAND LAYER (Command Pattern & Reflection Registry)
 └── SetCommand, GetCommand, DelCommand, ExitCommand
       │
STORAGE ENGINE LAYER (Bitcask Architecture)
 ├── In-Memory Offset Index: Map<String, Long>
 ├── Persistence: Append-Only Write-Ahead Log (WAL) with binary framing
 ├── Random Seek: NIO FileChannel reads without file scanning
 └── Concurrency: ReentrantReadWriteLock (N concurrent readers, 1 exclusive writer)
```

---

## 🚀 Key Features

- **Sequential Disk Writes (WAL)**: Zero write amplification. All mutations append binary-framed records `[op][key_len][val_len][key][val]` directly to disk.
- **O(1) Random Access**: Reads jump directly to byte offsets using `FileChannel.position(offset)` + `ByteBuffer`. Values are not duplicated in heap memory.
- **Crash Recovery**: Automatically replays the WAL on startup to rebuild the in-memory index.
- **Multithreaded TCP Server**: Accepts concurrent client connections on port `6379`, dispatching each client to a lightweight **Virtual Thread**.
- **No Third-Party Frameworks**: Built using pure Core Java 21, Java NIO, Java Concurrency (`java.util.concurrent`), and JUnit 5.

---

## 🛠️ Build & Run

### Prerequisites
- JDK 21+
- Apache Maven 3.9+

### 1. Build & Run Tests
```bash
mvn clean test
```

### 2. Package Executable JAR
```bash
mvn clean package
```

### 3. Launch Interactive CLI
```bash
java -jar target/mini-kv-1.0-SNAPSHOT.jar
```
```text
SET user Alice
+OK
GET user
+Alice
DEL user
+OK
EXIT
Bye.
```

### 4. Launch TCP Server (Redis-compatible port)
```bash
java -jar target/mini-kv-1.0-SNAPSHOT.jar --server
```

In another terminal:
```bash
nc localhost 6379
SET greeting hello
+OK
GET greeting
+hello
EXIT
Bye.
```

---

## 🧪 Testing Suite
- `minikv.KvStoreTest`: Unit tests for in-memory operations, WAL crash recovery, large 1MB payloads, and multi-threaded stress tests.
- `minikv.ServerTest`: Concurrent network integration test with 10 parallel clients hitting the TCP socket server simultaneously via `CountDownLatch`.

---

## 📜 License
MIT
