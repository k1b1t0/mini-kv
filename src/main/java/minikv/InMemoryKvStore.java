package minikv;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryKvStore implements KvStore {
    public Map<String, String> map = new HashMap<>();

    public void set(String key, String value) throws IOException {
        map.put(key, value);
    }

    public Optional<String> get(String key) throws IOException {
        return Optional.ofNullable(map.get(key));
    }

    public boolean delete(String key) throws IOException {
        map.remove(key);
        return true;
    }

}
