package dev.railbound.trainset.placement;

import dev.railbound.testutil.TestDesigns;
import dev.railbound.trainset.design.ParsedDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class CarriagePlannerTest {

    private static final ParsedDesign COACH = ParsedDesign.of(TestDesigns.sample());
    private static final AssemblyTrack EAST_TRACK = new AssemblyTrack(new BlockPos(0, 64, 0), Direction.EAST, 40);

    private static BlockPos worldOf(PlacementPlan plan, BlockPos local) {
        return plan.parts().stream().filter(p -> p.local().equals(local)).findFirst().orElseThrow().pos();
    }

    @Test
    void firstCarriagePutsItsFrontBogeyOnOffsetZero() {
        int start = CarriagePlanner.startOffset(COACH, false, OptionalInt.empty());
        assertEquals(-4, start);
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, start, false);
        assertEquals(List.of(0, 7), plan.bogeyOffsets());
        assertEquals(List.of(new BlockPos(0, 65, 0), new BlockPos(7, 65, 0)), plan.bogeys());
        assertEquals(-4, plan.firstOffset());
        assertEquals(11, plan.lastOffset());
    }

    @Test
    void carriageFacesTheStation() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, -3, false);
        assertEquals(Direction.WEST, plan.facing());
    }

    @Test
    void mapsLocalCellsToWorld() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, -3, false);
        assertEquals(new BlockPos(5, 68, 0), plan.anchor());
        // left seat at row 3 on the walking layer: offset 0, two above track, left of a west-facing car is south (+z)
        assertEquals(new BlockPos(0, 66, 1), worldOf(plan, new BlockPos(-1, 1, 3)));
    }

    @Test
    void floorLayerSitsBesideTheBogey() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, -3, false);
        BlockPos frontBogey = plan.bogeys().get(0);
        assertEquals(frontBogey.south(), worldOf(plan, new BlockPos(-1, 0, 4)));
        assertTrue(plan.parts().stream().noneMatch(p -> p.pos().equals(frontBogey)), "the bogey's own cell stays free");
    }

    @Test
    void reversedCarriageFacesAwayAndMirrorsAlongTheTrack() {
        int start = CarriagePlanner.startOffset(COACH, true, OptionalInt.empty());
        assertEquals(-4, start);
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, start, true);
        assertEquals(Direction.EAST, plan.facing());
        assertEquals(List.of(7, 0), plan.bogeyOffsets());
        // left of an east-facing car is north (-z); row 3 now lands on offset 8
        assertEquals(new BlockPos(8, 66, -1), worldOf(plan, new BlockPos(-1, 1, 3)));
    }

    @Test
    void nextCarriageLeavesAOneBlockGap() {
        int start = CarriagePlanner.startOffset(COACH, false, OptionalInt.of(12));
        assertEquals(14, start);
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, start, false);
        assertEquals(List.of(18, 25), plan.bogeyOffsets());
    }

    @Test
    void acceptsAFittingCarriage() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, 14, false);
        assertEquals(Optional.empty(), CarriagePlanner.check(plan, EAST_TRACK, List.of(0, 9)));
    }

    @Test
    void rejectsACarriageLongerThanTheTrack() {
        AssemblyTrack shortTrack = new AssemblyTrack(new BlockPos(0, 64, 0), Direction.EAST, 20);
        PlacementPlan plan = CarriagePlanner.plan(COACH, shortTrack, 14, false);
        assertEquals(Optional.of(PlacementProblem.DOES_NOT_FIT), CarriagePlanner.check(plan, shortTrack, List.of()));
    }

    @Test
    void rejectsBogeysCloserThanThree() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, 14, false);
        assertEquals(Optional.of(PlacementProblem.TOO_CLOSE), CarriagePlanner.check(plan, EAST_TRACK, List.of(16)));
    }

    @Test
    void detectsACarriageTouchingAnotherCarriage() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, -3, false);
        // a part of another carriage directly south of this coach's left wall (parallel track three blocks over)
        BlockPos besideLeftWall = worldOf(plan, new BlockPos(-1, 1, 5)).south();
        assertTrue(CarriagePlanner.touchesOtherParts(plan, besideLeftWall::equals));
        assertFalse(CarriagePlanner.touchesOtherParts(plan, pos -> false));
    }

    @Test
    void ownPartsDoNotCountAsTouching() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, -3, false);
        java.util.Set<BlockPos> own = new java.util.HashSet<>();
        plan.parts().forEach(p -> own.add(p.pos()));
        assertFalse(CarriagePlanner.touchesOtherParts(plan, own::contains));
    }

    @Test
    void planCoversEveryCell() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, -3, false);
        assertEquals(COACH.cells().size(), plan.parts().size());
    }
}
