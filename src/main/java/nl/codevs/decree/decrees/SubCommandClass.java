package nl.codevs.decree.decrees;

import nl.codevs.decree.decree.objects.DecreeCommandExecutor;
import nl.codevs.decree.decree.objects.DecreeOrigin;
import nl.codevs.decree.decree.objects.Decree;

@Decree(name = "sub", aliases = {"0", "1", "2", "3", "five"}, description = "Sub commands", permission = "subs")
public class SubCommandClass implements DecreeCommandExecutor {

    @Decree(
            description = "Kill a player",
            origin = DecreeOrigin.PLAYER,
            aliases = "ded",
            permission = "kill",
            sync = true
    )
    public void hello(){
        sender().sendMessageRaw("yeet");
    }
}
