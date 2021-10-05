package nl.codevs.decree.handlers;


import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

public class LongHandler implements DecreeParameterHandler<Long> {
    @Override
    public KList<Long> getPossibilities() {
        return null;
    }

    @Override
    public @NotNull Long parse(String in, boolean force) throws DecreeParsingException {
        try {
            AtomicReference<String> r = new AtomicReference<>(in);
            double m = getMultiplier(r);
            return (long) (Long.valueOf(r.get()).doubleValue() * m);
        } catch (Throwable e) {
            throw new DecreeParsingException("Unable to parse long \"" + in + "\"");
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Long.class) || type.equals(long.class);
    }

    @Override
    public String toString(Long f) {
        return f.toString();
    }

    @Override
    public String getRandomDefault() {
        return String.valueOf(Maths.irand(0, 99));
    }
}
