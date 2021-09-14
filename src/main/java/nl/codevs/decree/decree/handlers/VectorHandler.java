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


import nl.codevs.decree.decree.objects.DecreeContext;
import nl.codevs.decree.decree.DecreeSystem;
import nl.codevs.decree.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.util.Form;
import nl.codevs.decree.decree.util.KList;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.security.InvalidParameterException;

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

    @SuppressWarnings({"RedundantThrows", "SpellCheckingInspection"})
    @Override
    public Vector parse(String in, boolean force) throws DecreeParsingException, DecreeWhichException {
        try {
            if (in.contains(",")) {
                String[] comp = in.split("\\Q,\\E");

                if (comp.length == 2) {
                    return new BlockVector(Double.parseDouble(comp[0].trim()), 0, Double.parseDouble(comp[1].trim()));
                } else if (comp.length == 3) {
                    return new BlockVector(Double.parseDouble(comp[0].trim()),
                            Double.parseDouble(comp[1].trim()),
                            Double.parseDouble(comp[2].trim()));
                } else {
                    throw new DecreeParsingException("Could not parse components for vector. You have " + comp.length + " components. Expected 2 or 3.");
                }
            } else if (in.equalsIgnoreCase("here") || in.equalsIgnoreCase("me") || in.equalsIgnoreCase("self")) {
                if (!DecreeContext.get().isPlayer()) {
                    throw new DecreeParsingException("You cannot specify me,self,here as a console.");
                }

                return DecreeContext.get().player().getLocation().toVector();
            } else if (in.equalsIgnoreCase("look") || in.equalsIgnoreCase("cursor") || in.equalsIgnoreCase("crosshair")) {
                if (!DecreeContext.get().isPlayer()) {
                    throw new DecreeParsingException("You cannot specify look,cursor,crosshair as a console.");
                }
                Block target = DecreeContext.get().player().getTargetBlockExact(256, FluidCollisionMode.NEVER);
                if (target == null) {
                    return null;
                }
                return target.getLocation().toVector();

            } else if (in.trim().toLowerCase().startsWith("player:")) {
                String v = in.trim().split("\\Q:\\E")[1];

                KList<?> px = DecreeSystem.getHandler(Player.class).getPossibilities(v);

                if (px != null && px.isNotEmpty()) {
                    return ((Player) px.get(0)).getLocation().toVector();
                } else {
                    throw new DecreeParsingException("Cannot find player: " + v);
                }
            } else {
                throw new InvalidParameterException(in + " is invalid because it has no ',' - Vectors are written as (number,number)");
            }
        } catch (Throwable e) {
            throw new DecreeParsingException("Unable to get Vector for \"" + in + "\" because of an uncaught exception: " + e);
        }
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
