package nl.codevs.decree.decree.objects;

import nl.codevs.decree.decree.DecreeSystem;
import nl.codevs.decree.decree.util.C;
import nl.codevs.decree.decree.util.KList;

import java.util.Arrays;

public interface Decreed {

    /**
     * The parent node of this node. Null if origin.
     */
    Decreed parent();

    /**
     * Decree access for this interface
     */
    Decree decree();

    /**
     * Get the primary name of the node
     */
    String getName();

    /**
     * Get the {@link DecreeSystem} managing this decreed
     */
    DecreeSystem system();

    /**
     * Tab auto-completions
     * @param args The arguments left to parse
     * @param sender The sender of the command
     * @return Auto-completions
     */
    KList<String> tab(KList<String> args, DecreeSender sender);

    /**
     * Invocation on command run
     * @param args The arguments left to parse
     * @param sender The sender that sent the command
     * @return True if successfully matched
     */
    boolean invoke(KList<String> args, DecreeSender sender);

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
     * Get the primary and alias names of the node<br>
     */
    default KList<String> getNames() {
        KList<String> names = new KList<>(getName());
        names.addAll(Arrays.asList(decree().aliases()));
        return names.removeDuplicates().qremoveIf(String::isEmpty);
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
        return decree().sync();
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
     * Get whether a string matches this node or not
     * @param in The string to check with
     */
    default boolean matches(String in) {

        debug("DeepComparing: " + C.DECREE + in + C.RESET + " with " + C.DECREE + getNames().toString(", "));
        for (String i : getNames()) {
            if (i.equalsIgnoreCase(in)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get whether a string matches this node on a deep match (ABBA in AB or AB in ABBA)
     * @param in The string to check with
     */
    default boolean deepMatches(String in) {

        if (in.isEmpty()) {
            return true;
        }

        if (matches(in)){
            return true;
        }

        parent().debug("DeepComparing: " + C.DECREE + in + C.RESET + " with " + C.DECREE + getNames().toString(", "));
        for (String i : getNames()) {
            if (i.toLowerCase().contains(in.toLowerCase()) || in.toLowerCase().contains(i.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Shallow check whether this node matches input and is allowed for a sender<br>
     * in == node
     * @param sender The sender that called the node
     * @param in The input string
     * @return True if allowed & match, false if not
     */
    default boolean doesMatchAllowed(DecreeSender sender, String in) {
        if (getOrigin().validFor(sender) && sender.hasPermission(getPermission())) {
            return matches(in);
        }
        return false;
    }

    /**
     * Deep check whether this node matches input and is allowed for a sender<br>
     * (node element-of in) || (in element-of node)
     * @param sender The sender that called the node
     * @param in The input string
     * @return True if allowed & match, false if not
     */
    default boolean doesDeepMatchAllowed(DecreeSender sender, String in){
        if (getOrigin().validFor(sender) && sender.hasPermission(getPermission())) {
            return deepMatches(in);
        }
        return false;
    }

    /**
     * Send a debug message
     * @param message The message
     */
    default void debug(String message) {
        debug(message, C.DECREE);
    }

    /**
     * Send a debug message
     * @param message The message
     * @param color The color to prefix with
     */
    default void debug(String message, C color) {
        system().debug(color + "C: " + getName() + " | Path: " + getPath() + " | " + message);
    }
}