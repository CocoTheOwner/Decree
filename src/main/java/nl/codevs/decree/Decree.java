package nl.codevs.decree;

import nl.codevs.decree.decrees.examples.MainCommandClass;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Decree extends JavaPlugin implements TabCompleter, CommandExecutor {

    private DecreeSystem decreeSystem;

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return decreeSystem.onTabComplete(sender, command, args);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return decreeSystem.onCommand(sender, command, args);
    }

    @Override
    public void onEnable() {
        decreeSystem = new DecreeSystem(new MainCommandClass(), this);
        Bukkit.getPluginManager().registerEvents(decreeSystem, this);
    }
}
