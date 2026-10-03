package dev.railbound.carriage;

import dev.railbound.trainset.design.PartType;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderSideTest {

    private static final double PX = 1 / 16d;

    @Test
    void sideColumnsGetAnOuterWall() {
        assertEquals(PlaceholderSide.LEFT, PlaceholderSide.of(PartType.SEAT, -1));
        assertEquals(PlaceholderSide.RIGHT, PlaceholderSide.of(PartType.SEAT, 1));
        assertEquals(PlaceholderSide.NONE, PlaceholderSide.of(PartType.SEAT, 0));
    }

    @Test
    void doorsStayOpen() {
        assertEquals(PlaceholderSide.NONE, PlaceholderSide.of(PartType.DOOR, -1));
        assertEquals(PlaceholderSide.NONE, PlaceholderSide.of(PartType.DOOR, 1));
    }

    @Test
    void leftSeatOfAnEastFacingCarHasItsWallOnTheNorth() {
        List<AABB> boxes = PlaceholderPartBlock.collisionFor(false, PlaceholderSide.LEFT, Direction.EAST).toAabbs();
        assertEquals(1, boxes.size());
        assertEquals(0, boxes.get(0).minZ, 1e-9);
        assertEquals(2 * PX, boxes.get(0).maxZ, 1e-9);
    }

    @Test
    void openSeatHasNoCollision() {
        assertTrue(PlaceholderPartBlock.collisionFor(false, PlaceholderSide.NONE, Direction.NORTH).isEmpty());
    }

    @Test
    void mirroringSwapsTheSide() {
        assertEquals(PlaceholderSide.RIGHT, PlaceholderSide.LEFT.mirrored());
        assertEquals(PlaceholderSide.NONE, PlaceholderSide.NONE.mirrored());
    }
}
