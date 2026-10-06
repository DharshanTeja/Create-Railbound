package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WagonDesignsTest {
    private static TrainsetDesign shipped(String id) throws Exception {
        try (var in = WagonDesignsTest.class.getResourceAsStream("/data/railbound/railbound/trainsets/" + id + ".json")) {
            return TestDesigns.parse(new String(Objects.requireNonNull(in, id).readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void goodsWagonsStandOnTheRailboundFreightBogey() throws Exception {
        for (String id : new String[] {"wagon_box_van", "wagon_tank"}) {
            for (BogeySpec bogey : shipped(id).bogeys()) {
                assertEquals(ResourceLocation.fromNamespaceAndPath("railbound", "freight"), bogey.style(), id);
            }
        }
    }
}
