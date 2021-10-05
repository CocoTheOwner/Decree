package nl.codevs.decree.handlers;


import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
import org.jetbrains.annotations.NotNull;

public class ByteHandler implements DecreeParameterHandler<Byte> {

    @Override
    public KList<Byte> getPossibilities() {
        return null;
    }

    @Override
    public String toString(Byte aByte) {
        return aByte.toString();
    }

    @Override
    public @NotNull Byte parse(String in, boolean force) throws DecreeParsingException {
        try {
            return Byte.parseByte(in);
        } catch (Throwable e) {
            throw new DecreeParsingException(Byte.class, in, e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Byte.class) || type.equals(byte.class);
    }

    @Override
    public String getRandomDefault() {
        return String.valueOf(Maths.irand(Byte.MIN_VALUE, Byte.MAX_VALUE));
    }
}
