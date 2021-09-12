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
import nl.codevs.decree.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.util.AtomicCache;
import nl.codevs.decree.decree.util.KList;
import nl.codevs.decree.decree.util.Maths;

import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Represents a parameter in an @{@link Decree} annotated function
 */
@Data
public class DecreeParameter {
    private static final String newline = "<reset>\n";
    private final Parameter parameter;
    private final Param param;
    private transient final AtomicCache<DecreeParameterHandler<?>> handlerCache = new AtomicCache<>();
    private transient final AtomicCache<KList<String>> exampleCache = new AtomicCache<>();

    /**
     * Create a parameter
     * @param parameter Parameter that is included in a {@link Decree} (must be annotated by @{@link Param})
     */
    public DecreeParameter(Parameter parameter) {
        if (!parameter.isAnnotationPresent(Param.class)) {
            throw new RuntimeException("Cannot instantiate DecreeParameter on " + parameter.getName() + " in method " + parameter.getDeclaringExecutable().getName() + "(...) in class " + parameter.getDeclaringExecutable().getDeclaringClass().getCanonicalName() + " not annotated by @Param");
        }
        this.parameter = parameter;
        this.param = parameter.getDeclaredAnnotation(Param.class);
    }

    /**
     * Get the handler for this parameter
     * @return A {@link DecreeParameterHandler} for this parameter's type
     */
    public DecreeParameterHandler<?> getHandler() {
        return handlerCache.acquire(() -> {
            try
            {
                return DecreeSystem.getHandler(getType());
            }
            catch(Throwable e)
            {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Get the type of this parameter
     * @return This parameter's type
     */
    public Class<?> getType() {
        return parameter.getType();
    }

    /**
     * Get the description of this parameter
     * @return The description of this parameter
     */
    public String getDescription() {
        return param.description().isEmpty() ? Param.DEFAULT_DESCRIPTION : param.description();
    }

    /**
     * @return Whether this parameter is required or not
     */
    public boolean isRequired() {
        return !hasDefault();
    }

    /**
     * Get the name of this parameter<br>
     * If the attached {@link Param} has a defined name, uses that. If not, uses the {@link Parameter}'s name.
     * @return This parameter's name
     */
    public String getName() {
        return param.name().isEmpty() ? parameter.getName() : param.name();
    }

    /**
     * Get the names that point to this parameter
     * @return The name concatenated with aliases
     */
    public KList<String> getNames() {
        KList<String> d = new KList<>(getName());
        d.addAll(Arrays.asList(param.aliases()));
        return d.removeDuplicates().qremoveIf(String::isEmpty);
    }

    /**
     * Get the default value for this parameter
     * @return The default value
     * @throws DecreeParsingException Thrown when default value cannot be parsed
     * @throws DecreeWhichException Thrown when there are more than one options resulting from parsing
     */
    public Object getDefaultValue() throws DecreeParsingException, DecreeWhichException {
        return hasDefault() ? getHandler().parse(getDefaultRaw(), true) : null;
    }

    /**
     * Get the default value from the attached {@link Param}
     * @return The default value
     */
    public String getDefaultRaw() {
        return param.defaultValue().trim();
    }

    /**
     * Retrieve whether this parameter has a default
     * @return true if it does, false if not
     */
    public boolean hasDefault() {
        return !param.defaultValue().trim().isEmpty();
    }

    /**
     * @return All possible random example values from possible values in the parameter
     */
    public KList<String> exampleValues() {
        return exampleCache.acquire(() -> {
            KList<String> results = new KList<>();
            KList<?> possibilities = getHandler().getPossibilities();

            if (possibilities == null || possibilities.isEmpty()){
                return results.qadd(getHandler().getRandomDefault());
            }

            results.addAll(possibilities.convert((i) -> getHandler().toStringForce(i)));

            if (results.isEmpty()){
                return new KList<>(getHandler().getRandomDefault());
            }

            return results;
        });
    }

    /**
     * @return Whether this is a contextual parameter
     */
    public boolean isContextual() {
        return param.contextual();
    }

    /**
     * @return Command help for this parameter
     */
    public String getHelp(DecreeSender sender, String runOnClick) {
        String hoverTitle = "<gradient:#d665f0:#a37feb>" + getNames().toString(", ");
        String hoverDescription = "<#3fe05a>✎ <#6ad97d><font:minecraft:uniform>" + getDescription();
        String hoverUsage;
        String hoverType = "<#cc00ff>✢ <#ff33cc><font:minecraft:uniform>This parameter is of type " + getType().getSimpleName() + ".";
        String hoverRun = "<#2e8bdf>⎆ <#24dfdb><font:minecraft:uniform>" + runOnClick;

        String realText;

        // Hover usage & real text
        String realTitle = "<gradient:#d665f0:#a37feb>" + getName();
        if (isContextual() && sender.isPlayer()) {
            hoverUsage = "<#ff9900>➱ <#ffcc00><font:minecraft:uniform>May be derived from environment context.";
            realText = "<#ffcc00>[" + realTitle + "<#ffcc00>] ";
        } else if (isRequired()) {
            hoverUsage = "<#db4321>⚠ <#faa796><font:minecraft:uniform>This parameter is required.";
            realText = "<red>[" + realTitle + "<red>] ";
        } else if (hasDefault()) {
            hoverUsage = "<#2181db>✔ <#78dcf0><font:minecraft:uniform>Defaults to \"" + getParam().defaultValue() + "\" if undefined.";
            realText = "<#4f4f4f>⊰" + realTitle + "<#4f4f4f>⊱";
        } else {
            hoverUsage = "<#a73abd>✔ <#78dcf0><font:minecraft:uniform>This parameter is optional.";
            realText = "<#4f4f4f>⊰" + realTitle + "<#4f4f4f>⊱";
        }

        return "<hover:show_text:'" +
                    hoverTitle + newline +
                    hoverDescription + newline +
                    hoverUsage + newline +
                    hoverType + newline +
                    hoverRun +
                "'>" +
                    "<click:suggest_command:" + runOnClick + ">" +
                        realText +
                    "</click>" +
                "</hover>";
    }
}
