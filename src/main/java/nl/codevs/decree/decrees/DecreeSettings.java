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
    private static File file;

    @Decree(description = "When entering arguments, should people be allowed to enter 'null'?")
    public void setAllowNullInput(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "false"
            )
                    Boolean enable
    ){
        allowNullInput = enable;
        save();
    }
    public boolean allowNullInput = false;

    @Decree(description = "Whether to use command sounds or not")
    public void setCommandSound(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "false"
            )
                    Boolean enable
    ){
        commandSound = enable;
        save();
    }
    public boolean commandSound = true;

    @Decree(description = "Whether to send debug messages or not")
    public void setDebug(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "false"
            )
                    Boolean enable
    ){
        debug = enable;
        save();
    }
    public boolean debug = false;

    @Decree(description = "Whether to debug matching or not. This is also ran on tab completion.")
    public void setDebugMatching(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "false"
            )
                    Boolean enable
    ){
        debugMatching = enable;
        save();
    }
    public boolean debugMatching = true;

    @Decree(description = "The maximal number of same-named root commands")
    public void setMaxRoots(
            @Param(
                    description = "The maximal amount of roots",
                    defaultValue = "10"
            )
                    Integer roots){
        maxRoots = roots;
        save();
    }
    public int maxRoots = 10;

    @Decree(description = "On argument parsing fail, pass 'null' instead. Can break argument parsing, best to leave 'false'", permission = "settings")
    public void setNullOnFailure(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "false"
            )
            Boolean enable
    ){
        nullOnFailure = enable;
        save();
    }
    public boolean nullOnFailure = false;

    @Decree(description = "Auto-pick the first option when multiple exist?")
    public void setPickFirstOnMultiple(
            @Param(
                    description = "Whether to set this setting to true or false",
                    defaultValue = "false"
            )
                    Boolean enable
    ){
        pickFirstOnMultiple = enable;
        save();
    }
    public boolean pickFirstOnMultiple = false;

    @Decree(description = "Command prefix")
    public void setDecreePrefix(
            @Param(
                    description = "The prefix to have Decree debug with",
                    defaultValue = "§c[§aDecree§c]§r"
            )
                    String prefix
    ){
        this.prefix = prefix;
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
