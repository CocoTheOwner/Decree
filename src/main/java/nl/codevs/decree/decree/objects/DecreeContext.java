/*
 * Iris is a World Generator for Minecraft Bukkit Servers
 * Copyright (c) 2021 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
