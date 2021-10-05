package nl.codevs.decree.context;

import nl.codevs.decree.util.DecreeSender;
import org.bukkit.entity.Player;

public class PlayerContextHandler implements DecreeContextHandler<Player> {
    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Player.class);
    }

    @Override
    public Player handle(DecreeSender sender) {
        return sender.player();
    }

    @Override
    public String handleToString(DecreeSender sender) {
        return handle(sender).getName();
    }
}
