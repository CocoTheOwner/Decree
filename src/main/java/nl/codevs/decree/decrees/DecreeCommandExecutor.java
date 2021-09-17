package nl.codevs.decree.decrees;

import nl.codevs.decree.DecreeSystem;
import nl.codevs.decree.util.DecreeSender;
import nl.codevs.decree.util.C;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Represents a decree executor, the interface all decree commands must implement
 */
public interface DecreeCommandExecutor {
    /**
     * @return The command sender
     */
    default DecreeSender sender() {
        return DecreeSystem.Context.get();
    }

    /**
     * @return Underlying player of the sender (null if not a player)
     */
    default Player player() {
        return sender().player();
    }

    /**
     * @return World where underlying player resides (null if not a player)
     */
    default World world() {
        return sender().isPlayer() ? player().getWorld() : null;
    }

    /**
     * @param message The message to send to the sender
     */
    default void message(String message){
        sender().sendMessage(message);
    }

    /**
     * @param message The error to send to the sender (RED)
     */
    default void error(String message){
        sender().sendMessage(C.RED + message);
    }

    /**
     * @param message The message to send to the sender (YELLOW)
     */
    default void warn(String message){
        sender().sendMessage(C.YELLOW + message);
    }

    /**
     * @param message The message to send to the sender (GREEN)
     */
    default void success(String message){
        sender().sendMessage(C.GREEN + message);
    }
}
