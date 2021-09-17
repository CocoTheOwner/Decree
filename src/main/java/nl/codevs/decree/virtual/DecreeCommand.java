package nl.codevs.decree.virtual;

import lombok.Data;
import nl.codevs.decree.DecreeSystem;
import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.util.DecreeOrigin;
import nl.codevs.decree.util.DecreeSender;
import nl.codevs.decree.util.C;
import nl.codevs.decree.util.Form;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
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
    private static final int nullParam = Integer.MAX_VALUE - 69420;
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
        return suggestions.subList(0, Math.min(max, suggestions.size() - 1));
    }

    @Override
    public void sendHelpTo(DecreeSender sender) {

        if (!sender.isPlayer()) {
            sender.sendMessage(getPath() + " " + getParameters().convert(p -> p.getName() + "=" + p.getHandler().getRandomDefault()).toString(" "));
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

        sender.sendMessageRaw("<hover:show_text:'" +
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
                appendedParameters);
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
    public KList<Decreed> get(KList<String> args, DecreeSender sender) {
        return new KList<>(this);
    }

    public KList<String> tab(KList<String> args) {
        return new KList<>(getName());
    }

    public boolean invoke(KList<String> args, DecreeSender sender) {
        if (args.isNotEmpty()) {
            debug("Entered arguments: " + C.YELLOW + args.toString(", "), C.GREEN);
        } else {
            debug("No entered arguments to parse", C.GREEN);
        }

        args.removeIf(Objects::isNull);
        args.removeIf(String::isEmpty);

        ConcurrentHashMap<DecreeParameter, Object> params = computeParameters(args, sender);

        if (params == null) {
            debug("Parameter parsing failed for " + C.YELLOW + getName(), C.RED);
            return false;
        }

        Object[] finalParams = new Object[getParameters().size()];

        // Final checksum
        int x = 0;
        for (DecreeParameter parameter : getParameters()) {
            if (!params.containsKey(parameter)) {
                debug("Failed to handle command, but did not notice one missing: " + C.YELLOW + parameter.getName() + C.RED + "!", C.RED);
                debug("Params stored: " + params, C.RED);
                debug("This is a big problem within the Decree system. Please contact the author(s).", C.RED);
                return false;
            }

            Object value = params.get(parameter);
            finalParams[x++] = value.equals(nullParam) ? null : value;
        }

        Runnable rx = () -> {
            try {
                try {
                    DecreeSystem.Context.touch(sender);
                    getMethod().setAccessible(true);
                    getMethod().invoke(getParent().getInstance(), finalParams);
                } catch (InvocationTargetException e) {
                    if (e.getCause().getMessage().endsWith("may only be triggered synchronously.")) {
                        debug("Sent asynchronously while it must be ran sync", C.RED);
                        e.printStackTrace();
                        sender.sendMessage(C.RED + "The command you tried to run (" + C.YELLOW + getPath() + C.RED + ") may only be run sync! Contact your admin!");
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

        return true;
    }

    /**
     * Compute parameter objects from string argument inputs
     * @param args The arguments (parameters) to parse into this command
     * @param sender The sender of the command
     * @return A {@link ConcurrentHashMap} from the parameter to the instantiated object for that parameter
     */
    private ConcurrentHashMap<DecreeParameter, Object> computeParameters(KList<String> args, DecreeSender sender) {

        /*
         * Apologies for the obscene amount of loops.
         * It is the only way this can be done functionally.
         *
         * Note that despite the great amount of loops the average runtime is still ~O(logn).
         * This is because of the ever-decreasing number of arguments & options that are already matched.
         * If all arguments are already matched in the first (quick equals) loop, the runtime is actually O(n)
         */

        ConcurrentHashMap<DecreeParameter, Object> parameters = new ConcurrentHashMap<>();
        KList<DecreeParameter> options = getParameters();
        KList<String> parseErrorArgs = new KList<>();
        KList<String> whichErrorArgs = new KList<>();
        KList<String> keylessArgs = new KList<>();
        KList<String> keyedArgs = new KList<>();
        KList<String> nullArgs = new KList<>();
        KList<String> badArgs = new KList<>();

        // Split args into correct corresponding handlers
        for (String arg : args) {

            // These are handled later, after other fulfilled options will already have been matched
            int equals = arg.split("=").length;
            if (equals == 0) {
                keylessArgs.add(arg);
                continue;
            } else if (equals == 2) {
                arg = arg.replace("==", "=");
                if (arg.split("=").length == 2) {
                    debug("Parameter fixed by replacing '==' with '=' (new arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
                } else {
                    badArgs.add(arg);
                    continue;
                }
            } else if (equals > 1) {
                debug("Parameter has multiple '=' signs (full arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
            }

            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];

            if (system().isAllowNullInput() && value.equalsIgnoreCase("null")) {
                debug("Null parameter added: " + C.YELLOW + arg, C.GREEN);
                nullArgs.add(key);
                continue;
            }

            if (key.isEmpty()) {
                debug("Parameter key has empty value (full arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
                badArgs.add(arg);
                continue;
            }

            if (value.isEmpty()) {
                debug("Parameter key: " + C.YELLOW + key + C.RED + " has empty value (full arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
                badArgs.add(arg);
                continue;
            }

            keyedArgs.add(arg);
        }

        // Quick equals
        looping: for (String arg : keyedArgs.copy()) {
            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];
            for (DecreeParameter option : options) {
                if (option.getNames().contains(key)) {
                    if (parseParamInto(parameters, option, value)) {
                        options.remove(option);
                        keyedArgs.remove(arg);
                    } else if (system().isNullOnFailure()) {
                        parameters.put(option, nullParam);
                    }
                    continue looping;
                }
            }
        }

        // Ignored case
        looping: for (String arg : keyedArgs.copy()) {
            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];
            for (DecreeParameter option : options) {
                for (String name : option.getNames()) {
                    if (name.equalsIgnoreCase(key)) {
                        if (parseParamInto(parameters, option, value)) {
                            options.remove(option);
                            keyedArgs.remove(arg);
                        } else if (system().isNullOnFailure()) {
                            parameters.put(option, nullParam);
                        }
                        continue looping;
                    }
                }
            }
        }

        // Name contains key (key substring of name)
        looping: for (String arg : keyedArgs.copy()) {
            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];
            for (DecreeParameter option : options) {
                for (String name : option.getNames()) {
                    if (name.contains(key)) {
                        if (parseParamInto(parameters, option, value)) {
                            options.remove(option);
                            keyedArgs.remove(arg);
                        } else if (system().isNullOnFailure()) {
                            parameters.put(option, nullParam);
                        }
                        continue looping;
                    }
                }
            }
        }

        // Key contains name (name substring of key)
        looping: for (String arg : keyedArgs.copy()) {
            String key = arg.split("\\Q=\\E")[0];
            String value = arg.split("\\Q=\\E")[1];
            for (DecreeParameter option : options) {
                for (String name : option.getNames()) {
                    if (key.contains(name)) {
                        if (parseParamInto(parameters, option, value)) {
                            options.remove(option);
                            keyedArgs.remove(arg);
                        } else if (system().isNullOnFailure()) {
                            parameters.put(option, nullParam);
                        }
                        continue looping;
                    }
                }
            }
        }

        // Quick equals null
        looping: for (String key : nullArgs.copy()) {
            for (DecreeParameter option : options) {
                if (option.getNames().contains(key)) {
                    parameters.put(option, nullParam);
                    options.remove(option);
                    nullArgs.remove(key);
                    continue looping;
                }
            }
        }

        // Ignored case null
        looping: for (String key : nullArgs.copy()) {
            for (DecreeParameter option : options) {
                for (String name : option.getNames()) {
                    if (name.equalsIgnoreCase(key)) {
                        parameters.put(option, nullParam);
                        options.remove(option);
                        nullArgs.remove(key);
                        continue looping;
                    }
                }
            }
        }

        // Name contains key (key substring of name), null
        looping: for (String key : nullArgs.copy()) {
            for (DecreeParameter option : options) {
                for (String name : option.getNames()) {
                    if (name.contains(key)) {
                        parameters.put(option, nullParam);
                        options.remove(option);
                        nullArgs.remove(key);
                        continue looping;
                    }
                }
            }
        }

        // Key contains name (name substring of key), null
        looping: for (String key : nullArgs.copy()) {
            for (DecreeParameter option : options) {
                for (String name : option.getNames()) {
                    if (key.contains(name)) {
                        parameters.put(option, nullParam);
                        options.remove(option);
                        nullArgs.remove(key);
                        continue looping;
                    }
                }
            }
        }

        // Keyless arguments
        looping: for (DecreeParameter option : options.copy()) {
            for (String keylessArg : keylessArgs.copy()) {

                try {
                    Object result = option.getHandler().parse(keylessArg);
                    options.remove(option);
                    keylessArgs.remove(keylessArg);
                    parameters.put(option, result);
                    if (parseErrorArgs.contains(keylessArg)) {
                        parseErrorArgs.qremoveDuplicates().remove(keylessArg);
                    }
                    if (whichErrorArgs.contains(keylessArg)) {
                        whichErrorArgs.qremoveDuplicates().remove(keylessArg);
                    }
                    continue looping;

                } catch (DecreeParsingException e) {
                    parseErrorArgs.add(keylessArg);
                } catch (DecreeWhichException e) {
                    whichErrorArgs.add(keylessArg);
                } catch (Throwable e) {
                    // This exception is actually something that is broken
                    debug("Parsing " + C.YELLOW + keylessArg + C.RED + " into " + C.YELLOW + option.getName() + C.RED + " failed because of: " + C.YELLOW + e.getMessage(), C.RED);
                    e.printStackTrace();
                    debug("If you see a handler in the stacktrace that we (" + C.DECREE + "Decree" + C.RED + ") wrote, please report this bug to us.", C.RED);
                    debug("If you see a custom handler of your own, there is an issue with it.", C.RED);
                }
            }
        }

        for (DecreeParameter parameter : options.copy()) {
            if (parameter.hasDefault()) {
                try {
                    parameters.put(parameter, parameter.getDefaultValue());
                    options.remove(parameter);
                } catch (DecreeParsingException e) {
                    if (getSystem().isNullOnFailure()) {
                        parameters.put(parameter, nullParam);
                        options.remove(parameter);
                    } else {
                        debug("Default value " + C.YELLOW + parameter.getDefaultRaw() + C.RED + " could not be parsed to " + parameter.getType().getSimpleName(), C.RED);
                        debug("Reason: " + C.YELLOW + e.getMessage(), C.RED);
                    }
                } catch (DecreeWhichException e) {
                    debug("Default value " + C.YELLOW + parameter.getDefaultRaw() + C.RED + " returned multiple options", C.RED);
                    if (getSystem().isPickFirstOnMultiple() || !sender.isPlayer()) {
                        debug("Adding: " + C.YELLOW + e.getOptions().get(0), C.GREEN);
                        parameters.put(parameter, e.getOptions().get(0));
                        options.remove(parameter);
                    } else {
                        debug("Adding: " + C.YELLOW + e.getOptions().get(0), C.GREEN);
                        parameters.put(parameter, e.getOptions().get(0));
                        options.remove(parameter);
                        debug("Player should be able to pick an option from the options, instead of the above!", C.RED);
                    }
                }
            } else if (parameter.isContextual() && sender.isPlayer()) {
                Object contextValue = DecreeSystem.Context.getHandlers().get(parameter.getType()).handle(sender);
                debug("Context value for " + C.YELLOW + parameter.getName() + C.GREEN + " set to: " + contextValue.toString(), C.GREEN);
                parameters.put(parameter, contextValue);
                options.remove(parameter);
            }
        }

        // Add leftover keyed & null arguments to leftovers
        nullArgs = nullArgs.convert(na -> na + "=null");

        // Debug
        if (system().isAllowNullInput()) {
            debug("Unmatched null argument" + (nullArgs.size() == 1 ? "" : "s") + ": " + C.YELLOW + (nullArgs.isNotEmpty() ? nullArgs.toString(", ") : "NONE"), nullArgs.isEmpty() ? C.GREEN : C.RED);
        }
        debug("Unmatched keyless argument" + (keylessArgs.size() == 1 ? "":"s") + ": " + C.YELLOW + (keylessArgs.isNotEmpty() ? keylessArgs.toString(", ") : "NONE"), keylessArgs.isEmpty() ? C.GREEN : C.RED);
        debug("Unmatched keyed argument" + (keyedArgs.size() == 1 ? "":"s") + ": " + C.YELLOW + (keyedArgs.isNotEmpty() ? keyedArgs.toString(", ") : "NONE"), keyedArgs.isEmpty() ? C.GREEN : C.RED);
        debug("Bad parameter" + (badArgs.size() == 1 ? "":"s") + ": " + C.YELLOW + (badArgs.isNotEmpty() ? badArgs.toString(", ") : "NONE"), badArgs.isEmpty() ? C.GREEN : C.RED);
        debug("Unfulfilled parameter" + (options.size() == 1 ? "":"s") + ": " + C.YELLOW + (options.isNotEmpty() ? options.convert(DecreeParameter::getName).toString(", ") : "NONE"), options.isEmpty() ? C.GREEN : C.RED);

        StringBuilder mappings = new StringBuilder("Parameter mapping:");
        parameters.forEach((param, object) -> mappings
                .append("\n")
                .append(C.GREEN)
                .append("\u0009 - ")
                .append(C.YELLOW)
                .append(param.getName())
                .append(C.GREEN)
                .append(" → ")
                .append(C.YELLOW)
                .append(object.toString()));
        options.forEach(param -> mappings
                .append("\n")
                .append(C.GREEN)
                .append("\u0009 - ")
                .append(C.YELLOW)
                .append(param.getName())
                .append(C.GREEN)
                .append(" → ")
                .append(C.RED)
                .append("NONE"));

        debug(mappings.toString(), C.GREEN);

        if (validateParameters(parameters, sender)) {
            return parameters;
        } else {
            return null;
        }
    }

    /**
     * Validate parameters
     * @param parameters The parameters to validate
     * @param sender The sender of the command
     * @return True if valid, false if not
     */
    private boolean validateParameters(ConcurrentHashMap<DecreeParameter, Object> parameters, DecreeSender sender) {
        boolean valid = true;
        for (DecreeParameter parameter : getParameters()) {
            if (!parameters.containsKey(parameter)) {
                debug("Parameter: " + C.YELLOW + parameter.getName() + C.RED + " not in mapping.", C.RED);
                sender.sendMessage(C.RED + "Parameter: " + C.YELLOW + parameter.getName() + C.RED + " required but not specified. Please add.");
                valid = false;
            }
        }
        return valid;
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