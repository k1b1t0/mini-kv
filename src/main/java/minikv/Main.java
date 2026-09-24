package minikv;

import java.util.Scanner;

import command.Command;
import command.CommandParser;

public class Main {
    public static void main(String[] args) {
        KvStore store = new FileKvStore("data/wal.log");

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Command command = CommandParser.parse(line);
            try {
                command.execute(store);
            } catch (Exception e) {
                System.out.println("-ERR: " + e.getMessage());
            }
        }
    }
}
