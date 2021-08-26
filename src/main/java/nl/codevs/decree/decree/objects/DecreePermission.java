package nl.codevs.decree.decree.objects;

import nl.codevs.decree.decree.util.KList;
import org.bukkit.entity.Player;

public interface DecreePermission {

    /**
     * The description of this node
     */
    default String description() {
        return "No description provided";
    };

    /**
     * The name of the permission
     */
    String node();

    /**
     * The children of this node
     */
    default KList<DecreePermission> children(){
        return new KList<>();
    }

    /**
     * Whether or not
     * @param player the player
     * @return has the permission -> true
     */
    default boolean hasPermission(Player player) {
        return player.hasPermission("op");
    }

    /**
     * What to send when no permission
     * @param player the player to send to
     * @return a {@link KList} of string messages
     */
    default KList<String> onNoPermission(Player player){
        return new KList<>(
                "You do not have the required permissions node for this command:"
        );
    }
}