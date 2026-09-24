package command;

import java.io.IOException;
import minikv.KvStore;

@FunctionalInterface
public interface Command {
    void execute(KvStore store) throws IOException;
}
