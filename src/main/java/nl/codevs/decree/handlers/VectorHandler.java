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

package nl.codevs.decree.handlers;


import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.util.Form;
import nl.codevs.decree.util.KList;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class VectorHandler implements DecreeParameterHandler<Vector> {

    private static final KList<String> randoms = new KList<>(
            "here",
            "0,0,0",
            "0,0",
            "look",
            "player:<name>"
    );

    @Override
    public KList<Vector> getPossibilities() {
        return null;
    }

    @Override
    public String toString(Vector v) {
        if (v.getY() == 0) {
            return Form.f(v.getX(), 2) + "," + Form.f(v.getZ(), 2);
        }

        return Form.f(v.getX(), 2) + "," + Form.f(v.getY(), 2) + "," + Form.f(v.getZ(), 2);
    }

    @Override
    public @NotNull Vector parse(String in, boolean force) throws DecreeParsingException, DecreeWhichException {
        return new BlockVectorHandler().parse(in, force);
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Vector.class);
    }

    @Override
    public String getRandomDefault() {
        return randoms.getRandom();
    }
}
