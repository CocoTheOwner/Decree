package nl.codevs.raiders;

import nl.codevs.raiders.decree.DecreeExecutor;
import nl.codevs.raiders.decree.DecreeSystem;
import nl.codevs.raiders.decrees.MainCommandClass;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Decree extends JavaPlugin implements DecreeSystem, DecreeExecutor {

    @Override
    public Plugin instance() {
        return this;
    }

    @Override
    public DecreeExecutor getRootClass() {
        return new MainCommandClass();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return decreeCommand(sender, command, label, args);
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return decreeTabComplete(sender, command, alias, args);
    }

    @Override
    public void onEnable() {
        Bukkit.getConsoleSender().sendMessage("Hello world!");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("Goodbeye world!");
    }
}
