package nl.codevs.decree.decree.objects;

import nl.codevs.decree.decree.util.ChronoLatch;
import nl.codevs.decree.decree.util.KList;

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
     * Add the {@link DecreeSender} to the context map
     * @param sender The sender
     */
    public static void touch(DecreeSender sender) {
        synchronized (context) {
            context.put(Thread.currentThread(), sender);

            if (cl.flip()) {
                for (Thread i : contextKeys()) {
                    if (!i.isAlive()) {
                        context.remove(i);
                    }
                }
            }
        }
    }

    /**
     * Get all keys in the context map
     * @return All context keys (threads)
     */
    private static KList<Thread> contextKeys() {
        KList<Thread> k = new KList<>();
        Enumeration<Thread> kk = DecreeContext.context.keys();

        while (kk.hasMoreElements()) {
            k.add(kk.nextElement());
        }

        return k;
    }
}
