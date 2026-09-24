package command;

public class CommandParser {
    public static Command parse(String input) {
        if (input == null || input.isBlank()) {
            return store -> System.out.println("-ERR empty input");
        }
        String[] tokens = input.trim().split("\\s+", 3);
        String keyword = tokens[0].toUpperCase();

        return switch (keyword) {
            case "SET" -> {
                if (tokens.length < 3)
                    yield store -> System.out.println("-ERR SET requires key and value");
                yield new SetCommand(tokens[1], tokens[2]);
            }
            case "GET" -> {
                if (tokens.length < 2)
                    yield store -> System.out.println("-ERR GET requires key");
                yield new GetCommand(tokens[1]);
            }
            case "DEL" -> {
                if (tokens.length < 2)
                    yield store -> System.out.println("-ERR DEL requires key");
                yield new DelCommand(tokens[1]);
            }
            case "EXIT", "QUIT" -> new ExitCommand();
            default -> store -> System.out.println("-ERR unknown command: " + keyword);
        };
    }
}
