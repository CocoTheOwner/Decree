package nl.codevs.decree.decree.objects;

import nl.codevs.decree.decree.util.ChronoLatch;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class DecreeContext {
    private static final ChronoLatch cl = new ChronoLatch(60000);
    private static final ConcurrentHashMap<Thread, DecreeSender> context = new ConcurrentHashMap<>();

    /**
     * Get the sender from the current thread's context
     * @return The {@link DecreeSender} for this thread
     */
    public static DecreeSender get() {
        return context.get(Thread.currentThread());
    }

    /**
     * Add the {@link DecreeSender} to the context map & removes dead threads
     * @param sender The sender
     */
    public static void touch(DecreeSender sender) {
        synchronized (context) {
            context.put(Thread.currentThread(), sender);

            if (cl.flip()) {
                Enumeration<Thread> contextKeys = DecreeContext.context.keys();

                while (contextKeys.hasMoreElements()) {
                    Thread thread = contextKeys.nextElement();
                    if (!thread.isAlive()) {
                        context.remove(thread);
                    }
                }
            }
        }
    }
}
