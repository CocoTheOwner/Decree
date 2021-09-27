package nl.codevs.decree.decrees;

import nl.codevs.decree.virtual.Decree;
import nl.codevs.decree.virtual.Param;
import org.bukkit.World;

@SuppressWarnings("SpellCheckingInspection")
@Decree(name = "command", aliases = {"cmmd", "cmd", "cd"}, description = "Main commands", permission = "decree")
public class MainCommandClass implements DecreeCommandExecutor {

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
                    description = "wowd",
                    contextual = true
            )
                    World poop,
            @Param(
                    defaultValue = "Hello!",
                    aliases = {"msg", "m"},
                    description = "Send a message to yourself"
            )
                    String message,
            @Param(
                    aliases = {"msgggg", "gghm"},
                    description = "dwad a message to yourself"
            )
                    String look
    ){
        sender().sendMessage(message + " / " + poop.getName() + " / " + look);
    }
}
