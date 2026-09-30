package minikv;

import java.util.Scanner;

import command.Command;
import command.CommandParser;

public class Main {
    public static void main(String[] args) {
        KvStore store = new FileKvStore("data/wal.log");

        if (args.length > 0 && args[0].equals("--server")) {
            try {
                new Server(6379, store).start();
            } catch (Exception e) {
                System.err.println("-ERR: " + e.getMessage());
            }

        } else {
            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Command command = CommandParser.parse(line);
                    try {
                        String response = command.execute(store);
                        if (response == null) {
                            System.out.println("Bye.");
                            break;
                        }
                        System.out.println(response);
                    } catch (Exception e) {
                        System.out.println("-ERR: " + e.getMessage());
                    }
                }
            }
        }
    }
}
