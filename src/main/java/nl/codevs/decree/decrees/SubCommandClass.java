package nl.codevs.decree.decrees;

import nl.codevs.decree.decree.objects.DecreeCommandExecutor;
import nl.codevs.decree.decree.objects.DecreeOrigin;
import nl.codevs.decree.decree.objects.Decree;
import nl.codevs.decree.decree.objects.Param;
import org.bukkit.entity.Player;

@Decree(name = "sub", description = "Sub commands", permission = "subs")
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
