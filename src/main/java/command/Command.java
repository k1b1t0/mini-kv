package command;

import java.io.IOException;
import minikv.KvStore;

@FunctionalInterface
public interface Command {
    String execute(KvStore store) throws IOException;
}
