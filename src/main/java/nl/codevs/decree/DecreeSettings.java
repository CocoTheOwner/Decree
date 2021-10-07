package nl.codevs.decree;

import nl.codevs.decree.util.C;
import nl.codevs.decree.util.DecreeSender;
import nl.codevs.decree.util.Maths;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;

@SuppressWarnings("CanBeFinal")
public class DecreeSettings {

    /**
     * Whether to print a one-line message about enabling this system upon startup. <br>
     * We kindly ask to leave this on, it helps others to find this system too!
     */
    public static boolean helpDecree = true;

    /**
     * When entering arguments, should people be allowed to enter "null"?
     */
    public static boolean allowNullInput = false;

    /**
     * Whether to use command sounds or not
     */
    public static boolean commandSound = true;

    /**
     * Whether to send debug messages or not.
     * You can also make runOnDebug equal to `(s) -> {};`
     */
    public static boolean debug = false;

    /**
     * Whether to debug matching or not. This is also ran on tab completion, so it causes a lot of debug.
     */
    public static boolean debugMatching = true;

    /**
     * The maximal number of same-named root commands.
     * Has barely any performance impact, and you'll likely never exceed 1, but just in case.
     */
    public static int maxRoots = 10;

    /**
     * When an argument parser fails, should the system parse null as the parameter value?
     * Note: While preventing issues when finding commands, this may totally break command parsing. Best to leave off.
     */
    public static boolean nullOnFailure = false;

    /**
     * When an argument parser returns multiple options for a certain input, should the system always pick the first element and continue?
     * Note: When the command sender is a console, this is done regardless.
     */
    public static boolean pickFirstOnMultiple = false;

    /**
     * Command prefix
     */
    public static String prefix = C.RED + "[" + C.GREEN + "Decree" + C.RED + "]";

    /**
     * What to do with debug messages. Best not to touch and let Decree handle. To disable, set 'debug' to false.
     */
    public static BiConsumer<String, Plugin> onDebug = (message, instance) -> new DecreeSender(Bukkit.getConsoleSender(), instance).sendMessage(DecreeSettings.prefix.trim() + C.RESET + " " + message);

    /**
     * What to do with sound effects. Best not to touch and let Decree handle. To disable, set 'commandSounds' to false.
     * Consumer takes 'success' ({@link Boolean}), a sound effect from ({@link DecreeSystem.SFX}), and 'sender' ({@link DecreeSender}).
     */
    public static TriConsumer<Boolean, DecreeSystem.SFX, DecreeSender> onSoundEffect = (success, sfx, sender) -> {
        switch (sfx) {
            case Tab -> {
                if (success) {
                    sender.playSound(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.25f, Maths.frand(0.125f, 1.95f));
                } else {
                    sender.playSound(Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.25f, Maths.frand(0.125f, 1.95f));
                }
            }
            case Command -> {
                if (success) {
                    sender.playSound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.77f, 1.65f);
                    sender.playSound(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.125f, 2.99f);
                } else {
                    sender.playSound(Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1f, 0.25f);
                    sender.playSound(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.2f, 1.95f);
                }
            }
            case Picked -> {
                if (success) {
                    sender.playSound(Sound.BLOCK_BEACON_ACTIVATE, 0.125f, 1.99f);
                } else {
                    sender.playSound(Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.77f, 0.65f);
                    sender.playSound(Sound.BLOCK_BEACON_DEACTIVATE, 0.125f, 1.99f);
                }
            }
        }
    };
}
