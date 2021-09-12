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

/*
import lombok.Data;
import nl.codevs.decree.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.util.C;
import nl.codevs.decree.decree.util.Form;
import nl.codevs.decree.decree.util.KList;
import org.bukkit.Bukkit;
import org.bukkit.Sound;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Data
public class DecreeVirtualCommand {

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


}

 */
