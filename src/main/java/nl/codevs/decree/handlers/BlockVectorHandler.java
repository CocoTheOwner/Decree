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

import nl.codevs.decree.DecreeSystem;
import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.util.DecreeSender;
import nl.codevs.decree.util.Form;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidParameterException;

public class BlockVectorHandler implements DecreeParameterHandler<BlockVector> {
    @Override
    public @NotNull KList<BlockVector> getPossibilities() {
        KList<BlockVector> vx = new KList<>();
        DecreeSender s = DecreeSystem.Context.get();

        if (s.isPlayer()) {
            vx.add(s.player().getLocation().toVector().toBlockVector());
        }

        return vx;
    }

    @Override
    public String toString(BlockVector v) {
        if (v.getY() == 0) {
            return Form.f(v.getBlockX(), 2) + "," + Form.f(v.getBlockZ(), 2);
        }

        return Form.f(v.getBlockX(), 2) + "," + Form.f(v.getBlockY(), 2) + "," + Form.f(v.getBlockZ(), 2);
    }

    @SuppressWarnings({"RedundantThrows", "SpellCheckingInspection"})
    @Override
    public @NotNull BlockVector parse(String in, boolean force) throws DecreeParsingException, DecreeWhichException {
        try {
            if (in.contains(",")) {
                String[] comp = in.split("\\Q,\\E");

                if (comp.length == 2) {
                    return new BlockVector(Integer.parseInt(comp[0].trim()), 0, Integer.parseInt(comp[1].trim()));
                } else if (comp.length == 3) {
                    return new BlockVector(Integer.parseInt(comp[0].trim()),
                            Integer.parseInt(comp[1].trim()),
                            Integer.parseInt(comp[2].trim()));
                } else {
                    throw new DecreeParsingException("Could not parse components for vector. You have " + comp.length + " components. Expected 2 or 3.");
                }
            } else if (in.equalsIgnoreCase("here") || in.equalsIgnoreCase("me") || in.equalsIgnoreCase("self")) {
                if (!DecreeSystem.Context.get().isPlayer()) {
                    throw new DecreeParsingException("You cannot specify me,self,here as a console.");
                }

                return DecreeSystem.Context.get().player().getLocation().toVector().toBlockVector();
            } else if (in.equalsIgnoreCase("look") || in.equalsIgnoreCase("cursor") || in.equalsIgnoreCase("crosshair")) {
                if (!DecreeSystem.Context.get().isPlayer()) {
                    throw new DecreeParsingException("You cannot specify look, cursor, crosshair as a console.");
                }
                Block target = DecreeSystem.Context.get().player().getTargetBlockExact(256, FluidCollisionMode.NEVER);
                if (target == null) {
                    throw new InvalidParameterException(in + " is invalid because the targeted location is null");
                }
                return target.getLocation().toVector().toBlockVector();
            } else if (in.trim().toLowerCase().startsWith("player:")) {
                String v = in.trim().split("\\Q:\\E")[1];

                KList<?> px = DecreeSystem.Handler.get(Player.class).getPossibilities(v);

                if (px != null && px.isNotEmpty()) {
                    return ((Player) px.get(0)).getLocation().toVector().toBlockVector();
                } else {
                    throw new DecreeParsingException("Cannot find player: " + v);
                }
            } else {
                throw new InvalidParameterException(in + " is invalid because it has no ',' - BlockVectors are written as (number,number)");
            }
        } catch (Throwable e) {
            throw new DecreeParsingException("Unable to get Vector for \"" + in + "\" because of an uncaught exception: " + e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(BlockVector.class);
    }

    @Override
    public String getRandomDefault() {
        return Maths.r(0.5) ? "0,0" : "0,0,0";
    }
}