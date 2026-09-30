package command;

import minikv.KvStore;

@CommandName("GET")
public record GetCommand(String key) implements Command {
    public String execute(KvStore store) {
        try {
            return store.get(key).map(val -> "+" + val).orElse("-ERR key not found");
        } catch (Exception e) {
            return "-ERR: " + e.getMessage();
        }

    }
}
