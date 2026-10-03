package dev.railbound.carriage;

import dev.railbound.trainset.design.LayoutCell;
import dev.railbound.trainset.design.ParsedDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashSet;
import java.util.Set;

/** Which world blocks belong to one carriage, worked out from its anchor (same mapping as CarriagePlanner). */
public final class CarriageFootprint {
    private CarriageFootprint() {}

    public static Set<BlockPos> positions(ParsedDesign design, BlockPos anchorWorld, Direction facing) {
        Set<BlockPos> positions = new HashSet<>();
        for (LayoutCell cell : design.cells()) {
            positions.add(worldPos(design, anchorWorld, facing, cell.pos()));
        }
        return positions;
    }

    /** The world block for one design cell. */
    public static BlockPos worldPos(ParsedDesign design, BlockPos anchorWorld, Direction facing, BlockPos local) {
        BlockPos anchorLocal = design.anchor();
        return anchorWorld
                .relative(facing.getOpposite(), local.getZ() - anchorLocal.getZ())
                .above(local.getY() - anchorLocal.getY())
                .relative(facing.getClockWise(), local.getX() - anchorLocal.getX());
    }
}
