package nl.codevs.decree.util;

public class ChronoLatch {
    private final long interval;
    private long since;

    public ChronoLatch(long interval) {
        this.interval = interval;
        since = System.currentTimeMillis() - interval * 2;
    }

    public boolean flip() {
        if (System.currentTimeMillis() - since > interval) {
            since = System.currentTimeMillis();
            return true;
        }

        return false;
    }
}
