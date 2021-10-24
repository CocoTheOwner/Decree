package nl.codevs.decree.decrees;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nl.codevs.decree.DecreeSystem;
import nl.codevs.decree.util.C;
import nl.codevs.decree.util.DecreeSender;
import nl.codevs.decree.util.Maths;
import nl.codevs.decree.virtual.Decree;
import nl.codevs.decree.virtual.Param;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.BiConsumer;

@Decree(name = "decree", aliases = {"dec", "dc"}, description = "Native Decree Commands", permission = "decree")
public class DecreeSettings implements DecreeCommandExecutor {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static long lastChanged;
    private static File file;

    @Decree(description = "When entering arguments, should people be allowed to enter 'null'?")
    public void allowNullInput(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "toggle"
            )
                    Boolean enable
    ){
        DecreeSystem.settings.allowNullInput = enable == null ? !DecreeSystem.settings.allowNullInput : enable;
        sender().sendMessage(C.GREEN + "Set " + C.GOLD + "allow null input " + C.GREEN + "to: " + C.GOLD + DecreeSystem.settings.allowNullInput);
        save();
    }
    public boolean allowNullInput = false;

    @Decree(description = "Whether to use command sounds or not")
    public void commandSound(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "toggle"
            )
                    Boolean enable
    ){
        DecreeSystem.settings.commandSound = enable == null ? !DecreeSystem.settings.commandSound : enable;
        sender().sendMessage(C.GREEN + "Set " + C.GOLD + "command sound " + C.GREEN + "to: " + C.GOLD + DecreeSystem.settings.commandSound);
        save();
    }
    public boolean commandSound = true;

    @Decree(description = "Whether to send debug messages or not")
    public void debug(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "toggle"
            )
                    Boolean enable
    ){
        DecreeSystem.settings.debug = enable == null ? !DecreeSystem.settings.debug : enable;
        sender().sendMessage(C.GREEN + "Set " + C.GOLD + "debug " + C.GREEN + "to: " + C.GOLD + DecreeSystem.settings.debug);
        save();
    }
    public boolean debug = false;

    @Decree(description = "Whether to send debug runtime messages or not")
    public void debugRuntime(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "toggle"
            )
                    Boolean enable
    ){
        DecreeSystem.settings.debugRuntime = enable == null ? !DecreeSystem.settings.debugRuntime : enable;
        sender().sendMessage(C.GREEN + "Set " + C.GOLD + "debugRuntime " + C.GREEN + "to: " + C.GOLD + DecreeSystem.settings.debugRuntime);
        save();
    }
    public boolean debugRuntime;

    @Decree(description = "Whether to debug matching or not. This is also ran on tab completion.")
    public void debugMatching(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "toggle"
            )
                    Boolean enable
    ){
        DecreeSystem.settings.debugMatching = enable == null ? !DecreeSystem.settings.debugMatching : enable;
        sender().sendMessage(C.GREEN + "Set " + C.GOLD + "debug matching " + C.GREEN + "to: " + C.GOLD + DecreeSystem.settings.debugMatching);
        save();
    }
    public boolean debugMatching = true;

    @Decree(description = "The maximal number of same-named root commands")
    public void maxRoots(
            @Param(
                    description = "The maximal amount of roots",
                    defaultValue = "10"
            )
                    Integer roots){
        DecreeSystem.settings.maxRoots = roots;
        sender().sendMessage(C.GREEN + "Set " + C.GOLD + "max roots " + C.GREEN + "to: " + C.GOLD + DecreeSystem.settings.maxRoots);
        save();
    }
    public int maxRoots = 10;

    @Decree(description = "On argument parsing fail, pass 'null' instead. Can break argument parsing, best to leave 'false'", permission = "settings")
    public void nullOnFailure(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "toggle"
            )
            Boolean enable
    ){
        DecreeSystem.settings.nullOnFailure = enable == null ? !DecreeSystem.settings.nullOnFailure : enable;
        sender().sendMessage(C.GREEN + "Set " + C.GOLD + "null on failure " + C.GREEN + "to: " + C.GOLD + DecreeSystem.settings.nullOnFailure);
        save();
    }
    public boolean nullOnFailure = false;

    @Decree(description = "Auto-pick the first option when multiple exist?")
    public void pickFirstOnMultiple(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "toggle"
            )
                    Boolean enable
    ){
        DecreeSystem.settings.pickFirstOnMultiple = enable == null ? !DecreeSystem.settings.pickFirstOnMultiple : enable;
        sender().sendMessage(C.GREEN + "Set " + C.GOLD + "pick first on multiple " + C.GREEN + "to: " + C.GOLD + DecreeSystem.settings.pickFirstOnMultiple);
        save();
    }
    public boolean pickFirstOnMultiple = false;

    @Decree(description = "Command prefix")
    public void decreePrefix(
            @Param(
                    description = "The prefix to have Decree debug with",
                    defaultValue = "§c[§aDecree§c]§r"
            )
                    String prefix
    ){
        DecreeSystem.settings.prefix = prefix;
        sender().sendMessage(C.GREEN + "Set " + C.GOLD + "decree prefix " + C.GREEN + "to: " + DecreeSystem.settings.prefix + C.RESET + " ");
        save();
    }
    public String prefix = C.RED + "[" + C.GREEN + "Decree" + C.RED + "]" + C.RESET;

    /**
     * "What to do with debug messages. Best not to touch. To disable debug, set 'debug' to false."
     */
    public static BiConsumer<String, Plugin> onDebug = (message, instance) -> new DecreeSender(Bukkit.getConsoleSender(), instance).sendMessage(DecreeSystem.settings.prefix.trim() + C.RESET + " " + message);

    /**
     * "What to do with sound effects. Best not to touch. To disable sounds, set 'commandSounds' to false."
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

    /**
     * Save the config to the file location stored
     */
    private void save() {
        saveToConfig(file);
    }

    /**
     * Load a new decree Decrees file from json
     * @param file The file to read json from
     * @return The new {@link DecreeSettings}
     */
    public static DecreeSettings fromConfigJson(File file) {
        DecreeSettings.file = file;
        DecreeSettings.lastChanged = file.lastModified();
        try {
            if (!file.exists() || file.length() == 0) {
                file.getParentFile().mkdirs();
                DecreeSettings new_ = new DecreeSettings();
                FileWriter f = new FileWriter(file);
                gson.toJson(new_, DecreeSettings.class, f);
                f.close();
                System.out.println(C.GREEN + "Made new Decree config (" + C.YELLOW + file.getParent().replace("\\", "/")  + "/" + file.getName() + C.GREEN + ")");
                return new_;
            }
            System.out.println(C.GREEN + "Loaded existing Decree config (" + C.YELLOW + file.getParent().replace("\\", "/") + "/" + file.getName() + C.GREEN + ")");
            return new Gson().fromJson(new FileReader(file), DecreeSettings.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save the config to
     * @param file a file (path)
     */
    public void saveToConfig(File file) {
        try {
            FileWriter f = new FileWriter(file);
            gson.toJson(this, DecreeSettings.class, f);
            f.close();
            System.out.println(C.GREEN + "Saved Decree Decrees");
            lastChanged = file.lastModified();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hotload settings from file
     */
    public DecreeSettings hotload() {
        if (lastChanged != file.lastModified()) {
            lastChanged = file.lastModified();
            System.out.println(C.GREEN + "Hotloaded Decree Settings");
            return fromConfigJson(file);
        }
        return this;
    }
}
