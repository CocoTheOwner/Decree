package nl.codevs.decree.context;

import nl.codevs.decree.DecreeSender;

public interface DecreeContextHandler<T> {

    /**
     * The type this context handler handles
     * @return the type
     */
    Class<T> getType();

    /**
     * The handler for this context. Can use any data found in the sender object for context derivation.
     * @param sender The sender whose data may be used
     * @return The value in the assigned type
     */
    T handle(DecreeSender sender);
}
