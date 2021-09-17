package nl.codevs.decree.virtual;


import nl.codevs.decree.util.DecreeOrigin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Decree {

    String DEFAULT_DESCRIPTION = "No Description Provided";

    String NO_PERMISSION = "No Permission Required";

    /**
     * The name of this command, which is the Method's name by default
     */
    String name() default "";

    /**
     * The aliases of this parameter (instead of just the {@link #name() name} (if specified) or Method Name (name of method))<br>
     * Can be initialized as just a string (ex. "alias") or as an array (ex. {"alias1", "alias2"})<br>
     * If someone uses /plugin foo, and you specify alias="f" here, /plugin f will do the exact same.
     */
    String[] aliases() default "";

    /**
     * The description of this command.<br>
     * Is {@link #DEFAULT_DESCRIPTION} by default
     */
    String description() default DEFAULT_DESCRIPTION;

    /**
     * The origin this command must come from.<br>
     * Must be elements of the {@link DecreeOrigin} enum<br>
     * By default, is {@link DecreeOrigin#BOTH}, meaning both console & player can send the command
     */
    DecreeOrigin origin() default DecreeOrigin.BOTH;

    /**
     * The permissions class that gives the required permission for this command.<p>
     * By default, it requires no permissions
     * @return The permission node for this decree command
     */
    String permission() default NO_PERMISSION;

    /**
     * If the node's functions MUST be run in sync, set this to true.<br>
     * Defaults to false
     */
    boolean sync() default false;
}
