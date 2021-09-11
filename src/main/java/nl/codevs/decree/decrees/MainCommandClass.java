package nl.codevs.decree.decrees;

import nl.codevs.decree.decree.objects.DecreeCommandExecutor;
import nl.codevs.decree.decree.objects.DecreeOrigin;
import nl.codevs.decree.decree.objects.Decree;
import nl.codevs.decree.decree.objects.Param;

@Decree(name = "command", aliases = {"cmmd", "cmd", "cd"}, description = "Main commands", permission = "decree")
public class MainCommandClass implements DecreeCommandExecutor {

    // This line is a category pointer
    private SubCommandClass nameDoesNotMatterHere;

    @Decree
    public void hello(){
        sender().sendMessage("Hey!");
    }
}
