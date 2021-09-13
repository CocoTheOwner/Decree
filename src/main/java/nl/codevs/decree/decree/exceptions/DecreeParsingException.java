package nl.codevs.decree.decree.exceptions;

/**
 * Thrown when a decree parameter is parsed, but parsing fails
 */
public class DecreeParsingException extends Exception {
    public DecreeParsingException(String message) {
        super(message);
    }
}
