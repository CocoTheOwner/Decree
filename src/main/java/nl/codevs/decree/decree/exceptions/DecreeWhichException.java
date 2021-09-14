package nl.codevs.decree.decree.exceptions;

import lombok.Getter;
import nl.codevs.decree.decree.util.KList;

/**
 * Thrown when more than one option is available for a singular mapping<br>
 * Like having a hashmap where one input maps to two outputs.
 */
public class DecreeWhichException extends Exception {
    @Getter
    private final KList<?> options;
    public DecreeWhichException(Class<?> type, String input, KList<?> options) {
        super("Cannot parse \"" + input + "\" into type " + type.getSimpleName() + " because of multiple options");
        this.options = options;
    }
}
