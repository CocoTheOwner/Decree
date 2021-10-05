package nl.codevs.decree.handlers;


import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
import org.jetbrains.annotations.NotNull;

public class BooleanHandler implements DecreeParameterHandler<Boolean> {
    @Override
    public @NotNull KList<Boolean> getPossibilities() {
        return new KList<>(true, false);
    }

    @Override
    public String toString(Boolean aByte) {
        return aByte.toString();
    }

    @Override
    public Boolean parse(String in, boolean force) throws DecreeParsingException {
        if (in.equalsIgnoreCase("null") || in.equalsIgnoreCase("other") || in.equalsIgnoreCase("flip") || in.equalsIgnoreCase("toggle")) {
            return null;
        }
        try {
            return Boolean.parseBoolean(in);
        } catch (Throwable e) {
            throw new DecreeParsingException(Boolean.class, in, e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Boolean.class) || type.equals(boolean.class);
    }

    @Override
    public String getRandomDefault() {
        return Maths.r(0.5) + "";
    }
}
