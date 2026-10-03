package dev.railbound.carriage;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.load.TrainsetDesigns;
import dev.railbound.util.BlockGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/** Removes a whole carriage (hidden blocks + its bogeys). Server thread only. */
public final class CarriageRemover {
    public static final int MAX_PARTS = 4096;
    private static int removing;

    private CarriageRemover() {}

    public static boolean isRemoving() {
        return removing > 0;
    }

    /** Removes the carriage that the (still present) part at partPos belongs to; returns its design. */
    public static Optional<ResourceLocation> removeCarriage(Level level, BlockPos partPos) {
        return removeConnected(level, Set.of(partPos.immutable()), partPos.immutable());
    }

    /** Removes what is left after the part at pos was already removed; returns the design if the anchor was found. */
    public static Optional<ResourceLocation> removeRemainderAround(Level level, BlockPos pos) {
        Set<BlockPos> seeds = new LinkedHashSet<>();
        for (Direction direction : Direction.values()) {
            seeds.add(pos.relative(direction));
        }
        return removeConnected(level, seeds, pos.immutable());
    }

    /** The design of the carriage that the part at partPos belongs to, without changing anything. */
    public static Optional<ResourceLocation> findDesign(Level level, BlockPos partPos) {
        return Optional.ofNullable(collect(level, Set.of(partPos.immutable()), partPos.immutable()).design());
    }

    /** A standing carriage found from one of its parts: where its anchor is, which way it faces, and its design. */
    public record FoundCarriage(BlockPos anchor, Direction facing, ParsedDesign design) {}

    /** The carriage that the part at partPos belongs to, if its anchor and design can be found. */
    public static Optional<FoundCarriage> findCarriage(Level level, BlockPos partPos) {
        return found(level, collect(level, Set.of(partPos.immutable()), partPos.immutable()).anchor());
    }

    /** Swaps a part for another block without the rest of the carriage reacting to the removal. */
    public static void replacePart(Level level, BlockPos pos, BlockState state) {
        removing++;
        try {
            level.setBlock(pos, state, Block.UPDATE_ALL);
        } finally {
            removing--;
        }
    }

    private record CarriageScan(Set<BlockPos> parts, ResourceLocation design, BlockPos anchor) {}

    private static CarriageScan collect(Level level, Set<BlockPos> seeds, BlockPos target) {
        Predicate<BlockPos> isPart = p -> CarriagePart.is(level.getBlockState(p));
        Set<BlockPos> parts = new LinkedHashSet<>();
        for (BlockPos seed : seeds) {
            if (!parts.contains(seed)) {
                parts.addAll(BlockGraph.connected(seed, isPart, MAX_PARTS));
            }
        }

        List<BlockPos> anchors = new ArrayList<>();
        for (BlockPos part : parts) {
            if (level.getBlockEntity(part) instanceof AnchorBlockEntity) {
                anchors.add(part);
            }
        }
        BlockPos chosen = anchors.isEmpty() ? null : anchors.get(0);
        if (anchors.size() > 1) {
            // Touching carriages: keep only the one the target block belongs to.
            for (BlockPos anchor : anchors) {
                Optional<Set<BlockPos>> footprint = footprintOf(level, anchor);
                if (footprint.isPresent() && footprint.get().contains(target)) {
                    parts.retainAll(footprint.get());
                    chosen = anchor;
                    break;
                }
            }
        }
        return new CarriageScan(parts, chosen == null ? null : designAt(level, chosen), chosen);
    }

    private static Optional<ResourceLocation> removeConnected(Level level, Set<BlockPos> seeds, BlockPos target) {
        CarriageScan carriage = collect(level, seeds, target);
        Set<BlockPos> parts = carriage.parts();

        Set<BlockPos> bogeys = new LinkedHashSet<>();
        for (BlockPos part : parts) {
            for (Direction direction : Direction.values()) {
                BlockPos next = part.relative(direction);
                if (level.getBlockState(next).getBlock() instanceof AbstractBogeyBlock<?>) {
                    bogeys.add(next);
                }
            }
        }

        removing++;
        try {
            // Fittings built into the walls are the player's own blocks: drop them rather than leave them floating.
            found(level, carriage.anchor()).ifPresent(found -> {
                for (BlockPos pos : CarriageFootprint.positions(found.design(), found.anchor(), found.facing())) {
                    if (CarriageFittings.is(level.getBlockState(pos))) {
                        level.destroyBlock(pos, true);
                    }
                }
            });
            for (BlockPos pos : parts) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            for (BlockPos pos : bogeys) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        } finally {
            removing--;
        }
        return Optional.ofNullable(carriage.design());
    }

    private static Optional<FoundCarriage> found(Level level, @Nullable BlockPos anchor) {
        if (anchor == null) {
            return Optional.empty();
        }
        BlockState state = level.getBlockState(anchor);
        if (!state.hasProperty(AnchorBlock.FACING)) {
            return Optional.empty();
        }
        return TrainsetDesigns.get(designAt(level, anchor))
                .map(design -> new FoundCarriage(anchor, state.getValue(AnchorBlock.FACING), design));
    }

    private static ResourceLocation designAt(Level level, BlockPos anchor) {
        return level.getBlockEntity(anchor) instanceof AnchorBlockEntity entity ? entity.designId() : null;
    }

    private static Optional<Set<BlockPos>> footprintOf(Level level, BlockPos anchor) {
        BlockState state = level.getBlockState(anchor);
        Optional<ParsedDesign> design = TrainsetDesigns.get(designAt(level, anchor));
        if (design.isEmpty() || !state.hasProperty(AnchorBlock.FACING)) {
            return Optional.empty();
        }
        return Optional.of(CarriageFootprint.positions(design.get(), anchor, state.getValue(AnchorBlock.FACING)));
    }
}
