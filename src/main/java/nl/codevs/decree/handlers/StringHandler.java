package nl.codevs.decree.handlers;


import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.util.KList;
import org.jetbrains.annotations.NotNull;

/**
 * Abstraction can sometimes breed stupidity
 */
public class StringHandler implements DecreeParameterHandler<String> {
    @Override
    public KList<String> getPossibilities() {
        return null;
    }

    @Override
    public String toString(String s) {
        return s;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public @NotNull String parse(String in, boolean force) throws DecreeParsingException {
        return in;
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(String.class);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private final KList<String> defaults = new KList<>(
            "text",
            "string",
            "blah",
            "derp",
            "yolo"
    );

    @Override
    public String getRandomDefault() {
        return defaults.getRandom();
    }
}
