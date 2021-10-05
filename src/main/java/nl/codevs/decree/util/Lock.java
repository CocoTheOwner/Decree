package nl.codevs.decree.util;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.concurrent.locks.ReentrantLock;

@Data
@Accessors(chain = true)
public class Lock {
    private transient final ReentrantLock lock;
    private transient final String name;
    private transient boolean disabled = false;

    public Lock(String name) {
        this.name = name;
        lock = new ReentrantLock(false);
    }

    public void lock() {
        if (disabled) {
            return;
        }

        lock.lock();
    }

    public void unlock() {
        if (disabled) {
            return;
        }
        try {
            lock.unlock();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
