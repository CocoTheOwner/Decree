package nl.codevs.decree.decree.objects;

import nl.codevs.decree.decree.DecreeSender;
import nl.codevs.decree.decree.util.KList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@Deprecated // because we're just going to be using DecreeVirtualCommand
public class DecreeCategory implements Decreed {
    public final DecreeCategory parent;
    public final KList<DecreeCommand> commands;
    public final KList<DecreeCategory> subCats;
    public final Decree decree;
    private final Object instance;

    public DecreeCategory(DecreeCategory parent, Object instance, Decree decree) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        this.parent = parent;
        this.decree = decree;
        this.instance = instance;
        this.commands = calcCommands();
        this.subCats = calcSubCats();
    }

    /**
     * Calculate subcategories in this category
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

            subCats.add(new DecreeCategory(this, childRoot, childRoot.getClass().getDeclaredAnnotation(Decree.class)));
        }

        return subCats;
    }

    /**
     * Calculate commands in this category
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

            commands.add(new DecreeCommand(parent(), command));
        }

        return commands;
    }

    /**
     * Match a subcategory or command of this category
     * @param in The string to use to query
     * @param skip A {@link KList} of {@link Decreed}s to skip while searching
     * @param sender The {@link DecreeSender} to use to search
     * @return A {@link Decreed} or null
     */
    public Decreed match(String in, KList<Decreed> skip, DecreeSender sender){
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
        return null;
    }

    @Override
    public boolean invoke(KList<String> args, DecreeSender sender) {
        return false;
    }
}
