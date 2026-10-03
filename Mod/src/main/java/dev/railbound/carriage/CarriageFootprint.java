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
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        BlockPos anchorLocal = design.anchor();
        Set<BlockPos> positions = new HashSet<>();
        for (LayoutCell cell : design.cells()) {
            BlockPos local = cell.pos();
            positions.add(anchorWorld
                    .relative(back, local.getZ() - anchorLocal.getZ())
                    .above(local.getY() - anchorLocal.getY())
                    .relative(right, local.getX() - anchorLocal.getX()));
        }
        return positions;
    }
}
