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

import lombok.Data;
import nl.codevs.decree.decree.exceptions.DecreeException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.handlers.*;
import nl.codevs.decree.decree.objects.*;
import nl.codevs.decree.decree.util.AtomicCache;
import nl.codevs.decree.decree.util.C;
import nl.codevs.decree.decree.util.KList;
import nl.codevs.decree.decree.util.Maths;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class DecreeSystem implements Listener {
    private final AtomicCache<DecreeVirtualCommand> commandCache = new AtomicCache<>();
    private final ConcurrentHashMap<String, CompletableFuture<String>> futures = new ConcurrentHashMap<>();
    private static final KList<DecreeParameterHandler<?>> handlers = new KList<>(
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
     * The root of the command tree as an instantiated class
     */
    private final DecreeCommandExecutor rootInstance;

    /**
     * The instance of the plugin
     */
    private final Plugin instance;

    /**
     * Whether to use command sounds or not
     */
    private final boolean commandSound = true;

    public DecreeSystem(DecreeCommandExecutor rootInstance, Plugin instance) {
        this.rootInstance = rootInstance;
        this.instance = instance;
    }

    /**
     * Handles the cases where there are multiple options following from the entered command values
     * @param e The event to check
     */
    @EventHandler
    public void on(PlayerCommandPreprocessEvent e)
    {
        String msg = e.getMessage().startsWith("/") ? e.getMessage().substring(1) : e.getMessage();

        if(msg.startsWith("decreefuture "))
        {
            String[] args = msg.split("\\Q \\E");
            CompletableFuture<String> future = futures.get(args[1]);

            if(future != null)
            {
                future.complete(args[2]);
                e.setCancelled(true);
            }
        }
    }

    /**
     * Post a future which assists in figuring out {@link DecreeWhichException}s
     * @param password The password to access this future (appended to the onclick)
     * @param future The future to fulfill
     */
    public void postFuture(String password, CompletableFuture<String> future) {
        futures.put(password, future);
    }

    /**
     * What to do with debug messages
     * @param message The debug message
     */
    public void debug(String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }

    /**
     * Get the root {@link DecreeVirtualCommand}
     */
    public DecreeVirtualCommand getRoot() {
        return commandCache.aquire(() -> {
            try {
                //return new DecreeCategory(null, getRootInstance(), getRootInstance().getClass().getDeclaredAnnotation(Decree.class));
                return DecreeVirtualCommand.createOrigin(rootInstance, rootInstance.getClass().getDeclaredAnnotation(Decree.class), this);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        });
    }


    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        KList<String> v = getRoot().invokeTabComplete(new KList<>(args), new DecreeSender(sender, getInstance(), this));
        v.removeDuplicates();
        if (sender instanceof Player && isCommandSound()) {
            new DecreeSender(sender, instance, this).playSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.25f, Maths.frand(0.125f, 1.95f));
        }
        return v;
    }

    @SuppressWarnings("deprecation")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(getInstance(), () -> {
            DecreeSender decreeSender = new DecreeSender(sender, getInstance(), this);
            DecreeContext.touch(decreeSender);

            if (getRoot().invoke(decreeSender, new KList<>(args), new KList<>())) {
                if (decreeSender.isPlayer()) {
                    decreeSender.playSound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.77f, 1.65f);
                    decreeSender.playSound(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.125f, 2.99f);
                }
            } else {
                sender.sendMessage(C.RED + "Unknown Decree Command");
                if(decreeSender.isPlayer())
                {
                    decreeSender.playSound(Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1f, 0.25f);
                    decreeSender.playSound(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.2f, 1.95f);
                }
            }
        });
        return true;
    }

    /**
     * Get the handler for the specified type
     *
     * @param type The type to handle
     * @return The corresponding {@link DecreeParameterHandler}, or null
     */
    public static DecreeParameterHandler<?> getHandler(Class<?> type) throws DecreeException {
        for (DecreeParameterHandler<?> i : handlers) {
            if (i.supports(type)) {
                return i;
            }
        }
        throw new DecreeException("Unhandled type in Decree Parameter: " + type.getName() + ". This is bad! Contact your admin! (Remove param or add handler)");
    }
}