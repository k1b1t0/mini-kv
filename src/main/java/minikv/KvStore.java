package minikv;

import java.io.IOException;
import java.util.Optional;

/**
 * Core interface for the key-value storage engine.
 */
public interface KvStore {
    void set(String key, String value) throws IOException;
    Optional<String> get(String key) throws IOException;
    boolean delete(String key) throws IOException;
}
