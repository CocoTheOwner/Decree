package nl.codevs.decree.context;

import nl.codevs.decree.util.DecreeSender;
import org.bukkit.World;

public class WorldContextHandler implements DecreeContextHandler<World> {
    @Override
    public boolean supports(Class<?> type) {
        return type.equals(World.class);
    }

    @Override
    public World handle(DecreeSender sender) {
        return sender.isPlayer() ? sender.player().getWorld() : null;
    }

    @Override
    public String handleToString(DecreeSender sender) {
        return handle(sender).getName();
    }
}
