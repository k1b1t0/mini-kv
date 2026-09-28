package command;

import minikv.KvStore;

@CommandName("DEL")
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
