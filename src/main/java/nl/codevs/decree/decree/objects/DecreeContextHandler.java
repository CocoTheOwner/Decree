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

import nl.codevs.decree.decree.context.WorldContextHandler;
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
