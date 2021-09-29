package nl.codevs.decree.virtual;

import nl.codevs.decree.DecreeSystem;
import nl.codevs.decree.util.DecreeOrigin;
import nl.codevs.decree.util.DecreeSender;
import nl.codevs.decree.util.C;
import nl.codevs.decree.util.KList;

import java.util.Arrays;

public interface Decreed {

    /**
     * Decree access for this interface
     */
    Decree decree();

    /**
     * Get the primary name of the node
     */
    String getName();

    /**
     * The parent node of this node. Null if origin.
     */
    Decreed parent();

    /**
     * Get the {@link DecreeSystem} managing this decreed
     */
    DecreeSystem system();

    /**
     * Send help to a sender
     */
    void sendHelpTo(DecreeSender sender);

    /**
     * Run this decreed
     * @param args The arguments to parse
     * @param sender The sender to parse for
     */
    boolean run(KList<String> args, DecreeSender sender);

    /**
     * Get the origin of the node
     */
    default DecreeOrigin getOrigin() {
        return decree().origin();
    }

    /**
     * Get the required permission for this node
     */
    default String getPermission() {
        return decree().permission();
    }

    /**
     * Get the description of the node
     */
    default String getDescription() {
        return decree().description();
    }

    /**
     * Get whether this node requires sync runtime or not
     */
    default boolean isSync() {
        return decree().sync() || parent().isSync();
    }

    /**
     * Send a debug message
     * @param message The message
     * @param color The color to prefix with
     */
    default void debug(String message, C color) {
        system().debug(C.YELLOW + getPath() + color + " → " + C.YELLOW + getName() + color + " | " + message);
    }

    /**
     * Get the primary and alias names of the node<br>
     */
    default KList<String> getNames() {
        KList<String> names = new KList<>(getName());
        names.addAll(Arrays.asList(decree().aliases()));
        return names.qremoveIf(String::isEmpty).qremoveDuplicates();
    }

    /**
     * Get the command path to this node
     */
    default String getPath() {
        String shortest = getName();
        for (String name : getNames()) {
            if (name.length() < shortest.length()) {
                shortest = name;
            }
        }

        if (parent() == null) {
            return "/" + shortest;
        }
        return parent().getPath() + " " + shortest;
    }

    /**
     * Debug a mismatch
     * @param reason the reason for the mismatch
     * @param sender the sender who sent the command
     */
    default void debugMismatch(String reason, DecreeSender sender) {
        if (system().isDebugMismatchReason()) {
            parent().debug("Name " + C.YELLOW + getName() + C.GREEN + " invalid for sender " + C.YELLOW + sender.getName() + C.GREEN + " because of " + C.YELLOW + reason, C.GREEN);
        }
    }

    /**
     * Match against only a sender. Basically an is-allowed check.
     * @param sender The sender to check against
     * @return True if permitted & origin matches
     */
    default boolean doesMatch(DecreeSender sender) {
        return doesMatch(null, sender);
    }

    /**
     * Deep check whether this node matches input and is allowed for a sender<br>
     * @param sender The sender that called the node
     * @param in The input string
     * @return True if allowed & match, false if not
     */
    default boolean doesMatch(String in, DecreeSender sender){
        if (!getOrigin().validFor(sender)) {
            debugMismatch("Origin Mismatch", sender);
            return false;
        }
        if (!sender.hasPermission(getPermission())) {
            debugMismatch("Permission Mismatch", sender);
            return false;
        }

        String compare = "Comparison: " + C.YELLOW + in + C.GREEN + " with " + C.YELLOW + getNames().toString(", ") + C.GREEN + ": ";

        if (in == null || in.isEmpty()) {
            parent().debug(compare + "MATCHED", C.GREEN);
            return true;
        }

        for (String i : getNames()) {
            if (i.equalsIgnoreCase(in) || i.toLowerCase().contains(in.toLowerCase()) || in.toLowerCase().contains(i.toLowerCase())) {
                if (system().isDebugMatching()) {
                    parent().debug(compare + "MATCHED", C.GREEN);
                }
                return true;
            }
        }

        if (system().isDebugMatching()) {
            parent().debug(compare + C.RED + "NO MATCH", C.GREEN);
        }
        return false;
    }
}