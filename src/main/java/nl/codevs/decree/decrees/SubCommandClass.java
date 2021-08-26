package nl.codevs.decree.decrees;

import nl.codevs.decree.decree.objects.DecreeNodeExecutor;
import nl.codevs.decree.decree.objects.DecreeOrigin;
import nl.codevs.decree.decree.objects.Decree;
import nl.codevs.decree.decree.objects.Param;
import org.bukkit.entity.Player;

@Decree(name = "sub", description = "Sub commands", permission = "subs")
public class SubCommandClass implements DecreeNodeExecutor {

    @Decree(
            description = "Kill a player",
            origin = DecreeOrigin.PLAYER,
            aliases = "ded",
            permission = "kill",
            sync = true
    )
    public void hello(
            @Param(
                    defaultValue = "self",
                    description = "The player to kill"
            )
                    Player player
    ){
        player.setHealth(0.0d);
    }
}
