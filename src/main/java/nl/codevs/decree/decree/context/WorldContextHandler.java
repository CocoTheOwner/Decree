package nl.codevs.decree.decree.context;

import nl.codevs.decree.decree.objects.DecreeSender;
import org.bukkit.World;

public class WorldContextHandler implements DecreeContextHandler<World> {
    public Class<World> getType() {
        return World.class;
    }

    public World handle(DecreeSender sender) {
        return sender.isPlayer() ? sender.player().getWorld() : null;
    }
}
