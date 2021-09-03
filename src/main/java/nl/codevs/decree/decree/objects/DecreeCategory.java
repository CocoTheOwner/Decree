package nl.codevs.decree.decree.objects;

import nl.codevs.decree.decree.DecreeSender;
import nl.codevs.decree.decree.DecreeSystem;
import nl.codevs.decree.decree.util.KList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

        for (Field subCat : instance.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(subCat.getModifiers()) || Modifier.isFinal(subCat.getModifiers()) || Modifier.isTransient(subCat.getModifiers()) || Modifier.isVolatile(subCat.getModifiers())) {
                continue;
            }

            if (!subCat.getType().isAnnotationPresent(Decree.class)) {
                continue;
            }

            subCat.setAccessible(true);
            Object childRoot = subCat.get(instance);

            if (childRoot == null) {
                childRoot = subCat.getType().getConstructor().newInstance();
                subCat.set(instance, childRoot);
            }

            subCats.add(new DecreeCategory(this, childRoot, childRoot.getClass().getDeclaredAnnotation(Decree.class), system));
        }

        return subCats;
    }

    /**
     * Calculate {@link DecreeCommand}s in this category
     */
    private KList<DecreeCommand> calcCommands(){
        KList<DecreeCommand> commands = new KList<>();

        for (Method command : instance.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(command.getModifiers()) || Modifier.isFinal(command.getModifiers()) || Modifier.isPrivate(command.getModifiers())) {
                continue;
            }

            if (!command.isAnnotationPresent(Decree.class)) {
                continue;
            }

            commands.add(new DecreeCommand(parent(), command, system));
        }

        return commands;
    }

    /**
     * Match a subcategory or command of this category
     * @param in The string to use to query
     * @param sender The {@link DecreeSender} to use to search
     * @return A {@link Decreed} or null
     */
    public Decreed matchOne(String in, DecreeSender sender) {
        return matchOne(in, new KList<>(), sender);
    }

    /**
     * Match a subcategory or command of this category
     * @param in The string to use to query
     * @param skip A {@link KList} of {@link Decreed}s to skip while searching
     * @param sender The {@link DecreeSender} to use to search
     * @return A {@link Decreed} or null
     */
    public Decreed matchOne(String in, KList<Decreed> skip, DecreeSender sender){
        if (in.trim().isEmpty()){
            return null;
        }

        for (DecreeCommand command : commands) {
            if (!skip.contains(command) && command.doesMatchAllowed(sender, in)){
                return command;
            }
        }

        for (DecreeCategory subCat : subCats) {
            if (!skip.contains(subCat) && subCat.doesMatchAllowed(sender, in)){
                return subCat;
            }
        }

        for (DecreeCommand command : commands) {
            if (!skip.contains(command) && command.doesDeepMatchAllowed(sender, in)){
                return command;
            }
        }

        for (DecreeCategory subCat : subCats) {
            if (!skip.contains(subCat) && subCat.doesDeepMatchAllowed(sender, in)){
                return subCat;
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
        return matchAll(in, new KList<>(), sender);
    }

    /**
     * Match a subcategory or command of this category
     * @param in The string to use to query
     * @param skip A {@link KList} of {@link Decreed}s to skip while searching
     * @param sender The {@link DecreeSender} to use to search
     * @return A list of {@link Decreed} or null
     */
    public KList<Decreed> matchAll(String in, KList<Decreed> skip, DecreeSender sender){

        KList<Decreed> matches = new KList<>();

        for (DecreeCommand command : commands) {
            if (!skip.contains(command) && !matches.contains(command) && command.doesMatchAllowed(sender, in)){
                matches.add(command);
            }
        }

        for (DecreeCategory subCat : subCats) {
            if (!skip.contains(subCat) && !matches.contains(subCat)  && subCat.doesMatchAllowed(sender, in)){
                matches.add(subCat);
            }
        }

        for (DecreeCommand command : commands) {
            if (!skip.contains(command) && !matches.contains(command)  && command.doesDeepMatchAllowed(sender, in)){
                matches.add(command);
            }
        }

        for (DecreeCategory subCat : subCats) {
            if (!skip.contains(subCat) && !matches.contains(subCat)  && subCat.doesDeepMatchAllowed(sender, in)){
                matches.add(subCat);
            }
        }

        return matches;
    }

    @Override
    public Decreed parent() {
        return parent;
    }

    @Override
    public Decree decree() {
        return decree;
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
        return false;
    }
}
