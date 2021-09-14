package nl.codevs.decree.decree.objects;

import lombok.Data;
import nl.codevs.decree.decree.DecreeSystem;
import nl.codevs.decree.decree.context.DecreeContextHandler;
import nl.codevs.decree.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.util.C;
import nl.codevs.decree.decree.util.Form;
import nl.codevs.decree.decree.util.KList;
import nl.codevs.decree.decree.util.Maths;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single command (non-category)
 */
@Data
public class DecreeCommand implements Decreed {
    private static final String newline = "<reset>\n";
    private final KList<DecreeParameter> parameters;
    private final Method method;
    private final DecreeCategory parent;
    private final Decree decree;
    private final DecreeSystem system;

    /**
     * Create a node
     * @param parent The instantiated class containing the
     * @param method Method that represents a Decree (must be annotated by @{@link Decree})
     */
    public DecreeCommand(DecreeCategory parent, Method method, DecreeSystem system) {
        if (!method.isAnnotationPresent(Decree.class)) {
            throw new RuntimeException("Cannot instantiate DecreeCommand on method " + method.getName() + " in " + method.getDeclaringClass().getCanonicalName() + " not annotated by @Decree");
        }
        this.parent = parent;
        this.method = method;
        this.system = system;
        this.decree = method.getDeclaredAnnotation(Decree.class);
        this.parameters = calcParameters();
    }

    /**
     * Calculate the parameters in this method<br>
     * Sorted by required & contextuality
     * @return {@link KList} of {@link DecreeParameter}s
     */
    private KList<DecreeParameter> calcParameters() {
        KList<DecreeParameter> parameters = new KList<>();
        Arrays.stream(method.getParameters()).filter(p -> p.isAnnotationPresent(Param.class)).forEach(p -> parameters.add(new DecreeParameter(p)));
        return parameters;
    }

    /**
     * Get sorted parameters
     * @return Sorted parameters
     */
    public KList<DecreeParameter> getParameters() {
        return parameters.copy().qsort((o1, o2) -> {
                if (o1.isRequired()) {
                    if (o2.isRequired()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (o2.isRequired()) {
                    return 1;
                }

                if (o1.isContextual()) {
                    if (o2.isContextual()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (o2.isRequired()) {
                    return 1;
                }

                return 0;
        });
    }

    /**
     * Get random suggestions for this command
     * @param max The maximal amount of suggestions
     * @return A list of suggestions
     */
    private KList<String> getRandomSuggestions(int max) {

        //            ✦ /command sub name
        String prefix = "<#aebef2>✦ <#5ef288>" + parent().getPath() + " <#42ecf5>";
        KList<String> suggestions = new KList<>();

        suggestions.add(prefix + getName() + " " + getParameters().convert(p -> p.getHandler().getRandomDefault()).toString(" "));

        for (int i = 0; i < 15 + max * 2; i++) {
            KList<String> params = new KList<>();
            if (Maths.r()) {
                // Unordered list with '=' signs & name prefix
                getParameters().shuffle().forEach(p -> {
                    if (p.isRequired() || Maths.r()) {
                        params.add("<#f2e15e>" + p.getNames().getRandom() + "=<#d665f0>" + p.exampleValues().getRandom());
                    }
                });
            } else {
                // Ordered list without '=' signs & name prefix
                for (DecreeParameter parameter : getParameters()) {
                    if (parameter.isRequired() || Maths.r()) {
                        params.add("<#d665f0>" + parameter.exampleValues().getRandom());
                    }
                }
            }
            // Random name prefix
            suggestions.add(prefix + getNames().getRandom() + " " + params.toString(" "));
        }
        
        suggestions.removeDuplicates();
        return new KList<>(suggestions.subList(0, Math.min(max, suggestions.size()) - 1));
    }

    public String getHelp(DecreeSender sender) {

        if (!sender.isPlayer()) {
            return getPath() + " " + getParameters().convert(p -> p.getName() + "=" + p.getHandler().getRandomDefault()).toString(" ");
        }

        String hoverTitle = "<#42ecf5>" + getNames().toString(", ");
        String hoverUsage = "<#bbe03f>✒ <#a8e0a2><font:minecraft:uniform>";
        String hoverDescription = "<#3fe05a>✎ <#6ad97d><font:minecraft:uniform>" + getDescription();
        String hoverPermission;
        String hoverSuggestions;
        String hoverOrigin = "<#dbe61c>⌘ <#d61aba><#ff33cc><font:minecraft:uniform>" + Form.capitalize(getOrigin().toString().toLowerCase());

        String doOnClick;
        String runOnClick = getPath();
        String realText = "<#46826a>⇀<gradient:#42ecf5:#428df5> " + getName();

        StringBuilder appendedParameters = new StringBuilder(" ");

        // Usage and clicking
        if (getParameters().isEmpty()){
            hoverUsage += "There are no parameters. Click to run.";
            doOnClick = "run_command";
        } else {
            hoverUsage += "Hover over parameters. Click to suggest.";
            doOnClick = "suggest_command";
        }

        // Permission
        if (!getDecree().permission().equals(Decree.NO_PERMISSION)){
            String granted;
            if (sender.isOp() || sender.hasPermission(getDecree().permission())){
                granted = "<#a73abd>(Granted)";
            } else {
                granted = "<#db4321>(Not Granted)";
            }
            hoverPermission = "<#2181db>⏍ <#78dcf0><font:minecraft:uniform>Permission: <#ffa500>" + getDecree().permission() + " " + granted;
        } else {
            hoverPermission = "";
        }

        // Origin
        if (getOrigin().equals(DecreeOrigin.BOTH)){
            hoverOrigin = "";
        } else if (getOrigin().validFor(sender)) {
            hoverOrigin += "<#0ba10b> origin, so you can run it.";
        } else {
            hoverOrigin += "<#c4082e> origin, so you cannot run it.";
        }

        // Suggestions
        if (getParameters().isNotEmpty()) {
            hoverSuggestions = "<font:minecraft:uniform>" + getRandomSuggestions(Math.min(parameters.size() + 1, 5)).toString("\n") + "<reset>";
        } else {
            hoverSuggestions = "";
        }

        // Parameters
        if (getParameters().isEmpty()) {
            appendedParameters = new StringBuilder();
        } else {
            StringBuilder requiredFirst = new StringBuilder();
            for (DecreeParameter parameter : getParameters()) {

                // Name
                String shortestName = parameter.getName();
                for (String name : parameter.getNames()) {
                    if (name.length() < shortestName.length()) {
                        shortestName = name;
                    }
                }

                // Value
                String value;
                if (parameter.hasDefault()) {
                    value = parameter.getDefaultRaw();
                } else {
                    value = parameter.getHandler().getRandomDefault();
                }

                // Full onclick
                String onClick = getPath() + " " + requiredFirst + " " + shortestName + "=" + value;

                // Cleanup
                while(onClick.contains("  ")){
                    onClick = onClick.replaceAll("\\Q  \\E", " ");
                }

                // If required && not contextual & player added to requirements
                if (parameter.isRequired() && !(parameter.isContextual() && sender.isPlayer())) {
                    requiredFirst.append(shortestName).append("=").append(value).append(" ");
                }

                // Add this parameter
                appendedParameters.append(parameter.getHelp(sender, onClick));
            }
        }

        return "<hover:show_text:'" +
                    hoverTitle +
                    newline + hoverDescription +
                    newline + hoverUsage +
                    (hoverPermission.isEmpty() ? "" : newline) + hoverPermission +
                    (hoverOrigin.isEmpty() ? "" : newline) + hoverOrigin +
                    (hoverSuggestions.isEmpty() ? "" : newline) + hoverSuggestions +
                "'>" +
                    "<click:" + doOnClick + ":" + runOnClick + ">" +
                        realText +
                    "</click>" +
                "</hover>" +
                appendedParameters;
    }

    @Override
    public Decreed parent() {
        return getParent();
    }

    @Override
    public Decree decree() {
        return getDecree();
    }

    @Override
    public String getName() {
        return decree().name().isEmpty() ? getMethod().getName() : decree().name();
    }

    @Override
    public DecreeSystem system() {
        return getSystem();
    }

    @Override
    public KList<String> tab(KList<String> args, DecreeSender sender) {
        system.debug("Branched to " + getName());
        if (args.isEmpty()) {
            system.debug("Empty " + getName());
            return new KList<>();
        }

        KList<String> tabs = new KList<>();
        String last = args.popLast();
        KList<DecreeParameter> left = getParameters().copy();

        // Remove auto-completions for existing keys
        for (String a : args) {
            String sea = a.contains("=") ? a.split("\\Q=\\E")[0] : a;
            sea = sea.trim();

            searching:
            for (DecreeParameter i : left.copy()) {
                for (String m : i.getNames()) {
                    if (m.equalsIgnoreCase(sea) || m.toLowerCase().contains(sea.toLowerCase()) || sea.toLowerCase().contains(m.toLowerCase())) {
                        left.remove(i);
                        continue searching;
                    }
                }
            }
        }

        system.debug("Command " + getName() + " possible tabs left: " + left.convert(DecreeParameter::getName).toString(", "));

        // Add auto-completions
        for (DecreeParameter i : left) {

            int quantity = 0;

            if (last.contains("=")) {
                String[] vv = last.trim().split("\\Q=\\E");
                String vx = vv.length == 2 ? vv[1] : "";
                for (String possibility : i.getHandler().getPossibilities(vx).convert((v) -> i.getHandler().toStringForce(v))) {
                    quantity++;
                    tabs.add(i.getName() + "=" + possibility);
                }
            } else {
                for (String possibility : i.getHandler().getPossibilities("").convert((v) -> i.getHandler().toStringForce(v))) {
                    quantity++;
                    tabs.add(i.getName() + "=" + possibility);
                }
            }

            if (quantity == 0) {
                tabs.add(i.getName() + "=");
                tabs.add(i.getName() + "=" + i.getDefaultRaw());
            }
        }

        return tabs;
    }

    // TODO: Write invocation
    @Override
    public boolean invoke(KList<String> args, DecreeSender sender) {
        debug("Parameters: " + C.YELLOW + args.toString(", "), C.GREEN);

        args.removeIf(Objects::isNull);
        args.removeIf(String::isEmpty);

        ConcurrentHashMap<DecreeParameter, Object> params = computeParameters(args, sender);

        boolean invalid = false;
        for (DecreeParameter parameter : getParameters()) {
            if (params.containsValue(parameter)) {
                continue;
            }

            if (parameter.hasDefault()) {
                Object value = null;
                try {
                    value = parameter.getDefaultValue();
                } catch (DecreeParsingException e) {
                    debug("Default value " + C.YELLOW + parameter.getDefaultRaw() + C.RED + " could not be parsed to " + parameter.getType().getSimpleName(), C.RED);
                    debug("Reason: " + C.YELLOW + e.getMessage(), C.RED);
                } catch (DecreeWhichException e) {
                    debug("Default value " + C.YELLOW + parameter.getDefaultRaw() + C.RED + " returned multiple options. Adding: " + e.getOptions().get(0), C.RED);
                    value = e.getOptions().get(0);
                }

                if (value != null) {
                    params.put(parameter, value);
                    continue;
                }
            }

            if (parameter.isContextual() && sender.isPlayer()) {
                Object contextValue = DecreeContextHandler.contextHandlers.get(parameter.getType()).handle(sender);
                debug("Context value for " + C.YELLOW + parameter.getName() + C.GREEN + " set to: " + contextValue.toString(), C.GREEN);
                params.put(parameter, contextValue);
                continue;
            }

            debug("Parameter " + C.YELLOW + parameter.getName() + C.RED + " not provided", C.RED);
            invalid = true;
        }

        if (invalid) {
            debug("Failed to handle command.", C.RED);
            return false;
        }

        Object[] finalParams = new Object[getParameters().size()];


        int x = 0;
        for (DecreeParameter parameter : getParameters()) {
            if (!params.containsKey(parameter)) {
                debug("Failed to handle command, but did not notice one missing: " + C.YELLOW + parameter.getName() + C.RED + "!", C.RED);
                debug("Params stored: " + params, C.RED);
                return false;
            }

            finalParams[x++] = params.get(parameter);
        }

        Runnable rx = () -> {
            try {
                try {
                    DecreeContext.touch(sender);
                    getMethod().setAccessible(true);
                    getMethod().invoke(getParent().getInstance(), finalParams);
                } catch (InvocationTargetException e) {
                    if (e.getCause().getMessage().endsWith("may only be triggered synchronously.")) {
                        debug("Sent asynchronously while it must be ran sync", C.RED);
                        e.printStackTrace();
                        sender.sendMessage(C.RED + "The command you tried to run (" + getPath() + ") may only be run sync! Contact your admin!");
                        return;
                    }
                    throw e;
                }
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to execute " + getPath());
            }
        };

        if (isSync()) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(system.getInstance(), rx);
        } else {
            rx.run();
        }

        return false;
    }

    /**
     * Compute parameter objects from string argument inputs
     * @param args The arguments (parameters) to parse into this command
     * @param sender The sender of the command
     * @return A {@link ConcurrentHashMap} from the parameter to the instantiated object for that parameter
     */
    private ConcurrentHashMap<DecreeParameter, Object> computeParameters(KList<String> args, DecreeSender sender) {
        ConcurrentHashMap<DecreeParameter, Object> parameters = new ConcurrentHashMap<>();
        KList<DecreeParameter> options = getParameters();
        KList<String> keylessArgs = new KList<>();
        KList<String> remainingArgs = new KList<>();
        KList<String> skipped = new KList<>();

        // Keyed arguments (key=value)
        for (String arg : args) {

            // These are handled later, after other (keyed) options were removed
            if (!arg.contains("=")) {
                keylessArgs.add(arg);
                continue;
            }

            if (arg.split("=").length > 2) {
                debug("Parameter has multiple '=' signs (full arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
                skipped.add(arg);
                continue;
            }

            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];

            if (key.isEmpty()) {
                debug("Parameter key has empty value (full arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
                skipped.add(arg);
                continue;
            }

            if (value.isEmpty()) {
                debug("Parameter key: " + C.YELLOW + key + C.RED + " has empty value (full arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
                skipped.add(arg);
                continue;
            }

            remainingArgs.add(arg);
        }

        // Quick equals
        looping: for (String arg : remainingArgs.copy()) {
            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];
            for (DecreeParameter option : options) {
                if (option.getNames().contains(key)) {
                    if (parseParamInto(parameters, option, value)) {
                        options.remove(option);
                        remainingArgs.remove(arg);
                    }
                    continue looping;
                }
            }
        }

        // Ignored case
        looping: for (String arg : remainingArgs.copy()) {
            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];
            for (DecreeParameter option : options) {
                for (String name : option.getNames()) {
                    if (name.equalsIgnoreCase(key)) {
                        if (parseParamInto(parameters, option, value)) {
                            options.remove(option);
                            remainingArgs.remove(arg);
                        }
                        continue looping;
                    }
                }
            }
        }

        // Name contains key (key substring of name)
        looping: for (String arg : remainingArgs.copy()) {
            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];
            for (DecreeParameter option : options) {
                for (String name : option.getNames()) {
                    if (name.contains(key)) {
                        if (parseParamInto(parameters, option, value)) {
                            options.remove(option);
                            remainingArgs.remove(arg);
                        }
                        continue looping;
                    }
                }
            }
        }

        // Key contains name (name substring of key)
        looping: for (String arg : remainingArgs.copy()) {
            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];
            for (DecreeParameter option : options) {
                for (String name : option.getNames()) {
                    if (key.contains(name)) {
                        if (parseParamInto(parameters, option, value)) {
                            options.remove(option);
                            remainingArgs.remove(arg);
                        }
                        continue looping;
                    }
                }
            }
        }

        debug("Conducted default processing on arguments", C.GREEN);

        // Keyless arguments debug
        if (keylessArgs.isNotEmpty()) {
            debug("Leftover arguments: " + C.YELLOW + keylessArgs.toString(", ") + C.RED + " / Leftover options: " + C.YELLOW + options.convert(DecreeParameter::getName).toString(", "), C.RED);
            if (options.isEmpty()) {
                debug("Leftover arguments " + C.YELLOW + keylessArgs.toString(", ") + C.RED + " found but no remaining options.", C.RED);
                skipped.addAll(keylessArgs);
            }
        }

        // Keyless arguments (not accepting DecreeWhichExceptions)
        looping: for (DecreeParameter option : options.copy()) {
            for (String keylessArg : keylessArgs.copy()) {

                try {
                    Object result = option.getHandler().parse(keylessArg);
                    if (result == null) {
                        throw new NullPointerException("Parsed value is null");
                    }
                    options.remove(option);
                    keylessArgs.remove(keylessArg);
                    parameters.put(option, result);
                    continue looping;

                } catch (DecreeParsingException | DecreeWhichException ignored) {
                    // This argument could not be parsed here, hence is not mapped here

                } catch (Throwable e) {
                    // This exception is actually something that is broken
                    debug("Parsing " + C.YELLOW + keylessArg + C.RED + " into " + C.YELLOW + option.getName() + C.RED + " failed because of: " + C.YELLOW + e.getMessage(), C.RED);
                    e.printStackTrace();
                }
            }
        }

        // Keyless arguments (now does accept DecreeWhichExceptions)
        looping: for (DecreeParameter option : options.copy()) {
            for (String keylessArg : keylessArgs.copy()) {

                try {
                    Object result = option.getHandler().parse(keylessArg);
                    if (result == null) {
                        throw new NullPointerException("Parsed value is null");
                    }
                    options.remove(option);
                    keylessArgs.remove(keylessArg);
                    parameters.put(option, result);
                    continue looping;

                } catch (DecreeParsingException ignored) {
                    // This argument could not be parsed here, hence is not mapped here

                } catch (DecreeWhichException e) {
                    debug("Please handle this exception: " + C.YELLOW + e.getMessage(), C.RED);

                } catch (Throwable e) {
                    // This exception is actually something that is broken
                    debug("Parsing " + C.YELLOW + keylessArg + C.RED + " into " + C.YELLOW + option.getName() + C.RED + " failed because of: " + C.YELLOW + e.getMessage(), C.RED);
                    e.printStackTrace();
                }
            }
        }

        if (skipped.isNotEmpty()) {
            sender.sendMessage(C.RED + "Skipped arguments: " + skipped.toString(", "));
        }

        return parameters;
    }

    /**
     * Parses a parameter into a map after parsing
     * @param parameters The parameter map to store the value into
     * @param option The parameter type to parse into
     * @param value The value to parse
     * @return True if successful, false if not. Nothing is added on parsing failure.
     */
    private boolean parseParamInto(ConcurrentHashMap<DecreeParameter, Object> parameters, DecreeParameter option, String value) {
        try {
            parameters.put(option, option.getHandler().parse(value));
            return true;
        } catch (Throwable e) {
            system.debug("Failed to parse into: '" + option.getName() + "' value '" + value + "'");
            e.printStackTrace();
        }
        return false;
    }

    /*
    private ConcurrentHashMap<String, Object> map(DecreeSender sender, KList<String> in) {
        ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();
        KList<Integer> skip = new KList<>();

        for (int ix = 0; ix < in.size(); ix++) {
            String i = in.get(ix);

            if (i.contains("=")) {
                String[] v = i.split("\\Q=\\E");
                String key = v[0];
                String value = v[1];
                DecreeParameter param = null;

                // Shallow match
                for (DecreeParameter j : getNode().getParameters()) {
                    for (String k : j.getNames()) {
                        if (k.equalsIgnoreCase(key)) {
                            param = j;
                            break;
                        }
                    }
                }

                // Deep match
                if (param == null) {
                    for (DecreeParameter j : getNode().getParameters()) {
                        for (String k : j.getNames()) {
                            if (k.toLowerCase().contains(key.toLowerCase()) || key.toLowerCase().contains(k.toLowerCase())) {
                                param = j;
                                break;
                            }
                        }
                    }
                }

                // Skip param
                if (param == null) {
                    system.debug("Can't find parameter key for " + key + "=" + value + " in " + getPath());
                    sender.sendMessage(C.YELLOW + "Unknown Parameter: " + key);
                    continue;
                }

                key = param.getName();

                try {
                    data.put(key, param.getHandler().parse(value, skip.contains(ix)));
                } catch (DecreeParsingException e) {
                    system.debug("Can't parse parameter value for " + key + "=" + value + " in " + getPath() + " using handler " + param.getHandler().getClass().getSimpleName());
                    sender.sendMessage(C.RED + "Cannot convert \"" + value + "\" into a " + param.getType().getSimpleName());
                    return null;
                } catch (DecreeWhichException e) {
                    KList<?> validOptions = param.getHandler().getPossibilities(value);
                    system.debug("Found multiple results for " + key + "=" + value + " in " + getPath() + " using the handler " + param.getHandler().getClass().getSimpleName() + " with potential matches [" + validOptions.toString(",") + "]. Asking client to define one");
                    String update = pickValidOption(sender, validOptions, param.getHandler(), param.getName(), param.getType().getSimpleName());
                    if (update == null) { return null; }
                    system.debug("Client chose " + update + " for " + key + "=" + value + " (old) in " + getPath());
                    in.set(ix--, update);
                }
            } else {
                try {
                    DecreeParameter param = getNode().getParameters().get(ix);
                    try {
                        data.put(param.getName(), param.getHandler().parse(i, skip.contains(ix)));
                    } catch (DecreeParsingException e) {
                        system.debug("Can't parse parameter value for " + param.getName() + "=" + i + " in " + getPath() + " using handler " + param.getHandler().getClass().getSimpleName());
                        sender.sendMessage(C.RED + "Cannot convert \"" + i + "\" into a " + param.getType().getSimpleName());
                        e.printStackTrace();
                        return null;
                    } catch (DecreeWhichException e) {
                        system.debug("Can't parse parameter value for " + param.getName() + "=" + i + " in " + getPath() + " using handler " + param.getHandler().getClass().getSimpleName());
                        KList<?> validOptions = param.getHandler().getPossibilities(i);
                        String update = pickValidOption(sender, validOptions, param.getHandler(), param.getName(), param.getType().getSimpleName());
                        if (update == null) { return null; }
                        system.debug("Client chose " + update + " for " + param.getName() + "=" + i + " (old) in " + getPath());
                        skip.add(ix);
                        in.set(ix--, update);
                    }
                } catch (IndexOutOfBoundsException e) {
                    sender.sendMessage(C.YELLOW + "Unknown Parameter: " + i + " (" + Form.getNumberSuffixThStRd(ix + 1) + " argument)");
                }
            }
        }

        return data;
    }

    @SuppressWarnings("SpellCheckingInspection")
    String[] gradients = new String[]{
            "<gradient:#f5bc42:#45b32d>",
            "<gradient:#1ed43f:#1ecbd4>",
            "<gradient:#1e2ad4:#821ed4>",
            "<gradient:#d41ea7:#611ed4>",
            "<gradient:#1ed473:#1e55d4>",
            "<gradient:#6ad41e:#9a1ed4>"
    };

    private String pickValidOption(DecreeSender sender, KList<?> validOptions, DecreeParameterHandler<?> handler, String name, String type) {
        if (!sender.isPlayer()) {
            String picked = handler.toStringForce(validOptions.getRandom());
            sender.sendMessage(C.RED + "We went ahead and picked " + picked + " (" + name + " of " + type + ")");
            return picked;
        }
        sender.sendHeader("Pick a " + name + " (" + type + ")");
        sender.sendMessageRaw("<gradient:#1ed497:#b39427>This query will expire in 15 seconds.</gradient>");
        String password = UUID.randomUUID().toString().replaceAll("\\Q-\\E", "");
        int m = 0;

        for(String i : validOptions.convert(handler::toStringForce))
        {
            sender.sendMessage( "<hover:show_text:'" + gradients[m%gradients.length] + i+"</gradient>'><click:run_command:decree-future "+ password + " " + i+">"+"- " + gradients[m%gradients.length] +   i         + "</gradient></click></hover>");
            m++;
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        system.postFuture(password, future);

        if(system.isCommandSound() && sender.isPlayer())
        {
            (sender.player()).playSound((sender.player()).getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.77f, 0.65f);
            (sender.player()).playSound((sender.player()).getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.125f, 1.99f);
        }

        try {
            return future.get(15, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ignored) {

        }

        return null;
    }

    private boolean invokeNode(DecreeSender sender, ConcurrentHashMap<String, Object> map) {
        if (map == null) {
            return false;
        }

        Object[] params = new Object[getNode().getMethod().getParameterCount()];
        int vm = 0;
        for (DecreeParameter i : getNode().getParameters()) {
            Object value = map.get(i.getName());

            if (value == null && i.hasDefault()) {
                try {
                    value = i.getDefaultValue();
                } catch (DecreeParsingException e) {
                    system.debug("Can't parse parameter value for " + i.getName() + "=" + i.getParam().defaultValue() + " in " + getPath() + " using handler " + i.getHandler().getClass().getSimpleName());
                    sender.sendMessage(C.RED + "Cannot convert \"" + i.getParam().defaultValue() + "\" into a " + i.getType().getSimpleName());
                    return false;
                } catch (DecreeWhichException e) {
                    system.debug("Can't parse parameter value for " + i.getName() + "=" + i.getParam().defaultValue() + " in " + getPath() + " using handler " + i.getHandler().getClass().getSimpleName());
                    KList<?> validOptions = i.getHandler().getPossibilities(i.getParam().defaultValue());
                    String update = pickValidOption(sender, validOptions, i.getHandler(), i.getName(), i.getType().getSimpleName());
                    if (update == null) {
                        return false;
                    }
                    system.debug("Client chose " + update + " for " + i.getName() + "=" + i.getParam().defaultValue() + " (old) in " + getPath());
                    value = update;
                }
            }

            if (sender.isPlayer() && i.isContextual() && value == null) {
                DecreeContextHandler<?> ch = DecreeContextHandler.contextHandlers.get(i.getType());

                if (ch != null) {
                    value = ch.handle(sender);

                    if (value != null) {
                        system.debug("Parameter \"" + i.getName() + "\" derived a value of \"" + i.getHandler().toStringForce(value) + "\" from " + ch.getClass().getSimpleName());
                    } else {
                        system.debug("Parameter \"" + i.getName() + "\" could not derive a value from " + ch.getClass().getSimpleName());
                    }
                } else {
                    system.debug("Parameter \"" + i.getName() + "\" is contextual but has no context handler for " + i.getType().getCanonicalName());
                }
            }

            if (i.hasDefault() && value == null) {
                try {
                    system.debug("Parameter \"" + i.getName() + " is using default value \"" + i.getParam().defaultValue() + "\"");
                    value = i.getDefaultValue();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            if (i.isRequired() && value == null) {
                sender.sendMessage(C.RED + "Parameter missing: \"" + i.getName() + "\" (" + i.getType().getSimpleName() + ") as the " + Form.getNumberSuffixThStRd(vm + 1) + " argument.");
                sender.sendDecreeHelpNode(this);
                return false;
            }

            params[vm] = value;
            vm++;
        }

        Runnable rx = () -> {
            try {
                try {
                    DecreeContext.touch(sender);
                    getNode().getMethod().setAccessible(true);
                    getNode().getMethod().invoke(getNode().getParent(), params);
                } catch (InvocationTargetException e) {
                    if (e.getCause().getMessage().endsWith("may only be triggered synchronously.")) {
                        sender.sendMessage(C.RED + "The command you tried to run (" + getPath() + ") may only be run sync! Contact your admin!");
                        return;
                    }
                    throw e;
                }
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to execute " + getPath());
            }
        };

        if (getNode().isSync()) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(system.getInstance(), rx);
        } else {
            rx.run();
        }

        return true;
    }
    */
}
