package dev.railbound.carriage;

import dev.railbound.testutil.TestDesigns;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.placement.AssemblyTrack;
import dev.railbound.trainset.placement.CarriagePlanner;
import dev.railbound.trainset.placement.PlacedPart;
import dev.railbound.trainset.placement.PlacementPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CarriageFootprintTest {

    private static final ParsedDesign COACH = ParsedDesign.of(TestDesigns.sample());
    private static final AssemblyTrack EAST_TRACK = new AssemblyTrack(new BlockPos(0, 64, 0), Direction.EAST, 40);

    private static Set<BlockPos> planned(PlacementPlan plan) {
        return plan.parts().stream().map(PlacedPart::pos).collect(Collectors.toSet());
    }

    @Test
    void rebuildsThePlacedBlocksFromTheAnchor() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, -3, false);
        assertEquals(planned(plan), CarriageFootprint.positions(COACH, plan.anchor(), plan.facing()));
    }

    @Test
    void rebuildsAReversedCarriage() {
        PlacementPlan plan = CarriagePlanner.plan(COACH, EAST_TRACK, -3, true);
        assertEquals(planned(plan), CarriageFootprint.positions(COACH, plan.anchor(), plan.facing()));
    }

    @Test
    void rebuildsAfterATurn() {
        AssemblyTrack northTrack = new AssemblyTrack(new BlockPos(10, 70, 10), Direction.NORTH, 40);
        PlacementPlan plan = CarriagePlanner.plan(COACH, northTrack, 2, false);
        assertEquals(planned(plan), CarriageFootprint.positions(COACH, plan.anchor(), plan.facing()));
    }
}
