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

import nl.codevs.decree.decree.DecreeSender;
import nl.codevs.decree.decree.util.C;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Represents a decree executor, the interface all decree commands must implement
 */
public interface DecreeNodeExecutor {
    /**
     * @return The command sender
     */
    default DecreeSender sender() {
        return DecreeContext.get();
    }

    /**
     * @return Underlying player of the sender (null if not a player)
     */
    default Player player() {
        return sender().player();
    }

    /**
     * @return World where underlying player resides (null if not a player)
     */
    default World world() {
        return sender().isPlayer() ? player().getWorld() : null;
    }

    /**
     * @param message The message to send to the sender
     */
    default void message(String message){
        sender().sendMessage(message);
    }

    /**
     * @param message The error to send to the sender (RED)
     */
    default void error(String message){
        sender().sendMessage(C.RED + message);
    }

    /**
     * @param message The message to send to the sender (YELLOW)
     */
    default void warn(String message){
        sender().sendMessage(C.YELLOW + message);
    }

    /**
     * @param message The message to send to the sender (GREEN)
     */
    default void success(String message){
        sender().sendMessage(C.GREEN + message);
    }
}
