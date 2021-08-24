package nl.codevs.decree.decrees;

import nl.codevs.decree.decree.DecreeExecutor;
import nl.codevs.decree.decree.DecreeOrigin;
import nl.codevs.decree.decree.annotations.Decree;
import nl.codevs.decree.decree.annotations.Param;
import org.bukkit.entity.Player;

@Decree(name = "sub", description = "Sub commands")
public class SubCommandClass implements DecreeExecutor {

    @Decree(
            description = "Kill a player",
            origin = DecreeOrigin.PLAYER,
            aliases = "ded"
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
