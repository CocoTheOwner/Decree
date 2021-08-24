package nl.codevs.raiders.decrees;

import nl.codevs.raiders.decree.DecreeExecutor;
import nl.codevs.raiders.decree.DecreeOrigin;
import nl.codevs.raiders.decree.annotations.Decree;
import nl.codevs.raiders.decree.annotations.Param;

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
