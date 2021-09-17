package nl.codevs.decree;

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
     * Get all decreed for some arguments
     * @param args The arguments to parse
     * @param sender The sender to parse for
     */
    KList<Decreed> get(KList<String> args, DecreeSender sender);

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
     * Deep check whether this node matches input and is allowed for a sender<br>
     * @param sender The sender that called the node
     * @param in The input string
     * @return True if allowed & match, false if not
     */
    default boolean doesMatch(String in, DecreeSender sender){
        String reason;
        if (getOrigin().validFor(sender)) {
            if (sender.hasPermission(getPermission())) {

                if (in.isEmpty()) {
                    return true;
                }

                String compare = "Comparison: " + C.YELLOW + in + C.GREEN + " with " + C.YELLOW + getNames().toString(", ") + C.GREEN + ": ";
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

            } else {
                reason = "No Permission";
            }
        } else if (!sender.hasPermission(getPermission())) {
            reason = "No Permission & Origin Mismatch";
        } else {
            reason = "Origin Mismatch";
        }
        if (system().isDebugMismatchReason()) {
            debug("Name " + C.YELLOW + in + C.GREEN + " invalid for sender (" + C.YELLOW + sender.getName() + C.GREEN + ") because of " + C.YELLOW + reason, C.GREEN);
        }
        return false;
    }
}