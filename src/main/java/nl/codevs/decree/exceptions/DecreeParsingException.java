package nl.codevs.decree.exceptions;

import lombok.Getter;
import nl.codevs.decree.util.C;

/**
 * Thrown when a decree parameter is parsed, but parsing fails
 */
@Getter
public class DecreeParsingException extends Exception {
    private final Class<?> type;
    private final String input;
    private final String reason;
    private final String message;

    public DecreeParsingException(Class<?> type, String input, Throwable reason) {
        this(type, input, reason.getClass().getSimpleName() + " - " + reason.getMessage());
    }
    public DecreeParsingException(Class<?> type, String input, String reason) {
        super();
        this.type = type;
        this.input = input;
        this.reason = reason;
        this.message = C.RED + "Could not parse " + C.GOLD + input + C.RED + " (" + C.GOLD + type.getSimpleName() + C.RED + ") because of: " + C.GOLD + reason;
    }
}
