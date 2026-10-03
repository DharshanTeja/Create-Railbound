package dev.railbound.carriage;

/** How far and which way a carriage door leaf slides into its wall pocket. */
public final class DoorSlide {
    /** A fully open leaf has moved one block along the carriage, clear of the doorway. */
    public static final float OPEN_DISTANCE = 1f;

    private DoorSlide() {}

    /** Along design z: doors in the front half slide to the front (-1), the rest to the rear (+1). */
    public static int direction(int localZ, int length) {
        return localZ * 2 < length ? -1 : 1;
    }

    /** Blocks moved at animation progress 0..1, easing in and out. */
    public static float distance(float progress) {
        return OPEN_DISTANCE * progress * progress * (3 - 2 * progress);
    }
}
