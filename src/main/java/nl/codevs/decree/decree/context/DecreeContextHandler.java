package nl.codevs.decree.decree.context;

import nl.codevs.decree.decree.objects.DecreeSender;
import nl.codevs.decree.decree.util.KList;

import java.util.concurrent.ConcurrentHashMap;

public interface DecreeContextHandler<T> {

    /**
     * Add all context handlers to this list
     */
    KList<DecreeContextHandler<?>> handlers = new KList<>(
            new WorldContextHandler()
    );

    ConcurrentHashMap<Class<?>, DecreeContextHandler<?>> contextHandlers = buildContextHandlers();

    static ConcurrentHashMap<Class<?>, DecreeContextHandler<?>> buildContextHandlers() {
        ConcurrentHashMap<Class<?>, DecreeContextHandler<?>> contextHandlers = new ConcurrentHashMap<>();

        handlers.forEach(h -> contextHandlers.put(h.getType(), h));

        return contextHandlers;
    }

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
