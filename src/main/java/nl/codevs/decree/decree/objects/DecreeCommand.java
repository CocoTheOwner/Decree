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
import nl.codevs.decree.decree.util.KList;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

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
        sender.sendMessageRaw("YEet!" + getPath());
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
