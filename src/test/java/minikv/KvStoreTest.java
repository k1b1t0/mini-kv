package minikv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
