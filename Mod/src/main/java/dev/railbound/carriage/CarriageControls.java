package dev.railbound.carriage;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import dev.railbound.trainset.design.PartType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A loco's driver's controls are Create's own Train Controls, placed in the design's controls cells. Like bogeys they
 * are not our blocks, so the carriage holds them: they glue to it, go with it, and breaking one breaks the carriage.
 */
public final class CarriageControls {
    private CarriageControls() {}

    /** Blocks a carriage holds without them being parts: its bogeys and Train Controls. */
    public static boolean isHeld(BlockState state) {
        return state.getBlock() instanceof AbstractBogeyBlock<?> || AllBlocks.TRAIN_CONTROLS.has(state);
    }

    /** World positions of the controls cells of the carriage that the part at partPos belongs to. */
    public static Set<BlockPos> designControls(Level level, BlockPos partPos) {
        return CarriageRemover.findCarriage(level, partPos)
                .map(found -> found.design().cells().stream()
                        .filter(cell -> cell.part().type() == PartType.CONTROLS)
                        .map(cell -> CarriageFootprint.worldPos(found.design(), found.anchor(), found.facing(), cell.pos()))
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    /** Whether pos holds Train Controls that a neighbouring carriage's design put there. */
    public static boolean isDesignControls(Level level, BlockPos pos) {
        if (!AllBlocks.TRAIN_CONTROLS.has(level.getBlockState(pos))) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (CarriagePart.is(level.getBlockState(next))) {
                return designControls(level, next).contains(pos);
            }
        }
        return false;
    }
}
