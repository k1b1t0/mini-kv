package command;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    Map<String, Class<? extends Command>> registry = new HashMap<>();

    public void register(Class<? extends Command> clazz) {
        CommandName annotation = clazz.getAnnotation(CommandName.class);
        if (annotation != null) {
            String verb = annotation.value().toUpperCase();
            registry.put(verb, clazz);
        }
    }

    public CommandRegistry() {
        register(SetCommand.class);
        register(GetCommand.class);
        register(DelCommand.class);
        register(ExitCommand.class);
    }

    public Class<? extends Command> get(String verb) {
        return registry.get(verb);
    }
}
