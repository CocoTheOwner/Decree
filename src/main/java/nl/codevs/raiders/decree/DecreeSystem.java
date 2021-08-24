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

package nl.codevs.raiders.decree;



import nl.codevs.raiders.decree.exceptions.DecreeException;
import nl.codevs.raiders.decree.handlers.*;
import nl.codevs.raiders.decree.util.AtomicCache;
import nl.codevs.raiders.decree.util.C;
import nl.codevs.raiders.decree.util.KList;
import nl.codevs.raiders.decree.virtual.VirtualDecreeCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DecreeSystem extends CommandExecutor, TabCompleter {
    AtomicCache<VirtualDecreeCommand> commandCache = new AtomicCache<>();
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
    DecreeExecutor getRootClass();

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

    default VirtualDecreeCommand getRoot() {
        return commandCache.aquire(() -> {
            try {
                return VirtualDecreeCommand.createRoot(getRootClass(), this);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    default boolean call(DecreeSender sender, String[] args) {
        DecreeContext.touch(sender);
        return getRoot().invoke(sender, enhanceArgs(args));
    }

    default List<String> decreeTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        KList<String> enhanced = new KList<>(args);
        KList<String> v = getRoot().tabComplete(enhanced, enhanced.toString(" "));
        v.removeDuplicates();
        return v;
    }

    default boolean decreeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        Bukkit.getScheduler().scheduleAsyncDelayedTask(instance(), () -> {
            if (!call(new DecreeSender(sender, instance(), this), args)) {
                sender.sendMessage(C.RED + "Unknown Decree Command");
            }
        });
        return true;
    }

    static KList<String> enhanceArgs(String[] args) {
        return enhanceArgs(args, true);
    }

    static KList<String> enhanceArgs(String[] args, boolean trim) {
        KList<String> a = new KList<>();

        if (args.length == 0) {
            return a;
        }

        StringBuilder flat = new StringBuilder();
        for (String i : args) {
            if (trim) {
                if (i.trim().isEmpty()) {
                    continue;
                }

                flat.append(" ").append(i.trim());
            } else {
                if (i.endsWith(" ")) {
                    flat.append(" ").append(i.trim()).append(" ");
                }
            }
        }

        flat = new StringBuilder(flat.length() > 0 ? trim ? flat.toString().trim().length() > 0 ? flat.substring(1).trim() : flat.toString().trim() : flat.substring(1) : flat);
        StringBuilder arg = new StringBuilder();
        boolean quoting = false;

        for (int x = 0; x < flat.length(); x++) {
            char i = flat.charAt(x);
            char j = x < flat.length() - 1 ? flat.charAt(x + 1) : i;
            boolean hasNext = x < flat.length();

            if (i == ' ' && !quoting) {
                if (!arg.toString().trim().isEmpty() && trim) {
                    a.add(arg.toString().trim());
                    arg = new StringBuilder();
                }
            } else if (i == '"') {
                if (!quoting && (arg.length() == 0)) {
                    quoting = true;
                } else if (quoting) {
                    quoting = false;

                    if (hasNext && j == ' ') {
                        if (!arg.toString().trim().isEmpty() && trim) {
                            a.add(arg.toString().trim());
                            arg = new StringBuilder();
                        }
                    } else if (!hasNext) {
                        if (!arg.toString().trim().isEmpty() && trim) {
                            a.add(arg.toString().trim());
                            arg = new StringBuilder();
                        }
                    }
                }
            } else {
                arg.append(i);
            }
        }

        if (!arg.toString().trim().isEmpty() && trim) {
            a.add(arg.toString().trim());
        }

        return a;
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