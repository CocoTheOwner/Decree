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

package nl.codevs.decree.decree;



import nl.codevs.decree.decree.exceptions.DecreeException;
import nl.codevs.decree.decree.handlers.*;
import nl.codevs.decree.decree.objects.*;
import nl.codevs.decree.decree.util.AtomicCache;
import nl.codevs.decree.decree.util.C;
import nl.codevs.decree.decree.util.KList;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DecreeSystem extends CommandExecutor, TabCompleter, Plugin {
    AtomicCache<DecreeCategory> commandCache = new AtomicCache<>();
    KList<DecreeParameterHandler<?>> handlers = new KList<>(
            new BlockVectorHandler(),
            new BooleanHandler(),
            new ByteHandler(),
            new DoubleHandler(),
            new FloatHandler(),
            new IntegerHandler(),
            new LongHandler(),
            new PlayerHandler(),
            new ShortHandler(),
            new StringHandler(),
            new VectorHandler(),
            new WorldHandler()
    );

    /**
     * The root class to start command searching from
     */
    DecreeCommandExecutor getRootClass();

    /**
     * Before you fill out these functions. Read the README.md file in the decree directory.
     *
     * @return The instance of the plugin that is running Decree (literal 'this')
     */
    Plugin instance();

    /**
     * What to do with debug messages
     * @param message The debug message
     */
    default void debug(String message) {
        Bukkit.getConsoleSender().sendMessage();
    }

    /**
     * Should return the root class as a virtual category.<br>
     * Uses {@link DecreeSystem}#getRootClass to retrieve the class<br>
     * Because of caching & performance issues, do not overwrite this, but that instead.
     * @return The {@link DecreeCategory}
     */
    default DecreeCategory getRoot() {
        return commandCache.aquire(() -> {
            try {
                return DecreeCategory.createOrigin(getRootClass(), this);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Decree tab completion
     * @param sender The sender that needs completion
     * @param command The command entered thus far
     * @param alias The alias for the command
     * @param args Arguments passed with the command
     * @return A List of strings representing options
     */
    default List<String> decreeTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        KList<String> enhanced = new KList<>(args);
        KList<String> v = getRoot().tabComplete(enhanced, enhanced.toString(" "), new DecreeSender(sender, instance(), this));
        v.removeDuplicates();
        return v;
    }


    default boolean decreeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        Bukkit.getScheduler().scheduleAsyncDelayedTask(instance(), () -> {
            DecreeSender decreeSender = new DecreeSender(sender, instance(), this);
            DecreeContext.touch(decreeSender);
            if (getRoot().invoke(decreeSender, enhanceArgs(args)).isFailed()) {
                decreeSender.sendMessage(C.RED + "Unknown Decree Command");
            }
        });
        return true;
    }

    /**
     * Enhance arguments into a {@link KList}
     * @param args The arguments to enhance
     * @return The enhanced args
     */
    static KList<String> enhanceArgs(String[] args) {
        KList<String> arguments = new KList<>();

        if (args.length == 0) {
            return arguments;
        }

        StringBuilder flat = new StringBuilder();
        for (String i : args) {
            if (i.trim().isEmpty()) {
                continue;
            }

            flat.append(" ").append(i.trim());
        }

        flat = new StringBuilder(flat.length() > 0 ? flat.toString().trim().length() > 0 ? flat.substring(1).trim() : flat.toString().trim() : flat);
        StringBuilder arg = new StringBuilder();
        boolean quoting = false;

        for (int x = 0; x < flat.length(); x++) {
            char i = flat.charAt(x);
            char j = x < flat.length() - 1 ? flat.charAt(x + 1) : i;
            boolean hasNext = x < flat.length();

            if (i == ' ' && !quoting) {
                if (!arg.toString().trim().isEmpty()) {
                    arguments.add(arg.toString().trim());
                    arg = new StringBuilder();
                }
            } else if (i == '"') {
                if (!quoting && (arg.length() == 0)) {
                    quoting = true;
                } else if (quoting) {
                    quoting = false;

                    if (hasNext && j == ' ') {
                        if (!arg.toString().trim().isEmpty()) {
                            arguments.add(arg.toString().trim());
                            arg = new StringBuilder();
                        }
                    } else if (!hasNext) {
                        if (!arg.toString().trim().isEmpty()) {
                            arguments.add(arg.toString().trim());
                            arg = new StringBuilder();
                        }
                    }
                }
            } else {
                arg.append(i);
            }
        }

        if (!arg.toString().trim().isEmpty()) {
            arguments.add(arg.toString().trim());
        }

        return arguments;
    }

    /**
     * Get the handler for the specified type
     *
     * @param type The type to handle
     * @return The corresponding {@link DecreeParameterHandler}, or null
     */
    static DecreeParameterHandler<?> getHandler(Class<?> type) throws DecreeException {
        for (DecreeParameterHandler<?> i : handlers) {
            if (i.supports(type)) {
                return i;
            }
        }
        throw new DecreeException("Unhandled type in Decree Parameter: " + type.getName() + ". This is bad! Please remove the parameter or add a handler for it");
    }
}