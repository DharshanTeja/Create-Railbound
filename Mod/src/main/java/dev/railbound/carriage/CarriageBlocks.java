package dev.railbound.carriage;

import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import dev.railbound.registry.RailboundBlocks;
import dev.railbound.trainset.design.HiddenPart;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.design.PartType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class CarriageBlocks {
    private CarriageBlocks() {}

    /** The block for one layout cell; local is the cell's design position (x across, y layer, z along). */
    public static BlockState stateFor(ParsedDesign design, HiddenPart part, Direction facing, BlockPos local) {
        return switch (part.type()) {
            case FRAME -> RailboundBlocks.FRAME.get().defaultBlockState()
                    .setValue(FrameBlock.SHAPE, part.shape().orElseThrow())
                    .setValue(FrameBlock.FACING, facing);
            case ANCHOR -> RailboundBlocks.ANCHOR.get().defaultBlockState()
                    .setValue(AnchorBlock.FACING, facing);
            case SEAT -> {
                Direction seatFacing = InteriorParts.seatFacing(facing, drives(design, local, Direction.NORTH),
                        drives(design, local, Direction.SOUTH));
                yield RailboundBlocks.SEAT.get().defaultBlockState()
                        .setValue(SeatPartBlock.FACING, seatFacing)
                        .setValue(SeatPartBlock.SIDE, InteriorParts.seatSide(PlaceholderSide.of(part.type(), local.getX()),
                                seatFacing != facing));
            }
            case DOOR -> RailboundBlocks.DOOR.get().defaultBlockState()
                    .setValue(DoorPartBlock.FACING, InteriorParts.doorOutward(facing, local, design.length()))
                    .setValue(DoorPartBlock.END, local.getX() == 0)
                    .setValue(DoorPartBlock.HALF, InteriorParts.doorHalf(design.partAt(local.below())
                            .map(below -> below.type() == PartType.DOOR).orElse(false)))
                    .setValue(DoorPartBlock.OPEN, false);
            case CONTROLS -> {
                Direction controlsFacing = InteriorParts.controlsFacing(facing, seatToward(design, local, Direction.NORTH));
                yield RailboundBlocks.CAB_CONTROLS.get().defaultBlockState()
                        .setValue(ControlsBlock.FACING, controlsFacing)
                        .setValue(CabControlsBlock.SIDE, InteriorParts.seatSide(PlaceholderSide.of(part.type(), local.getX()),
                                controlsFacing != facing));
            }
            case BUNKER -> RailboundBlocks.BUNKER.get().defaultBlockState();
            case WATER_TANK -> RailboundBlocks.WATER_TANK.get().defaultBlockState();
            case FIREBOX -> RailboundBlocks.FIREBOX.get().defaultBlockState();
            case CARGO_ITEM -> RailboundBlocks.CARGO_HOLD.get().defaultBlockState();
            case CARGO_FLUID -> RailboundBlocks.CARGO_TANK.get().defaultBlockState();
            case STEP -> RailboundBlocks.STEP.get().defaultBlockState()
                    .setValue(StepPartBlock.FACING, InteriorParts.outward(facing, local.getX()))
                    .setValue(StepPartBlock.HIGH, InteriorParts.stepIsHigh(design.partAt(local.offset(-Integer.signum(local.getX()), 0, 0))));
            case AIR -> throw new IllegalArgumentException("Air cells are never placed");
            default -> RailboundBlocks.PLACEHOLDER.get().defaultBlockState()
                    .setValue(PlaceholderPartBlock.FLOOR, local.getY() == 0)
                    .setValue(PlaceholderPartBlock.SIDE, PlaceholderSide.of(part.type(), local.getX()))
                    .setValue(PlaceholderPartBlock.FACING, facing);
        };
    }

    private static boolean isType(ParsedDesign design, BlockPos local, PartType type) {
        return design.partAt(local).map(part -> part.type() == type).orElse(false);
    }

    /** Whether controls one way (ahead: north, behind: south) belong to the seat at local, beside it or across a doorway. */
    private static boolean drives(ParsedDesign design, BlockPos local, Direction way) {
        return isType(design, local.relative(way), PartType.CONTROLS)
                || design.partAt(local.relative(way)).isEmpty() && isType(design, local.relative(way, 2), PartType.CONTROLS);
    }

    /** Whether the controls at local have their seat that way, beside them or across a doorway. */
    private static boolean seatToward(ParsedDesign design, BlockPos local, Direction way) {
        return isType(design, local.relative(way), PartType.SEAT)
                || design.partAt(local.relative(way)).isEmpty() && isType(design, local.relative(way, 2), PartType.SEAT);
    }
}
