package nl.codevs.decree.decree.objects;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.codevs.decree.decree.DecreeSystem;
import nl.codevs.decree.decree.util.C;
import nl.codevs.decree.decree.util.Form;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a volume sender. A command sender with extra crap in it
 *
 * @author cyberpwn
 */
@SuppressWarnings("SpellCheckingInspection")
public class DecreeSender implements CommandSender {
    private final CommandSender s;
    private final Audience audience;
    private final DecreeSystem system;
    private final String tag;
    private final int spinh = -20;
    private final int spins = 10;
    private final int spinb = 20;

    @Getter
    @Setter
    private String command;

    /**
     * Wrap a command sender
     *
     * @param s the command sender
     */
    public DecreeSender(CommandSender s, Plugin instance, DecreeSystem system) {
        this(s, "", instance, system);
    }

    public DecreeSender(CommandSender s, String tag, Plugin instance, DecreeSystem system) {
        this.audience = BukkitAudiences.create(instance).sender(s);
        this.system = system;
        this.tag = tag;
        this.s = s;
    }

    /**
     * Get the command tag
     *
     * @return the command tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Is this sender a player?
     *
     * @return true if it is
     */
    public boolean isPlayer() {
        return getS() instanceof Player;
    }

    /**
     * Force cast to player (be sure to check first)
     *
     * @return the cast player
     */
    public Player player() {
        return (Player) getS();
    }

    /**
     * Get the origin sender this object is wrapping
     *
     * @return the command sender
     */
    public CommandSender getS() {
        return s;
    }



    public static long getTick() {
        return System.currentTimeMillis() / 16;
    }

    public static String pulse(String colorA, String colorB, double speed) {
        return "<gradient:" + colorA + ":" + colorB + ":" + pulse(speed) + ">";
    }

    public static String pulse(double speed) {
        return Form.f(invertSpread((((getTick() * 15D * speed) % 1000D) / 1000D)), 3).replaceAll("\\Q,\\E", ".").replaceAll("\\Q?\\E", "-");
    }

    public static double invertSpread(double v) {
        return ((1D - v) * 2D) - 1D;
    }

    private Component createComponent(String message) {
        String t = C.translateAlternateColorCodes('&', getTag() + message);
        String a = C.aura(t, spinh, spins, spinb);
        return MiniMessage.get().parse(a);
    }

    private Component createComponentRaw(String message) {
        String t = C.translateAlternateColorCodes('&', getTag() + message);
        return MiniMessage.get().parse(t);
    }

    public void sendMessageRaw(String message) {
        if (message.contains("<NOMINI>")) {
            s.sendMessage(message.replaceAll("\\Q<NOMINI>\\E", ""));
            return;
        }

        try {
            audience.sendMessage(createComponentRaw(message));
        } catch (Throwable e) {
            String t = C.translateAlternateColorCodes('&', getTag() + message);
            String a = C.aura(t, spinh, spins, spinb);

            system.debug("<NOMINI>Failure to parse " + a);
            s.sendMessage(C.translateAlternateColorCodes('&', getTag() + message));
        }
    }

    public void sendHeader(String name, int overrideLength) {
        int h = name.length() + 2;
        String s = Form.repeat(" ", overrideLength - h - 4);
        String si = "(((";
        String so = ")))";
        String sf = "[";
        String se = "]";

        if (name.trim().isEmpty()) {
            sendMessageRaw("<font:minecraft:uniform><strikethrough><gradient:#34eb6b:#32bfad>" + sf + s + "<reset><font:minecraft:uniform><strikethrough><gradient:#32bfad:#34eb6b>" + s + se);
        } else {
            sendMessageRaw("<font:minecraft:uniform><strikethrough><gradient:#34eb6b:#32bfad>" + sf + s + si + "<reset> <gradient:#3299bf:#323bbf>" + name + "<reset> <font:minecraft:uniform><strikethrough><gradient:#32bfad:#34eb6b>" + so + s + se);
        }
    }

    public void sendHeader(String name) {
        sendHeader(name, 40);
    }

    public void playSound(Sound sound, float volume, float pitch) {
        if (isPlayer()) {
            player().playSound(player().getLocation(), sound, volume, pitch);
        }
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return s.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return s.isPermissionSet(perm);
    }

    @Override
    public boolean hasPermission(@NotNull String name) {
        return s.hasPermission(name);
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return s.hasPermission(perm);
    }


    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        return s.addAttachment(plugin, name, value);
    }


    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        return s.addAttachment(plugin);
    }

    @Override
    public PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
        return s.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        return s.addAttachment(plugin, ticks);
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {
        s.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        s.recalculatePermissions();
    }


    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return s.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return s.isOp();
    }

    @Override
    public void setOp(boolean value) {
        s.setOp(value);
    }

    @Override
    public void sendMessage(String message) {
        if (message.contains("<NOMINI>")) {
            s.sendMessage(message.replaceAll("\\Q<NOMINI>\\E", ""));
            return;
        }

        try {
            audience.sendMessage(createComponent(message));
        } catch (Throwable e) {
            String t = C.translateAlternateColorCodes('&', getTag() + message);
            String a = C.aura(t, spinh, spins, spinb);

            system.debug("<NOMINI>Failure to parse " + a);
            s.sendMessage(C.translateAlternateColorCodes('&', getTag() + message));
        }
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String str : messages)
            sendMessage(str);
    }

    @Override
    public void sendMessage(UUID uuid, @NotNull String message) {
        sendMessage(message);
    }

    @Override
    public void sendMessage(UUID uuid, String[] messages) {
        sendMessage(messages);
    }


    @Override
    public @NotNull Server getServer() {
        return s.getServer();
    }


    @Override
    public @NotNull String getName() {
        return s.getName();
    }


    @Override
    public @NotNull Spigot spigot() {
        return s.spigot();
    }
}
