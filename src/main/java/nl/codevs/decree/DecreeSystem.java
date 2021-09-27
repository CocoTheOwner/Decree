package nl.codevs.decree;

import lombok.*;
import nl.codevs.decree.context.DecreeContextHandler;
import nl.codevs.decree.context.WorldContextHandler;
import nl.codevs.decree.decrees.DecreeCommandExecutor;
import nl.codevs.decree.exceptions.DecreeException;
import nl.codevs.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.handlers.*;
import nl.codevs.decree.util.*;
import nl.codevs.decree.virtual.Decree;
import nl.codevs.decree.virtual.DecreeCategory;
import nl.codevs.decree.virtual.Decreed;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Getter
@Setter
public class DecreeSystem implements Listener {

    /**
     * Command roots ({@link ConcurrentHashMap})
     */
    private final Roots roots;

    /**
     * The instance of the plugin
     */
    private final Plugin instance;

    public DecreeSystem(DecreeCommandExecutor rootInstance, Plugin plugin){
        this(new KList<>(rootInstance), plugin);
    }

    public DecreeSystem(KList<DecreeCommandExecutor> rootInstances, Plugin plugin) {
        this.roots = new Roots(rootInstances, this);
        this.instance = plugin;
    }

    /**
     * When entering arguments, should people be allowed to enter "null"?
     */
    private boolean allowNullInput = false;

    /**
     * Whether to use command sounds or not
     */
    private boolean commandSound = true;

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
     * When an argument match fails because of failed permissions/origin, should this be debugged?
     * Note: This is not always accurate, there are some false negatives (debugs when actually successful)
     */
    private boolean debugMismatchReason = true;

    /**
     * The maximal number of same-named root commands.
     * Has barely any performance impact, and you'll likely never exceed 1, but just in case.
     */
    private int maxRoots = 10;

    /**
     * When an argument parser fails, should the system parse null as the parameter value?
     * Note: While preventing issues when finding commands, this may totally break command parsing. Best to leave off.
     */
    private boolean nullOnFailure = false;

    /**
     * When an argument parser returns multiple options for a certain input, should the system always pick the first element and continue?
     * Note: When the command sender is a console, this is done regardless.
     */
    private boolean pickFirstOnMultiple = true;

    /**
     * Command prefix
     */
    private String prefix = C.RED + "[" + C.GREEN + "Decree" + C.RED + "]";

    /**
     * What to do with debug messages. Best not to touch and let Decree handle. To disable, set 'debug' to false.
     */
    Consumer<String> onDebug = (message) -> new DecreeSender(Bukkit.getConsoleSender(), getInstance()).sendMessage(getPrefix().trim() + C.RESET + " " + message);

    /**
     * What to do with sound effects. Best not to touch and let Decree handle. To disable, set 'commandSounds' to false.
     * Consumer takes 'success' ({@link Boolean}), 'isTab' ({@link Boolean}), and 'sender' ({@link DecreeSender}).<br>
     * If ifTab is false, it means the effect is called from command invocation.
     */
    TriConsumer<Boolean, Boolean, DecreeSender> onSoundEffect = (success, isTab, sender) -> {
        if (isTab) {
            if (success) {
                sender.playSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.25f, Maths.frand(0.125f, 1.95f));
            } else {
                sender.playSound(Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.25f, Maths.frand(0.125f, 1.95f));
            }
        } else {
            if (success) {
                debug(C.GREEN + "Successfully ran command!");
                sender.playSound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.77f, 1.65f);
                sender.playSound(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.125f, 2.99f);
            } else {
                debug(C.RED + "Unknown Decree Command");
                sender.playSound(Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1f, 0.25f);
                sender.playSound(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.2f, 1.95f);
            }
        }
    };

    /**
     * What to do with debug messages
     * @param message The debug message
     */
    public void debug(String message) {
        if (debug) {
            onDebug.accept(message);
        }
    }

    /**
     * Play command sounds
     * @param success Whether the result is successful or not
     * @param isTab whether this is tab (true) or command (false)
     * @param sender The sender of the tab/command
     */
    private void playSound(boolean success, boolean isTab, DecreeSender sender) {
        if (sender.isPlayer() && commandSound) {
            onSoundEffect.accept(success, isTab, sender);
        }
    }

    /**
     * Handles the cases where there are multiple options following from the entered command values
     * @param e The event to check
     */
    @EventHandler
    public void on(PlayerCommandPreprocessEvent e) {
        e.setCancelled(Completer.pick(e.getMessage()));
    }

    /**
     * Handles the cases where there are multiple options following from the entered command values, except for Consoles instead of players.
     * @param e The event to check
     */
    @EventHandler
    public void on(ServerCommandEvent e) {
        e.setCancelled(Completer.pickConsole(e.getCommand()));
    }

    /**
     * Tab completion
     * @param commandSender The sender of the tab-complete
     * @param arguments The existing arguments
     * @param command The root command
     * @return List of completions
     */
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String[] arguments) {

        DecreeSender sender = new DecreeSender(commandSender, getInstance());
        KList<String> args = new KList<>(arguments).qremoveIf(String::isEmpty);
        KList<String> completions = new KList<>();

        return completions;
    }

    @SuppressWarnings({"deprecation", "SameReturnValue"})
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String[] arguments) {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(getInstance(), () -> {

            KList<String> args = new KList<>(arguments).qremoveIf(String::isEmpty);
            DecreeSender sender = new DecreeSender(commandSender, getInstance());
            Context.touch(sender);

            for (Decreed root : getRoots().get(command.getName())) {
                if (root.run(args, sender)) {
                    playSound(true, false, sender);
                    return;
                }
            }

            playSound(false, false, sender);
        });
        return true;
    }

    public static class Completer {

        public static final ConcurrentHashMap<String, CompletableFuture<String>> futures = new ConcurrentHashMap<>();
        public static CompletableFuture<String> consoleFuture;

        /**
         * Try fulfilling a {@link CompletableFuture}
         * @param message the message that will fulfill the contract
         * @return True if any {@link CompletableFuture} was fulfilled, false if not
         */
        public static boolean pick(String message) {
            String msg = message.startsWith("/") ? message.substring(1) : message;

            if(msg.startsWith("decree-future ")) {
                String[] args = msg.split("\\Q \\E");
                CompletableFuture<String> future = futures.get(args[1]);

                if(future != null)
                {
                    future.complete(args[2]);
                    futures.remove(args[0]);
                    return true;
                }
            }

            return false;
        }

        /**
         * Try fulfilling a {@link CompletableFuture}
         * @param command The command to use to fulfill the future
         * @return True if the future was fulfilled, false if not.
         */
        public static boolean pickConsole(String command) {
            if (consoleFuture != null && !consoleFuture.isCancelled() && !consoleFuture.isDone()) {
                if (!command.contains(" ")) {
                    consoleFuture.complete(command.trim().toLowerCase(Locale.ROOT));
                    return true;
                }
            }
            return false;
        }

        /**
         * Post a future which assists in figuring out {@link DecreeWhichException}s
         * @param password The password to access this future (appended to the onclick)
         * @param future The future to fulfill
         */
        public static void postFuture(String password, CompletableFuture<String> future) {
            futures.put(password, future);
        }

        /**
         * Post a future which assists in figuring out {@link DecreeWhichException}s
         * @param future The future to fulfill
         */
        public static void postConsoleFuture(CompletableFuture<String> future) {
            consoleFuture = future;
        }
    }

    public static class Handler {
        /**
         * Parameter handlers list. You can add/remove handlers to/from this list.
         * Parameters must implement {@link DecreeParameterHandler}.
         * Parameter handlers handle string-to-type conversion (and back).
         */
        @Getter
        @Setter
        private static KList<DecreeParameterHandler<?>> handlers = new KList<>(
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
         * Add a new handler to the list of handlers.
         * @param handler The handler to add
         * @return True if the handler is new, false if it was already added.
         */
        public static boolean addHandler(DecreeParameterHandler<?> handler) {
            if (handlers.contains(handler)) {
                return false;
            }
            handlers.add(handler);
            return true;
        }

        /**
         * Get the handler for the specified type
         *
         * @param type The type to handle
         * @return The corresponding {@link DecreeParameterHandler}, or null
         */
        public static DecreeParameterHandler<?> get(Class<?> type) throws DecreeException {
            for (DecreeParameterHandler<?> i : handlers) {
                if (i.supports(type)) {
                    return i;
                }
            }
            throw new DecreeException("Unhandled type in Decree Parameter: " + type.getName() + ". This is bad! Contact your admin! (Remove param or add handler)");
        }
    }

    public static class Context {

        private static final ChronoLatch cl = new ChronoLatch(60000);
        private static final ConcurrentHashMap<Thread, DecreeSender> context = new ConcurrentHashMap<>();

        /**
         * Context handlers mapping. You can add/remove handlers to/from this map.
         * Parameters must implement {@link DecreeContextHandler}.
         * Context handlers extract data that can be auto-filled in commands from only the sender.
         */
        @Getter
        @Setter
        private static ConcurrentHashMap<Class<?>, DecreeContextHandler<?>> handlers = new ConcurrentHashMap<>() {
            {
                put(World.class, new WorldContextHandler());
            }
        };

        /**
         * Get the sender from the current thread's context
         * @return The {@link DecreeSender} for this thread
         */
        public static DecreeSender get() {
            return context.get(Thread.currentThread());
        }

        /**
         * Add the {@link DecreeSender} to the context map & removes dead threads
         * @param sender The sender
         */
        public static void touch(DecreeSender sender) {
            synchronized (context) {
                context.put(Thread.currentThread(), sender);

                if (cl.flip()) {
                    Enumeration<Thread> contextKeys = context.keys();

                    while (contextKeys.hasMoreElements()) {
                        Thread thread = contextKeys.nextElement();
                        if (!thread.isAlive()) {
                            context.remove(thread);
                        }
                    }
                }
            }
        }
    }

    private static class Roots extends ConcurrentHashMap<String, KList<DecreeCategory>> {

        private Roots(KList<DecreeCommandExecutor> roots, DecreeSystem system) {
            KList<DecreeCommandExecutor> rootInstancesFailed = new KList<>();
            KList<DecreeCommandExecutor> rootInstancesSuccess = new KList<>();
            KList<String> registeredRootNames = new KList<>();

            roots.forEach(r -> {
                if (r.getClass().isAnnotationPresent(Decree.class)) {
                    rootInstancesSuccess.add(r);
                } else {
                    rootInstancesFailed.add(r);
                    return;
                }

                // Get decree, names, and category representation for root instance
                Decree decree = r.getClass().getDeclaredAnnotation(Decree.class);
                KList<String> names = new KList<>(decree.name()).qAddAll(Arrays.asList(decree.aliases()));
                DecreeCategory root = new DecreeCategory(null, r, decree, system);

                // Add names to root map (supports multiple)
                names.forEach(n -> {
                    registeredRootNames.add(n);
                    if (!containsKey(n)) {
                        put(n, new KList<>(root));
                    } else {
                        KList<DecreeCategory> rootsIn = get(n);
                        assert rootsIn != null;
                        rootsIn.addIfMissing(root);
                        put(n, rootsIn);
                    }
                });
            });

            registeredRootNames.removeDuplicates();

            if (rootInstancesSuccess.isEmpty()) {
                System.out.println(C.RED + "No successful root instances registered. Did you register all commands in the creator?");
            } else {
                System.out.println(C.GREEN + "Loaded root instances: " + C.YELLOW + rootInstancesSuccess.convert(ri -> ri.getClass().getSimpleName()).toString(", "));
            }

            if (rootInstancesFailed.isNotEmpty()) {
                System.out.println(C.RED + "Failed root instances: " + C.YELLOW + rootInstancesFailed.convert(ri -> ri.getClass().getSimpleName()).toString(", "));
            }

            if (registeredRootNames.isEmpty()) {
                System.out.println(C.RED + "No root commands registered! Did you register all commands in the creator? Did you give them names?");
            } else {
                System.out.println(C.GREEN + "Loaded root commands: " + C.YELLOW + registeredRootNames.convert(rn -> "/" + rn).toString(", "));
            }
        }
    }
}