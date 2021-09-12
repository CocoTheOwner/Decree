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


import nl.codevs.decree.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.util.KList;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface DecreeParameterHandler<T> {
    /**
     * Should return the possible values for this type
     *
     * @return Possibilities for this type.
     */
    KList<T> getPossibilities();

    /**
     * Converting the type back to a string (inverse of the {@link #parse(String) parse} method)
     *
     * @param t The input of the designated type to convert to a String
     * @return The resulting string
     */
    String toString(T t);

    /**
     * Return a random value that may be entered
     * @return A random default value
     */
    String getRandomDefault();

    /**
     * Forces conversion to the designated type before converting to a string using {@link #toString(T t)}
     *
     * @param t The object to convert to string (that should be of this type)
     * @return The resulting string.
     */
    @SuppressWarnings("unchecked")
    default String toStringForce(Object t) {
        return toString((T) t);
    }

    /**
     * Parse an input string to an output of the assigned type
     * @param in The input string
     * @return The output type
     * @throws DecreeParsingException When the input cannot be parsed into the output
     * @throws DecreeWhichException Multiple outputs would be possible for the same input
     */
    default T parse(String in) throws DecreeParsingException, DecreeWhichException {
        return parse(in, false);
    }


    /**
     * Should parse a String into the designated type
     * @param in The string to parse
     * @param force Force an option instead of throwing a {@link DecreeWhichException} if possible (can allow it throwing!)
     * @return The value extracted from the string, of the designated type
     * @throws DecreeParsingException Thrown when the parsing fails (ex: "oop" translated to an integer throws this)
     * @throws DecreeWhichException   Thrown when multiple results are possible
     */
    @SuppressWarnings("RedundantThrows")
    T parse(String in, boolean force) throws DecreeParsingException, DecreeWhichException;

    /**
     * Returns whether a certain type is supported by this handler<br>
     *
     * @param type The type to check
     * @return True if supported, false if not
     */
    boolean supports(Class<?> type);

    /**
     * The possible entries for the inputted string (support for autocomplete on partial entries)
     *
     * @param input The inputted string to check against
     * @return A {@link List} of possibilities
     */
    default KList<T> getPossibilities(String input) {
        if (input.trim().isEmpty()) {
            KList<T> f = getPossibilities();
            return f == null ? new KList<>() : f;
        }

        input = input.trim();
        KList<T> possible = getPossibilities();
        KList<T> matches = new KList<>();

        if (possible == null || possible.isEmpty()) {
            return matches;
        }

        if (input.isEmpty()) {
            return getPossibilities();
        }

        List<String> converted = possible.convert(v -> toString(v).trim());

        for (int i = 0; i < converted.size(); i++) {
            String g = converted.get(i);
            // if
            // G == I or
            // I in G or
            // G in I
            if (g.equalsIgnoreCase(input) || g.toLowerCase().contains(input.toLowerCase()) || input.toLowerCase().contains(g.toLowerCase())) {
                matches.add(possible.get(i));
            }
        }

        return matches;
    }

    /**
     * Calculate integer multiplier value for an input<br>
     * Values used are<br>
     * - k > 1.000<br>
     * - m > 1.000.000<br>
     * - r > 512<br>
     * - h > 100<br>
     * - c > 16<br>
     * ! This does not return the actual value, just the multiplier!
     * @param value The inputted value
     * @return The multiplier
     */
    default int getMultiplier(AtomicReference<String> value) {
        int multiplier = 1;
        String in = value.get();
        boolean valid = true;
        while (valid) {
            boolean trim = false;
            if (in.toLowerCase().endsWith("k")) {
                multiplier *= 1000;
                trim = true;
            } else if (in.toLowerCase().endsWith("m")) {
                multiplier *= 1000000;
                trim = true;
            } else if (in.toLowerCase().endsWith("h")) {
                multiplier *= 100;
                trim = true;
            } else if (in.toLowerCase().endsWith("c")) {
                multiplier *= 16;
                trim = true;
            } else if (in.toLowerCase().endsWith("r")) {
                multiplier *= (16 * 32);
                trim = true;
            } else {
                valid = false;
            }

            if (trim) {
                in = in.substring(0, in.length() - 1);
            }
        }

        value.set(in);
        return multiplier;
    }
}
