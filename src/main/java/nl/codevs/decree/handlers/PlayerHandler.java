package nl.codevs.decree.handlers;

import nl.codevs.decree.DecreeSystem;
import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.util.KList;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PlayerHandler implements DecreeParameterHandler<Player> {
    @Override
    public @NotNull KList<Player> getPossibilities() {
        return new KList<>(new ArrayList<>(Bukkit.getOnlinePlayers()));
    }

    @Override
    public String toString(Player player) {
        return player.getName();
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public @NotNull Player parse(String in, boolean force) throws DecreeParsingException, DecreeWhichException {
        try {
            KList<Player> options = getPossibilities(in);
            KList<String> names = getPossibilities().convert(HumanEntity::getName);

            if (!names.contains("self") && in.equals("self") && DecreeSystem.Context.get().isPlayer()){
                return DecreeSystem.Context.get().player();
            }
            if (!names.contains("me") && in.equals("me") && DecreeSystem.Context.get().isPlayer()){
                return DecreeSystem.Context.get().player();
            }
            if (!names.contains("random") && in.equals("random")){
                return options.getRandom();
            }

            if (options.isEmpty()) {
                throw new DecreeParsingException("Unable to find Player \"" + in + "\"");
            } else if (options.size() > 1) {
                if (force) {
                    return options.getRandom();
                }
                throw new DecreeWhichException(Player.class, in, options);
            }

            return options.get(0);
        } catch (Throwable e) {
            throw new DecreeParsingException("Unable to find Player \"" + in + "\" because of an uncaught exception: " + e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Player.class);
    }

    @SuppressWarnings("SpellCheckingInspection")
    private final KList<String> defaults = new KList<>(
            "playername",
            "self",
            "random"
    );

    @Override
    public String getRandomDefault() {
        return defaults.getRandom();
    }
}
