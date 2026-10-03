package dev.railbound.carriage;

import dev.railbound.registry.RailboundBlocks;
import dev.railbound.trainset.design.HiddenPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class CarriageBlocks {
    private CarriageBlocks() {}

    /** The block for one layout cell; local is the cell's design position (x across, y layer, z along). */
    public static BlockState stateFor(HiddenPart part, Direction facing, BlockPos local) {
        return switch (part.type()) {
            case FRAME -> RailboundBlocks.FRAME.get().defaultBlockState()
                    .setValue(FrameBlock.SHAPE, part.shape().orElseThrow())
                    .setValue(FrameBlock.FACING, facing);
            case ANCHOR -> RailboundBlocks.ANCHOR.get().defaultBlockState()
                    .setValue(AnchorBlock.FACING, facing);
            case AIR -> throw new IllegalArgumentException("Air cells are never placed");
            default -> RailboundBlocks.PLACEHOLDER.get().defaultBlockState()
                    .setValue(PlaceholderPartBlock.PART, PlaceholderPart.of(part.type()).orElseThrow())
                    .setValue(PlaceholderPartBlock.FLOOR, local.getY() == 0)
                    .setValue(PlaceholderPartBlock.SIDE, PlaceholderSide.of(part.type(), local.getX()))
                    .setValue(PlaceholderPartBlock.FACING, facing);
        };
    }
}
