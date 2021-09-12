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

package nl.codevs.decree.decree.util;

/**
 * Math
 *
 * @author cyberpwn
 */
@SuppressWarnings("SpellCheckingInspection")
public class Maths {

    /**
     * Get true or false based on random percent
     *
     * @param d between 0 and 1
     * @return true if true
     */
    public static boolean r(Double d) {
        //noinspection ReplaceNullCheck
        if (d == null) {
            return Math.random() < 0.5;
        }

        return Math.random() < d;
    }

    /**
     * Get true or false, randomly
     */
    public static boolean r() {
        return Math.random() < 0.5;
    }

    /**
     * Get a random int from to (inclusive)
     *
     * @param f the lower bound
     * @param t the upper bound
     * @return the value
     */
    public static int irand(int f, int t) {
        return f + (int) (Math.random() * ((t - f) + 1));
    }

    /**
     * Get a random float from to (inclusive)
     *
     * @param f the lower bound
     * @param t the upper bound
     * @return the value
     */
    public static float frand(float f, float t) {
        return f + (float) (Math.random() * ((t - f) + 1));
    }

    /**
     * Get a random double from to (inclusive)
     *
     * @param f the lower bound
     * @param t the upper bound
     * @return the value
     */
    public static double drand(double f, double t) {
        return f + (Math.random() * ((t - f) + 1));
    }
}
