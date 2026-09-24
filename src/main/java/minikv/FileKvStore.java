package minikv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.stream.Stream;

import command.Command;
import command.CommandParser;

public class FileKvStore implements KvStore {
    Path path;
    KvStore store = new InMemoryKvStore();

    public FileKvStore(String path) {
        this.path = Path.of(path);

        if (this.path.getParent() != null) {
            try {
                Files.createDirectories(this.path.getParent());
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        this.initStore();
    }

    public void initStore() {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach((line) -> {
                String[] tokens = line.split(":", 3);
                switch (tokens[0].toUpperCase()) {
                    case "SET":
                        try {
                            store.set(tokens[1], tokens[2]);
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                        break;
                    case "DEL":
                        try {
                            store.delete(tokens[1]);
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                        break;
                    default:
                        break;
                }
            });
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void set(String key, String value) throws IOException {
        store.set(key, value);
        try {
            Files.writeString(path, "set:" + key + ":" + value + "\n", StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public Optional<String> get(String key) throws IOException {
        return store.get(key);
    }

    public boolean delete(String key) throws IOException {
        try {
            Files.writeString(path, "del:" + key + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.out.println(e);
        }
        return store.delete(key);
    }
}
