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
import nl.codevs.decree.decree.util.KList;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Comparator;

/**
 * Represents a single command (non-category)
 */
@Data
public class DecreeCommand implements Decreed {
    private final KList<DecreeParameter> parameters;
    private final Method method;
    private final Object parent;
    private final Decree decree;

    /**
     * Create a node
     * @param parent The instantiated class containing the
     * @param method Method that represents a Decree (must be annotated by @{@link Decree})
     */
    public DecreeCommand(Object parent, Method method) {
        if (!method.isAnnotationPresent(Decree.class)) {
            throw new RuntimeException("Cannot instantiate DecreeCommand on method " + method.getName() + " in " + method.getDeclaringClass().getCanonicalName() + " not annotated by @Decree");
        }
        this.parent = parent;
        this.method = method;
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

    @Override
    public Decreed parent() {
        return (Decreed) getParent();
    }

    @Override
    public Decree decree() {
        return getDecree();
    }
}
