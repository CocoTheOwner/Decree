package nl.codevs.decree.decrees;

import nl.codevs.decree.DecreeCommandExecutor;
import nl.codevs.decree.Decree;
import nl.codevs.decree.Param;

@SuppressWarnings("SpellCheckingInspection")
@Decree(name = "command", aliases = {"cmmd", "cmd", "cd"}, description = "Main commands", permission = "decree")
public class MainCommandClass implements DecreeCommandExecutor {

    // This line is a category pointer
    private SubCommandClass nameDoesNotMatterHere;

    @Decree(
            description = "Description goes here",
            aliases = {"alias1", "alias2"},
            sync = true,
            permission = "decree.hello",
            name = "command123"
    )
    public void hello(

            @Param(
                    aliases = {"msawdag", "www"},
                    description = "wawd a message to yourself"
            )
                    String poop,
            @Param(
                    defaultValue = "Hello!",
                    aliases = {"msg", "m"},
                    description = "Send a message to yourself"
            )
                    String message,
            @Param(
                    aliases = {"msgggg", "gghm"},
                    description = "dwad a message to yourself",
                    contextual = true
            )
                    String look
    ){
        sender().sendMessage(message);
    }
}
