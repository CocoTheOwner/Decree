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
import org.bukkit.Sound;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

@Data
public class DecreeVirtualCommand implements Decreed {
    private final DecreeVirtualCommand parent;
    private final Decree decree;
    private final KList<DecreeVirtualCommand> nodes;
    private final DecreeCommand node;
    private final DecreeSystem system;

    private DecreeVirtualCommand(DecreeVirtualCommand parent, Decree decree, KList<DecreeVirtualCommand> nodes, DecreeCommand node, DecreeSystem system) {
        this.parent = parent;
        this.decree = decree;
        this.nodes = nodes;
        this.node = node;
        this.system = system;
    }

    public static DecreeVirtualCommand createOrigin(Object v, Decree decree, DecreeSystem system) throws Throwable {
        return createRoot(null, v, decree, system);
    }

    public static DecreeVirtualCommand createRoot(DecreeVirtualCommand parent, Object v, Decree decree, DecreeSystem system) throws Throwable {
        DecreeVirtualCommand c = new DecreeVirtualCommand(parent, decree, new KList<>(), null, system);

        for (Field i : v.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(i.getModifiers()) || Modifier.isFinal(i.getModifiers()) || Modifier.isTransient(i.getModifiers()) || Modifier.isVolatile(i.getModifiers())) {
                continue;
            }

            if (!i.getType().isAnnotationPresent(Decree.class)) {
                continue;
            }

            i.setAccessible(true);
            Object childRoot = i.get(v);

            if (childRoot == null) {
                childRoot = i.getType().getConstructor().newInstance();
                i.set(v, childRoot);
            }

            c.getNodes().add(createRoot(c, childRoot, childRoot.getClass().getDeclaredAnnotation(Decree.class), system));
        }

        for (Method i : v.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(i.getModifiers()) || Modifier.isFinal(i.getModifiers()) || Modifier.isPrivate(i.getModifiers())) {
                continue;
            }

            if (!i.isAnnotationPresent(Decree.class)) {
                continue;
            }

            c.getNodes().add(new DecreeVirtualCommand(c, decree, new KList<>(), new DecreeCommand(v, i, system), system));
        }

        return c;
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

    public boolean isNode() {
        return getNode() != null;
    }

    public String getName() {
        return isNode() ? getNode().getName() : getDecree().name();
    }

    public DecreeOrigin getOrigin(){
        return isNode() ? getNode().getOrigin() : getDecree().origin();
    }

    public String getPermission(){
        return isNode() ? getNode().getPermission() : getDecree().permission();
    }

    public String getDescription() {
        return isNode() ? getNode().getDescription() : getDecree().description();
    }

    public KList<String> getNames() {
        if (isNode()) {
            return getNode().getNames();
        }

        KList<String> d = new KList<>();
        for (String i : getDecree().aliases()) {
            if (i.isEmpty()) {
                continue;
            }

            d.add(i);
        }

        d.add(getDecree().name());
        d.removeDuplicates();

        return d;
    }

    public KList<String> invokeTabComplete(KList<String> args, DecreeSender sender) {

        if (isNode() || args.isEmpty() || args.size() <= 1 && !args.get(0).endsWith(" ")) {
            return tab(args);
        }

        DecreeVirtualCommand match = matchNode(args.get(0), new KList<>(), sender);

        if (match == null) {
            return new KList<>();
        }

        args.remove(0);
        return match.invokeTabComplete(args, sender);
    }

    private KList<String> tab(KList<String> args) {

        if (args.isEmpty()) {
            return new KList<>();
        }

        KList<String> tabs = new KList<>();
        String last = args.popLast();
        KList<DecreeParameter> ignore = new KList<>();

        // Remove auto-completions for existing keys
        for (String a : args) {
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
        }

        // Add auto-completions
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
                    tabs.add(i.getName() + "=" + i.getDefaultRaw());
                }
            }
        } else {
            for (DecreeVirtualCommand i : getNodes()) {
                String m = i.getName();
                if (m.equalsIgnoreCase(last) || m.toLowerCase().contains(last.toLowerCase()) || last.toLowerCase().contains(m.toLowerCase())) {
                    tabs.addAll(i.getNames());
                }
            }
        }

        return tabs;
    }

    private ConcurrentHashMap<String, Object> map(DecreeSender sender, KList<String> in) {
        ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();
        KList<Integer> skip = new KList<>();

        for (int ix = 0; ix < in.size(); ix++) {
            String i = in.get(ix);

            if (i == null)
            {
                system.debug("Param " + ix + " is null? (\"" + in.toString(",") + "\")");
                continue;
            }

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
            sender.sendMessage( "<hover:show_text:'" + gradients[m%gradients.length] + i+"</gradient>'><click:run_command:decreefuture "+ password + " " + i+">"+"- " + gradients[m%gradients.length] +   i         + "</gradient></click></hover>");
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

    public boolean invoke(DecreeSender sender, KList<String> args, KList<Integer> skip) {

        system.debug("@ " + getPath() + " with " + args.toString(", "));
        if (isNode()) {
            system.debug("Invoke " + getPath() + "(" + args.toString(",") + ") at ");
            if (invokeNode(sender, map(sender, args))) {
                return true;
            }

            skip.add(hashCode());
            return false;
        }

        if (args.isEmpty()) {
            sender.sendDecreeHelp(this);

            return true;
        }

        String head = args.get(0);
        DecreeVirtualCommand match = matchNode(head, skip, sender);

        if (match != null) {
            args.pop();
            return match.invoke(sender, args, skip);
        }

        skip.add(hashCode());

        return false;
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
                String update = pickValidOption(sender, validOptions, i.getHandler(), i.getName(), i.getType().getSimpleName());
                if (update == null) { return false; }
                system.debug("Client chose " + update + " for " + i.getName() + "=" + i + " (old) in " + getPath());
                value = update;
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

    // Category
    public DecreeVirtualCommand matchNode(String in, KList<Integer> skip, DecreeSender sender) {

        if (in.trim().isEmpty()) {
            return null;
        }

        for (DecreeVirtualCommand i : nodes) {
            if (skip.contains(i.hashCode()) || !i.getOrigin().validFor(sender) || !sender.hasPermission(i.getPermission())) {
                continue;
            }

            if (i.matches(in)) {
                return i;
            }
        }

        for (DecreeVirtualCommand i : nodes) {
            if (skip.contains(i.hashCode()) || !i.getOrigin().validFor(sender) || !sender.hasPermission(i.getPermission())) {
                continue;
            }

            if (i.deepMatches(in)) {
                return i;
            }
        }

        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), decree(), getPath());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DecreeVirtualCommand)) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }
}
