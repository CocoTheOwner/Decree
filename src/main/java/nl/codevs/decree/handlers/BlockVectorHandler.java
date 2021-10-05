package nl.codevs.decree.handlers;

import nl.codevs.decree.Decree;
import nl.codevs.decree.DecreeSystem;
import nl.codevs.decree.exceptions.DecreeParsingException;
import nl.codevs.decree.exceptions.DecreeWhichException;
import nl.codevs.decree.util.DecreeSender;
import nl.codevs.decree.util.Form;
import nl.codevs.decree.util.KList;
import nl.codevs.decree.util.Maths;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidParameterException;
import java.util.Locale;

public class BlockVectorHandler implements DecreeParameterHandler<BlockVector> {
    @Override
    public @NotNull KList<BlockVector> getPossibilities() {
        KList<BlockVector> vx = new KList<>();
        DecreeSender s = DecreeSystem.Context.get();

        if (s.isPlayer()) {
            vx.add(s.player().getLocation().toVector().toBlockVector());
        }

        return vx;
    }

    @Override
    public String toString(BlockVector v) {
        if (v.getY() == 0) {
            return Form.f(v.getBlockX(), 2) + "," + Form.f(v.getBlockZ(), 2);
        }

        return Form.f(v.getBlockX(), 2) + "," + Form.f(v.getBlockY(), 2) + "," + Form.f(v.getBlockZ(), 2);
    }

    @SuppressWarnings({"RedundantThrows", "SpellCheckingInspection"})
    @Override
    public @NotNull BlockVector parse(String in, boolean force) throws DecreeParsingException, DecreeWhichException {
        try {
            if (in.contains(",")) {
                String[] comp = in.split("\\Q,\\E");

                if (comp.length == 2) {
                    return new BlockVector(Integer.parseInt(comp[0].trim()), 0, Integer.parseInt(comp[1].trim()));
                } else if (comp.length == 3) {
                    return new BlockVector(Integer.parseInt(comp[0].trim()),
                            Integer.parseInt(comp[1].trim()),
                            Integer.parseInt(comp[2].trim()));
                } else {
                    throw new IllegalArgumentException("Too many components, you have " + comp.length + ". Expected 2 or 3.");
                }
            } else if (in.equalsIgnoreCase("here") || in.equalsIgnoreCase("me") || in.equalsIgnoreCase("self")) {
                if (!DecreeSystem.Context.get().isPlayer()) {
                    throw new DecreeParsingException(BlockVector.class, in, "You cannot specify me,self,here as a console.");
                }

                return DecreeSystem.Context.get().player().getLocation().toVector().toBlockVector();
            } else if (in.equalsIgnoreCase("look") || in.equalsIgnoreCase("cursor") || in.equalsIgnoreCase("crosshair")) {
                if (!DecreeSystem.Context.get().isPlayer()) {
                    throw new DecreeParsingException(BlockVector.class, in, "You cannot specify look, cursor, crosshair as a console.");
                }
                Block target = DecreeSystem.Context.get().player().getTargetBlockExact(256, FluidCollisionMode.NEVER);
                if (target == null) {
                    throw new InvalidParameterException(in + " is invalid because the targeted location is null");
                }
                return target.getLocation().toVector().toBlockVector();
            } else if (in.equalsIgnoreCase("random")) {
                return new BlockVector(Maths.frand(-30_000_000, 30_000_000), Maths.frand(0, 256), Maths.frand(-30_000_000, 30_000_000));
            } else if (in.trim().toLowerCase().startsWith("player:")) {
                String v = in.trim().split("\\Q:\\E")[1];

                KList<?> px = DecreeSystem.Handler.get(Player.class).getPossibilities(v);

                if (px != null && px.isNotEmpty()) {
                    return ((Player) px.get(0)).getLocation().toVector().toBlockVector();
                } else {
                    throw new DecreeParsingException(BlockVector.class, in, "Cannot find player: " + v);
                }
            } else {
                throw new InvalidParameterException("Invalid because it has no ',' - BlockVectors are written as (number,number)");
            }
        } catch (DecreeParsingException e) {
            throw e;
        } catch (Throwable e) {
            throw new DecreeParsingException(BlockVector.class, in, e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(BlockVector.class);
    }

    @Override
    public String getRandomDefault() {
        return Maths.r(0.5) ? "0,0" : "0,0,0";
    }
}
