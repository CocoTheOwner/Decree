package nl.codevs.decree.decree.objects;

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
     * The help information for this Decreed
     */
    String help();

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
     * Get the primary name of the node
     */
    default String getName() {
        return decree().name();
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
        if (parent() == null) {
            return "/" + getName();
        }
        return parent().getPath() + " " + getName();
    }

    /**
     * Get whether a string matches this node or not
     * @param in The string to check with
     */
    default boolean matches(String in) {

        for (String i : getNames()) {
            if (i.equalsIgnoreCase(in)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get whether a string matches this node on a deep match (a in b or b in a)
     * @param in The string to check with
     */
    default boolean deepMatches(String in) {

        if (in.isEmpty()) {
            return true;
        }

        if (matches(in)){
            return true;
        }

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
     * (node in in) || (in in node)
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
}