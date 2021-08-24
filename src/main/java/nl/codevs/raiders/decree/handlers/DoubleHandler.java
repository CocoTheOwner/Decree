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

package nl.codevs.raiders.decree.handlers;


import nl.codevs.raiders.decree.DecreeParameterHandler;
import nl.codevs.raiders.decree.exceptions.DecreeParsingException;
import nl.codevs.raiders.decree.util.Form;
import nl.codevs.raiders.decree.util.KList;
import nl.codevs.raiders.decree.util.Maths;

import java.util.concurrent.atomic.AtomicReference;

public class DoubleHandler implements DecreeParameterHandler<Double> {
    @Override
    public KList<Double> getPossibilities() {
        return null;
    }

    @Override
    public Double parse(String in) throws DecreeParsingException {
        try {
            AtomicReference<String> r = new AtomicReference<>(in);
            double m = getMultiplier(r);
            return Double.parseDouble(r.get()) * m;
        } catch (Throwable e) {
            throw new DecreeParsingException("Unable to parse double \"" + in + "\"");
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Double.class) || type.equals(double.class);
    }

    @Override
    public String toString(Double f) {
        return f.toString();
    }

    @Override
    public String getRandomDefault() {
        return Form.f(Maths.drand(0, 99.99), 1) + "";
    }
}
