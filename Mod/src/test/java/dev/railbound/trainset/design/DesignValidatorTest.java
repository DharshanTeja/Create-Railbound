package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DesignValidatorTest {

    private static final TrainsetDesign SAMPLE = TestDesigns.sample();

    private static void assertProblem(TrainsetDesign d, String expectedFragment) {
        List<String> problems = DesignValidator.validate(d);
        assertTrue(problems.stream().anyMatch(p -> p.contains(expectedFragment)),
                () -> "expected a problem containing '" + expectedFragment + "' but got " + problems);
    }

    @Test
    void sampleIsValid() {
        assertEquals(List.of(), DesignValidator.validate(SAMPLE));
    }

    @Test
    void rejectsOneBogey() {
        assertProblem(TestDesigns.withBogeys(SAMPLE, List.of(SAMPLE.bogeys().get(0))), "exactly 2 bogeys, found 1");
    }

    @Test
    void rejectsThreeBogeys() {
        List<BogeySpec> three = new ArrayList<>(SAMPLE.bogeys());
        three.add(new BogeySpec(8, BogeySpec.DEFAULT_STYLE));
        assertProblem(TestDesigns.withBogeys(SAMPLE, three), "exactly 2 bogeys, found 3");
    }

    @Test
    void rejectsBogeyOutsideCarriage() {
        assertProblem(TestDesigns.withBogeys(SAMPLE, List.of(
                new BogeySpec(3, BogeySpec.DEFAULT_STYLE), new BogeySpec(16, BogeySpec.DEFAULT_STYLE))), "bogey z=16");
    }

    @Test
    void rejectsBogeysAtSamePosition() {
        assertProblem(TestDesigns.withBogeys(SAMPLE, List.of(
                new BogeySpec(5, BogeySpec.DEFAULT_STYLE), new BogeySpec(5, BogeySpec.DEFAULT_STYLE))), "different positions");
    }

    @Test
    void rejectsWidthTwo() {
        assertProblem(TestDesigns.withSize(SAMPLE, new CarriageSize(16, 2, 3)), "width must be 1 or 3");
    }

    @Test
    void rejectsWrongLayerCount() {
        assertProblem(TestDesigns.withSize(SAMPLE, new CarriageSize(16, 3, 4)), "3 layers, expected height 4");
    }

    @Test
    void rejectsWrongRowCount() {
        assertProblem(TestDesigns.withSize(SAMPLE, new CarriageSize(17, 3, 3)), "layer 0 has 16 rows, expected length 17");
    }

    @Test
    void rejectsWrongRowWidth() {
        List<List<String>> layers = copyLayers();
        layers.get(1).set(4, "L..R");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(SAMPLE.layout().palette(), layers)),
                "layer 1 row 4 has 4 cells, expected width 3");
    }

    @Test
    void rejectsCharacterMissingFromPalette() {
        List<List<String>> layers = copyLayers();
        layers.get(0).set(5, "S#X");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(SAMPLE.layout().palette(), layers)),
                "layer 0 row 5: character 'X' is not in the palette");
    }

    @Test
    void rejectsUnknownPaletteValue() {
        Map<String, String> palette = new HashMap<>(SAMPLE.layout().palette());
        palette.put("S", "sofa");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(palette, SAMPLE.layout().layers())),
                "palette 'S': unknown part 'sofa'");
    }

    @Test
    void rejectsMultiCharacterPaletteKey() {
        Map<String, String> palette = new HashMap<>(SAMPLE.layout().palette());
        palette.put("SS", "seat");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(palette, SAMPLE.layout().layers())),
                "palette key 'SS' must be a single character");
    }

    @Test
    void rejectsMissingAnchor() {
        List<List<String>> layers = copyLayers();
        layers.get(2).set(8, "qtp");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(SAMPLE.layout().palette(), layers)),
                "exactly 1 anchor, found 0");
    }

    @Test
    void rejectsTwoAnchors() {
        List<List<String>> layers = copyLayers();
        layers.get(2).set(3, "qAp");
        assertProblem(TestDesigns.withLayout(SAMPLE, new LayoutSpec(SAMPLE.layout().palette(), layers)),
                "exactly 1 anchor, found 2");
    }

    @Test
    void rejectsDoorSpecNotOnDoorCell() {
        assertProblem(TestDesigns.withDoors(SAMPLE, List.of(new DoorSpec("door_left_front", new BlockPos(-1, 0, 5)))),
                "door 'door_left_front'");
    }

    @Test
    void rejectsBlankName() {
        assertProblem(TestDesigns.withName(SAMPLE, " "), "name must not be empty");
    }

    private static List<List<String>> copyLayers() {
        List<List<String>> layers = new ArrayList<>();
        SAMPLE.layout().layers().forEach(layer -> layers.add(new ArrayList<>(layer)));
        return layers;
    }
}
