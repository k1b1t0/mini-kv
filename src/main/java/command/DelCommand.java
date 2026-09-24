package command;

import minikv.KvStore;

public record DelCommand(String key) implements Command {

    public void execute(KvStore store) {
        try {
            store.delete(key);
        } catch (Exception e) {
            System.out.println(e);
        }

        System.out.println("+OK");
    }
}
