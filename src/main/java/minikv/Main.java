package minikv;

import java.util.Scanner;

import command.Command;
import command.CommandParser;
import command.ExitCommand;

public class Main {
    public static void main(String[] args) {
        KvStore store = new InMemoryKvStore();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Command command = CommandParser.parse(line);
            try {
                command.execute(store);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}
