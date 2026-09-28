package minikv;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class FileKvStore implements KvStore, Closeable {
    int OP_SET = 1;
    int OP_DEL = 2;
    Path path;
    FileChannel fileChannel;
    ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    Lock readLock = rwLock.readLock();
    Lock writeLock = rwLock.writeLock();
    /**
     * [op][key_len][val_len][key][val]
     * op: 1-SET; 2-DEL
     */

    // InMemoryKvStore store = new InMemoryKvStore();
    Map<String, Long> store = new HashMap<>();

    public FileKvStore(String path) {
        this.path = Path.of(path);

        try {
            if (this.path.getParent() != null) {
                Files.createDirectories(this.path.getParent());
            }
            this.fileChannel = FileChannel.open(this.path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
            this.initStore();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize WAL storage at: " + path, e);
        }
    }

    private void initStore() throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        readLock.lock();

        long currentOffset = 0;
        long fileSize = fileChannel.size();
        ByteBuffer header = ByteBuffer.allocate(9);

        while (currentOffset < fileSize) {
            header.clear();

            while (header.hasRemaining()) {
                fileChannel.read(header, currentOffset);
            }
            header.flip();

            byte op = header.get();
            int keyLength = header.getInt();
            int valLength = header.getInt();

            ByteBuffer keyBuff = ByteBuffer.allocate(keyLength);
            while (keyBuff.hasRemaining()) {
                fileChannel.read(keyBuff, currentOffset + 9);
            }
            String key = new String(keyBuff.array(), StandardCharsets.UTF_8);

            if (op == OP_DEL) {
                store.remove(key);
            } else if (op == OP_SET) {
                store.put(key, currentOffset);
            }

            currentOffset += 9 + keyLength + valLength;
        }

        readLock.unlock();
    }

    public void set(String key, String value) throws IOException {
        writeLock.lock();
        Long offset = fileChannel.size();

        byte op = 1;
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valBytes = value.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(9 + keyBytes.length + valBytes.length)
                .put(op)
                .putInt(keyBytes.length)
                .putInt(valBytes.length)
                .put(keyBytes)
                .put(valBytes);

        buffer.flip();
        fileChannel.position(offset);
        try {
            while (buffer.hasRemaining()) {
                fileChannel.write(buffer);
            }
            store.put(key, offset);
        } finally {
            writeLock.unlock();
        }
    }

    public Optional<String> get(String key) throws IOException {
        if (!store.containsKey(key)) {
            return Optional.empty();
        }

        readLock.lock();

        Long offset = store.get(key);
        fileChannel.position(offset + 1 + 4);

        ByteBuffer header = ByteBuffer.allocate(9);

        while (header.hasRemaining()) {
            fileChannel.read(header, offset);
        }

        header.flip();

        header.get();
        int keyLength = header.getInt();
        int valLength = header.getInt();

        ByteBuffer valBuf = ByteBuffer.allocate(valLength);
        while (valBuf.hasRemaining()) {
            fileChannel.read(valBuf, offset + 9 + keyLength);
        }

        readLock.unlock();
        return Optional.ofNullable(new String(valBuf.array(), StandardCharsets.UTF_8));
    }

    public boolean delete(String key) throws IOException {
        writeLock.lock();
        Long offset = fileChannel.size();

        byte op = 2;
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(9 + keyBytes.length)
                .put(op)
                .putInt(keyBytes.length)
                .putInt(0)
                .put(keyBytes);

        buffer.flip();
        fileChannel.position(offset);
        try {
            while (buffer.hasRemaining()) {
                fileChannel.write(buffer);
            }
            store.put(key, offset);
        } finally {
            writeLock.unlock();
        }
        store.remove(key);
        return true;
    }

    @Override
    public void close() throws IOException {
        if (fileChannel != null && fileChannel.isOpen()) {
            fileChannel.close();
        }
    }
}
