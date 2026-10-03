package dev.railbound.trainset.placement;

import dev.railbound.trainset.design.BogeySpec;
import dev.railbound.trainset.design.LayoutCell;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.design.PartType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

public final class CarriagePlanner {
    /** Empty blocks between two carriages. */
    public static final int GAP = 1;
    /** Create refuses bogeys closer than this (StationBlockEntity#assemble). */
    public static final int MIN_BOGEY_SPACING = 3;
    /** The bogey sits one block above the track, and layer 0 sits beside it: its floor slab is level with the bogey's top. */
    public static final int TRACK_TO_FLOOR = 1;

    private CarriagePlanner() {}

    /** Where the carriage's first block goes, given the furthest offset already occupied (if any). */
    public static int startOffset(ParsedDesign design, boolean reversed, OptionalInt lastOccupied) {
        if (lastOccupied.isPresent()) {
            return lastOccupied.getAsInt() + 1 + GAP;
        }
        return -stationEndBogeyOffset(design, reversed);
    }

    private static int stationEndBogeyOffset(ParsedDesign design, boolean reversed) {
        int length = design.length();
        return design.design().bogeys().stream()
                .mapToInt(bogey -> reversed ? length - 1 - bogey.z() : bogey.z())
                .min()
                .orElse(0);
    }

    public static PlacementPlan plan(ParsedDesign design, AssemblyTrack track, int start, boolean reversed) {
        int length = design.length();
        Direction facing = reversed ? track.direction() : track.direction().getOpposite();
        Direction right = facing.getClockWise();
        IntUnaryOperator offsetOf = z -> reversed ? start + (length - 1 - z) : start + z;

        List<PlacedPart> parts = new ArrayList<>();
        BlockPos anchor = null;
        for (LayoutCell cell : design.cells()) {
            BlockPos local = cell.pos();
            BlockPos world = track.track(offsetOf.applyAsInt(local.getZ()))
                    .above(TRACK_TO_FLOOR + local.getY())
                    .relative(right, local.getX());
            parts.add(new PlacedPart(world, cell.part(), local));
            if (cell.part().type() == PartType.ANCHOR) {
                anchor = world;
            }
        }

        List<Integer> bogeyOffsets = new ArrayList<>();
        List<BlockPos> bogeys = new ArrayList<>();
        for (BogeySpec bogey : design.design().bogeys()) {
            int offset = offsetOf.applyAsInt(bogey.z());
            bogeyOffsets.add(offset);
            bogeys.add(track.track(offset).above());
        }
        return new PlacementPlan(facing, start, start + length - 1,
                List.copyOf(parts), List.copyOf(bogeys), List.copyOf(bogeyOffsets), anchor);
    }

    /** True if any planned part would sit face to face with a part of another carriage (they would glue together). */
    public static boolean touchesOtherParts(PlacementPlan plan, Predicate<BlockPos> isExistingPart) {
        Set<BlockPos> own = new HashSet<>();
        plan.parts().forEach(part -> own.add(part.pos()));
        for (BlockPos pos : own) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = pos.relative(direction);
                if (!own.contains(neighbour) && isExistingPart.test(neighbour)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Optional<PlacementProblem> check(PlacementPlan plan, AssemblyTrack track, Collection<Integer> existingBogeyOffsets) {
        for (int offset : plan.bogeyOffsets()) {
            if (offset < 0 || offset >= track.length()) {
                return Optional.of(PlacementProblem.DOES_NOT_FIT);
            }
        }
        for (int offset : plan.bogeyOffsets()) {
            for (int existing : existingBogeyOffsets) {
                if (Math.abs(offset - existing) < MIN_BOGEY_SPACING) {
                    return Optional.of(PlacementProblem.TOO_CLOSE);
                }
            }
        }
        return Optional.empty();
    }
}
