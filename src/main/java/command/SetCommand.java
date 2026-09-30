package command;

import minikv.KvStore;

@CommandName("SET")
public record SetCommand(String key, String value) implements Command {
    public String execute(KvStore store) {
        try {
            store.set(key, value);
        } catch (Exception e) {
            return "-ERR: " + e.getMessage();
        }

        return "+OK";
    }
}
