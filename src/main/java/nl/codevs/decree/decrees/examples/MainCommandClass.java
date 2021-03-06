package nl.codevs.decree.decrees.examples;

import nl.codevs.decree.decrees.DecreeCommandExecutor;
import nl.codevs.decree.decrees.DecreeSettings;
import nl.codevs.decree.util.C;
import nl.codevs.decree.virtual.Decree;
import nl.codevs.decree.virtual.Param;
import org.bukkit.World;

@SuppressWarnings("SpellCheckingInspection")
@Decree(name = "command", aliases = {"cmmd", "cmd", "cd"}, description = "Main commands", permission = "decree")
// If you omit "name = ..." from the annotation, the name of this category would become 'main-command-class'
// Permission node for this command is "decree", since it is a root category
public class MainCommandClass implements DecreeCommandExecutor { // Categories must implement DecreeCommandExecutor

    // Commands that allow in-game/console commands to modify Decree settings.
    // If you do not want to have this as a possibility, do not add this to your commands.
    // This requires the permission for this class (+ parent classes) with `.settings`, in this case 'decree.settings`
    DecreeSettings settings;

    SubCommandClass subcommands; // The name of the variable does not matter

    @Decree(
            description = "Description goes here", // The description which is displayed when hovering over the command in chat as a player
            aliases = {"hi", "heyo"}, // Alternative names for the command
            sync = true, // Runs the command on the main thread. Some stuff requires this. Not sure? Leave this on false and set it to true if it doesn't work
            permission = "hello", // Actual permission node is 'decree.hello' because the permission node of the parent is 'decree'
            name = "sayHello" // Later converted to 'say-hello'. This doesn't actually change anything because the method is called "sayHello" too
    )
    public void sayHello( // Method name is not the name of the command because 'name' is specified in the annotation

            @Param(
                    description = "The world to say hello in",
                    contextual = true // Set to true to auto-import the world the person sending the command is in
                    // Note: This requires a custom context-handler in DecreeSystem.Context#getHandlers. Context handlers implement DecreeContextHandler
            )
                    World world, // The names of these variables are used in the command unless "name" is specified in the annotation
                       // These are not translated where 'CaPs' is replaced by 'ca-ps', like we saw before
            @Param(
                    defaultValue = "Hello!", // This means the parameter can be omitted. If omitted, it will assume "Hello!" as its value
                    aliases = "msg", // You can also set array-likes with single elements, without brackets
                    description = "The message to send"
            )
                    String message,
            @Param(
                    // There are no aliases for this, but 't' and 'ta' will still map to 'tag'. Decree's intelligent mapping system will find a match
                    description = "dwad a message to yourself"
            )
                    Integer amount // Notice we use 'Integer' instead of 'int'. If you turn on DecreeSystem.settings.allowNullInput, you MUST use Integer because 'int' cannot be null
                       // If you do not have this on (default), you can use 'int' safely
    ){
        // sender() returns the sender of the command
        // player() returns the player instance of the sender, or null if sent by console
        // world() returns the world of the player, or null if player() is null

        for (int i = 0; i < amount; i++) {

            // Decree comes with a class "C" which contains color codes, which you can add like this:
            sender().sendMessage(C.GOLD + world.getName() + C.GREEN + " -> " + C.GOLD + message);
        }
    }
}
