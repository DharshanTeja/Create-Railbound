package dev.railbound.coupling;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarriageFlipTest {
    @Test
    void aTrainTurnedRoundHasItsSpacingTheOtherWayRound() {
        assertEquals(List.of(10, 9, 8), CarriageFlip.reversedSpacing(List.of(8, 9, 10)));
        assertEquals(List.of(), CarriageFlip.reversedSpacing(List.of()));
    }

    @Test
    void aCarriageFacingTheTrainsWayKeepsItsControlsSides() {
        assertTrue(CarriageFlip.forTrain(true, false, false).getFirst());
        assertFalse(CarriageFlip.forTrain(true, false, false).getSecond());
    }

    @Test
    void aReversedCarriagesForwardControlsDriveTheTrainBackwards() {
        // a loco backed on for push-pull faces the other way: its cab looks back along the train
        assertFalse(CarriageFlip.forTrain(true, false, true).getFirst());
        assertTrue(CarriageFlip.forTrain(true, false, true).getSecond());
    }

    @Test
    void stationDisassemblyMovesAReversedCarriageToItsOwnFirstBogey() {
        // Create places each carriage from its train-leading bogey; a reversed carriage starts at the other one
        assertEquals(5, CarriageFlip.disassemblyDistance(5, 9, false, false));
        assertEquals(14, CarriageFlip.disassemblyDistance(5, 9, false, true));
        assertEquals(5, CarriageFlip.disassemblyDistance(14, 9, true, true));
        assertEquals(14, CarriageFlip.disassemblyDistance(14, 9, true, false));
    }

    @Test
    void oneDriverDrivesFromWhicheverEndHasControlsFacingThatWay() {
        assertTrue(CarriageFlip.canDrive(true, false, false), "a driver facing this way, as in Create");
        assertTrue(CarriageFlip.canDrive(false, true, true), "a driver elsewhere and a cab facing this way");
        assertFalse(CarriageFlip.canDrive(false, false, true), "no driver anywhere");
        assertFalse(CarriageFlip.canDrive(false, true, false), "nobody's cab faces this way");
    }
}
