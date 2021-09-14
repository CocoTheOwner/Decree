package nl.codevs.decree.decree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nl.codevs.decree.decree.exceptions.DecreeException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.handlers.*;
import nl.codevs.decree.decree.objects.*;
import nl.codevs.decree.decree.util.C;
import nl.codevs.decree.decree.util.KList;
import nl.codevs.decree.decree.util.Maths;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class DecreeSystem implements Listener {
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
     * The root of the command tree as an instantiated object
     */
    private final ConcurrentHashMap<String, KList<DecreeCategory>> roots;

    /**
     * The instance of the plugin
     */
    @Getter
    private final Plugin instance;


    /**
     * Whether to use command sounds or not
     */
    @Setter
    @Getter
    private boolean commandSound = true;

    /**
     * Command prefix
     */
    @Setter
    @Getter
    private String tag = C.RED + "[" + C.GREEN + "Decree" + C.RED + "]";

    public DecreeSystem(KList<DecreeCommandExecutor> rootInstances, Plugin instance) {
        this.roots = buildRoots(rootInstances);
        this.instance = instance;
    }

    /**
     * Build roots for instances
     * @param rootInstances The instances to build new roots for
     * @return The built roots in a {@link ConcurrentHashMap}
     */
    private ConcurrentHashMap<String, KList<DecreeCategory>> buildRoots(KList<DecreeCommandExecutor> rootInstances) {
        ConcurrentHashMap<String, KList<DecreeCategory>> roots = new ConcurrentHashMap<>();
        rootInstances.stream().filter(i -> i.getClass().isAnnotationPresent(Decree.class)).forEach(i -> {
            Decree decree = i.getClass().getDeclaredAnnotation(Decree.class);

            KList<String> names = new KList<>(decree.name());
            names.addAll(Arrays.asList(decree.aliases()));

            DecreeCategory root = new DecreeCategory(null, i, decree, this);
            System.out.println("Roots: " + names.toString(", "));
            for (String name : names) {
                if (roots.containsKey(name)) {
                    KList<DecreeCategory> rootsIn = roots.get(name);
                    rootsIn.addIfMissing(root);
                    roots.put(name, rootsIn);
                } else {
                    roots.put(name, new KList<>(root));
                }
            }
        });
        return roots;
    }

    /**
     * Handles the cases where there are multiple options following from the entered command values
     * @param e The event to check
     */
    @EventHandler
    public void on(PlayerCommandPreprocessEvent e)
    {
        String msg = e.getMessage().startsWith("/") ? e.getMessage().substring(1) : e.getMessage();

        if(msg.startsWith("decree-future "))
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
        new DecreeSender(Bukkit.getConsoleSender(), instance, this).sendMessage(tag.trim() + C.RESET + " " + message);
    }

    /**
     * Get the root {@link DecreeCategory}
     * @param name The name of the root command (first argument) to start from. This allows for multi-root support.
     */
    public KList<DecreeCategory> getRoots(String name) {
        if (roots.containsKey(name)) {
            return roots.get(name);
        }
        debug(C.RED + "Failed to get command(s) belonging to root command: " + name);
        return null;
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Command command) {

        DecreeSender decreeSender = new DecreeSender(sender, instance, this);
        KList<DecreeCategory> roots = getRoots(command.getName());
        KList<String> v = new KList<>();

        while (roots.isNotEmpty()) {
            try {
                v.addAll(roots.pop().tab(new KList<>(args), decreeSender));
            } catch (Throwable e) {
                decreeSender.sendMessage(C.RED + "Exception: " + e.getClass().getSimpleName() + " thrown while executing tab completion. Check console for details.");
                e.printStackTrace();
            }
        }

        v.removeDuplicates();

        if (decreeSender.isPlayer() && commandSound) {
            if (v.isNotEmpty()) {
                decreeSender.playSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.25f, Maths.frand(0.125f, 1.95f));
            } else {
                decreeSender.playSound(Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.25f, Maths.frand(0.125f, 1.95f));
            }
        }

        return v;
    }

    @SuppressWarnings({"deprecation", "SameReturnValue"})
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Command command) {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(instance, () -> {

            DecreeSender decreeSender = new DecreeSender(sender, instance, this);
            DecreeContext.touch(decreeSender);
            KList<String> noEmptyArgs = new KList<>(args).qremoveIf(String::isEmpty);
            KList<DecreeCategory> roots = getRoots(command.getName());

            while (roots.isNotEmpty()) {
                try {
                    if (roots.pop().invoke(noEmptyArgs, decreeSender)) {
                        if (decreeSender.isPlayer()) {
                            decreeSender.playSound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.77f, 1.65f);
                            decreeSender.playSound(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.125f, 2.99f);
                        }
                    } else {
                        debug(C.RED + "Unknown Decree Command");
                        if (decreeSender.isPlayer()) {
                            decreeSender.playSound(Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1f, 0.25f);
                            decreeSender.playSound(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.2f, 1.95f);
                        }
                    }

                } catch (Throwable e) {
                    decreeSender.sendMessage(C.RED + "Exception: " + e.getClass().getSimpleName() + " thrown while executing command. Check console for details.");
                    e.printStackTrace();
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