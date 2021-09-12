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
import java.util.Random;

/**
 * Represents a single command (non-category)
 */
@Data
public class DecreeCommand implements Decreed {
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
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(Param.class)){
                parameters.add(new DecreeParameter(parameter));
            }
        }

        parameters.sort((o1, o2) -> {
            int i = 0;
            if (o1.isRequired()) {
                i += 5;
            }
            if (o2.isRequired()) {
                i -= 3;
            }
            if (o1.isContextual()) {
                i += 2;
            }
            if (o2.isContextual()) {
                i -= 1;
            }   
            return i;
        });

        return parameters;
    }

    public KList<DecreeParameter> getParameters() {
        return parameters.copy();
    }

    /**
     * Get random suggestions for this command
     * @param max The maximal amount of suggestions
     * @return A list of suggestions
     */
    private KList<String> getRandomSuggestions(int max) {

        KList<String> suggestions = new KList<>();
        int maxTries = 25;

        while (maxTries > 0 && suggestions.size() < max) {
            suggestions.qadd(
                    /*
                     ✦ /command sub name parameter=value p2=v
                     */
                    "<#aebef2>✦ <#5ef288>" + parent().getPath() + " <#42ecf5>" + getName() + " "
                    + getParameters().shuffle(new Random()).convert((f)
                            -> (f.isRequired() || Maths.drand(0, 1) > 0.5
                            ? "<#f2e15e>" + f.exampleName() + "="
                            + "<#d665f0>" + f.exampleValue()
                            : ""))
                    .toString(" ")
            ).removeDuplicates();
            maxTries--;
        }

        return suggestions.convert(r -> r.replaceAll("\\Q  \\E", " "));
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
    public void sendHelpTo(DecreeSender sender) {

        String newline = "<reset>\n";

        String hoverTitle = getNames().copy().reverse().convert((f) -> "<#42ecf5>" + f).toString(", ");
        String hoverDescription = "<#3fe05a>✎ <#6ad97d><font:minecraft:uniform>" + getDescription();
        String hoverPermission;
        String hoverSuggestions;
        String hoverOrigin = "<#dbe61c>⌘ <#d61aba><#ff33cc><font:minecraft:uniform>" + Form.capitalize(getOrigin().toString().toLowerCase());
        String hoverUsage = "<#bbe03f>✒ <#a8e0a2><font:minecraft:uniform>";

        String doOnClick;
        String realText = getPath() + " >" + "<#46826a>⇀<gradient:#42ecf5:#428df5> " + getName();


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

        // Suggestions
        if (getParameters().isNotEmpty()) {
            hoverSuggestions = "<#aebef2>✦ <#5ef288><font:minecraft:uniform>" + parent().getPath() + " <#42ecf5>" + getName() + " "
                    + getParameters().convert((f) -> "<#d665f0>" + f.exampleValue()).toString(" ") + newline;
            hoverSuggestions += "<font:minecraft:uniform>" + getRandomSuggestions(Math.min(parameters.size() + 1, 5)).toString(newline);
        } else {
            hoverSuggestions = "";
        }

        // Origin
        if (getOrigin().validFor(sender)) {
            hoverOrigin += "<#0ba10b> origin, so you can run it.";
        } else {
            hoverOrigin += "<#c4082e> origin, so you cannot run it.";
        }

        // Usage and clicking
        if (getParameters().isEmpty()){
            hoverUsage += "There are no parameters. Click to run.";
            doOnClick = "run_command";
        } else {
            hoverUsage += "Hover over parameters to learn more. Click to put in chat.";
            doOnClick = "suggest_command";
        }

        String result = "<hover:show_text:'" +
                hoverTitle + newline +
                hoverDescription + newline +
                hoverPermission + (hoverPermission.isEmpty() ? "" : newline) +
                hoverSuggestions + (hoverSuggestions.isEmpty() ? "" : newline) +
                hoverOrigin + newline +
                hoverUsage +
                "'>" +
                "<click:" +
                doOnClick +
                ":" +
                realText +
                "</hover>" +
                " " +
                realNodes +
                "</click>";

        while(result.contains("  ")){
            result = result.replaceAll("\\Q  \\E", " ");
        }

        sender.sendMessageRaw(

        );

        /// Params
        StringBuilder nodes = new StringBuilder();
        for (DecreeParameter p : getParameters()) {

            String nTitle = "<gradient:#d665f0:#a37feb>" + p.getName();
            String nHoverTitle = p.getNames().convert((ff) -> "<#d665f0>" + ff).toString(", ");
            String nDescription = "<#3fe05a>✎ <#6ad97d><font:minecraft:uniform>" + p.getDescription();
            String nUsage;
            String fullTitle;
            if (p.isContextual() && sender.isPlayer()) {
                fullTitle = "<#ffcc00>[" + nTitle + "<#ffcc00>] ";
                nUsage = "<#ff9900>➱ <#ffcc00><font:minecraft:uniform>The value may be derived from environment context.";
            } else if (p.isRequired()) {
                fullTitle = "<red>[" + nTitle + "<red>] ";
                nUsage = "<#db4321>⚠ <#faa796><font:minecraft:uniform>This parameter is required.";
            } else if (p.hasDefault()) {
                fullTitle = "<#4f4f4f>⊰" + nTitle + "<#4f4f4f>⊱";
                nUsage = "<#2181db>✔ <#78dcf0><font:minecraft:uniform>Defaults to \"" + p.getParam().defaultValue() + "\" if undefined.";
            } else {
                fullTitle = "<#4f4f4f>⊰" + nTitle + "<#4f4f4f>⊱";
                nUsage = "<#a73abd>✔ <#78dcf0><font:minecraft:uniform>This parameter is optional.";
            }
            String type = "<#cc00ff>✢ <#ff33cc><font:minecraft:uniform>This parameter is of type " + p.getType().getSimpleName() + ".";

            nodes
                    .append("<hover:show_text:'")
                    .append(nHoverTitle).append(newline)
                    .append(nDescription).append(newline)
                    .append(nUsage).append(newline)
                    .append(type)
                    .append("'>")
                    .append(fullTitle)
                    .append("</hover>");
        }
    }

    @Override
    public String getName() {
        return decree().name().isEmpty() ? getMethod().getName() : decree().name();
    }

    @Override
    public KList<String> tab(KList<String> args, DecreeSender sender) {
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

    @Override
    public boolean invoke(KList<String> args, DecreeSender sender) {
        // TODO: Write invocation
        system.debug("Wow! Reached a command (" + getName() + ")! " + getPath());
        return true;
    }
}
