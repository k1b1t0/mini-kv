package minikv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.stream.Stream;

public class FileKvStore implements KvStore {
    Path path;
    InMemoryKvStore store = new InMemoryKvStore();

    public FileKvStore(String path) {
        this.path = Path.of(path);

        try {
            if (this.path.getParent() != null) {
                Files.createDirectories(this.path.getParent());
            }
            this.initStore();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize WAL storage at: " + path, e);
        }
    }

    private void initStore() throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach((line) -> {
                if (line.isBlank()) {
                    return;
                }
                String[] tokens = line.split(":", 3);
                switch (tokens[0].toUpperCase()) {
                    case "SET":
                        store.set(tokens[1], tokens[2]);
                        break;
                    case "DEL":
                        store.delete(tokens[1]);
                        break;
                    default:
                        break;
                }
            });
        }
    }

    public void set(String key, String value) throws IOException {
        Files.writeString(path, "set:" + key + ":" + value + "\n", StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        store.set(key, value);
    }

    public Optional<String> get(String key) throws IOException {
        return store.get(key);
    }

    public boolean delete(String key) throws IOException {
        Files.writeString(path, "del:" + key + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return store.delete(key);
    }
}
