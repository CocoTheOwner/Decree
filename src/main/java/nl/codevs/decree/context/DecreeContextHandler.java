package nl.codevs.decree.context;

import nl.codevs.decree.util.DecreeSender;

public interface DecreeContextHandler<T> {

    /**
     * The type this context handler handles
     * @return the type
     */
    boolean supports(Class<?> type);

    /**
     * The handler for this context. Can use any data found in the sender object for context derivation.
     * @param sender The sender whose data may be used
     * @return The value in the assigned type
     */
    T handle(DecreeSender sender);

    /**
     * Use #handle to return the current string name
     * @return The string name (or whatever) for this context's return
     */
    default String handleToString(DecreeSender sender) {
        return handle(sender).toString();
    }
}
