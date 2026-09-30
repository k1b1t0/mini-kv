package command;

import minikv.KvStore;

@CommandName("DEL")
public record DelCommand(String key) implements Command {

    public String execute(KvStore store) {
        try {
            store.delete(key);
        } catch (Exception e) {
            return "-ERR: " + e.getMessage();
        }

        return "+OK";
    }
}
