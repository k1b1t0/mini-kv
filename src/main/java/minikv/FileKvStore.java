package minikv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class FileKvStore implements KvStore {
    Path path;
    // InMemoryKvStore store = new InMemoryKvStore();
    Map<String, Long> store = new HashMap<>();

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
            AtomicLong index = new AtomicLong(0);
            lines.forEach((line) -> {
                String[] tokens = line.split(":", 3);
                switch (tokens[0].toUpperCase()) {
                    case "SET":
                        // store.set(tokens[1], tokens[2]);
                        store.put(tokens[1], index.longValue());
                        break;
                    case "DEL":
                        // store.delete(tokens[1]);
                        store.remove(tokens[1]);
                        break;
                    default:
                        break;
                }
                index.addAndGet(line.getBytes(StandardCharsets.UTF_8).length + 1);
            });
        }
    }

    public void set(String key, String value) throws IOException {
        Long offset = Files.exists(path) ? Files.size(path) : 0L;

        Files.writeString(path, "set:" + key + ":" + value + "\n", StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        store.put(key, offset);
    }

    public Optional<String> get(String key) throws IOException {
        if (!store.containsKey(key)) {
            return Optional.empty();
        }
        Long offset = store.get(key);
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
            channel.position(offset);
            BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8));
            String line = reader.readLine();
            String[] tokens = line.split(":", 3);
            return Optional.ofNullable(tokens[2]);
        }
    }

    public boolean delete(String key) throws IOException {
        Files.writeString(path, "del:" + key + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        if (store.containsKey(key)) {
            store.remove(key);
            return true;
        }
        return false;
    }
}
