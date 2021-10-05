package nl.codevs.decree.virtual;

import lombok.Getter;
import nl.codevs.decree.DecreeSettings;
import nl.codevs.decree.DecreeSystem;
import nl.codevs.decree.decrees.DecreeCommandExecutor;
import nl.codevs.decree.util.DecreeOrigin;
import nl.codevs.decree.util.DecreeSender;
import nl.codevs.decree.util.C;
import nl.codevs.decree.util.Form;
import nl.codevs.decree.util.KList;

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
    private final DecreeCommandExecutor instance;
    private final DecreeSystem system;

    public DecreeCategory(DecreeCategory parent, DecreeCommandExecutor instance, Decree decree, DecreeSystem system) {
        this.parent = parent;
        this.decree = decree;
        this.instance = instance;
        this.system = system;
        this.commands = prepCommands();
        this.subCats = prepSubCats(system);
    }

    /**
     * Calculate {@link DecreeCategory}s in this category
     * @param system The system to send debug messages upon failure
     */
    private KList<DecreeCategory> prepSubCats(DecreeSystem system) {
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
                system.debug("Could not get child \"" + subCat.getName() + "\" from instance: \"" + getInstance().getClass().getSimpleName() + "\"");
                system.debug("Because of: " + e.getMessage());
                continue;
            }
            if (childRoot == null) {
                try {
                    childRoot = subCat.getType().getConstructor().newInstance();
                    subCat.set(getInstance(), childRoot);
                } catch (NoSuchMethodException e) {
                    system.debug("Method \"" + subCat.getName() + "\" does not exist in instance: \"" + getInstance().getClass().getSimpleName() + "\"");
                    system.debug("Because of: " + e.getMessage());
                } catch (IllegalAccessException e) {
                    system.debug("Could get, but not access child \"" + subCat.getName() + "\" from instance: \"" + getInstance().getClass().getSimpleName() + "\"");
                    system.debug("Because of: " + e.getMessage());
                } catch (InstantiationException e) {
                    system.debug("Could not instantiate \"" + subCat.getName() + "\" from instance: \"" + getInstance().getClass().getSimpleName() + "\"");
                    system.debug("Because of: " + e.getMessage());
                } catch (InvocationTargetException e) {
                    system.debug("Invocation exception on \"" + subCat.getName() + "\" from instance: \"" + getInstance().getClass().getSimpleName() + "\"");
                    system.debug("Because of: " + e.getMessage());
                    system.debug("Underlying exception: " + e.getTargetException().getMessage());
                }
            }

            if (childRoot == null) {
                continue;
            }

            subCats.add(new DecreeCategory(this, (DecreeCommandExecutor) childRoot, childRoot.getClass().getDeclaredAnnotation(Decree.class), system));
        }

        return subCats;
    }

    /**
     * Calculate {@link DecreeCommand}s in this category
     */
    private KList<DecreeCommand> prepCommands() {
        KList<DecreeCommand> commands = new KList<>();

        for (Method command : getInstance().getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(command.getModifiers()) || Modifier.isFinal(command.getModifiers()) || Modifier.isPrivate(command.getModifiers())) {
                continue;
            }

            if (!command.isAnnotationPresent(Decree.class)) {
                continue;
            }

            commands.add(new DecreeCommand(this, command, system));
        }

        return commands;
    }

    /**
     * @return Subcategories
     */
    private KList<DecreeCategory> getSubCats() {
        return subCats.copy();
    }

    /**
     * @return Commands
     */
    private KList<DecreeCommand> getCommands() {
        return commands.copy();
    }

    /**
     * Match a subcategory or command of this category
     * @param in The string to use to query
     * @param sender The {@link DecreeSender} to use to search
     * @return A list of {@link Decreed} or null
     */
    public KList<Decreed> matchAll(String in, DecreeSender sender){

        if (DecreeSettings.debugMatching) {
            if (!subCats.isEmpty()) {
                debug("Comparing: " + C.GOLD + in + C.GREEN + " with Categories " + C.GOLD + (getSubCats().isEmpty() ? "NONE" : getSubCats().convert(c -> c.getNames().toString(C.GREEN + ", " + C.GOLD)).toString(C.GREEN + " / " + C.GOLD)), C.GREEN);
            }
            if (!commands.isEmpty()) {
                debug("Comparing: " + C.GOLD + in + C.GREEN + " with Commands " + C.GOLD + (getCommands().isEmpty() ? "NONE" : getCommands().convert(c -> c.getNames().toString(C.GREEN + ", " + C.GOLD)).toString(C.GREEN + " / " + C.GOLD)), C.GREEN);
            }
        }

        KList<Decreed> matches = new KList<>();

        for (DecreeCategory subCat : getSubCats()) {
            if (subCat.doesMatch(in, sender) > 0) {
                matches.add(subCat);
            }
        }

        for (DecreeCommand command : getCommands()) {
            if (command.doesMatch(in, sender) > 0) {
                matches.add(command);
            }
        }

        return matches.qremoveDuplicates();
    }

    /**
     * Send help to a sender for the category header (as it would appear in the help list of another category)
     * @param sender The sender to send help to
     */
    public void sendNodeHelp(DecreeSender sender) {

        String hoverTitle = "<#42ecf5>" + getNames().toString(", ");
        String hoverDescription = "<#3fe05a>✎ <#6ad97d><font:minecraft:uniform>" + getDescription();
        String hoverUsage = "<#bbe03f>✒ <#a8e0a2><font:minecraft:uniform>This is a command category. Click to run.";
        String hoverPermission;
        String hoverOrigin = "<#dbe61c>⌘ <#d61aba><#ff33cc><font:minecraft:uniform>" + Form.capitalize(getOrigin().toString().toLowerCase());

        String runOnClick = getPath();
        String realText = "<#46826a>⇀<gradient:#42ecf5:#428df5> " + getName() + "<gradient:#afe3d3:#a2dae0> - Category of Commands";

        // Permission
        if (!getPermission().equals(Decree.NO_PERMISSION)){
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
            hoverOrigin += "<#0ba10b> origin, so you can use it.";
        } else {
            hoverOrigin += "<#c4082e> origin, so you cannot use it.";
        }

        sender.sendMessageRaw("<hover:show_text:'" +
                hoverTitle +
                newline + hoverDescription +
                newline + hoverUsage +
                (hoverPermission.isEmpty() ? "" : newline) + hoverPermission +
                (hoverOrigin.isEmpty() ? "" : newline) + hoverOrigin + "'>" +
                "<click:run_command:" + runOnClick + ">" + realText + "</click>" +
                "</hover>");
    }

    /**
     * Send category help to a player
     * @param sender The sender to send help to
     */
    @Override
    public void sendHelpTo(DecreeSender sender) {

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

        KList<Decreed> matches = matchAll(null, sender);
        if (matches.isNotEmpty()) {
            for (Decreed match : matches) {
                if (match instanceof DecreeCategory c) {
                    if (c.matchAll(null, sender).isNotEmpty()) {
                        c.sendNodeHelp(sender);
                    }
                } else if (match.doesMatch(sender) > 0){
                    match.sendHelpTo(sender);
                }
            }
        } else {
            sender.sendMessage(C.RED + "There are no subcommands or categories in this group! Contact an administrator, this is a command design issue!");
        }
    }

    @Override
    public boolean run(KList<String> args, DecreeSender sender) {
        debug("Arguments: " + C.GOLD + args.toString(C.GREEN + ", " + C.GOLD), C.GREEN);
        if (args.isEmpty()) {
            debug("Finished here", C.GREEN);
            sendHelpTo(sender);
            return true;
        }
        for (Decreed decreed : matchAll(args.get(0), sender)) {
            // If there are no allowed / visible nodes in a category, do not show
            if (decreed instanceof DecreeCategory c) {
                if (c.matchAll(null, sender).isEmpty()) {
                    continue;
                }
            }
            debug("Running matched Decreed: " + C.GOLD + decreed.getShortestName(), C.GREEN);
            if (decreed.run(args.subList(1, args.size()), sender)) {
                return true;
            }
        }
        sendHelpTo(sender);
        sender.sendMessage(C.RED + "Could not find command or subcategory " + C.GOLD + args.get(0));
        sender.sendMessage(C.YELLOW + "Please double-check your command, or click on one above.");
        return false;
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
    public DecreeSystem system() {
        return getSystem();
    }

    @Override
    public String getName() {
        return capitalToLine(decree().name().isEmpty() ? getInstance().getClass().getSimpleName() : decree().name());
    }
}
