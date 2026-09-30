package command;

import java.io.IOException;

import minikv.KvStore;

@CommandName("EXIT")
public class ExitCommand implements Command {
    public String execute(KvStore store) throws IOException {
        return null;
    }
}
