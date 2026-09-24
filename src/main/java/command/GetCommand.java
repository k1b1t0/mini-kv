package command;

import java.util.Optional;

import minikv.KvStore;

public record GetCommand(String key) implements Command {
    public void execute(KvStore store) {
        try {
            Optional<String> result = store.get(key);
            result.ifPresentOrElse(val -> System.out.println("+" + val),
                    () -> System.out.println("-ERR key not found"));
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
