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
import nl.codevs.decree.decree.DecreeSystem;
import nl.codevs.decree.decree.util.Form;
import nl.codevs.decree.decree.util.KList;
import nl.codevs.decree.decree.util.Maths;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Represents a single command (non-category)
 */
@Data
public class DecreeCommand implements Decreed {
    private static final String newline = "<reset>\n";
    private final KList<DecreeParameter> parameters;
    private final Method method;
    private final Object parent;
    private final Decree decree;
    private final DecreeSystem system;

    /**
     * Create a node
     * @param parent The instantiated class containing the
     * @param method Method that represents a Decree (must be annotated by @{@link Decree})
     */
    public DecreeCommand(Object parent, Method method, DecreeSystem system) {
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
        // TODO: Solve command order; contextual is ending up before required
        return parameters.copy().qsort((o1, o2) -> {
                if (o1.isRequired()) {
                    return 0;
                }
                if (o2.isRequired()) {
                    return 1;
                }
                if (o1.isContextual()) {
                    return 0;
                }
                if (o2.isContextual()) {
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
        return new KList<>(suggestions.subList(0, Math.min(max, suggestions.size()) - 1));
    }

    public String getHelp(DecreeSender sender) {

        if (!sender.isPlayer()) {
            return getPath() + " " + getParameters().convert(p -> p.getName() + "=" + p.getHandler().getRandomDefault()).toString(" ");
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

        return "<hover:show_text:'" +
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
                appendedParameters;
    }

    @Override
    public Decreed parent() {
        return (Decreed) getParent();
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
    public KList<String> tab(KList<String> args, DecreeSender sender) {
        // TODO: Fix the other TODO in tab
        if (args.isEmpty()) {
            return new KList<>();
        }

        KList<String> tabs = new KList<>();
        String last = args.popLast();
        KList<DecreeParameter> left = getParameters().copy();

        // Remove auto-completions for existing keys
        for (String a : args) {
            String sea = a.contains("=") ? a.split("\\Q=\\E")[0] : a;
            sea = sea.trim();

            searching:
            for (DecreeParameter i : left) {
                for (String m : i.getNames()) {
                    if (m.equalsIgnoreCase(sea) || m.toLowerCase().contains(sea.toLowerCase()) || sea.toLowerCase().contains(m.toLowerCase())) {
                        left.remove(i);
                        continue searching;
                    }
                }
            }
        }

        // Add auto-completions
        for (DecreeParameter i : left) {

            int quantity = 0;

            if (last.contains("=")) {
                String[] vv = last.trim().split("\\Q=\\E");
                String vx = vv.length == 2 ? vv[1] : "";
                for (String possibility : i.getHandler().getPossibilities(vx).convert((v) -> i.getHandler().toStringForce(v))) {
                    quantity++;
                    tabs.add(i.getName() + "=" + possibility);
                }
            } else {
                for (String possibility : i.getHandler().getPossibilities("").convert((v) -> i.getHandler().toStringForce(v))) {
                    quantity++;
                    tabs.add(i.getName() + "=" + possibility);
                }
            }

            if (quantity == 0) {
                tabs.add(i.getName() + "=");
                tabs.add(i.getName() + "=" + i.getDefaultRaw());
            }
        }

        return tabs;
    }

    // TODO: Write invocation
    @Override
    public boolean invoke(KList<String> args, DecreeSender sender) {
        system.debug("Wow! Reached a command (" + getName() + ")! " + getPath());
        return true;
    }
}
