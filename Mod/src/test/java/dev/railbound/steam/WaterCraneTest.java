package dev.railbound.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WaterCraneTest {

    @Test
    void theHoseHangsJustAboveALocoTankFillerBesideTheTrack() {
        // high enough to swing clear of the loco's tanks and boiler; out far enough to reach the near side tank
        assertEquals(new Vec3(10.5, 68.0, 1.75), WaterCrane.spout(new BlockPos(10, 64, 0), Direction.SOUTH));
        assertEquals(new Vec3(9.25, 68.0, 0.5), WaterCrane.spout(new BlockPos(10, 64, 0), Direction.WEST));
    }

    @Test
    void aTankFillerWithinTwoBlocksOfTheSpoutIsInReach() {
        Vec3 spout = new Vec3(10.5, 68.0, 1.75);
        assertTrue(WaterCrane.inReach(spout, new Vec3(10.5, 67.0, 2.75)));
        assertTrue(WaterCrane.inReach(spout, new Vec3(12.4, 68.0, 1.75)));
        assertFalse(WaterCrane.inReach(spout, new Vec3(13, 68.0, 1.75)), "the next block along the train");
    }

    @Test
    void itFacesTheNearestTrackWhenPlaced() {
        assertEquals(java.util.Optional.of(Direction.EAST),
                WaterCrane.towardsTrack(d -> d == Direction.EAST ? java.util.OptionalInt.of(2) : java.util.OptionalInt.empty()));
        assertEquals(java.util.Optional.of(Direction.NORTH), WaterCrane.towardsTrack(d -> switch (d) {
            case NORTH -> java.util.OptionalInt.of(1);
            case SOUTH -> java.util.OptionalInt.of(3);
            default -> java.util.OptionalInt.empty();
        }), "the closer of two tracks");
        assertEquals(java.util.Optional.empty(), WaterCrane.towardsTrack(d -> java.util.OptionalInt.empty()));
    }

    @Test
    void theArmSwingsOutToFillAndBackAlongTheTrackWhenDone() {
        assertEquals(WaterCrane.PARKED_ANGLE, WaterCrane.armAngle(0), 1e-9);
        assertEquals(0, WaterCrane.armAngle(1), 1e-9, "straight out over the track");
        double half = WaterCrane.armAngle(0.5);
        assertTrue(half < WaterCrane.PARKED_ANGLE && half > 0);
    }

    @Test
    void theArmMovesAtAnEvenPace() {
        assertEquals(0.25, WaterCrane.swingTowards(0.2, true), 1e-9);
        assertEquals(0.15, WaterCrane.swingTowards(0.2, false), 1e-9);
        assertEquals(1, WaterCrane.swingTowards(0.98, true), 1e-9);
        assertEquals(0, WaterCrane.swingTowards(0.02, false), 1e-9);
    }
}
