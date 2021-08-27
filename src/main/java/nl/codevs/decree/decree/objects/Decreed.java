package nl.codevs.decree.decree.objects;

import nl.codevs.decree.decree.DecreeSender;
import nl.codevs.decree.decree.util.KList;

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
     * Which auto-completions to display
     * @param args The arguments parsed with the command
     * @param sender The sender of the command
     * @return The auto-completions in a {@link KList}
     */
    KList<String> tab(KList<String> args, DecreeSender sender);

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
        return new KList<>(decree().aliases()).removeDuplicates().qremoveIf(String::isEmpty).qadd(getName());
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