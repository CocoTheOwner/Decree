package nl.codevs.decree.decree.exceptions;

/**
 * Thrown when more than one option is available for a singular mapping<br>
 * Like having a hashmap where one input maps to two outputs.
 */
public class DecreeWhichException extends Exception {
    public DecreeWhichException(Class<?> type, String input) {
        super("Cannot parse \"" + input + "\" into type " + type.getSimpleName() + " because of multiple options");
    }
}
