package nl.codevs.decree.decree.objects;

import lombok.Data;
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
    public final DecreeCategory parent;
    public final KList<DecreeCommand> commands;
    public final KList<DecreeCategory> subCats;
    public final Decree decree;
    private final Object instance;
    private final DecreeSystem system;

    public DecreeCategory(DecreeCategory parent, Object instance, Decree decree, DecreeSystem system) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        this.parent = parent;
        this.decree = decree;
        this.instance = instance;
        this.system = system;
        this.commands = calcCommands();
        this.subCats = calcSubCats();
    }

    /**
     * Calculate {@link DecreeCategory}s in this category
     */
    private KList<DecreeCategory> calcSubCats() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        KList<DecreeCategory> subCats = new KList<>();

        for (Field subCat : getInstance().getClass().getDeclaredFields()) {
            if (Modifier.isStatic(subCat.getModifiers()) || Modifier.isFinal(subCat.getModifiers()) || Modifier.isTransient(subCat.getModifiers()) || Modifier.isVolatile(subCat.getModifiers())) {
                continue;
            }

            if (!subCat.getType().isAnnotationPresent(Decree.class)) {
                continue;
            }

            subCat.setAccessible(true);
            Object childRoot = subCat.get(getInstance());

            if (childRoot == null) {
                childRoot = subCat.getType().getConstructor().newInstance();
                subCat.set(getInstance(), childRoot);
            }

            subCats.add(new DecreeCategory(this, childRoot, childRoot.getClass().getDeclaredAnnotation(Decree.class), getSystem()));
        }

        return subCats;
    }

    /**
     * Calculate {@link DecreeCommand}s in this category
     */
    private KList<DecreeCommand> calcCommands(){
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
    public KList<Decreed> matchAll(String in, DecreeSender sender){

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

        for (DecreeCategory subCat : getSubCats()) {
            if (!matches.contains(subCat)  && subCat.doesDeepMatchAllowed(sender, in)){
                matches.add(subCat);
            }
        }

        for (DecreeCommand command : getCommands()) {
            if (!matches.contains(command)  && command.doesDeepMatchAllowed(sender, in)){
                matches.add(command);
            }
        }

        return matches;
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
    public void sendHelpTo(DecreeSender sender) {
        sender.sendHeader(Form.capitalize(getName()) + " Category");
        sender.sendMessage(C.GREEN + "Categories (" + getSubCats().size() + ")");
        getSubCats().convert(Decreed::getPath).forEach(sender::sendMessageRaw);
        sender.sendMessage(C.GREEN + "Commands (" + getCommands().size() + ")");
        getCommands().convert(Decreed::getPath).forEach(sender::sendMessageRaw);
    }

    @Override
    public String getName() {
        return decree().name().isEmpty() ? getInstance().getClass().getSimpleName() : decree().name();
    }

    @Override
    public KList<String> tab(KList<String> args, DecreeSender sender) {
        if (args.isEmpty()) {
            return new KList<>();
        } else if (args.size() == 1) {
            return matchAll(args.get(0), sender).convert(Decreed::getName);
        } else {
            Decreed match = matchOne(args.remove(0), sender);
            if (match == null) {
                return new KList<>();
            }
            return match.tab(args, sender);
        }
    }

    @Override
    public boolean invoke(KList<String> args, DecreeSender sender) {
        if (args.isNotEmpty()) {

            getSystem().debug("Category: \"" + getName() + "\" - Processed: \"" + getPath() + "\" - Remaining: [" + args.toString(", ") + "]");

            KList<Decreed> matches = matchAll(args.pop(), sender);
            for (Decreed match : matches) {
                if (match.invoke(args, sender)){
                    return true;
                }
            }
            getSystem().debug(C.RED + "FAILED: \"" + getName() + "\"" + " from path \"" + getPath() + "\". Remaining: " + (args.isEmpty() ? "NONE" : "\"" + args.toString(", ") + "\""));
            return false;
        }

        getSystem().debug("Category: \"" + getName() + "\" - Processed: \"" + getPath() + "\" - Remaining: [] - Action: Sending help");
        sendHelpTo(sender);
        return true;
    }
}
