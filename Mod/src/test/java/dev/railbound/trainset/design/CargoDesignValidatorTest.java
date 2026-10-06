package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargoDesignValidatorTest {
    /** A small goods wagon: solid all through, its hold in the middle. */
    private static final String VAN = """
            {"name":"n","category":"box_car","size":{"length":5,"width":3,"height":2},
             "bogeys":[{"z":0},{"z":4}],"cargo":{"item_slots":160},
             "layout":{"palette":{"X":"frame:full","H":"cargo_item","A":"anchor",".":"air"},
               "layers":[["X.X","XXX","XHX","XXX","X.X"],
                         ["XXX","XXX","XAX","XXX","XXX"]]}}
            """;

    private static TrainsetDesign van(String from, String to) {
        assertTrue(VAN.contains(from), from);
        return TestDesigns.parse(VAN.replace(from, to));
    }

    private static void assertProblem(TrainsetDesign d, String fragment) {
        List<String> problems = DesignValidator.validate(d);
        assertTrue(problems.stream().anyMatch(p -> p.contains(fragment)),
                () -> "expected a problem containing '" + fragment + "' but got " + problems);
    }

    private static TrainsetDesign shipped(String id) throws Exception {
        try (InputStream in = CargoDesignValidatorTest.class.getResourceAsStream("/data/railbound/railbound/trainsets/" + id + ".json")) {
            return TestDesigns.parse(new String(Objects.requireNonNull(in, id).readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void aWagonWithOneHoldIsValid() {
        assertEquals(List.of(), DesignValidator.validate(TestDesigns.parse(VAN)));
    }

    @Test
    void itemCargoNeedsExactlyOneHoldCell() {
        assertProblem(van("XHX", "XXX"), "item_slots needs exactly 1 cargo_item cell, found 0");
        assertProblem(van("[\"XXX\",\"XXX\",\"XAX\"", "[\"XXX\",\"XHX\",\"XAX\""), "item_slots needs exactly 1 cargo_item cell, found 2");
    }

    @Test
    void aHoldCellNeedsCargoToHold() {
        assertProblem(van("\"cargo\":{\"item_slots\":160},", ""), "cargo_item cell but no \"cargo\" item_slots");
    }

    @Test
    void fluidCargoNeedsExactlyOneTankCell() {
        TrainsetDesign tank = van("\"cargo\":{\"item_slots\":160}", "\"cargo\":{\"fluid_mb\":144000}");
        assertProblem(tank, "fluid_mb needs exactly 1 cargo_fluid cell, found 0");
        assertProblem(tank, "cargo_item cell but no \"cargo\" item_slots");
    }

    @Test
    void theShippedBoxVanHoldsOneHundredAndSixtyStacks() throws Exception {
        TrainsetDesign boxVan = shipped("wagon_box_van");
        assertEquals(List.of(), DesignValidator.validate(boxVan));
        assertEquals(160, boxVan.cargo().itemSlots());
        assertEquals(TrainsetCategory.BOX_CAR, boxVan.category());
    }

    @Test
    void theShippedTankWagonHoldsOneHundredAndFortyFourBuckets() throws Exception {
        TrainsetDesign tank = shipped("wagon_tank");
        assertEquals(List.of(), DesignValidator.validate(tank));
        assertEquals(144_000, tank.cargo().fluidMb());
        assertEquals(TrainsetCategory.TANK_CAR, tank.category());
    }
}
