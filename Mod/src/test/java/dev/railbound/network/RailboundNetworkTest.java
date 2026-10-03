package dev.railbound.network;

import dev.railbound.testutil.TestDesigns;
import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RailboundNetworkTest {

    @Test
    void dropsInvalidSyncedDesigns() {
        ResourceLocation good = ResourceLocation.fromNamespaceAndPath("railbound", "good");
        ResourceLocation bad = ResourceLocation.fromNamespaceAndPath("railbound", "bad");
        TrainsetDesign broken = TestDesigns.withBogeys(TestDesigns.sample(), List.of(TestDesigns.sample().bogeys().get(0)));
        Map<ResourceLocation, TrainsetDesign> accepted = RailboundNetwork.acceptValid(Map.of(good, TestDesigns.sample(), bad, broken));
        assertEquals(Map.of(good, TestDesigns.sample()), accepted);
    }
}
