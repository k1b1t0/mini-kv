package minikv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {

    private static final Path TEST_LOG = Path.of("target/server-test-wal.log");
    private static final int TEST_PORT = 6389;

    @BeforeEach
    @AfterEach
    void cleanUp() throws IOException {
        Files.deleteIfExists(TEST_LOG);
    }

    @Test
    void testConcurrentNetworkClients() throws Exception {
        FileKvStore store = new FileKvStore(TEST_LOG.toString());
        Server server = new Server(TEST_PORT, store);

        Thread.ofVirtual().start(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Server stopped or socket closed
            }
        });

        Thread.sleep(150);

        int clientCount = 10;
        ExecutorService clientExecutor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(clientCount);

        // Launch 10 concurrent network clients
        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            clientExecutor.submit(() -> {
                try {
                    // Block until the main test thread fires the startGun
                    startGun.await();

                    // Connect over TCP to localhost:6389
                    try (Socket socket = new Socket("localhost", TEST_PORT);
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                            PrintWriter writer = new PrintWriter(
                                    socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

                        String key = "net-user-" + clientId;
                        String val = "val-" + clientId;

                        // Verify SET command
                        writer.println("SET " + key + " " + val);
                        assertEquals("+OK", reader.readLine(), "Client " + clientId + " SET failed");

                        // Verify GET command
                        writer.println("GET " + key);
                        assertEquals("+" + val, reader.readLine(), "Client " + clientId + " GET failed");

                        // Verify EXIT command
                        writer.println("EXIT");
                        assertEquals("Bye.", reader.readLine(), "Client " + clientId + " EXIT failed");
                    }
                } catch (Exception e) {
                    fail("Client " + clientId + " encountered unexpected error: " + e.getMessage());
                } finally {
                    finishLine.countDown();
                }
            });
        }

        // Fire the gun: releases all 10 client threads simultaneously
        startGun.countDown();

        boolean allFinished = finishLine.await(5, TimeUnit.SECONDS);
        assertTrue(allFinished, "Concurrent network test timed out");

        clientExecutor.shutdown();
        store.close();
    }
}
