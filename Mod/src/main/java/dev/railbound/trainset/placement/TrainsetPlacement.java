package dev.railbound.trainset.placement;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.railbound.carriage.CarriageBuilder;
import dev.railbound.carriage.CarriagePartBlock;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.item.TrainsetItem;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

/** Server-side: turns a trainset item click on assembly track into a carriage. */
public final class TrainsetPlacement {
    /** Blocks above the track scanned for an existing carriage: bogey layer plus three carriage layers and one spare. */
    public static final int SCAN_HEIGHT = 5;

    private TrainsetPlacement() {}

    public static InteractionResult tryPlace(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);
        Player player = context.getPlayer();
        if (!(clickedState.getBlock() instanceof ITrackBlock) || player == null) {
            return InteractionResult.PASS;
        }

        ResourceLocation designId = TrainsetItem.designId(stack);
        Optional<ParsedDesign> design = TrainsetDesigns.get(designId);
        if (design.isEmpty()) {
            return refuse(player, "unknown_design");
        }
        Optional<StationBlockEntity> station = assemblingStationAt(level, clicked);
        if (station.isEmpty()) {
            return refuse(player, "no_station");
        }
        if (!(clickedState.getBlock() instanceof TrackBlock track)
                || track.getMaterial().trackType != TrackMaterial.TrackType.STANDARD) {
            return refuse(player, "wrong_gauge");
        }
        AssemblyTrack assemblyTrack = trackOf(level, station.get());
        if (assemblyTrack == null) {
            return refuse(player, "no_station");
        }

        boolean reversed = player.isShiftKeyDown();
        int start = CarriagePlanner.startOffset(design.get(), reversed, lastOccupied(level, assemblyTrack));
        PlacementPlan plan = CarriagePlanner.plan(design.get(), assemblyTrack, start, reversed);
        Optional<PlacementProblem> problem = CarriagePlanner.check(plan, assemblyTrack, bogeyOffsets(level, assemblyTrack));
        if (problem.isPresent()) {
            return refuse(player, problem.get().key());
        }
        if (CarriagePlanner.touchesOtherParts(plan, pos -> level.getBlockState(pos).getBlock() instanceof CarriagePartBlock)) {
            return refuse(player, PlacementProblem.TOUCHING.key());
        }
        Optional<BlockPos> blocked = firstBlocked(level, plan);
        if (blocked.isPresent()) {
            player.displayClientMessage(Component.translatable("railbound.placement.blocked", blocked.get().toShortString())
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        CarriageBuilder.build(level, plan, designId);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("railbound.placement.placed",
                Component.translatable(design.get().design().name())), true);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult refuse(Player player, String key) {
        player.displayClientMessage(Component.translatable("railbound.placement." + key).withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
    }

    static Optional<StationBlockEntity> assemblingStationAt(Level level, BlockPos trackPos) {
        for (Map.Entry<BlockPos, BoundingBox> entry : StationBlockEntity.assemblyAreas.get(level).entrySet()) {
            if (entry.getValue().isInside(trackPos)
                    && level.getBlockEntity(entry.getKey()) instanceof StationBlockEntity station
                    && station.isAssembling()) {
                return Optional.of(station);
            }
        }
        return Optional.empty();
    }

    @Nullable
    static AssemblyTrack trackOf(Level level, StationBlockEntity station) {
        station.refreshAssemblyInfo();
        BoundingBox area = StationBlockEntity.assemblyAreas.get(level).get(station.getBlockPos());
        Direction direction = station.getAssemblyDirection();
        if (area == null || direction == null) {
            return null;
        }
        BlockPos first = station.edgePoint.getGlobalPosition().relative(direction);
        int length = switch (direction.getAxis()) {
            case X -> area.getXSpan();
            case Y -> area.getYSpan();
            case Z -> area.getZSpan();
        };
        return new AssemblyTrack(first, direction, length);
    }

    /** Furthest assembly offset with anything standing on it (our carriages or player-built ones). */
    static OptionalInt lastOccupied(Level level, AssemblyTrack track) {
        Direction side = track.direction().getClockWise();
        for (int i = track.length() - 1; i >= 0; i--) {
            BlockPos base = track.track(i);
            for (int y = 1; y <= SCAN_HEIGHT; y++) {
                for (int x = -1; x <= 1; x++) {
                    if (!level.getBlockState(base.above(y).relative(side, x)).canBeReplaced()) {
                        return OptionalInt.of(i);
                    }
                }
            }
        }
        return OptionalInt.empty();
    }

    static List<Integer> bogeyOffsets(Level level, AssemblyTrack track) {
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < track.length(); i++) {
            if (level.getBlockState(track.track(i).above()).getBlock() instanceof AbstractBogeyBlock<?>) {
                offsets.add(i);
            }
        }
        return offsets;
    }

    static Optional<BlockPos> firstBlocked(Level level, PlacementPlan plan) {
        return Stream.concat(plan.bogeys().stream(), plan.parts().stream().map(PlacedPart::pos))
                .filter(pos -> !level.getBlockState(pos).canBeReplaced())
                .findFirst();
    }
}
