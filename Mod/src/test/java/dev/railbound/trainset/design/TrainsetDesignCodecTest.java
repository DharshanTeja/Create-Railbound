package dev.railbound.trainset.design;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.railbound.testutil.TestDesigns;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrainsetDesignCodecTest {

    @Test
    void decodesSampleCoach() {
        TrainsetDesign d = TestDesigns.sample();
        assertEquals("trainset.railbound.coach_standard", d.name());
        assertEquals(TrainsetCategory.PASSENGER, d.category());
        assertEquals(new CarriageSize(16, 3, 4), d.size());
        assertEquals(List.of(
                new BogeySpec(4, ResourceLocation.fromNamespaceAndPath("railbound", "coach")),
                new BogeySpec(11, ResourceLocation.fromNamespaceAndPath("railbound", "coach"))), d.bogeys());
        assertEquals(PowerType.NONE, d.power());
        assertEquals(CargoSpec.NONE, d.cargo());
        assertEquals(new DoorSpec("door_left_front", new BlockPos(-1, 1, 2)), d.doors().get(0));
        assertEquals(4, d.layout().layers().size());
        assertEquals("frame:floor", d.layout().palette().get("#"));
    }

    @Test
    void optionalFieldsDefault() {
        TrainsetDesign d = TestDesigns.parse("""
                {"name":"n","category":"box_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],
                 "layout":{"palette":{"A":"anchor","#":"frame:floor"},"layers":[["A","#"]]}}
                """);
        assertEquals(PowerType.NONE, d.power());
        assertEquals(CargoSpec.NONE, d.cargo());
        assertEquals(List.of(), d.doors());
        assertEquals(ResourceLocation.fromNamespaceAndPath("create", "standard"), d.bogeys().get(0).style());
    }

    @Test
    void roundTrips() {
        TrainsetDesign d = TestDesigns.sample();
        JsonElement encoded = TrainsetDesign.CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow();
        assertEquals(d, TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void rejectsUnknownCategory() {
        DataResult<TrainsetDesign> r = TrainsetDesign.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"name":"n","category":"spaceship","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],"layout":{"palette":{},"layers":[]}}
                """));
        assertTrue(r.error().isPresent());
    }

    @Test
    void cargoUsesSnakeCaseKeys() {
        TrainsetDesign d = TestDesigns.parse("""
                {"name":"n","category":"tank_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],"cargo":{"item_slots":0,"fluid_mb":64000},
                 "layout":{"palette":{"A":"anchor","#":"frame:floor"},"layers":[["A","#"]]}}
                """);
        assertEquals(new CargoSpec(0, 64000), d.cargo());
    }
}
