package command;

import java.io.IOException;

import minikv.KvStore;

@CommandName("EXIT")
public class ExitCommand implements Command {
    public void execute(KvStore store) throws IOException {
        System.out.println("Bye");
        System.exit(0);
    }
}
