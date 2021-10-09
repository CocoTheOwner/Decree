package nl.codevs.decree.decrees.examples;

import nl.codevs.decree.decrees.DecreeCommandExecutor;
import nl.codevs.decree.util.DecreeOrigin;
import nl.codevs.decree.virtual.Decree;
import nl.codevs.decree.virtual.Param;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Decree(name = "sub", description = "Sub commands", permission = "subs", sync = true, origin = DecreeOrigin.CONSOLE)
// Sync = true, so all commands in this category + sub categories are run synchronously
// Origin = DecreeOrigin.CONSOLE, so all commands in this category + sub categories are only available to consoles
// Commands & categories with no visible/allowed commands/subcategories are hidden from the sender requesting all commands/categories in some category
public class SubCommandClass implements DecreeCommandExecutor {

    @Decree(
            description = "Kill → player",
            aliases = "kill",
            permission = "kill"
    )
    public void killPlayer( // Remember, becomes: kill-player

            @Param(
                    description = "The player to kill",
                    defaultValue = "self" // Some parsers have extra keywords built-in. See a full list here: https://www.github.com/CocoTheOwner/Decree/
            )
            Player player,
            @Param(
                    aliases = {"death-message"},
                    description = "The message to send to the person you've killed",
                    defaultValue = "Text!"
            ) String message
    ){
        player.setHealth(0);
        sender().sendMessageRaw(message);
    }
}
