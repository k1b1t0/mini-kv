package minikv;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryKvStore implements KvStore {
    public Map<String, String> map = new HashMap<>();

    public void set(String key, String value) {
        map.put(key, value);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(map.get(key));
    }

    public boolean delete(String key) {
        map.remove(key);
        return true;
    }

}
