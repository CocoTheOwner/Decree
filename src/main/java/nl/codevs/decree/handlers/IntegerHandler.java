package nl.codevs.decree.handlers;


import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class IntegerHandler implements DecreeParameterHandler<Integer> {
    @Override
    public KList<Integer> getPossibilities() {
        return null;
    }

    @Override
    public @NotNull Integer parse(String in, boolean force) throws DecreeParsingException {
        try {
            AtomicReference<String> r = new AtomicReference<>(in);
            double m = getMultiplier(r);
            return (int) (Integer.valueOf(r.get()).doubleValue() * m);
        } catch (Throwable e) {
            throw new DecreeParsingException("Unable to parse integer \"" + in + "\"");
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Integer.class) || type.equals(int.class);
    }

    @Override
    public String toString(Integer f) {
        return f.toString();
    }

    @Override
    public String getRandomDefault() {
        return String.valueOf(Maths.irand(0, 99));
    }
}
