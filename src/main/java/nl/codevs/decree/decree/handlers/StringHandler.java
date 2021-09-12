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

package nl.codevs.decree.decree.handlers;


import nl.codevs.decree.decree.objects.DecreeParameterHandler;
import nl.codevs.decree.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.decree.util.KList;

/**
 * Abstraction can sometimes breed stupidity
 */
public class StringHandler implements DecreeParameterHandler<String> {
    @Override
    public KList<String> getPossibilities() {
        return null;
    }

    @Override
    public String toString(String s) {
        return s;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public String parse(String in, boolean force) throws DecreeParsingException {
        return in;
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(String.class);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private final KList<String> defaults = new KList<>(
            "text",
            "string",
            "blah",
            "derp",
            "yolo"
    );

    @Override
    public String getRandomDefault() {
        return defaults.getRandom();
    }
}
