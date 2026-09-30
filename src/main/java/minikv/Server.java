package minikv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;

import command.Command;
import command.CommandParser;

public class Server {
    private final int port;
    private final KvStore store;

    public Server(int port, KvStore store) {
        this.port = port;
        this.store = store;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port: " + port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                Thread.ofVirtual().start(() -> handleClient(clientSocket));
            }
        }
    }

    public void handleClient(Socket socket) {
        try (
                socket;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);) {
            System.out.println("Socket connected: " + socket.toString());
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank())
                    continue;
                Command command = CommandParser.parse(line);
                String response = command.execute(store);

                // exit
                if (response == null) {
                    writer.println("Bye.");
                    System.out.println("Socket closed: " + socket.toString());
                    break;
                }

                writer.println(response);

            }
        } catch (Exception e) {
            System.err.println("Client disconnected or error: " + e.getMessage());
        }
    }
}
