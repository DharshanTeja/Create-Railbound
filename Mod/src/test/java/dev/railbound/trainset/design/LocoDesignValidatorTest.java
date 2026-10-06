package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LocoDesignValidatorTest {
    /** A small steam loco: water tanks, a cab with controls ahead of and behind one seat, and a coal bunker. */
    private static final String LOCO = """
            {"name":"n","category":"locomotive","size":{"length":7,"width":3,"height":2},
             "bogeys":[{"z":0},{"z":6}],"power":"steam",
             "performance":{"top_speed":20,"curve_speed":10,"acceleration":2},
             "steam":{"water":16000,"bunker_slots":2},
             "drive":{"wheel_radius":9.5,"axle_y":-17,"axles":[-40,-10,20],"main_axle":1,
                      "crank_radius":6,"crosshead_y":-7,"rod_length":75},
             "layout":{"palette":{"#":"frame:floor","C":"controls","S":"seat","B":"bunker","W":"water_tank","A":"anchor",".":"air"},
               "layers":[["#.#","W#W","#C#","#S#","#C#","B##","#.#"],
                         ["...","...","...",".A.","...","...","..."]]}}
            """;

    private static TrainsetDesign loco(String from, String to) {
        assertTrue(LOCO.contains(from), from);
        return TestDesigns.parse(LOCO.replace(from, to));
    }

    private static void assertProblem(TrainsetDesign d, String fragment) {
        List<String> problems = DesignValidator.validate(d);
        assertTrue(problems.stream().anyMatch(p -> p.contains(fragment)),
                () -> "expected a problem containing '" + fragment + "' but got " + problems);
    }

    @Test
    void aSteamLocoIsValid() {
        assertEquals(List.of(), DesignValidator.validate(TestDesigns.parse(LOCO)));
    }

    @Test
    void theShippedTankEngineIsValid() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/railbound/railbound/trainsets/loco_steam_tank.json")) {
            TrainsetDesign tank = TestDesigns.parse(new String(java.util.Objects.requireNonNull(in).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(List.of(), DesignValidator.validate(tank));
        }
    }

    @Test
    void theTankEngineCabHasADriverEachWayAClearAisleAndADoorway() throws Exception {
        try (var in = getClass().getResourceAsStream("/data/railbound/railbound/trainsets/loco_steam_tank.json")) {
            ParsedDesign tank = ParsedDesign.of(TestDesigns.parse(new String(java.util.Objects.requireNonNull(in).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8)));
            // standing level (layer 1); x -1 is the left side, z runs from the front
            assertEquals(Optional.of(PartType.CONTROLS), type(tank, -1, 11), "left: controls ahead of the driver");
            assertEquals(Optional.of(PartType.SEAT), type(tank, -1, 12));
            assertEquals(Optional.of(PartType.SEAT), type(tank, 1, 12), "right: a seat facing the bunker, ahead of the door");
            assertEquals(Optional.of(PartType.CONTROLS), type(tank, 1, 14), "its controls after the door, at the back");
            for (int z = 11; z <= 13; z++) {
                assertEquals(Optional.empty(), type(tank, 0, z), "the aisle at z " + z);
            }
            assertEquals(Optional.empty(), type(tank, -1, 13), "left doorway");
            assertEquals(Optional.empty(), type(tank, 1, 13), "right doorway");
        }
    }

    private static Optional<PartType> type(ParsedDesign design, int x, int z) {
        return design.partAt(new net.minecraft.core.BlockPos(x, 1, z)).map(HiddenPart::type);
    }

    @Test
    void readsPerformanceAndSteam() {
        TrainsetDesign d = TestDesigns.parse(LOCO);
        assertEquals(Optional.of(new PerformanceSpec(20, 10, 2)), d.performance());
        assertEquals(Optional.of(new SteamSpec(16000, 2)), d.steam());
        assertEquals(Optional.empty(), TestDesigns.sample().performance(), "a coach has neither");
        assertEquals(Optional.empty(), TestDesigns.sample().steam());
    }

    @Test
    void readsTheDriveAndNamesItsMovingParts() {
        DriveSpec drive = TestDesigns.parse(LOCO).drive().orElseThrow();
        assertEquals(List.of(-40.0, -10.0, 20.0), drive.axles());
        assertEquals(List.of("drive_wheels_1_right", "drive_wheels_1_left", "drive_wheels_2_right", "drive_wheels_2_left",
                "drive_wheels_3_right", "drive_wheels_3_left", "drive_coupling_rod_right", "drive_coupling_rod_left",
                "drive_connecting_rod_right", "drive_connecting_rod_left", "drive_crosshead_right", "drive_crosshead_left"),
                drive.parts());
    }

    @Test
    void theDriveMustBeAbleToTurn() {
        assertProblem(loco("\"main_axle\":1", "\"main_axle\":3"), "main_axle must pick one of the 3 axles");
        assertProblem(loco("\"wheel_radius\":9.5", "\"wheel_radius\":0"), "wheel_radius must be above 0");
        assertProblem(loco("\"crank_radius\":6", "\"crank_radius\":0"), "crank_radius must be above 0");
        // a rod shorter than the crank throw plus the drop to the crosshead line cannot reach round the crank
        assertProblem(loco("\"rod_length\":75", "\"rod_length\":15"), "rod_length is too short");
        assertProblem(loco("\"axles\":[-40,-10,20]", "\"axles\":[]"), "needs at least one axle");
    }

    @Test
    void aLocomotiveNeedsPerformance() {
        assertProblem(loco("\"performance\":{\"top_speed\":20,\"curve_speed\":10,\"acceleration\":2},", ""),
                "needs a \"performance\" section");
    }

    @Test
    void performanceMustMakeSense() {
        assertProblem(loco("\"top_speed\":20", "\"top_speed\":0"), "top_speed must be above 0");
        assertProblem(loco("\"curve_speed\":10", "\"curve_speed\":30"), "curve_speed must not be above top_speed");
        assertProblem(loco("\"acceleration\":2", "\"acceleration\":-1"), "acceleration must be above 0");
    }

    @Test
    void aSteamLocoNeedsASteamSection() {
        assertProblem(loco("\"steam\":{\"water\":16000,\"bunker_slots\":2},", ""), "needs a \"steam\" section");
    }

    @Test
    void steamCapacitiesMustMakeSense() {
        assertProblem(loco("\"water\":16000", "\"water\":0"), "water must be above 0");
        assertProblem(loco("\"bunker_slots\":2", "\"bunker_slots\":10"), "bunker_slots must be 1 to 9");
    }

    @Test
    void aSteamLocoNeedsOneBunkerAndAWaterTank() {
        assertProblem(loco("\"B##\"", "\"###\""), "exactly 1 coal bunker");
        assertProblem(loco("\"W#W\"", "\"###\""), "at least 1 water tank");
    }

    @Test
    void aLocomotiveNeedsTrainControls() {
        assertProblem(loco("\"#C#\",\"#S#\",\"#C#\"", "\"###\",\"#S#\",\"###\""), "needs Train Controls");
    }

    @Test
    void controlsNeedTheConductorSeatDirectlyAheadOrBehind() {
        assertProblem(loco("\"#S#\"", "\"###\""), "needs a seat directly ahead or behind");
    }

    @Test
    void controlsMayFaceTheirSeatAcrossAnOpenDoorway() {
        // seat, an open cell, controls: valid; seat, a wall, controls: not (the anchor moved off the open cell)
        String anchorLater = LOCO.replace("[\"...\",\"...\",\"...\",\".A.\",\"...\",\"...\",\"...\"]",
                "[\"...\",\"...\",\"...\",\"...\",\"...\",\".A.\",\"...\"]");
        assertNotEquals(LOCO, anchorLater);
        assertEquals(List.of(), DesignValidator.validate(TestDesigns.parse(
                anchorLater.replace("\"#C#\",\"#S#\",\"#C#\"", "\"#S#\",\"#.#\",\"#C#\""))));
        assertProblem(TestDesigns.parse(anchorLater.replace("\"#C#\",\"#S#\",\"#C#\"", "\"#S#\",\"###\",\"#C#\"")),
                "needs a seat directly ahead or behind");
    }

    /** The test loco with firebox cells either side of its front controls. */
    private static String withFirebox(String loco) {
        return loco.replace("\"W\":\"water_tank\",", "\"W\":\"water_tank\",\"F\":\"firebox\",")
                .replace("\"#C#\",\"#S#\"", "\"FCF\",\"#S#\"");
    }

    @Test
    void theFireboxDoorBelongsToASteamLoco() {
        assertEquals(List.of(), DesignValidator.validate(TestDesigns.parse(withFirebox(LOCO))));
        assertProblem(TestDesigns.parse(withFirebox(LOCO).replace("\"power\":\"steam\"", "\"power\":\"diesel\"")),
                "firebox cells belong on a steam locomotive");
    }

    @Test
    void onlyLocomotivesAndMultipleUnitsCarryControls() {
        assertProblem(loco("\"category\":\"locomotive\"", "\"category\":\"passenger\""),
                "only locomotives and multiple units");
    }
}
