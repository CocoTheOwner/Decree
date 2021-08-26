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
import nl.codevs.decree.decree.util.KList;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Represents a single command (non-category)
 */
@Data
public class DecreeNode implements Decreed {
    private final Method method;
    private final Object parent;
    private final Decree decree;

    /**
     * Create a node
     * @param parent The instantiated class containing the
     * @param method Method that represents a Decree (must be annotated by @{@link Decree})
     */
    public DecreeNode(Object parent, Method method) {
        if (!method.isAnnotationPresent(Decree.class)) {
            throw new RuntimeException("Cannot instantiate DecreeNode on method " + method.getName() + " in " + method.getDeclaringClass().getCanonicalName() + " not annotated by @Decree");
        }
        this.parent = parent;
        this.method = method;
        this.decree = method.getDeclaredAnnotation(Decree.class);
    }

    /**
     * Get the parameters of this decree node
     *
     * @return The list of parameters if ALL are annotated by @{@link Param}, else null
     */
    public KList<DecreeParameter> getParameters() {
        KList<DecreeParameter> required = new KList<>();
        KList<DecreeParameter> optional = new KList<>();

        for (Parameter i : method.getParameters()) {
            DecreeParameter p = new DecreeParameter(i);
            if (p.isRequired()){
                required.add(p);
            } else {
                optional.add(p);
            }
        }

        required.addAll(optional);

        return required;
    }


    @Override
    public String getName() {
        return decree.name().isEmpty() ? method.getName() : decree.name();
    }

    @Override
    public Decreed parent() {
        return (Decreed) parent;
    }

    @Override
    public Decree decree() {
        return decree;
    }
}
