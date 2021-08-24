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

package nl.codevs.raiders.decree;

import nl.codevs.raiders.decree.context.WorldContextHandler;
import nl.codevs.raiders.decree.util.KList;

import java.util.concurrent.ConcurrentHashMap;

public interface DecreeContextHandler<T> {

    KList<DecreeContextHandler<?>> handlers = new KList<>(
            new WorldContextHandler()
    );

    ConcurrentHashMap<Class<?>, DecreeContextHandler<?>> contextHandlers = buildContextHandlers();

    static ConcurrentHashMap<Class<?>, DecreeContextHandler<?>> buildContextHandlers() {
        ConcurrentHashMap<Class<?>, DecreeContextHandler<?>> contextHandlers = new ConcurrentHashMap<>();



        handlers.forEach(h -> contextHandlers.put(h.getType(), h));

        return contextHandlers;
    }

    Class<T> getType();

    T handle(DecreeSender sender);
}
