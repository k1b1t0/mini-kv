package command;

import minikv.KvStore;

@CommandName("SET")
public record SetCommand(String key, String value) implements Command {
    public void execute(KvStore store) {
        try {
            store.set(key, value);
        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println("+OK");
    }
}
