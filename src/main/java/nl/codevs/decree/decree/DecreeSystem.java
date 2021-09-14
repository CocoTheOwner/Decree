package nl.codevs.decree.decree;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nl.codevs.decree.decree.context.DecreeContextHandler;
import nl.codevs.decree.decree.context.WorldContextHandler;
import nl.codevs.decree.decree.exceptions.DecreeException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.handlers.*;
import nl.codevs.decree.decree.objects.*;
import nl.codevs.decree.decree.util.C;
import nl.codevs.decree.decree.util.KList;
import nl.codevs.decree.decree.util.Maths;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@AllArgsConstructor
@Getter
@Setter
public class DecreeSystem implements Listener {

    public DecreeSystem(KList<DecreeCommandExecutor> rootInstances, Plugin instance) {
        this.roots = buildRoots(rootInstances);
        this.instance = instance;
    }

    private final ConcurrentHashMap<String, CompletableFuture<String>> futures = new ConcurrentHashMap<>();

    /**
     * Parameter handlers list. You can add/remove handlers to/from this list.
     * Parameters must implement {@link DecreeParameterHandler}.
     * Parameter handlers handle string-to-type conversion (and back).
     */
    @Getter
    @Setter
    private static KList<DecreeParameterHandler<?>> parameterHandlers = new KList<>(
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
     * Context handlers mapping. You can add/remove handlers to/from this map.
     * Parameters must implement {@link DecreeContextHandler}.
     * Context handlers extract data that can be auto-filled in commands from only the sender.
     */
    @Getter
    @Setter
    private static ConcurrentHashMap<Class<?>, DecreeContextHandler<?>> contextHandlers = new ConcurrentHashMap<>() {
        {
            put(World.class, new WorldContextHandler());
        }
    };

    /**
     * The root of the command tree as an instantiated object
     */
    private final ConcurrentHashMap<String, KList<DecreeCategory>> roots;

    /**
     * The instance of the plugin
     */
    private final Plugin instance;

    /**
     * Whether to send debug messages or not.
     * You can also make runOnDebug equal to `(s) -> {};`
     */
    private boolean debug = true;

    /**
     * Whether to debug matching or not. This is also ran on tab completion, so it causes a lot of debug.
     */
    private boolean debugMatching = false;

    /**
     * Whether to use command sounds or not
     */
    private boolean commandSound = true;

    /**
     * When an argument parser fails, should the system parse null as the parameter value?
     * Note: While preventing issues when finding commands, this may totally break command parsing. Best to leave off.
     */
    private boolean nullOnFailure = false;

    /**
     * When entering arguments, should people be allowed to enter "null"?
     */
    private boolean allowNullInput = false;

    /**
     * When an argument parser returns multiple options for a certain input, should the system always pick the first element and continue?
     * Note: When the command sender is a console, this is done regardless.
     */
    private boolean pickFirstOnMultiple = true;

    /**
     * When an argument match fails because of failed permissions/origin, should this be debugged?
     * Note: This is not always accurate, there are some false negatives (debugs when actually successful)
     */
    private boolean debugMismatchReason = true;

    /**
     * When a tab completion is requested, should tab completes be matched deeply?
     * Note: This results in more but less accurate auto-completions.
     * The best auto-completions will be first in the list.
     */
    private boolean tabMatchDeep = true;

    /**
     * Command prefix
     */
    private String prefix = C.RED + "[" + C.GREEN + "Decree" + C.RED + "]";

    /**
     * The maximal number of same-named root commands.
     * Has barely any performance impact, and you'll likely never exceed 1, but just in case.
     */
    private int maxRoots = 10;

    /**
     * What to do with debug messages. Best not to touch and let Decree handle.
     */
    Consumer<String> runOnDebug = (message) -> new DecreeSender(Bukkit.getConsoleSender(), getInstance(), this).sendMessage(getPrefix().trim() + C.RESET + " " + message);

    /**
     * Build roots for instances
     * @param rootInstances The instances to build new roots for
     * @return The built roots in a {@link ConcurrentHashMap}
     */
    private ConcurrentHashMap<String, KList<DecreeCategory>> buildRoots(KList<DecreeCommandExecutor> rootInstances) {

        ConcurrentHashMap<String, KList<DecreeCategory>> roots = new ConcurrentHashMap<>();

        KList<DecreeCommandExecutor> rootInstancesFailed = new KList<>();
        KList<DecreeCommandExecutor> rootInstancesSuccess = new KList<>();
        KList<String> registeredRootNames = new KList<>();


        for (DecreeCommandExecutor rootInstance : rootInstances) {

            if (!rootInstance.getClass().isAnnotationPresent(Decree.class)) {
                rootInstancesFailed.add(rootInstance);
                continue;
            } else {
                rootInstancesSuccess.add(rootInstance);
            }



            // Get decree, names, and category representation for root instance
            Decree decree = rootInstance.getClass().getDeclaredAnnotation(Decree.class);
            KList<String> names = new KList<>(decree.name()).qAddAll(Arrays.asList(decree.aliases()));
            DecreeCategory root = new DecreeCategory(null, rootInstance, decree, this);

            // Add names to root map (supports multiple)
            for (String name : names) {
                registeredRootNames.addIfMissing(name);
                if (roots.containsKey(name)) {
                    KList<DecreeCategory> rootsIn = getRoots().get(name);
                    rootsIn.addIfMissing(root);
                    roots.put(name, rootsIn);
                } else {
                    roots.put(name, new KList<>(root));
                }
            }
        }

        if (rootInstancesSuccess.isEmpty()) {
            System.out.println(C.RED + "No successful root instances registered. Did you register all commands in the creator?");
        } else {
            System.out.println(C.GREEN + "Loaded root instances: " + C.YELLOW + rootInstancesSuccess.convert(ri -> ri.getClass().getSimpleName()).toString(", "));
        }

        if (rootInstancesFailed.isNotEmpty()) {
            System.out.println(C.RED + "Failed root instances: " + C.YELLOW + rootInstancesFailed.convert(ri -> ri.getClass().getSimpleName()).toString(", "));
        }

        if (registeredRootNames.isEmpty()) {
            System.out.println(C.RED + "No root commands registered! Did you register all commands in hte creator? Did you give them names?");
        } else {
            System.out.println(C.GREEN + "Loaded root commands: " + C.YELLOW + registeredRootNames.toString(", "));
        }

        return roots;
    }

    /**
     * Handles the cases where there are multiple options following from the entered command values
     * @param e The event to check
     */
    @EventHandler
    public void on(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().startsWith("/") ? e.getMessage().substring(1) : e.getMessage();

        if(msg.startsWith("decree-future "))
        {
            String[] args = msg.split("\\Q \\E");
            CompletableFuture<String> future = getFutures().get(args[1]);

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
        getFutures().put(password, future);
    }

    /**
     * What to do with debug messages
     * @param message The debug message
     */
    public void debug(String message) {
        if (debug) {
            runOnDebug.accept(message);
        }
    }

    /**
     * Get the root {@link DecreeCategory}
     * @param name The name of the root command (first argument) to start from. This allows for multi-root support.
     */
    public KList<DecreeCategory> getRoots(String name) {
        if (getRoots().containsKey(name)) {
            return getRoots().get(name).copy();
        }
        debug(C.RED + "Failed to get command(s) belonging to root command: " + name);
        return null;
    }

    /**
     * Tab completion
     * @param sender The sender of the tab-complete
     * @param args The existing arguments
     * @param command The root command
     * @return List of completions
     */
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Command command) {

        DecreeSender decreeSender = new DecreeSender(sender, getInstance(), this);
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

        if (decreeSender.isPlayer() && isCommandSound()) {
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
        Bukkit.getScheduler().scheduleAsyncDelayedTask(getInstance(), () -> {

            DecreeSender decreeSender = new DecreeSender(sender, getInstance(), this);
            DecreeContext.touch(decreeSender);
            KList<String> noEmptyArgs = new KList<>(args).qremoveIf(String::isEmpty);
            KList<DecreeCategory> roots = getRoots(command.getName());
            if (roots.isEmpty()) {
                debug(C.RED + "Found no roots for: " + C.YELLOW + command.getName() + C.RED + " | Mapping: ");
                Iterator<String> a = getRoots().keys().asIterator();
                String s;
                while (a.hasNext()) {
                    s = a.next();
                    debug(C.RED + " - " + C.YELLOW + s + C.RED + " -> " + C.YELLOW + getRoots().get(s));
                }
            } else {
                debug(C.GREEN + "Found " + C.YELLOW + roots.size() + C.GREEN + " roots: " + C.YELLOW + roots.convert(DecreeCategory::getName).toString(", "));
            }

            int maxTriesLeft = getMaxRoots();
            boolean success = false;
            while (!success && roots.isNotEmpty() && maxTriesLeft-- > 0) {
                try {
                    if (roots.pop().invoke(noEmptyArgs.copy(), decreeSender)) {
                        success = true;
                    }
                } catch (Throwable e) {
                    decreeSender.sendMessage(C.RED + "Exception: " + e.getClass().getSimpleName() + " thrown while executing command. Check console for details.");
                    e.printStackTrace();
                }
            }

            if (success) {
                debug(C.GREEN + "Successfully ran command!");
                if (decreeSender.isPlayer() && isCommandSound()) {
                    decreeSender.playSound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.77f, 1.65f);
                    decreeSender.playSound(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.125f, 2.99f);
                }
            } else {
                debug(C.RED + "Unknown Decree Command");
                if (decreeSender.isPlayer() && isCommandSound()) {
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
        for (DecreeParameterHandler<?> i : DecreeSystem.getParameterHandlers()) {
            if (i.supports(type)) {
                return i;
            }
        }
        throw new DecreeException("Unhandled type in Decree Parameter: " + type.getName() + ". This is bad! Contact your admin! (Remove param or add handler)");
    }
}