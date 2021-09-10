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


/**
 * The origin from which the {@link Decree} command must come
 */
public enum DecreeOrigin {
    PLAYER,
    CONSOLE,
    /**
     * Both the player and the console
     */
    BOTH;

    /**
     * Check if the {@link DecreeOrigin} is valid for a sender
     *
     * @param sender The {@link DecreeSender} to check
     * @return True if valid for this {@link DecreeOrigin}
     */
    public boolean validFor(DecreeSender sender) {
        if (sender.isPlayer()) {
            return this.equals(PLAYER) || this.equals(BOTH);
        } else {
            return this.equals(CONSOLE) || this.equals(BOTH);
        }
    }
}
