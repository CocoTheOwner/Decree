package nl.codevs.decree.context;

import nl.codevs.decree.util.DecreeSender;
import org.bukkit.World;

public class WorldContextHandler implements DecreeContextHandler<World> {
    public Class<World> getType() {
        return World.class;
    }

    public World handle(DecreeSender sender) {
        return sender.isPlayer() ? sender.player().getWorld() : null;
    }

    @Override
    public String handleToString(DecreeSender sender) {
        return handle(sender).getName();
    }
}
