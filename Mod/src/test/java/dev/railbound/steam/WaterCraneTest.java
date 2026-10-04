package dev.railbound.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WaterCraneTest {

    private static final Vec3 BASE = new Vec3(10.5, 64, 0.5);

    @Test
    void aFillerUpToThreeBlocksOutAndBelowTheArmIsInReach() {
        assertTrue(WaterCrane.inReach(BASE, new Vec3(10.5, 67, 1.5)), "right beside the crane");
        assertTrue(WaterCrane.inReach(BASE, new Vec3(10.5, 67, 3.5)), "with a block between");
        assertTrue(WaterCrane.inReach(BASE, new Vec3(12.5, 67, 2.5)), "off to one side");
        assertFalse(WaterCrane.inReach(BASE, new Vec3(10.5, 67, 4.5)), "too far out");
        assertFalse(WaterCrane.inReach(BASE, new Vec3(10.5, 67, 0.7)), "inside the column");
        assertFalse(WaterCrane.inReach(BASE, new Vec3(10.5, 68.5, 2.5)), "above the arm");
    }

    @Test
    void theArmReachesOutToTheFillerAndTheHoseHangsDownOntoIt() {
        WaterCrane.Aim aim = WaterCrane.aim(BASE, Direction.SOUTH, new Vec3(10.5, 67, 2.5));
        assertEquals(0, aim.angle(), 1e-9, "straight out the way the crane faces");
        assertEquals(2, aim.reach(), 1e-9);
        // the arm is 4.5 blocks up; a collar and a nozzle take a pixel each
        assertEquals(4.5 - 3 - 2 / 16.0, aim.hose(), 1e-9);
    }

    @Test
    void theArmTurnsTowardsAFillerOffToOneSide() {
        // facing south (+z), the filler one block east (+x) and two out: the arm turns towards east
        WaterCrane.Aim aim = WaterCrane.aim(BASE, Direction.SOUTH, new Vec3(11.5, 67, 2.5));
        assertEquals(Math.toDegrees(Math.atan2(1, 2)), aim.angle(), 1e-9);
        assertEquals(Math.sqrt(5), aim.reach(), 1e-9);
        // the same filler seen from a crane facing east is the other way round
        assertEquals(Math.toDegrees(Math.atan2(1, 2)) - 90, WaterCrane.aim(BASE, Direction.EAST, new Vec3(11.5, 67, 2.5)).angle(), 1e-9);
    }

    @Test
    void theNozzleEndsOnTheFiller() {
        Vec3 filler = new Vec3(11.5, 67, 2.5);
        Vec3 nozzle = WaterCrane.nozzle(BASE, Direction.SOUTH, WaterCrane.aim(BASE, Direction.SOUTH, filler));
        assertEquals(filler.x, nozzle.x, 1e-9);
        assertEquals(filler.y, nozzle.y, 1e-9);
        assertEquals(filler.z, nozzle.z, 1e-9);
    }

    @Test
    void swingingOutGoesFromParkedToTheFiller() {
        WaterCrane.Aim target = new WaterCrane.Aim(20, 2.5, 1.5);
        assertEquals(WaterCrane.PARKED, WaterCrane.swung(target, 0));
        assertEquals(target, WaterCrane.swung(target, 1));
        WaterCrane.Aim half = WaterCrane.swung(target, 0.5);
        assertEquals((WaterCrane.PARKED_ANGLE + 20) / 2, half.angle(), 1e-9);
        assertEquals((WaterCrane.ARM + 2.5) / 2, half.reach(), 1e-9);
    }

    @Test
    void theCraneStandsFiveBlocksTallUpToItsArm() {
        assertEquals(4, WaterCrane.TOP);
        assertTrue(WaterCrane.TOP + 1 > WaterCrane.ARM_HEIGHT, "the top block holds the arm");
    }

    @Test
    void eachPartOfTheCraneNeedsThePartsNextToIt() {
        // breaking any block of the crane brings the rest down with it
        assertTrue(WaterCrane.sectionStands(0, false, true), "the base needs only the column above it");
        assertFalse(WaterCrane.sectionStands(0, false, false));
        assertTrue(WaterCrane.sectionStands(2, true, true));
        assertFalse(WaterCrane.sectionStands(2, false, true));
        assertFalse(WaterCrane.sectionStands(2, true, false));
        assertTrue(WaterCrane.sectionStands(WaterCrane.TOP, true, false), "nothing goes above the top");
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
    void theArmMovesAtAnEvenPace() {
        assertEquals(0.25, WaterCrane.swingTowards(0.2, true), 1e-9);
        assertEquals(0.15, WaterCrane.swingTowards(0.2, false), 1e-9);
        assertEquals(1, WaterCrane.swingTowards(0.98, true), 1e-9);
        assertEquals(0, WaterCrane.swingTowards(0.02, false), 1e-9);
    }
}
