package nl.codevs.decree;

import lombok.*;
import nl.codevs.decree.context.DecreeContextHandler;
import nl.codevs.decree.context.PlayerContextHandler;
import nl.codevs.decree.context.WorldContextHandler;
import nl.codevs.decree.decrees.DecreeCommandExecutor;
import nl.codevs.decree.exceptions.DecreeException;
import nl.codevs.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.handlers.*;
import nl.codevs.decree.util.*;
import nl.codevs.decree.virtual.Decree;
import nl.codevs.decree.virtual.DecreeCategory;
import nl.codevs.decree.virtual.Decreed;
import org.bukkit.Bukkit;
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
import java.util.stream.Stream;

@Getter
@Setter
public class DecreeSystem implements Listener {

    /**
     * Decree System version.
     */
    public static final String version = "1.2";

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
        roots = new Roots(rootInstances, this);
        instance = plugin;
        if (DecreeSettings.helpDecree) {
            System.out.println("Enabled Advanced Command System " + C.YELLOW + "Decree v" + version + C.RESET + " for " + C.YELLOW + plugin.getName() + " v" + plugin.getDescription().getVersion());
            System.out.println("See our GitHub page: " + C.YELLOW + "https://www.github.com/CocoTheOwner/Decree");
        }
    }

    /**
     * What to do with debug messages
     * @param message The debug message
     */
    public void debug(String message) {
        if (DecreeSettings.debug) {
            DecreeSettings.onDebug.accept(message, instance);
        }
    }

    /**
     * Play command sounds
     * @param success Whether the result is successful or not
     * @param sfx The sound effect type
     * @param sender The sender of the tab/command
     */
    public void playSound(boolean success, SFX sfx, DecreeSender sender) {
        if (sender.isPlayer() && DecreeSettings.commandSound) {
            DecreeSettings.onSoundEffect.accept(success, sfx, sender);
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

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String[] arguments) {

        DecreeSender sender = new DecreeSender(commandSender, getInstance());
        KList<String> args = new KList<>(arguments).qremoveIf(String::isEmpty);
        KList<String> completions = new KList<>();

        // TODO: Tab completions

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
                    playSound(true, SFX.Command, sender);
                    return;
                }
            }

            playSound(false, SFX.Command, sender);
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
         * Remove all handlers
         * @param type of a certain type
         * @return True if any existed
         */
        public static boolean removeHandlers(Class<?> type) {
            Stream<DecreeParameterHandler<?>> matchedHandlers = handlers.stream().filter(h -> h.supports(type));
            if (matchedHandlers.noneMatch(h -> true)){
                return false;
            } else {
                matchedHandlers.forEach(h -> handlers.remove(h));
                return true;
            }
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

    public enum SFX {
        Tab,
        Command,
        Picked
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
        private static KList<DecreeContextHandler<?>> handlers = new KList<>(new WorldContextHandler(), new PlayerContextHandler());

        /**
         * Add a new handler to the list of handlers.
         * @param handler The handler to add
         * @return True if the handler is new, false if it was already added.
         */
        public static boolean addHandler(DecreeContextHandler<?> handler) {
            if (handlers.contains(handler)) {
                return false;
            }
            handlers.add(handler);
            return true;
        }

        /**
         * Remove all handlers
         * @param type of a certain type
         * @return True if any existed
         */
        public static boolean removeHandlers(Class<?> type) {
            Stream<DecreeContextHandler<?>> matchedHandlers = handlers.stream().filter(h -> h.supports(type));
            if (matchedHandlers.noneMatch(h -> true)){
                return false;
            } else {
                matchedHandlers.forEach(h -> handlers.remove(h));
                return true;
            }
        }

        /**
         * Get the handler for the specified type
         *
         * @param type The type to handle
         * @return The corresponding {@link DecreeContextHandler}, or null
         */
        public static DecreeContextHandler<?> getHandler(Class<?> type) throws DecreeException {
            for (DecreeContextHandler<?> i : handlers) {
                if (i.supports(type)) {
                    return i;
                }
            }
            throw new DecreeException("Unhandled type in Decree Parameter: " + type.getName() + ". This is bad! Contact your admin! (Remove param or add handler)");
        }

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

            if (roots.size() > DecreeSettings.maxRoots) {
                System.out.println(C.RED + "Too many roots! Excluding " + (roots.subList(DecreeSettings.maxRoots, roots.size() - 1).convert(r -> r.getClass().getSimpleName()).toString(", ")) + "!");
                roots = roots.subList(0, DecreeSettings.maxRoots - 1);
            }

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
                System.out.println(C.GREEN + "Loaded root category classes: " + C.YELLOW + rootInstancesSuccess.convert(ri -> ri.getClass().getSimpleName()).toString(", "));
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