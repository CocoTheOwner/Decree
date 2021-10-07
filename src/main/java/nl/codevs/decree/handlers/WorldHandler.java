package nl.codevs.decree.handlers;

import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.util.KList;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class WorldHandler implements DecreeParameterHandler<World> {
    @Override
    public @NotNull KList<World> getPossibilities() {
        return new KList<>(Bukkit.getWorlds());
    }

    @Override
    public String toString(World world) {
        return world.getName();
    }

    @Override
    public @NotNull World parse(String in, boolean force) throws DecreeParsingException, DecreeWhichException {
        try {
            KList<World> options = getPossibilities(in);

            if (options.stream().noneMatch(w -> w.getName().equalsIgnoreCase("random")) && in.equalsIgnoreCase("random")) {
                return options.getRandom();
            }

            if (options.isEmpty()) {
                throw new DecreeParsingException(World.class, in, "No worlds match that input");
            } else if (options.size() > 1) {
                if (force) {
                    return options.getRandom();
                }
                throw new DecreeWhichException(World.class, in, options);
            }
            return options.get(0);
        } catch (DecreeWhichException | DecreeParsingException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecreeParsingException(World.class, in, e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(World.class);
    }

    @Override
    public String getRandomDefault() {
        return getPossibilities().getRandom().getName();
    }
}
