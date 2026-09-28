package minikv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class KvStoreTest {

    private static final Path TEST_LOG = Path.of("target/test-wal.log");

    @BeforeEach
    @AfterEach
    void cleanUp() throws IOException {
        Files.deleteIfExists(TEST_LOG);
    }

    @Test
    void testInMemoryStoreBasicOperations() throws IOException {
        KvStore store = new InMemoryKvStore();

        store.set("greeting", "hello");
        assertEquals("hello", store.get("greeting").orElse(null));

        store.delete("greeting");
        assertTrue(store.get("greeting").isEmpty());
    }

    @Test
    void testFileKvStoreCrashRecovery() throws IOException {
        // Phase 1: First session writes data and terminates
        KvStore session1 = new FileKvStore(TEST_LOG.toString());
        session1.set("user", "Alice");
        session1.set("role", "admin");
        session1.set("temp", "to-be-deleted");
        session1.delete("temp");

        // Overwrite user
        session1.set("user", "Bob");

        // Phase 2: Simulate crash and reboot with a fresh store instance
        KvStore session2 = new FileKvStore(TEST_LOG.toString());

        // Verify recovered state from WAL replay
        assertEquals("Bob", session2.get("user").orElse(null), "Overwritten key should have latest value");
        assertEquals("admin", session2.get("role").orElse(null), "Key should be present");
        assertTrue(session2.get("temp").isEmpty(), "Deleted key should not be present");
        assertTrue(session2.get("non_existent").isEmpty(), "Unset key should be empty");
    }

    @Test
    void testLargePayloadsAndRecovery() throws IOException {
        String bigVal = "x".repeat(1024 * 1024);
        FileKvStore store = new FileKvStore(TEST_LOG.toString());

        for (int i = 0; i < 20; i++) {
            store.set(String.valueOf(i), bigVal);
        }

        store.close();

        FileKvStore newStore = new FileKvStore(TEST_LOG.toString());
        for (int i = 0; i < 20; i++) {
            Optional<String> res = newStore.get(String.valueOf(i));
            assertTrue(res.isPresent());
            assertEquals(bigVal.length(), res.get().length());
            assertEquals(bigVal, res.get());
        }
    }

    @Test
    void testConcurrentreadWrite() throws IOException, InterruptedException {
        int threads = 10;
        int keysPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        KvStore store = new FileKvStore(TEST_LOG.toString());

        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startGun.await();

                    for (int j = 0; j < keysPerThread; j++) {
                        String key = "thread-" + threadId + "-key-" + j;
                        String val = "val-" + j;
                        store.set(key, val);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();

        boolean completed = finishLine.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Các luồng không hoàn thành kịp thời gian");

        executor.shutdown();
        for (int i = 0; i < threads; i++) {
            for (int j = 0; j < keysPerThread; j++) {
                String key = "thread-" + i + "-key-" + j;
                String val = "val-" + j;
                assertEquals(Optional.of(val), store.get(key));
            }
        }
    }
}
