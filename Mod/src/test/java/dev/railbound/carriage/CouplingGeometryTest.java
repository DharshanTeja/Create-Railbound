package dev.railbound.carriage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CouplingGeometryTest {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    /** Two carriages on a straight track along x, ends one block apart, facing each other. */
    private static final CouplingGeometry.End A = new CouplingGeometry.End(new Vec3(10, 64, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), UP);
    private static final CouplingGeometry.End B = new CouplingGeometry.End(new Vec3(11, 64, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, -1), UP);

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, 1e-9, "x");
        assertEquals(expected.y, actual.y, 1e-9, "y");
        assertEquals(expected.z, actual.z, 1e-9, "z");
    }

    @Test
    void endsWhoseKnucklesReachEachOtherTouch() {
        // a block apart, two half-block knuckles: their heads meet
        assertTrue(CouplingGeometry.touching(A, B, 1.0));
    }

    @Test
    void endsFurtherApartThanTheirKnucklesReachDoNotTouch() {
        CouplingGeometry.End away = new CouplingGeometry.End(new Vec3(11.3, 64, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, -1), UP);
        assertFalse(CouplingGeometry.touching(A, away, 1.0));
    }

    @Test
    void endsPushedPastEachOtherStillTouch() {
        // a fast train overshoots in one tick: its end is already beyond the other's knuckle
        CouplingGeometry.End past = new CouplingGeometry.End(new Vec3(10.2, 64, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, -1), UP);
        assertTrue(CouplingGeometry.touching(A, past, 1.0));
    }

    @Test
    void endsFacingTheSameWayOrOnAnotherTrackDoNotTouch() {
        CouplingGeometry.End sameWay = new CouplingGeometry.End(new Vec3(11, 64, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), UP);
        assertFalse(CouplingGeometry.touching(A, sameWay, 1.0));
        CouplingGeometry.End nextTrack = new CouplingGeometry.End(new Vec3(11, 64, 3), new Vec3(-1, 0, 0), new Vec3(0, 0, -1), UP);
        assertFalse(CouplingGeometry.touching(A, nextTrack, 1.0));
    }

    @Test
    void theKnucklesMeetHalfwayBetweenTheEnds() {
        assertVec(new Vec3(10.5, 64, 0), CouplingGeometry.meet(A, B));
    }

    @Test
    void onAStraightTheFrameBetweenTheEndsLooksAlongTheTrackFromA() {
        CouplingGeometry.End half = CouplingGeometry.between(A, B, 0.5);
        assertVec(new Vec3(10.5, 64, 0), half.centre());
        assertVec(A.outward(), half.outward());
        assertVec(A.right(), half.right());
        assertVec(UP, half.up());
    }

    @Test
    void onACurveTheBellowsFrameTurnsFromAToBAsItGoes() {
        double a = Math.toRadians(20);
        CouplingGeometry.End turned = new CouplingGeometry.End(new Vec3(11, 64, 0.2), new Vec3(-Math.cos(a), 0, Math.sin(a)),
                new Vec3(-Math.sin(a), 0, -Math.cos(a)), UP);
        CouplingGeometry.End start = CouplingGeometry.between(A, turned, 0);
        assertVec(A.centre(), start.centre());
        assertVec(A.outward(), start.outward());
        CouplingGeometry.End end = CouplingGeometry.between(A, turned, 1);
        assertVec(turned.centre(), end.centre());
        assertVec(turned.outward().scale(-1), end.outward());
        assertVec(turned.right().scale(-1), end.right());
        CouplingGeometry.End half = CouplingGeometry.between(A, turned, 0.5);
        assertEquals(1, half.outward().length(), 1e-9);
        assertEquals(0, half.outward().dot(half.right()), 1e-9);
        assertEquals(Math.toRadians(10), Math.acos(half.outward().dot(A.outward())), 1e-6, "halfway round the turn");
    }

    /** A carriage end at x along the track, pointing out towards +x (or -x). */
    private static CouplingGeometry.End endAt(double x, double z, int out) {
        return new CouplingGeometry.End(new Vec3(x, 64, z), new Vec3(out, 0, 0), new Vec3(0, 0, out), UP);
    }

    /** Two 16-block carriages on a straight: A from x 0 to 16, B from 17 to 33. */
    private static final List<CouplingGeometry.End> A_ENDS = List.of(endAt(0, 0, -1), endAt(16, 0, 1));
    private static final Vec3 A_MIDDLE = new Vec3(8, 64, 0);
    private static final Vec3 B_MIDDLE = new Vec3(25, 64, 0);

    @Test
    void ofTwoCarriagesTheEndsThatFaceEachOtherJoin() {
        assertArrayEquals(new int[] {1, 0}, CouplingGeometry.facing(A_ENDS, A_MIDDLE, List.of(endAt(17, 0, -1), endAt(33, 0, 1)),
                B_MIDDLE).orElseThrow(), "A's back meets B's front");
        assertArrayEquals(new int[] {1, 1}, CouplingGeometry.facing(A_ENDS, A_MIDDLE, List.of(endAt(33, 0, 1), endAt(17, 0, -1)),
                B_MIDDLE).orElseThrow(), "B the other way round: A's back meets B's back");
    }

    @Test
    void endsSwungFarApartOnATightCurveStillJoin() {
        // B's front swung 5 blocks out to the side and away: still the pair that faces each other
        CouplingGeometry.End bFront = new CouplingGeometry.End(new Vec3(19, 64, 5), new Vec3(-0.6, 0, -0.8), new Vec3(-0.8, 0, 0.6), UP);
        assertArrayEquals(new int[] {1, 0}, CouplingGeometry.facing(A_ENDS, A_MIDDLE, List.of(bFront), new Vec3(26, 64, 14))
                .orElseThrow());
    }

    @Test
    void aCreateCarriageWithACouplerOnlyAtItsFarEndDoesNotCouple() {
        // B's only coupler block is on its far end, pointing away from A: that join keeps Create's chain
        assertEquals(Optional.empty(), CouplingGeometry.facing(A_ENDS, A_MIDDLE, List.of(endAt(33, 0, 1)), B_MIDDLE));
        assertEquals(Optional.empty(), CouplingGeometry.facing(List.of(), A_MIDDLE, List.of(endAt(17, 0, -1)), B_MIDDLE),
                "no coupler at all");
    }

    @Test
    void atAFreeEndTheKnuckleRestsAFixedWayOut() {
        assertVec(new Vec3(10.75, 64, 0), CouplingGeometry.rest(A, 0.75));
    }

    @Test
    void onAStraightTheKnuckleDoesNotSlide() {
        // ends a block apart, knuckles half a block long: they lock in the middle as they are
        assertEquals(0, CouplingGeometry.slide(A, CouplingGeometry.meet(A, B), 0.5), 1e-9);
    }

    @Test
    void onACurveTheKnuckleSlidesOutJustFarEnoughToKeepTheHeadsLocked() {
        // ends swung apart round a tight curve: the meeting point is ~1.58 blocks out, the knuckle is 0.5 long
        CouplingGeometry.End far = new CouplingGeometry.End(new Vec3(13, 64, 1), new Vec3(-1, 0, 0), new Vec3(0, 0, -1), UP);
        Vec3 meet = CouplingGeometry.meet(A, far);
        assertEquals(meet.distanceTo(A.centre()) - 0.5, CouplingGeometry.slide(A, meet, 0.5), 1e-9);
    }

    @Test
    void endsCloserThanTwoKnucklesDoNotSlide() {
        assertEquals(0, CouplingGeometry.slide(A, CouplingGeometry.meet(A, B), 0.75), 1e-9);
    }

    @Test
    void aCouplerBlocksKnuckleStartsAtTheBackOfItsBlockAtHalfHeight() {
        // facing out of the carriage towards +x: the carriage body is behind it, at -x
        assertVec(new Vec3(3, 1.5, 5.5), CouplingGeometry.blockEnd(new BlockPos(3, 1, 5), Direction.EAST));
        assertVec(new Vec3(3.5, 1.5, 5), CouplingGeometry.blockEnd(new BlockPos(3, 1, 5), Direction.SOUTH));
    }

    @Test
    void aFreeCouplerBlocksKnuckleStaysInsideItsOwnBlock() {
        // its coupling face rests on the block's outer face, as long as the block standing in the world
        CouplingGeometry.End end = new CouplingGeometry.End(CouplingGeometry.blockEnd(new BlockPos(3, 1, 5), Direction.EAST),
                new Vec3(1, 0, 0), new Vec3(0, 0, 1), UP);
        assertVec(new Vec3(4, 1.5, 5.5), CouplingGeometry.rest(end, CarriageCouplers.BLOCK_REACH));
    }
}
