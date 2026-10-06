package dev.railbound.coupling;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Turning a real Create train round on a straight track, as a front-to-front or rear-to-rear join does. */
class CarriageReversalTest {
    private TrackGraph graph;
    private TrackNode west;
    private TrackNode east;
    private TrackEdge eastward;

    /** Two carriages heading east (towards +x): A in front (bogeys at x 94 and 85), B behind (77 and 68), gap 8. */
    private Train train() {
        graph = new TrackGraph(UUID.randomUUID());
        west = new TrackNode(new TrackNodeLocation(0, 64, 0).in(Level.OVERWORLD), 1, new Vec3(0, 1, 0));
        east = new TrackNode(new TrackNodeLocation(200, 64, 0).in(Level.OVERWORLD), 2, new Vec3(0, 1, 0));
        graph.addNode(west);
        graph.addNode(east);
        eastward = new TrackEdge(west, east, null, TrackMaterial.ANDESITE);
        graph.putConnection(west, east, eastward);
        graph.putConnection(east, west, new TrackEdge(east, west, null, TrackMaterial.ANDESITE));

        List<Carriage> carriages = new ArrayList<>(List.of(carriage(94, 85), carriage(77, 68)));
        Train train = new Train(UUID.randomUUID(), null, graph, carriages, new ArrayList<>(List.of(8)), false, 0);
        carriages.forEach(Carriage::updateContraptionAnchors);   // as last tick left them
        return train;
    }

    private Carriage carriage(double front, double back) {
        return new Carriage(bogey(front), bogey(back), (int) (front - back));
    }

    private CarriageBogey bogey(double x) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("Test", true);
        return new CarriageBogey(AllBlocks.SMALL_BOGEY.get(), false, data,
                new TravellingPoint(west, east, eastward, x + 1, false), new TravellingPoint(west, east, eastward, x - 1, false));
    }

    private static Carriage.DimensionalCarriageEntity anchors(Carriage carriage) {
        return carriage.getDimensional(Level.OVERWORLD);
    }

    @Test
    void turnedRoundTheBackCarriageLeadsFromItsFarBogey() {
        Train train = train();
        CarriageReversal.reverse(train);
        Carriage lead = train.carriages.get(0);
        assertEquals(68, anchors(lead).leadingAnchor().x, 1e-4, "B leads now, from its far (west) bogey");
        assertEquals(77, anchors(lead).trailingAnchor().x, 1e-4);
    }

    @Test
    void turnedRoundTheGapBetweenTheCarriagesIsUnchanged() {
        // Create measures the strain on each coupling from these anchors before moving the train: a stale anchor
        // reads a gap a whole bogey span too long, and the train is stopped for "stress on couplings"
        Train train = train();
        CarriageReversal.reverse(train);
        double gap = anchors(train.carriages.get(1)).leadingAnchor().distanceTo(anchors(train.carriages.get(0)).trailingAnchor());
        assertEquals(train.carriageSpacing.get(0), gap, 1e-4);
    }

    @Test
    void turnedRoundEveryCarriageIsMarkedReversedAndTurningBackClearsIt() {
        Train train = train();
        CarriageReversal.reverse(train);
        assertTrue(train.carriages.stream().allMatch(CarriageReversal::reversed));
        CarriageReversal.reverse(train);
        assertTrue(train.carriages.stream().noneMatch(CarriageReversal::reversed));
    }

    @Test
    void turnedRoundTheWheelPointsRunWestwards() {
        Train train = train();
        CarriageReversal.reverse(train);
        Carriage lead = train.carriages.get(0);
        assertTrue(lead.getLeadingPoint().getPosition(graph).x < lead.getTrailingPoint().getPosition(graph).x,
                "the new front is further west");
        assertEquals(east, lead.getLeadingPoint().node1, "moving forwards now runs from east to west");
    }
}
