package dev.railbound.carriage;

import dev.railbound.trainset.design.TrainsetCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControlsRuleTest {

    @Test
    void onlyDrivingCarsTakeTrainControls() {
        assertTrue(ControlsRule.allowsControls(TrainsetCategory.LOCOMOTIVE));
        assertTrue(ControlsRule.allowsControls(TrainsetCategory.MULTIPLE_UNIT));
        assertFalse(ControlsRule.allowsControls(TrainsetCategory.PASSENGER));
        assertFalse(ControlsRule.allowsControls(TrainsetCategory.BOX_CAR));
        assertFalse(ControlsRule.allowsControls(TrainsetCategory.TANK_CAR));
    }
}
