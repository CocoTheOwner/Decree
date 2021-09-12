package nl.codevs.decree.decree.objects;

import lombok.Getter;
import nl.codevs.decree.decree.DecreeSystem;
import nl.codevs.decree.decree.util.C;
import nl.codevs.decree.decree.util.Form;
import nl.codevs.decree.decree.util.KList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@Getter
public class DecreeCategory implements Decreed {
    private static final String newline = "<reset>\n";
    private final DecreeCategory parent;
    private final KList<DecreeCommand> commands;
    private final KList<DecreeCategory> subCats;
    private final Decree decree;
    private final Object instance;
    private final DecreeSystem system;

    public DecreeCategory(DecreeCategory parent, Object instance, Decree decree, DecreeSystem system) {
        this.parent = parent;
        this.decree = decree;
        this.instance = instance;
        this.system = system;
        this.commands = calcCommands();
        this.subCats = calcSubCats(system);
    }

    /**
     * Calculate {@link DecreeCategory}s in this category
     * @param system The system to send debug messages upon failure
     */
    private KList<DecreeCategory> calcSubCats(DecreeSystem system) {
        KList<DecreeCategory> subCats = new KList<>();

        for (Field subCat : getInstance().getClass().getDeclaredFields()) {
            if (Modifier.isStatic(subCat.getModifiers()) || Modifier.isFinal(subCat.getModifiers()) || Modifier.isTransient(subCat.getModifiers()) || Modifier.isVolatile(subCat.getModifiers())) {
                continue;
            }

            if (!subCat.getType().isAnnotationPresent(Decree.class)) {
                continue;
            }

            subCat.setAccessible(true);
            Object childRoot;
            try {
                childRoot = subCat.get(getInstance());
            } catch (IllegalAccessException e) {
                system.debug("Could not get child \"" + subCat.getName() + "\" from instance: \"" + getInstance() + "\"");
                system.debug("Because of: " + e.getMessage());
                continue;
            }
            try {
                if (childRoot == null) {
                    childRoot = subCat.getType().getConstructor().newInstance();
                    subCat.set(getInstance(), childRoot);
                }
            } catch (NoSuchMethodException e) {
                system.debug("Method \"" + subCat.getName() + "\" does not exist in instance: \"" + getInstance() + "\"");
                system.debug("Because of: " + e.getMessage());
            } catch (IllegalAccessException e) {
                system.debug("Could get, but not access child \"" + subCat.getName() + "\" from instance: \"" + getInstance() + "\"");
                system.debug("Because of: " + e.getMessage());
            } catch (InstantiationException e) {
                system.debug("Could instantiate \"" + subCat.getName() + "\" from instance: \"" + getInstance() + "\"");
                system.debug("Because of: " + e.getMessage());
            } catch (InvocationTargetException e) {
                system.debug("Invocation exception on \"" + subCat.getName() + "\" from instance: \"" + getInstance() + "\"");
                system.debug("Because of: " + e.getMessage());
                system.debug("Underlying exception: " + e.getTargetException().getMessage());
            }

            if (childRoot == null) {
                continue;
            }

            subCats.add(new DecreeCategory(this, childRoot, childRoot.getClass().getDeclaredAnnotation(Decree.class), getSystem()));
        }

        return subCats;
    }

    /**
     * Calculate {@link DecreeCommand}s in this category
     */
    private KList<DecreeCommand> calcCommands() {
        KList<DecreeCommand> commands = new KList<>();

        for (Method command : getInstance().getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(command.getModifiers()) || Modifier.isFinal(command.getModifiers()) || Modifier.isPrivate(command.getModifiers())) {
                continue;
            }

            if (!command.isAnnotationPresent(Decree.class)) {
                continue;
            }

            commands.add(new DecreeCommand(this, command, getSystem()));
        }

        return commands;
    }

    /**
     * Match a subcategory or command of this category
     * @param in The string to use to query
     * @param sender The {@link DecreeSender} to use to search
     * @return A {@link Decreed} or null
     */
    public Decreed matchOne(String in, DecreeSender sender){
        if (in.trim().isEmpty()){
            return null;
        }

        for (DecreeCategory subCat : getSubCats()) {
            if (subCat.doesMatchAllowed(sender, in)){
                return subCat;
            }
        }

        for (DecreeCommand command : getCommands()) {
            if (command.doesMatchAllowed(sender, in)){
                return command;
            }
        }

        for (DecreeCategory subCat : getSubCats()) {
            if (subCat.doesDeepMatchAllowed(sender, in)){
                return subCat;
            }
        }

        for (DecreeCommand command : getCommands()) {
            if (command.doesDeepMatchAllowed(sender, in)){
                return command;
            }
        }

        return null;
    }

    /**
     * Match a subcategory or command of this category
     * @param in The string to use to query
     * @param sender The {@link DecreeSender} to use to search
     * @return A list of {@link Decreed} or null
     */
    public KList<Decreed> matchAll(String in, DecreeSender sender) {
        return matchAll(in, sender, true);
    }

    /**
     * Match a subcategory or command of this category
     * @param in The string to use to query
     * @param sender The {@link DecreeSender} to use to search
     * @param deepSearch Whether to also search for overlap, instead of 1:1 matches
     * @return A list of {@link Decreed} or null
     */
    public KList<Decreed> matchAll(String in, DecreeSender sender, boolean deepSearch){

        KList<Decreed> matches = new KList<>();

        for (DecreeCategory subCat : getSubCats()) {
            if (!matches.contains(subCat)  && subCat.doesMatchAllowed(sender, in)){
                matches.add(subCat);
            }
        }

        for (DecreeCommand command : getCommands()) {
            if (!matches.contains(command) && command.doesMatchAllowed(sender, in)){
                matches.add(command);
            }
        }

        if (deepSearch) {
            for (DecreeCategory subCat : getSubCats()) {
                if (!matches.contains(subCat) && subCat.doesDeepMatchAllowed(sender, in)) {
                    matches.add(subCat);
                }
            }

            for (DecreeCommand command : getCommands()) {
                if (!matches.contains(command) && command.doesDeepMatchAllowed(sender, in)) {
                    matches.add(command);
                }
            }
        }

        return matches;
    }

    public KList<DecreeCommand> getCommands() {
        return commands.copy();
    }

    public KList<DecreeCategory> getSubCats() {
        return subCats.copy();
    }

    public void sendHelpTo(DecreeSender sender) {

        if (getSubCats().isNotEmpty() || getCommands().isNotEmpty()) {
            sender.sendHeader(Form.capitalize(getName()) + " Help");

            // Back button
            if (sender.isPlayer() && getParent() != null) {
                sender.sendMessageRaw(
                        "<hover:show_text:'<#b54b38>Click to go back to <#3299bf>" + Form.capitalize(getParent().getName()) + " Help'>" +
                            "<click:run_command:" + getParent().getPath() + ">" +
                                "<font:minecraft:uniform><#f58571>〈 Back" +
                            "</click>" +
                        "</hover>"
                );
            }

            for (DecreeCategory subCat : getSubCats()) {
                sender.sendMessageRaw(subCat.getHelp(sender));
            }
            for (DecreeCommand command : getCommands()) {
                sender.sendMessageRaw(command.getHelp(sender));
            }

        } else {
            sender.sendMessage(C.RED + "There are no subcommands in this group! Contact an administrator, this is a command design issue!");
        }

    }

    @Override
    public String getHelp(DecreeSender sender) {

        if (!sender.isPlayer()) {
            return getPath();
        }

        String hoverTitle = getNames().copy().convert((f) -> "<#42ecf5>" + f).toString(", ");
        String hoverUsage = "<#bbe03f>✒ <#a8e0a2><font:minecraft:uniform> This is a command category. Click to run.";
        String hoverPermission;
        String hoverOrigin = "<#dbe61c>⌘ <#d61aba><#ff33cc><font:minecraft:uniform>" + Form.capitalize(getOrigin().toString().toLowerCase());

        String runOnClick = getPath();
        String realText = "<#46826a>⇀<gradient:#42ecf5:#428df5> " + getName() + "<gradient:#afe3d3:#a2dae0> - Category of Commands";

        // Permission
        if (!getDecree().permission().equals(Decree.NO_PERMISSION)){
            String granted;
            if (sender.isOp() || sender.hasPermission(getDecree().permission())){
                granted = "<#a73abd>(Granted)";
            } else {
                granted = "<#db4321>(Not Granted)";
            }
            hoverPermission = "<#2181db>⏍ <#78dcf0><font:minecraft:uniform>Permission: <#ffa500>" + getDecree().permission() + " " + granted + newline;
        } else {
            hoverPermission = "";
        }

        // Origin
        if (getOrigin().equals(DecreeOrigin.BOTH)){
            hoverOrigin = "";
        } else if (getOrigin().validFor(sender)) {
            hoverOrigin += "<#0ba10b> origin, so you can use it.";
        } else {
            hoverOrigin += "<#c4082e> origin, so you cannot use it.";
        }

        return "<hover:show_text:'" +
                    hoverTitle + newline +
                    hoverUsage + newline +
                    hoverPermission + (hoverPermission.isEmpty() ? "" : newline) +
                    hoverOrigin + "'>" +
                    "<click:run_command:" + runOnClick + ">" + realText + "</click>" +
                "</hover>";
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
        return decree().name().isEmpty() ? getInstance().getClass().getSimpleName() : decree().name();
    }

    @Override
    public KList<String> tab(KList<String> args, DecreeSender sender) {
        if (args.isEmpty()) {
            // This node is reached but there are no more (partial) arguments
            return new KList<>();

        } else if (args.size() == 1) {
            // This is the final node, send all options from here
            return matchAll(args.get(0), sender).convert(Decreed::getName);

        } else {
            // This is not the final node, so follow all possible branches downwards
            String head = args.pop();

            KList<Decreed> matches = matchAll(head, sender, false);
            if (matches.isEmpty()) {
                matches = matchAll(head, sender, true);
            }
            matches.removeDuplicates();

            KList<String> tabs = new KList<>();
            for (Decreed match : matches) {
                tabs.addAll(match.tab(args.copy(), sender));
            }

            return tabs;
        }
    }

    @Override
    public boolean invoke(KList<String> args, DecreeSender sender) {
        if (args.isNotEmpty()) {

            getSystem().debug("Category: \"" + getName() + "\" - Processed: \"" + getPath() + "\" - Remaining: [" + args.toString(", ") + "]");

            String head = args.pop();
            KList<Decreed> matches = matchAll(head, sender);
            for (Decreed match : matches) {
                if (match.invoke(args, sender)){
                    return true;
                }
            }
            getSystem().debug(C.RED + "FAILED: \"" + getName() + "\"" + " from path \"" + getPath() + "\". Remaining: " + (args.isEmpty() ? "NONE" : "\"" + args.toString(", ") + "\""));
            sender.sendMessage(C.RED + "The " + getName() + " category could not find a match for " + head + ". If you believe this is an error, contact an admin.");
            return false;
        }

        getSystem().debug("Category: \"" + getName() + "\" - Processed: \"" + getPath() + "\" - Remaining: [] - Action: Sending help");
        sendHelpTo(sender);
        return true;
    }
}
