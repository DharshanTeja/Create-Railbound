package dev.railbound.coupling;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainCutTest {
    // loco, coach, coach, coach: spacings between them
    private static final List<Integer> SPACING = List.of(8, 9, 10);

    @Test
    void aSplitShareOutTheCarriagesAndTheSpacingEitherSideOfTheGap() {
        TrainCut.Split split = TrainCut.split(4, SPACING, 1, true, false);
        assertEquals(2, split.frontCount());
        assertEquals(List.of(8), split.frontSpacing());
        assertEquals(List.of(10), split.backSpacing());
    }

    @Test
    void theHalfWithTheLocoKeepsTheTrain() {
        assertTrue(TrainCut.split(4, SPACING, 0, true, false).frontKeeps());
        assertFalse(TrainCut.split(4, SPACING, 2, false, true).frontKeeps());
    }

    @Test
    void withALocoInBothHalvesTheFrontKeepsTheTrain() {
        assertTrue(TrainCut.split(4, SPACING, 1, true, true).frontKeeps());
    }

    @Test
    void thereIsNoGapAfterTheLastCarriage() {
        assertThrows(IllegalArgumentException.class, () -> TrainCut.split(4, SPACING, 3, true, false));
        assertThrows(IllegalArgumentException.class, () -> TrainCut.split(4, SPACING, -1, true, false));
    }

    @Test
    void backingTheRearIntoAnotherTrainsFrontPutsTheMovingTrainFirst() {
        assertEquals(new TrainCut.Join(TrainCut.Order.MOVING_FIRST, false), TrainCut.join(false, true));
    }

    @Test
    void runningTheFrontIntoAnotherTrainsRearPutsTheOtherTrainFirst() {
        assertEquals(new TrainCut.Join(TrainCut.Order.OTHER_FIRST, false), TrainCut.join(true, false));
    }

    @Test
    void frontToFrontTurnsTheOtherTrainRoundAndPutsItFirst() {
        // turned round, the other train's front becomes its rear, which meets the moving train's front
        assertEquals(new TrainCut.Join(TrainCut.Order.OTHER_FIRST, true), TrainCut.join(true, true));
    }

    @Test
    void rearToRearTurnsTheOtherTrainRoundAndPutsItBehind() {
        // a loco backed onto the back of a train for push-pull
        assertEquals(new TrainCut.Join(TrainCut.Order.MOVING_FIRST, true), TrainCut.join(false, false));
    }

    @Test
    void joinedSpacingRunsFirstTrainThenTheNewGapThenTheSecond() {
        assertEquals(List.of(8, 9, 7, 10), TrainCut.joinedSpacing(List.of(8, 9), 7, List.of(10)));
        assertEquals(List.of(7), TrainCut.joinedSpacing(List.of(), 7, List.of()));
    }

    @Test
    void slowlyTheyJoinFasterTheyBump() {
        assertEquals(TrainCut.Contact.JOIN, TrainCut.contact(0.04));
        assertEquals(TrainCut.Contact.JOIN, TrainCut.contact(TrainCut.JOIN_SPEED));
        assertEquals(TrainCut.Contact.BUMP, TrainCut.contact(0.06));
    }

    @Test
    void aTrainThatArrivedForwardsIsStillAtTheStationWhileItsFrontStaysOutermost() {
        // Create stops a train with its front at the station, or its back when it arrived backwards
        assertTrue(TrainCut.stillAtStation(true, false), "a loco coupled on behind it");
        assertFalse(TrainCut.stillAtStation(false, false), "a loco coupled on in front of it");
    }

    @Test
    void aTrainThatArrivedBackwardsIsStillAtTheStationWhileItsBackStaysOutermost() {
        assertTrue(TrainCut.stillAtStation(false, true));
        assertFalse(TrainCut.stillAtStation(true, true));
    }

    private static Optional<TrainCut.Pose> pose(float yaw) {
        return Optional.of(new TrainCut.Pose(yaw, 0));
    }

    @Test
    void aTrainStraightAtTheStationDisassemblesWhole() {
        assertEquals(3, TrainCut.fitAtStation(List.of(pose(90), pose(90), pose(270)), false), "facing either way along it");
    }

    @Test
    void aTrainBentRoundACurveDisassemblesUpToTheCurve() {
        // the station end first: the third carriage stands on the curve, the rest beyond it
        assertEquals(2, TrainCut.fitAtStation(List.of(pose(0), pose(180), pose(37.5f), pose(90), pose(90)), false));
    }

    @Test
    void aTrainThatArrivedBackwardsCountsFromItsBack() {
        assertEquals(1, TrainCut.fitAtStation(List.of(pose(90), pose(37.5f), pose(0)), true));
    }

    @Test
    void carriagesRoundASharpCornerDoNotFitEvenIfSquareToTheGrid() {
        // a carriage wholly past a tight corner is square again, but along the other track
        assertEquals(1, TrainCut.fitAtStation(List.of(pose(0), pose(90)), false));
    }

    @Test
    void aSlopeOrAnUnloadedCarriageEndsWhatFits() {
        assertEquals(1, TrainCut.fitAtStation(List.of(pose(0), Optional.of(new TrainCut.Pose(0, 5)), pose(0)), false));
        assertEquals(1, TrainCut.fitAtStation(List.of(pose(0), Optional.empty(), pose(0)), false));
        assertEquals(0, TrainCut.fitAtStation(List.of(pose(37.5f), pose(0)), false), "nothing fits");
    }

    @Test
    void joinSpeedIsOneBlockASecond() {
        // Create's train speeds are blocks per tick
        assertEquals(1 / 20.0, TrainCut.JOIN_SPEED, 1e-12);
    }
}
