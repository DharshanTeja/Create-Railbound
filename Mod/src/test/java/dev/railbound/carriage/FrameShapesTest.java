package dev.railbound.carriage;

import dev.railbound.trainset.design.FrameShape;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameShapesTest {

    private static final double PX = 1 / 16d;

    private static AABB single(FrameShape shape, Direction facing) {
        var boxes = FrameShapes.get(shape, facing).toAabbs();
        assertEquals(1, boxes.size());
        return boxes.get(0);
    }

    @Test
    void northFacingLeftWallIsOnTheWestSide() {
        AABB box = single(FrameShape.WALL_LEFT, Direction.NORTH);
        assertEquals(0, box.minX, 1e-9);
        assertEquals(2 * PX, box.maxX, 1e-9);
    }

    @Test
    void eastFacingLeftWallIsOnTheNorthSide() {
        AABB box = single(FrameShape.WALL_LEFT, Direction.EAST);
        assertEquals(0, box.minZ, 1e-9);
        assertEquals(2 * PX, box.maxZ, 1e-9);
        assertEquals(1, box.maxX - box.minX, 1e-9);
    }

    @Test
    void southFacingLeftWallIsOnTheEastSide() {
        AABB box = single(FrameShape.WALL_LEFT, Direction.SOUTH);
        assertEquals(14 * PX, box.minX, 1e-9);
        assertEquals(1, box.maxX, 1e-9);
    }

    @Test
    void westFacingLeftWallIsOnTheSouthSide() {
        AABB box = single(FrameShape.WALL_LEFT, Direction.WEST);
        assertEquals(14 * PX, box.minZ, 1e-9);
        assertEquals(1, box.maxZ, 1e-9);
    }

    @Test
    void roofLeavesHeadroomAndMatchesTheModel() {
        AABB box = single(FrameShape.ROOF, Direction.NORTH);
        assertEquals(4 * PX, box.minY, 1e-9);
        assertEquals(12 * PX, box.maxY, 1e-9);
    }

    @Test
    void floorIsAOnePixelSlabAtTheTopOfTheBlock() {
        // the floor layer sits at bogey height, so people walk on top of it, level with the bogey's top
        AABB box = single(FrameShape.FLOOR, Direction.WEST);
        assertEquals(15 * PX, box.minY, 1e-9);
        assertEquals(1, box.maxY, 1e-9);
    }

    @Test
    void mirroringSwapsLeftAndRight() {
        assertEquals(FrameShape.WALL_RIGHT, FrameShape.WALL_LEFT.mirrored());
        assertEquals(FrameShape.FLOOR_WALL_LEFT, FrameShape.FLOOR_WALL_RIGHT.mirrored());
        assertEquals(FrameShape.ROOF_WALL_RIGHT, FrameShape.ROOF_WALL_LEFT.mirrored());
        assertEquals(FrameShape.ROOF, FrameShape.ROOF.mirrored());
        for (FrameShape shape : FrameShape.values()) {
            assertEquals(shape, shape.mirrored().mirrored(), shape.name());
        }
    }

    @Test
    void everyShapeAndFacingIsDefined() {
        for (FrameShape shape : FrameShape.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                assertFalse(FrameShapes.get(shape, facing).isEmpty(), shape + " " + facing);
            }
        }
    }
}
