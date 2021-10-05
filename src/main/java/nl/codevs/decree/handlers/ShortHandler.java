package nl.codevs.decree.handlers;


import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class ShortHandler implements DecreeParameterHandler<Short> {
    @Override
    public KList<Short> getPossibilities() {
        return null;
    }

    @Override
    public @NotNull Short parse(String in, boolean force) throws DecreeParsingException {
        try {
            AtomicReference<String> r = new AtomicReference<>(in);
            double m = getMultiplier(r);
            return (short) (Short.valueOf(r.get()).doubleValue() * m);
        } catch (Throwable e) {
            throw new DecreeParsingException("Unable to parse short \"" + in + "\"");
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Short.class) || type.equals(short.class);
    }

    @Override
    public String toString(Short f) {
        return f.toString();
    }

    @Override
    public String getRandomDefault() {
        return String.valueOf(Maths.irand(0, 99));
    }
}
