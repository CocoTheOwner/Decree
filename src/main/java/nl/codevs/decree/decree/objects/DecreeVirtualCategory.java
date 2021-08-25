/*
 * Iris is a World Generator for Minecraft Bukkit Servers
 * Copyright (c) 2021 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package nl.codevs.decree.decree.objects;

import lombok.Data;
import nl.codevs.decree.decree.*;
import nl.codevs.decree.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.util.*;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class DecreeVirtualCategory {
    private final Class<? extends DecreeCommandExecutor> realClass;
    private final DecreeVirtualCategory parent;
    private final DecreeCommand node;
    private final DecreeSystem system;
    private final KList<DecreeVirtualCategory> nodes = new KList<>();
    private final KList<DecreeVirtualCategory> commands = new KList<>();

    private DecreeVirtualCategory(Class<? extends DecreeCommandExecutor> realClass, DecreeVirtualCategory parent, DecreeCommand node, DecreeSystem system) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        this.realClass = realClass;
        this.parent = parent;
        this.node = node;
        this.system = system;
        calcSubNodes(this, realClass, system);
        calcCommands(this, realClass, system);
    }

    public static DecreeVirtualCategory createOrigin(DecreeCommandExecutor rootClass, DecreeSystem system) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        return new DecreeVirtualCategory(rootClass.getClass(), null, null, system);
    }

    public static DecreeVirtualCategory createRoot(DecreeVirtualCategory parent, DecreeCommandExecutor rootClass, DecreeSystem system) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        return new DecreeVirtualCategory(rootClass.getClass(), parent, null, system);
    }

    /**
     * Calculate commands in this category
     * @param forCategory The virtual category to store commands in
     * @param fromClass The class to search for commands
     * @param system The system running this Decree instance
     */
    private static void calcCommands(DecreeVirtualCategory forCategory, Class<? extends DecreeCommandExecutor> fromClass, DecreeSystem system) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        for (Method i : fromClass.getDeclaredMethods()) {
            if (!i.isAnnotationPresent(Decree.class) ||
                    Modifier.isStatic(i.getModifiers()) ||
                    Modifier.isFinal(i.getModifiers()) ||
                    Modifier.isPrivate(i.getModifiers())
            ){
                continue;
            }

            forCategory.getCommands().add(new DecreeVirtualCategory(fromClass, forCategory, new DecreeCommand(fromClass, i), system));
        }
    }

    /**
     * Calculate subnodes of this category
     * @param forCategory The virtual category to store subnodes in
     * @param fromClass The class to search for subnodes
     * @param system The system running this Decree instance
     */
    private static void calcSubNodes(DecreeVirtualCategory forCategory, Class<? extends DecreeCommandExecutor> fromClass, DecreeSystem system) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        for (Field i : fromClass.getDeclaredFields()) {

            if (!i.getType().isAnnotationPresent(Decree.class) ||
                    Modifier.isStatic(i.getModifiers()) ||
                    Modifier.isFinal(i.getModifiers()) ||
                    Modifier.isTransient(i.getModifiers()) ||
                    Modifier.isVolatile(i.getModifiers())
            ){
                continue;
            }

            i.setAccessible(true);
            Object childRoot = i.get(fromClass);

            if (childRoot == null) {
                childRoot = i.getType().getConstructor().newInstance();
                i.set(fromClass, childRoot);
            }

            forCategory.getNodes().add(createRoot(forCategory, (DecreeCommandExecutor) childRoot, system));
        }
    }

    public DecreeOrigin getOrigin(){
        return isNode() ? getNode().getOrigin() : getRealClass().getDeclaredAnnotation(Decree.class).origin();
    }

    public String getPath() {
        KList<String> n = new KList<>();
        DecreeVirtualCategory cursor = this;

        while (cursor.getParent() != null) {
            cursor = cursor.getParent();
            n.add(cursor.getName());
        }

        return "/" + n.reverse().qadd(getName()).toString(" ");
    }

    public String getParentPath() {
        return getParent().getPath();
    }

    public String getName() {
        return isNode() ? getNode().getName() : getRealClass().getDeclaredAnnotation(Decree.class).name();
    }

    public String getDescription() {
        return isNode() ? getNode().getDescription() : getRealClass().getDeclaredAnnotation(Decree.class).description();
    }

    public KList<String> getNames() {
        if (isNode()) {
            return getNode().getNames();
        }

        KList<String> d = new KList<>();
        Decree dc = getRealClass().getDeclaredAnnotation(Decree.class);
        for (String i : dc.aliases()) {
            if (i.isEmpty()) {
                continue;
            }

            d.add(i);
        }

        d.add(dc.name());
        d.removeDuplicates();

        return d;
    }

    public boolean isNode() {
        return node != null;
    }

    public KList<String> tabComplete(KList<String> args, String raw, DecreeSender sender) {
        KList<Integer> skip = new KList<>();
        KList<String> tabs = new KList<>();
        invokeTabComplete(args, skip, tabs, raw, sender);
        return tabs;
    }

    private boolean invokeTabComplete(KList<String> args, KList<Integer> skip, KList<String> tabs, String raw, DecreeSender sender) {

        if (isNode()) {
            tab(args, tabs);
            skip.add(hashCode());
            return false;
        }

        if (args.isEmpty()) {
            tab(args, tabs);
            return true;
        }

        String head = args.get(0);

        if (args.size() > 1 || head.endsWith(" ")) {
            DecreeVirtualCategory match = matchNode(head, skip, sender);

            if (match != null) {
                args.pop();
                return match.invokeTabComplete(args, skip, tabs, raw, sender);
            }

            skip.add(hashCode());
        } else {
            tab(args, tabs);
        }

        return false;
    }

    private void tab(KList<String> args, KList<String> tabs) {
        String last = null;
        KList<DecreeParameter> ignore = new KList<>();
        Runnable la = () -> {

        };
        for (String a : args) {
            la.run();
            last = a;
            la = () -> {
                if (isNode()) {
                    String sea = a.contains("=") ? a.split("\\Q=\\E")[0] : a;
                    sea = sea.trim();

                    searching:
                    for (DecreeParameter i : getNode().getParameters()) {
                        for (String m : i.getNames()) {
                            if (m.equalsIgnoreCase(sea) || m.toLowerCase().contains(sea.toLowerCase()) || sea.toLowerCase().contains(m.toLowerCase())) {
                                ignore.add(i);
                                continue searching;
                            }
                        }
                    }
                }
            };
        }

        if (last != null) {
            if (isNode()) {
                for (DecreeParameter i : getNode().getParameters()) {
                    if (ignore.contains(i)) {
                        continue;
                    }

                    int g = 0;

                    if (last.contains("=")) {
                        String[] vv = last.trim().split("\\Q=\\E");
                        String vx = vv.length == 2 ? vv[1] : "";
                        for (String f : i.getHandler().getPossibilities(vx).convert((v) -> i.getHandler().toStringForce(v))) {
                            g++;
                            tabs.add(i.getName() + "=" + f);
                        }
                    } else {
                        for (String f : i.getHandler().getPossibilities("").convert((v) -> i.getHandler().toStringForce(v))) {
                            g++;
                            tabs.add(i.getName() + "=" + f);
                        }
                    }

                    if (g == 0) {
                        tabs.add(i.getName() + "=");
                    }
                }
            } else {
                for (DecreeVirtualCategory i : getNodes()) {
                    String m = i.getName();
                    if (m.equalsIgnoreCase(last) || m.toLowerCase().contains(last.toLowerCase()) || last.toLowerCase().contains(m.toLowerCase())) {
                        tabs.addAll(i.getNames());
                    }
                }
            }
        }
    }

    private ConcurrentHashMap<String, Object> map(DecreeSender sender, KList<String> in) {
        ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();

        for (int ix = 0; ix < in.size(); ix++) {
            String i = in.get(ix);
            if (i.contains("=")) {
                String[] v = i.split("\\Q=\\E");
                String key = v[0];
                String value = v[1];
                DecreeParameter param = null;

                for (DecreeParameter j : getNode().getParameters()) {
                    for (String k : j.getNames()) {
                        if (k.equalsIgnoreCase(key)) {
                            param = j;
                            break;
                        }
                    }
                }

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

                if (param == null) {
                    system.debug("Can't find parameter key for " + key + "=" + value + " in " + getPath());
                    sender.sendMessage(C.YELLOW + "Unknown Parameter: " + key);
                    continue;
                }

                key = param.getName();

                try {
                    data.put(key, param.getHandler().parse(value));
                } catch (DecreeParsingException e) {
                    system.debug("Can't parse parameter value for " + key + "=" + value + " in " + getPath() + " using handler " + param.getHandler().getClass().getSimpleName());
                    sender.sendMessage(C.RED + "Cannot convert \"" + value + "\" into a " + param.getType().getSimpleName());
                    return null;
                } catch (DecreeWhichException e) {
                    KList<?> validOptions = param.getHandler().getPossibilities(value);
                    system.debug("Found multiple results for " + key + "=" + value + " in " + getPath() + " using the handler " + param.getHandler().getClass().getSimpleName() + " with potential matches [" + validOptions.toString(",") + "]. Asking client to define one");
                    String update = null; // TODO: PICK ONE
                    system.debug("Client chose " + update + " for " + key + "=" + value + " (old) in " + getPath());
                    in.set(ix--, update);
                }
            } else {
                try {
                    DecreeParameter par = getNode().getParameters().get(ix);
                    try {
                        data.put(par.getName(), par.getHandler().parse(i));
                    } catch (DecreeParsingException e) {
                        system.debug("Can't parse parameter value for " + par.getName() + "=" + i + " in " + getPath() + " using handler " + par.getHandler().getClass().getSimpleName());
                        sender.sendMessage(C.RED + "Cannot convert \"" + i + "\" into a " + par.getType().getSimpleName());
                        return null;
                    } catch (DecreeWhichException e) {
                        system.debug("Can't parse parameter value for " + par.getName() + "=" + i + " in " + getPath() + " using handler " + par.getHandler().getClass().getSimpleName());
                        KList<?> validOptions = par.getHandler().getPossibilities(i);
                        String update = null; // TODO: PICK ONE
                        system.debug("Client chose " + update + " for " + par.getName() + "=" + i + " (old) in " + getPath());
                        in.set(ix--, update);
                    }
                } catch (IndexOutOfBoundsException e) {
                    sender.sendMessage(C.YELLOW + "Unknown Parameter: " + i + " (" + Form.getNumberSuffixThStRd(ix + 1) + " argument)");
                }
            }
        }

        return data;
    }

    public DecreeResult invoke(DecreeSender sender, KList<String> realArgs) {
        return invoke(sender, realArgs, new KList<>());
    }

    public DecreeResult invoke(DecreeSender sender, KList<String> args, KList<Integer> skip) {

        system.debug("@ " + getPath() + " with " + args.toString(", "));
        if (isNode()) {
            system.debug("Invoke " + getPath() + "(" + args.toString(",") + ") at ");
            if (invokeNode(sender, map(sender, args))) {
                return DecreeResult.SUCCESSFUL;
            }

            skip.add(hashCode());
            return DecreeResult.NOT_FOUND;
        }

        if (args.isEmpty()) {
            sender.sendDecreeHelp(this);

            return DecreeResult.NO_ARGUMENTS;
        }

        String head = args.get(0);
        DecreeVirtualCategory match = matchNode(head, skip, sender);

        if (match != null) {
            args.pop();
            return match.invoke(sender, args, skip);
        }

        skip.add(hashCode());

        return DecreeResult.NOT_FOUND;
    }

    private boolean invokeNode(DecreeSender sender, ConcurrentHashMap<String, Object> map) {
        if (map == null) {
            return false;
        }

        Object[] params = new Object[getNode().getMethod().getParameterCount()];
        int vm = 0;
        for (DecreeParameter i : getNode().getParameters()) {
            Object value = map.get(i.getName());

            try {
                if (value == null && i.hasDefault()) {
                    value = i.getDefaultValue();
                }
            } catch (DecreeParsingException e) {
                system.debug("Can't parse parameter value for " + i.getName() + "=" + i + " in " + getPath() + " using handler " + i.getHandler().getClass().getSimpleName());
                sender.sendMessage(C.RED + "Cannot convert \"" + i + "\" into a " + i.getType().getSimpleName());
                return false;
            } catch (DecreeWhichException e) {
                system.debug("Can't parse parameter value for " + i.getName() + "=" + i + " in " + getPath() + " using handler " + i.getHandler().getClass().getSimpleName());
                KList<?> validOptions = i.getHandler().getPossibilities(i.getParam().defaultValue());
                String update = null; // TODO: PICK ONE
                system.debug("Client chose " + update + " for " + i.getName() + "=" + i + " (old) in " + getPath());
                try {
                    value = i.getDefaultValue();
                } catch (DecreeParsingException x) {
                    x.printStackTrace();
                    system.debug("Can't parse parameter value for " + i.getName() + "=" + i + " in " + getPath() + " using handler " + i.getHandler().getClass().getSimpleName());
                    sender.sendMessage(C.RED + "Cannot convert \"" + i + "\" into a " + i.getType().getSimpleName());
                    return false;
                } catch (DecreeWhichException x) {
                    x.printStackTrace();
                }
            }

            if (i.isContextual() && value == null) {
                DecreeContextHandler<?> ch = DecreeContextHandler.contextHandlers.get(i.getType());

                if (ch != null) {
                    value = ch.handle(sender);

                    if (value != null) {
                        system.debug("Null Parameter " + i.getName() + " derived a value of " + i.getHandler().toStringForce(value) + " from " + ch.getClass().getSimpleName());
                    } else {
                        system.debug("Null Parameter " + i.getName() + " could not derive a value from " + ch.getClass().getSimpleName());
                    }
                } else {
                    system.debug("Null Parameter " + i.getName() + " is contextual but has no context handler for " + i.getType().getCanonicalName());
                }
            }

            if (i.hasDefault() && value == null) {
                try {
                    system.debug("Null Parameter " + i.getName() + " is using default value " + i.getParam().defaultValue());
                    value = i.getDefaultValue();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            if (i.isRequired() && value == null) {
                sender.sendMessage("Missing: " + i.getName() + " (" + i.getType().getSimpleName() + ") as the " + Form.getNumberSuffixThStRd(vm + 1) + " argument.");
                return false;
            }

            params[vm] = value;
            vm++;
        }

        Runnable rx = () -> {
            try {
                DecreeContext.touch(sender);
                getNode().getMethod().setAccessible(true);
                getNode().getMethod().invoke(getNode().getInstance(), params);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to execute <INSERT REAL NODE HERE>"); // TODO:
            }
        };

        if (getNode().isSync()) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(system.instance(), rx);
        } else {
            rx.run();
        }

        return true;
    }

    public KList<DecreeVirtualCategory> matchAllNodes(String in, DecreeSender sender) {
        KList<DecreeVirtualCategory> g = new KList<>();

        if (in.trim().isEmpty()) {
            g.addAll(nodes);
            return g;
        }

        for (DecreeVirtualCategory i : nodes) {
            if (i.matches(in) && i.getOrigin().validFor(sender)) {
                g.add(i);
            }
        }

        for (DecreeVirtualCategory i : nodes) {
            if (i.deepMatches(in) && i.getOrigin().validFor(sender)) {
                g.add(i);
            }
        }

        g.removeDuplicates();
        return g;
    }

    public DecreeVirtualCategory matchNode(String in, KList<Integer> skip, DecreeSender sender) {

        if (in.trim().isEmpty()) {
            return null;
        }

        for (DecreeVirtualCategory i : nodes) {
            if (skip.contains(i.hashCode())) {
                continue;
            }

            if (i.matches(in) && i.getOrigin().validFor(sender)) {
                return i;
            }
        }

        for (DecreeVirtualCategory i : nodes) {
            if (skip.contains(i.hashCode())) {
                continue;
            }

            if (i.deepMatches(in) && i.getOrigin().validFor(sender)) {
                return i;
            }
        }

        return null;
    }

    public boolean deepMatches(String in) {
        KList<String> a = getNames();

        for (String i : a) {
            if (i.toLowerCase().contains(in.toLowerCase()) || in.toLowerCase().contains(i.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDescription(), getRealClass(), getPath());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DecreeVirtualCategory)) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    public boolean matches(String in) {
        KList<String> a = getNames();

        for (String i : a) {
            if (i.equalsIgnoreCase(in)) {
                return true;
            }
        }

        return false;
    }
}
