package dev.railbound.carriage;

import dev.railbound.trainset.design.TrainsetCategory;

/** Only cars that a crew drives may carry Create's Train Controls. */
public final class ControlsRule {
    private ControlsRule() {}

    public static boolean allowsControls(TrainsetCategory category) {
        return category == TrainsetCategory.LOCOMOTIVE || category == TrainsetCategory.MULTIPLE_UNIT;
    }
}
