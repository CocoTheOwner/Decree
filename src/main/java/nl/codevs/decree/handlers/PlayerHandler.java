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

            if (!names.contains("self") && in.equalsIgnoreCase("self") && DecreeSystem.Context.get().isPlayer()) {
                return DecreeSystem.Context.get().player();
            }
            if (!names.contains("me") && in.equalsIgnoreCase("me") && DecreeSystem.Context.get().isPlayer()) {
                return DecreeSystem.Context.get().player();
            }
            if (!names.contains("random") && in.equalsIgnoreCase("random")) {
                return options.getRandom();
            }
            if (!names.contains("closest") && in.equalsIgnoreCase("closest") && DecreeSystem.Context.get().isPlayer()) {
                Player closest = null;
                double distance = -1;
                for (Player option : options) {
                    if (option.getLocation().getWorld() == DecreeSystem.Context.get().player().getWorld()) {

                        if (closest == null) {
                            closest = option;
                            distance = option.getLocation().distance(DecreeSystem.Context.get().player().getLocation());
                            continue;
                        }

                        double d = option.getLocation().distance(DecreeSystem.Context.get().player().getLocation());
                        if (d < distance) {
                            closest = option;
                            distance = d;
                        }
                    }
                }
                if (closest == null) {
                    return options.getRandom();
                } else {
                    return closest;
                }
            }

            if (options.isEmpty()) {
                throw new DecreeParsingException(Byte.class, in, "No players match that input");
            } else if (options.size() > 1) {
                if (force) {
                    return options.getRandom();
                }
                throw new DecreeWhichException(Player.class, in, options);
            }

            return options.get(0);
        } catch (DecreeParsingException | DecreeWhichException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecreeParsingException(Player.class, in, e);
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
