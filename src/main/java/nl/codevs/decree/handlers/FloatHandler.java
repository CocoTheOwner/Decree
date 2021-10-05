package nl.codevs.decree.handlers;


import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.util.Form;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class FloatHandler implements DecreeParameterHandler<Float> {
    @Override
    public KList<Float> getPossibilities() {
        return null;
    }

    @Override
    public @NotNull Float parse(String in, boolean force) throws DecreeParsingException {
        try {
            AtomicReference<String> r = new AtomicReference<>(in);
            double m = getMultiplier(r);
            return (float) (Float.parseFloat(r.get()) * m);
        } catch (Throwable e) {
            throw new DecreeParsingException(Float.class, in, e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Float.class) || type.equals(float.class);
    }

    @Override
    public String toString(Float f) {
        return f.toString();
    }

    @Override
    public String getRandomDefault() {
        return Form.f(Maths.frand(0, 99.99f), 1) + "";
    }
}
