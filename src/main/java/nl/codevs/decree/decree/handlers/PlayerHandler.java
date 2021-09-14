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
import nl.codevs.decree.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.util.KList;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PlayerHandler implements DecreeParameterHandler<Player> {
    @Override
    public @NotNull KList<Player> getPossibilities() {
        return new KList<>(new ArrayList<>(Bukkit.getOnlinePlayers()));
    }

    @Override
    public String toString(Player player) {
        return player.getName();
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public Player parse(String in, boolean force) throws DecreeParsingException, DecreeWhichException {
        try {
            KList<Player> options = getPossibilities(in);
            KList<String> names = getPossibilities().convert(HumanEntity::getName);

            if (!names.contains("self") && in.equals("self") && DecreeContext.get().isPlayer()){
                return DecreeContext.get().player();
            }
            if (!names.contains("me") && in.equals("me") && DecreeContext.get().isPlayer()){
                return DecreeContext.get().player();
            }
            if (!names.contains("random") && in.equals("random")){
                return options.getRandom();
            }

            if (options.isEmpty()) {
                throw new DecreeParsingException("Unable to find Player \"" + in + "\"");
            } else if (options.size() > 1) {
                if (force) {
                    return options.getRandom();
                }
                throw new DecreeWhichException(Player.class, in, options);
            }

            return options.get(0);
        } catch (Throwable e) {
            throw new DecreeParsingException("Unable to find Player \"" + in + "\" because of an uncaught exception: " + e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Player.class);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private final KList<String> defaults = new KList<>(
            "playername",
            "self",
            "random"
    );

    @Override
    public String getRandomDefault() {
        return defaults.getRandom();
    }
}
