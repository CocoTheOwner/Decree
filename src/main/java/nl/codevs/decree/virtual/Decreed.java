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
     * Tab completions
     * @param args Command arguments
     * @param sender Command sender
     * @return Tab completions for this node
     */
    KList<String> tab(KList<String> args, DecreeSender sender);

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
        return (parent() == null || parent().getPermission().equals(Decree.NO_PERMISSION) ? "" : parent().getPermission() + ".") + decree().permission();
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
        return decree().sync() || (parent() != null && parent().isSync());
    }

    /**
     * Send a debug message
     * @param message The message
     * @param color The color to prefix with
     */
    default void debug(String message, C color) {
        system().debug(C.GOLD + (parent() == null ? "" : parent().getPath() + " ") + color + "→ " + C.GOLD + getShortestName() + color + " | " + message);
    }

    /**
     * Get the primary and alias names of the node<br>
     */
    default KList<String> getNames() {
        KList<String> names = new KList<>(getName());
        names.addAll(Arrays.asList(decree().aliases()));
        return names.qremoveIf(String::isEmpty).qremoveDuplicates().convert(this::capitalToLine);
    }

    /**
     * Get the shortest name for this node (includes aliases)
     */
    default String getShortestName() {
        String shortest = getName();
        for (String name : getNames()) {
            if (name.length() < shortest.length()) {
                shortest = name;
            }
        }
        return shortest;
    }

    /**
     * Get the command path to this node
     */
    default String getPath() {

        if (parent() == null) {
            return "/" + getShortestName();
        }
        return parent().getPath() + " " + getShortestName();
    }

    /**
     * Debug a mismatch
     * @param reason the reason for the mismatch
     * @param sender the sender who sent the command
     */
    default void debugMismatch(String reason, DecreeSender sender) {
        if (DecreeSystem.settings.debugMatching) {
            parent().debug("Hiding decreed " + C.GOLD + getShortestName() + C.GREEN + " for sender " + C.GOLD + sender.getName() + C.GREEN + " because of " + C.GOLD + reason, C.GREEN);
        }
    }

    /**
     * Match against only a sender. Basically an is-allowed check.
     * @param sender The sender to check against
     * @return True if permitted & origin matches
     */
    default int doesMatch(DecreeSender sender) {
        return doesMatch(null, sender);
    }

    /**
     * Deep check whether this node matches input and is allowed for a sender<br>
     * @param sender The sender that called the node
     * @param in The input string
     * @return
     * 0 If not allowed<br>
     * 1 if allowed and the input contains a name<br>
     * 2 if allowed and a name contains the input<br>
     * 3 if allowed and the input is a 1:1 match with a name, or the input is null/empty.<br>
     * <i>Name/names includes aliases.</i><br>
     * Higher number = better
     *
     */
    default int doesMatch(String in, DecreeSender sender){
        if (!getOrigin().validFor(sender)) {
            debugMismatch("Origin Mismatch - 0", sender);
            return 0;
        }
        if (!sender.hasPermission(getPermission())) {
            debugMismatch("Permission Mismatch - 0", sender);
            return 0;
        }

        String compare = "Comparison: " + C.GOLD + in + C.GREEN + " with " + C.GOLD + getNames().toString(C.GREEN + ", " + C.GOLD) + C.GREEN + ": ";

        if (in == null || in.isEmpty()) {
            if (DecreeSystem.settings.debugMatching) {
                parent().debug(compare + "MATCHED - 3", C.GREEN);
            }
            return 3;
        }

        for (String i : getNames()) {
            if (i.equalsIgnoreCase(in)) {
                if (DecreeSystem.settings.debugMatching) {
                    parent().debug(compare + "MATCHED - 3", C.GREEN);
                }
                return 3;
            }
            if (i.toLowerCase().contains(in.toLowerCase())) {
                if (DecreeSystem.settings.debugMatching) {
                    parent().debug(compare + "MATCHED - 2", C.GREEN);
                }
                return 2;
            }
            if (in.toLowerCase().contains(i.toLowerCase())) {
                if (DecreeSystem.settings.debugMatching) {
                    parent().debug(compare + "MATCHED - 1", C.GREEN);
                }
                return 1;
            }
        }

        if (DecreeSystem.settings.debugMatching) {
            parent().debug(compare + C.RED + "NO MATCH - 0", C.GREEN);
        }
        return 0;
    }

    /**
     * Replace all capital letters in a string with a '-' and lowercase representation
     * @param string The string to convert 'IMineDiamondsForFun'
     * @return The converted string 'i-mine-diamonds-for-fun'
     */
    default String capitalToLine(String string) {
        char[] chars = string.toCharArray();
        StringBuilder name = new StringBuilder();
        for (char aChar : chars) {
            if (Character.isUpperCase(aChar)) {
                name.append("-").append(Character.toLowerCase(aChar));
            } else {
                name.append(aChar);
            }
        }
        String result = name.toString();
        return result.startsWith("-") ? result.substring(1) : result;
    }
}