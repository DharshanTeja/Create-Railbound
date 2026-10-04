package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StepClimbingTest {

    /** A step at (10, 64, 0) whose ladder faces east: its outer face is the plane x = 11. */
    private static final BlockPos STEP = new BlockPos(10, 64, 0);

    /** A player-sized box (0.6 wide, 1.8 tall) standing on the ground a block below the step. */
    private static AABB playerAt(double minX, double centreZ) {
        return new AABB(minX, 63, centreZ - 0.3, minX + 0.6, 64.8, centreZ + 0.3);
    }

    @Test
    void climbsWhenTouchingTheLadderFromOutside() {
        assertTrue(StepClimbing.touchesLadder(playerAt(11.0, 0.5), STEP, Direction.EAST, false));
        assertTrue(StepClimbing.touchesLadder(playerAt(11.05, 0.5), STEP, Direction.EAST, false), "a hair away still counts");
    }

    @Test
    void doesNotClimbFromFurtherAway() {
        assertFalse(StepClimbing.touchesLadder(playerAt(11.4, 0.5), STEP, Direction.EAST, false));
    }

    @Test
    void doesNotClimbBesideTheLadder() {
        assertFalse(StepClimbing.touchesLadder(playerAt(11.0, 1.5), STEP, Direction.EAST, false), "next block along the coach");
    }

    @Test
    void doesNotClimbFromUnderTheCoach() {
        assertFalse(StepClimbing.touchesLadder(playerAt(9.2, 0.5), STEP, Direction.EAST, false), "the inner side is not the ladder");
    }

    @Test
    void aCabStepKeepsClimbingUpToItsTread() {
        // feet already above a coach ladder's top, still on a cab step's ladder
        AABB halfwayUp = new AABB(11.0, 64.6, 0.2, 11.6, 66.4, 0.8);
        assertTrue(StepClimbing.touchesLadder(halfwayUp, STEP, Direction.EAST, true));
        assertFalse(StepClimbing.touchesLadder(halfwayUp, STEP, Direction.EAST, false));
    }

    @Test
    void doesNotClimbFromHighAbove() {
        AABB onTheRoof = new AABB(11.0, 67, 0.2, 11.6, 68.8, 0.8);
        assertFalse(StepClimbing.touchesLadder(onTheRoof, STEP, Direction.EAST, false));
    }
}
