package nl.codevs.decree.decrees;

import nl.codevs.decree.util.DecreeOrigin;
import nl.codevs.decree.virtual.Decree;
import nl.codevs.decree.virtual.Param;

@Decree(name = "sub", aliases = {"0", "1", "2", "3", "five"}, description = "Sub commands", permission = "subs")
public class SubCommandClass implements DecreeCommandExecutor {

    @Decree(
            description = "Kill a player",
            origin = DecreeOrigin.CONSOLE,
            aliases = "ded",
            permission = "kill",
            sync = true
    )
    public void hello(
            @Param(
                    name = "Name",
                    aliases = {"1", "16"},
                    description = "Idk",
                    defaultValue = "Text!"
            ) String message
    ){
        sender().sendMessageRaw(message);
    }
}
