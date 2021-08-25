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
import nl.codevs.decree.decree.DecreeSender;
import nl.codevs.decree.decree.DecreeSystem;
import nl.codevs.decree.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.decree.util.AtomicCache;
import nl.codevs.decree.decree.util.KList;

import java.lang.reflect.Parameter;

/**
 * Represents a parameter in an @{@link Decree} annotated function
 */
@Data
public class DecreeParameter {
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
        return handlerCache.aquire(() -> {
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
     * TODO: Pull upstream because of potentially interfering {@link DecreeContextHandler}s
     * @return
     */
    @Deprecated
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

        for (String i : param.aliases()) {
            if (i.isEmpty()) {
                continue;
            }

            d.add(i);
        }

        d.removeDuplicates();

        return d;
    }

    /**
     * Get the default value for this parameter
     * @return The default value
     * @throws DecreeParsingException Thrown when default value cannot be parsed
     * @throws DecreeWhichException Thrown when there are more than one options resulting from parsing
     */
    public Object getDefaultValue() throws DecreeParsingException, DecreeWhichException {
        return hasDefault() ? getHandler().parse(getDefaultRaw()) : null;
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
     * @return A random example value from possible values in the parameter
     */
    public String exampleValue() {
        KList<String> examples = exampleCache.aquire(() -> {
            KList<String> f = getHandler().getPossibilities().convert((i) -> getHandler().toStringForce(i));
            if (f.isEmpty()){
                return new KList<>(getHandler().getRandomDefault());
            } else {
                return f;
            }
        });

        return examples.getRandom();
    }

    /**
     * @return A random example name
     */
    public String exampleName() {
        return getNames().getRandom();
    }

    public boolean isContextual() {
        return param.contextual();
    }
}
