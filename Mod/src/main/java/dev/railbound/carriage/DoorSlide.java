package dev.railbound.carriage;

import dev.railbound.trainset.design.DoorSlideDirection;
import dev.railbound.trainset.design.DoorSpec;

/** How far and which way a carriage door leaf slides into its wall pocket. */
public final class DoorSlide {
    /** A fully open leaf has moved one block along the carriage, clear of the doorway. */
    public static final float OPEN_DISTANCE = 1f;

    private DoorSlide() {}

    /** Along design z: doors in the front half slide to the front (-1), the rest to the rear (+1). */
    public static int direction(int localZ, int length) {
        return localZ * 2 < length ? -1 : 1;
    }

    /** The design's slide for this door, or along the carriage towards its nearer end. */
    public static DoorSlideDirection of(DoorSpec door, int length) {
        return door.slide().orElse(direction(door.pos().getZ(), length) < 0 ? DoorSlideDirection.FRONT : DoorSlideDirection.REAR);
    }

    /** Blocks moved at animation progress 0..1, easing in and out. */
    public static float distance(float progress) {
        return OPEN_DISTANCE * progress * progress * (3 - 2 * progress);
    }
}
