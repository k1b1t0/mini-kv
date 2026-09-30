package command;

import java.lang.reflect.Constructor;
import java.util.Arrays;

public class CommandParser {
    public static CommandRegistry commandRegistry = new CommandRegistry();

    public static Command parse(String input) {
        if (input == null || input.isBlank()) {
            return store -> {
                return "-ERR empty input";
            };
        }
        String[] tokens = input.trim().split("\\s+", 3);
        String verb = tokens[0].toUpperCase();

        Class<? extends Command> clazz = commandRegistry.get(verb);
        if (clazz == null) {
            return store -> {
                return "-ERR unknown command: " + verb;
            };
        }
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> constructor = constructors[0];

        int paramCount = constructor.getParameterCount();
        if (tokens.length - 1 < paramCount) {
            return store -> {
                return "-ERR not enough args, require " + paramCount + " arguments";
            };
        }

        try {
            Object[] args = Arrays.copyOfRange(tokens, 1, 1 + paramCount);
            return (Command) constructor.newInstance(args);
        } catch (Exception e) {
            return store -> {
                return "-ERR failed to create command: " + e.getMessage();
            };
        }

    }
}
