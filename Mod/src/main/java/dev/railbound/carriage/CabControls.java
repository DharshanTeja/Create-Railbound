package dev.railbound.carriage;

import net.minecraft.core.BlockPos;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * A loco's cab draws its controls (regulator, gauges, backhead) on the hidden cab blocks around Create's Train
 * Controls, which sit low in front of the driver's seat because Create pairs a conductor seat with controls beside it
 * at the same height. On a train, a click on one of those cab blocks drives the train as a click on the controls would.
 */
public final class CabControls {
    private CabControls() {}

    /**
     * The Train Controls a click on a cab block drives: one touching it (sides, edges or corners) at its own height or
     * one layer below, the nearest if there are two.
     */
    public static Optional<BlockPos> controlsFor(BlockPos clicked, Predicate<BlockPos> isControls) {
        BlockPos best = null;
        for (int dy = -1; dy <= 0; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = clicked.offset(dx, dy, dz);
                    if (isControls.test(candidate) && (best == null || candidate.distSqr(clicked) < best.distSqr(clicked))) {
                        best = candidate;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
