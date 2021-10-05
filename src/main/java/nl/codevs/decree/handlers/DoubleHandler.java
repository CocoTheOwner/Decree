package nl.codevs.decree.handlers;


import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.util.Form;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class DoubleHandler implements DecreeParameterHandler<Double> {
    @Override
    public KList<Double> getPossibilities() {
        return null;
    }

    @Override
    public @NotNull Double parse(String in, boolean force) throws DecreeParsingException {
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
