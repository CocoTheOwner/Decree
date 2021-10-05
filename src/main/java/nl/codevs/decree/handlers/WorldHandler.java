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

    @SuppressWarnings("RedundantThrows")
    @Override
    public @NotNull World parse(String in, boolean force) throws DecreeParsingException, DecreeWhichException {
        try {
            KList<World> options = getPossibilities(in);

            if (options.isEmpty()) {
                throw new DecreeParsingException("Unable to find World \"" + in + "\"");
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
            throw new DecreeParsingException("Unable to find World \"" + in + "\" because of an uncaught exception: " + e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(World.class);
    }

    @Override
    public String getRandomDefault() {
        return "world";
    }
}
