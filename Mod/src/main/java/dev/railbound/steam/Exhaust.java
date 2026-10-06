package dev.railbound.steam;

/**
 * What a steam loco blows out this tick besides its chimney smoke (see {@link Plume}): the exhaust beat (chuff); white
 * steam from the cylinder drain cocks as it starts off and again in a burst when it comes to a stand, and from the
 * safety valve when it stands at full pressure. A dead fire makes no beat; steam left in the boiler can still be let
 * out.
 */
public record Exhaust(boolean chuff, boolean cylinderSteam, boolean blowdown, boolean safetyValve) {
    /** Below this speed (blocks per tick, about 3 m/s) a pulling engine still has its cylinder cocks open. */
    static final double STARTING_SPEED = 0.15;
    static final double STANDING_SPEED = 0.01;
    /** How long the drain cocks blow after coming to a stand, and the least pressure that has anything to blow. */
    static final int BLOWDOWN_TICKS = 40;
    static final double BLOWDOWN_PRESSURE = 0.3;
    /** The safety valve lifts at this pressure while standing, in short bursts every few ticks. */
    static final double SAFETY_VALVE_PRESSURE = 0.97;
    static final int SAFETY_VALVE_EVERY = 3;
    /** ticksStopped for an engine that has been standing for ever (or never moved). */
    public static final int LONG_STOPPED = Integer.MAX_VALUE / 2;

    /**
     * @param ticksStopped ticks since the loco came to a stand: -1 while moving, {@link #LONG_STOPPED} if it never moved
     */
    public static Exhaust of(int chuffs, boolean fireLit, double speed, boolean accelerating, long gameTime,
                             int ticksStopped, double pressure) {
        double moving = Math.abs(speed);
        boolean standing = moving < STANDING_SPEED;
        boolean blowdown = standing && ticksStopped >= 0 && ticksStopped <= BLOWDOWN_TICKS && pressure >= BLOWDOWN_PRESSURE;
        boolean valve = standing && pressure >= SAFETY_VALVE_PRESSURE && gameTime % SAFETY_VALVE_EVERY == 0;
        if (!fireLit) {
            return new Exhaust(false, false, blowdown, valve);
        }
        boolean cocks = accelerating && !standing && moving < STARTING_SPEED;
        return new Exhaust(chuffs > 0, cocks, blowdown, valve);
    }

    /** A model point (pixels) in design block space, as the converter's ModelSpace places the model. */
    public static double[] toDesign(double mx, double my, double mz, int length) {
        return new double[] {(mx + 8) / 16, my / 16 + 1, (mz + 8 * length) / 16};
    }
}
