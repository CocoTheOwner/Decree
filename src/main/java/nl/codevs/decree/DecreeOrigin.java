package nl.codevs.decree;


/**
 * The origin from which the {@link Decree} command must come
 */
public enum DecreeOrigin {
    PLAYER,
    CONSOLE,
    /**
     * Both the player and the console
     */
    BOTH;

    /**
     * Check if the {@link DecreeOrigin} is valid for a sender
     *
     * @param sender The {@link DecreeSender} to check
     * @return True if valid for this {@link DecreeOrigin}
     */
    public boolean validFor(DecreeSender sender) {
        if (sender.isPlayer()) {
            return this.equals(PLAYER) || this.equals(BOTH);
        } else {
            return this.equals(CONSOLE) || this.equals(BOTH);
        }
    }
}
