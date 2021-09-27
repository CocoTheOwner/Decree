package nl.codevs.decree.virtual;

import lombok.Data;
import nl.codevs.decree.DecreeSystem;
import nl.codevs.decree.context.DecreeContextHandler;
import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.handlers.DecreeParameterHandler;
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
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Represents a single command (non-category)
 */
@Data
public class DecreeCommand implements Decreed {
    private static final int nullParam = Integer.MAX_VALUE - 69420;
    private static final String[] gradients = new String[]{
            "<gradient:#f5bc42:#45b32d>",
            "<gradient:#1ed43f:#1ecbd4>",
            "<gradient:#1e2ad4:#821ed4>",
            "<gradient:#d41ea7:#611ed4>",
            "<gradient:#1ed473:#1e55d4>",
            "<gradient:#6ad41e:#9a1ed4>"
    };
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
            return;
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

    public KList<String> tab(KList<String> args) {
        return new KList<>(getName());
    }

    @Override
    public boolean run(KList<String> args, DecreeSender sender) {
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

        // Final checksum. Everything should already be valid, but this is just in case.
        int x = 0;
        for (DecreeParameter parameter : getParameters()) {
            if (!params.containsKey(parameter)) {
                debug("Failed to handle command because of missing param: " + C.YELLOW + parameter.getName() + C.RED + "!", C.RED);
                debug("Params stored: " + params, C.RED);
                debug("This is a big problem within the Decree system, as it should have been caught earlier. Please contact the author(s).", C.RED);
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
                    } else {
                        throw e;
                    }
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
        ConcurrentHashMap<DecreeParameter, String> parseExceptionArgs = new ConcurrentHashMap<>();

        KList<DecreeParameter> options = getParameters();
        KList<String> keylessArgs = new KList<>();
        KList<String> keyedArgs = new KList<>();
        KList<String> nullArgs = new KList<>();
        KList<String> badArgs = new KList<>();

        // Split args into correct corresponding handlers
        for (String arg : args) {

            // These are handled later, after other fulfilled options will already have been matched
            KList<String> splitArg = new KList<>(arg.split("="));

            if (splitArg.size() == 1) {

                keylessArgs.add(arg);
                continue;
            }

            if (splitArg.size() > 2) {
                String oldArg = null;
                while (!arg.equals(oldArg)) {
                    oldArg = arg;
                    arg = arg.replaceAll("==", "=");
                }

                splitArg = new KList<>(arg.split("="));

                if (splitArg.size() == 2) {
                    debug("Parameter fixed by replacing '==' with '=' (new arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
                } else {
                    badArgs.add(arg);
                    continue;
                }
            }

            if (system().isAllowNullInput() && splitArg.get(1).equalsIgnoreCase("null")) {
                debug("Null parameter added: " + C.YELLOW + arg, C.GREEN);
                nullArgs.add(splitArg.get(0));
                continue;
            }

            if (splitArg.get(0).isEmpty()) {
                debug("Parameter key has empty value (full arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
                badArgs.add(arg);
                continue;
            }

            if (splitArg.get(1).isEmpty()) {
                debug("Parameter key: " + C.YELLOW + splitArg.get(0) + C.RED + " has empty value (full arg: " + C.YELLOW + arg + C.RED + ")", C.RED);
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
                    if (parseParamInto(parameters, badArgs, parseExceptionArgs, option, value, sender)) {
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
                        if (parseParamInto(parameters, badArgs, parseExceptionArgs, option, value, sender)) {
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
                        if (parseParamInto(parameters, badArgs, parseExceptionArgs, option, value, sender)) {
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
                        if (parseParamInto(parameters, badArgs, parseExceptionArgs, option, value, sender)) {
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

                if (system().isAllowNullInput() && keylessArg.equalsIgnoreCase("null")) {
                    debug("Null parameter added: " + C.YELLOW + keylessArg, C.GREEN);
                    parameters.put(option, nullParam);
                    continue looping;
                }

                try {
                    Object result = option.getHandler().parse(keylessArg);
                    parseExceptionArgs.remove(option);
                    options.remove(option);
                    keylessArgs.remove(keylessArg);
                    parameters.put(option, result);
                    continue looping;

                } catch (DecreeParsingException e) {
                    parseExceptionArgs.put(option, keylessArg);
                } catch (DecreeWhichException e) {
                    parseExceptionArgs.remove(option);
                    options.remove(option);
                    keylessArgs.remove(keylessArg);

                    if (getSystem().isPickFirstOnMultiple()) {
                        parameters.put(option, e.getOptions().get(0));
                    } else {
                        Object result = pickValidOption(sender, e.getOptions(), option);
                        if (result == null) {
                            badArgs.add(keylessArg);
                        } else {
                            parameters.put(option, result);
                        }
                        continue looping;
                    }
                } catch (Throwable e) {
                    // This exception is actually something that is broken
                    debug("Parsing " + C.YELLOW + keylessArg + C.RED + " into " + C.YELLOW + option.getName() + C.RED + " failed because of: " + C.YELLOW + e.getMessage(), C.RED);
                    e.printStackTrace();
                    debug("If you see a handler in the stacktrace that we (" + C.DECREE + "Decree" + C.RED + ") wrote, please report this bug to us.", C.RED);
                    debug("If you see a custom handler of your own, there is an issue with it.", C.RED);
                }
            }
        }

        // Remaining parameters
        for (DecreeParameter option : options.copy()) {
            if (option.hasDefault()) {
                parseExceptionArgs.remove(option);
                try {
                    parameters.put(option, option.getDefaultValue());
                    options.remove(option);
                } catch (DecreeParsingException e) {
                    if (getSystem().isNullOnFailure()) {
                        parameters.put(option, nullParam);
                        options.remove(option);
                    } else {
                        debug("Default value " + C.YELLOW + option.getDefaultRaw() + C.RED + " could not be parsed to " + option.getType().getSimpleName(), C.RED);
                        debug("Reason: " + C.YELLOW + e.getMessage(), C.RED);
                    }
                } catch (DecreeWhichException e) {
                    debug("Default value " + C.YELLOW + option.getDefaultRaw() + C.RED + " returned multiple options", C.RED);
                    options.remove(option);
                    if (getSystem().isPickFirstOnMultiple()) {
                        debug("Adding: " + C.YELLOW + e.getOptions().get(0), C.GREEN);
                        parameters.put(option, e.getOptions().get(0));
                    } else {
                        Object result = pickValidOption(sender, e.getOptions(), option);
                        if (result == null) {
                            badArgs.add(option.getDefaultRaw());
                        } else {
                            parameters.put(option, result);
                        }
                    }
                }
            } else if (option.isContextual() && sender.isPlayer()) {
                parseExceptionArgs.remove(option);
                DecreeContextHandler<?> handler = DecreeSystem.Context.getHandlers().get(option.getType());
                if (handler == null) {
                    debug("Parameter" + option.getName() + " marked as contextual without available context handler (" + option.getType() + ").", C.RED);
                    sender.sendMessage(C.RED + "Parameter" + option.getName() + " marked as contextual without available context handler (" + option.getType() + "). Please context your admin.");
                    continue;
                }
                Object contextValue = handler.handle(sender);
                debug("Context value for " + C.YELLOW + option.getName() + C.GREEN + " set to: " + handler.handleToString(sender), C.GREEN);
                parameters.put(option, contextValue);
                options.remove(option);
            } else if (parseExceptionArgs.containsKey(option)) {
                debug("Parameter: " + option.getName() + " not fulfilled due to parseException raised prior.", C.RED);
                try {
                    option.getHandler().parse(parseExceptionArgs.get(option));
                } catch (DecreeParsingException e) {
                    e.printStackTrace();
                    sender.sendMessage(C.RED + "Failed to parse " + C.GOLD + parseExceptionArgs.get(option) + C.RED + " (" + C.GOLD + option.getType().getSimpleName() + C.RED + "): " + C.GOLD + e.getMessage());
                } catch (DecreeWhichException e) {
                    debug("Somehow, a parse-exception argument from before, is now throwing a WhichException. This is a problem in the Decree System. Please contact the authors.", C.RED);
                    sender.sendMessage(C.RED + "Somehow, a parse-exception argument from before, is now working. This is a problem in the Decree System. Please contact your admin and ask them to contact the authors of the command system.");
                }
            }
        }

        // Convert nullArgs
        nullArgs = nullArgs.convert(na -> na + "=null");

        // Debug
        if (system().isAllowNullInput()) {
            debug("Unmatched null argument" + (nullArgs.size() == 1 ? "" : "s") + ": " + C.YELLOW + (nullArgs.isNotEmpty() ? nullArgs.toString(", ") : "NONE"), nullArgs.isEmpty() ? C.GREEN : C.RED);
        }
        debug("Unmatched keyless argument" + (keylessArgs.size() == 1 ? "":"s") + ": " + C.YELLOW + (keylessArgs.isNotEmpty() ? keylessArgs.toString(", ") : "NONE"), keylessArgs.isEmpty() ? C.GREEN : C.RED);
        debug("Unmatched keyed argument" + (keyedArgs.size() == 1 ? "":"s") + ": " + C.YELLOW + (keyedArgs.isNotEmpty() ? keyedArgs.toString(", ") : "NONE"), keyedArgs.isEmpty() ? C.GREEN : C.RED);
        debug("Bad argument" + (badArgs.size() == 1 ? "":"s") + ": " + C.YELLOW + (badArgs.isNotEmpty() ? badArgs.toString(", ") : "NONE"), badArgs.isEmpty() ? C.GREEN : C.RED);
        debug("Failed argument" + (parseExceptionArgs.size() == 1 ? "":"s") + ": " + C.YELLOW + (parseExceptionArgs.size() != 0 ? new KList<>(parseExceptionArgs.values()).toString(", ") : "NONE"), parseExceptionArgs.isEmpty() ? C.GREEN : C.RED);
        debug("Unfulfilled parameter" + (options.size() == 1 ? "":"s") + ": " + C.YELLOW + (options.isNotEmpty() ? options.convert(DecreeParameter::getName).toString(", ") : "NONE"), options.isEmpty() ? C.GREEN : C.RED);

        StringBuilder mappings = new StringBuilder("Parameter mapping:");
        parameters.forEach((param, object) -> mappings
                .append("\n")
                .append(C.GREEN)
                .append("\u0009 - (")
                .append(C.YELLOW)
                .append(param.getType().getSimpleName())
                .append(C.GREEN)
                .append(") ")
                .append(C.YELLOW)
                .append(param.getName())
                .append(C.GREEN)
                .append(" → ")
                .append(C.YELLOW)
                .append(object.toString()));
        options.forEach(param -> mappings
                .append("\n")
                .append(C.GREEN)
                .append("\u0009 - (")
                .append(C.YELLOW)
                .append(param.getType().getSimpleName())
                .append(C.GREEN)
                .append(") ")
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
     * Instruct the sender to pick a valid option
     * @param sender The sender that must pick an option
     * @param validOptions The valid options that can be picked (as objects)
     * @return The string value for the selected option
     */
    private Object pickValidOption(DecreeSender sender, KList<?> validOptions, DecreeParameter parameter) {
        DecreeParameterHandler<?> handler = parameter.getHandler();

        int tries = 3;
        KList<String> options = validOptions.convert(handler::toStringForce);
        String result = null;

        sender.sendHeader("Pick a " + parameter.getName() + " (" + parameter.getType().getSimpleName() + ")");
        sender.sendMessageRaw("<gradient:#1ed497:#b39427>This query will expire in 15 seconds.</gradient>");

        while (tries-- > 0 && (result == null || !options.contains(result))) {
            sender.sendMessage("<gradient:#1ed497:#b39427>Please pick a valid option.");
            String password = UUID.randomUUID().toString().replaceAll("\\Q-\\E", "");
            int m = 0;

            for (String i : validOptions.convert(handler::toStringForce)) {
                sender.sendMessage("<hover:show_text:'" + gradients[m % gradients.length] + i + "</gradient>'><click:run_command:/irisdecree " + password + " " + i + ">" + "- " + gradients[m % gradients.length] + i + "</gradient></click></hover>");
                m++;
            }

            CompletableFuture<String> future = new CompletableFuture<>();
            if (sender.isPlayer()) {
                DecreeSystem.Completer.postFuture(password, future);
                system().playSound(false, DecreeSystem.SFX.Picked, sender);
            } else {
                DecreeSystem.Completer.postConsoleFuture(future);
            }

            try {
                result = future.get(15, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ignored) {

            }
        }

        if (result != null && options.contains(result)) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).equals(result)) {
                    return validOptions.get(i);
                }
            }
        } else {
            sender.sendMessage(C.RED + "You did not enter a correct option within 3 tries.");
            sender.sendMessage(C.RED + "Please double-check your arguments & option picking.");
        }

        return null;
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
                sender.sendMessage(C.RED + "Parameter: " + C.YELLOW + parameter.getName() + C.RED + " not specified. Please add.");
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Parses a parameter into a map after parsing
     * @param parameters The parameter map to store the value into
     * @param parseExceptionArgs
     * @param option The parameter type to parse into
     * @param value The value to parse
     * @return True if successful, false if not. Nothing is added on parsing failure.
     */
    private boolean parseParamInto(ConcurrentHashMap<DecreeParameter, Object> parameters, KList<String> badArgs, ConcurrentHashMap<DecreeParameter, String> parseExceptionArgs, DecreeParameter option, String value, DecreeSender sender) {
        try {
            parameters.put(option, option.getHandler().parse(value));
            return true;
        } catch (DecreeWhichException e) {
            debug("Value " + C.YELLOW + value + C.RED + " returned multiple options", C.RED);
            if (getSystem().isPickFirstOnMultiple()) {
                debug("Adding: " + C.YELLOW + e.getOptions().get(0), C.GREEN);
                parameters.put(option, e.getOptions().get(0));
            } else {
                Object result = pickValidOption(sender, e.getOptions(), option);
                if (result == null) {
                    badArgs.add(option.getDefaultRaw());
                } else {
                    parameters.put(option, result);
                }
            }
        } catch (DecreeParsingException e) {
            parseExceptionArgs.put(option, value);
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
