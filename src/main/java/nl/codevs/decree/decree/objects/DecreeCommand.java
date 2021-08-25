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
public class DecreeCommand {
    private final Method method;
    private final Object instance;
    private final Decree decree;

    /**
     * Create a node
     * @param instance The instantiated class containing the
     * @param method Method that represents a Decree (must be annotated by @{@link Decree})
     */
    public DecreeCommand(Object instance, Method method) {
        if (!method.isAnnotationPresent(Decree.class)) {
            throw new RuntimeException("Cannot instantiate DecreeCommand on method " + method.getName() + " in " + method.getDeclaringClass().getCanonicalName() + " not annotated by @Decree");
        }
        this.instance = instance;
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

    /**
     * Get the primary name of the node<br>
     * This uses the {@link Decree} if specified, or the method name if not.
     * @return The name of this node
     */
    public String getName() {
        return decree.name().isEmpty() ? method.getName() : decree.name();
    }

    /**
     * Get the origin of the node from the {@link Decree}<br>
     * @return The origin of this node
     */
    public DecreeOrigin getOrigin() {
        return decree.origin();
    }

    /**
     * Get the description of the node<br>
     * @return The description of this node
     */
    public String getDescription() {
        return decree.description().isEmpty() ? Decree.DEFAULT_DESCRIPTION : decree.description();
    }

    /**
     * Get a concatenated list of the primary & alias name(s)
     * @return The names of this node
     */
    public KList<String> getNames() {
        KList<String> d = new KList<>();

        for (String i : decree.aliases()) {
            if (i.isEmpty()) {
                continue;
            }

            d.add(i);
        }


        d.add(getName());
        d.removeDuplicates();
        return d;
    }

    /**
     * Get whether this node requires a synchronous runtime or not
     * @return True or false based on the above
     */
    public boolean isSync() {
        return decree.sync();
    }

    /**
     * Get the required permission for this node
     * @return The permission of this node
     */
    public String getPermission() {
        return decree.permission();
    }
}
