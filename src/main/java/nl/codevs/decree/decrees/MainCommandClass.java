package nl.codevs.decree.decrees;

import nl.codevs.decree.decree.DecreeExecutor;
import nl.codevs.decree.decree.DecreeOrigin;
import nl.codevs.decree.decree.annotations.Decree;
import nl.codevs.decree.decree.annotations.Param;

@Decree(name = "command", aliases = {"cmmd", "cmd", "cd"}, description = "Main commands")
public class MainCommandClass implements DecreeExecutor {

    // This line here
    private SubCommandClass nameDoesNotMatterHere;

    @Decree(
            description = "Send hello!",
            origin = DecreeOrigin.PLAYER,
            aliases = "owo",
            sync = true,
            name = "hi"
    )
    public void hello(
            @Param(
                    defaultValue = "Hello!",
                    aliases = "oki",
                    description = "The message to send"
            )
                    String message
    ){
        sender().sendMessage(message);
    }
}
