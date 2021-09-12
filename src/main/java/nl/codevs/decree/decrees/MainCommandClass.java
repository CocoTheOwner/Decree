package nl.codevs.decree.decrees;

import nl.codevs.decree.decree.objects.DecreeCommandExecutor;
import nl.codevs.decree.decree.objects.Decree;
import nl.codevs.decree.decree.objects.DecreeOrigin;

@SuppressWarnings("SpellCheckingInspection")
@Decree(name = "command", aliases = {"cmmd", "cmd", "cd"}, description = "Main commands", permission = "decree", origin = DecreeOrigin.PLAYER)
public class MainCommandClass implements DecreeCommandExecutor {

    // This line is a category pointer
    private SubCommandClass nameDoesNotMatterHere;

    @Decree(
            description = "Description goes here",
            aliases = {"alias1", "alias2"},
            sync = true,
            origin = DecreeOrigin.PLAYER,
            permission = "decree.hello",
            name = "command123"
    )
    public void hello(){
        sender().sendMessage("Hey!");
    }
}
