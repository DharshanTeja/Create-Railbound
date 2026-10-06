package dev.railbound.steam;

import java.util.Optional;

/**
 * The chimney's smoke, the way Immersive Railroading blows it: a steady stream every tick while the fire is lit, each
 * exhaust beat (chuff) a bigger, faster puff. Puffs spread out as they rise and thin away; working hard they are thick
 * and linger, standing idle they are thin and short. Darker just after the fireman has put coal on.
 */
public final class Plume {
    /** A puff's lifetime (ticks) at full effort and speed is twice this; standing idle a fifth of it. */
    static final int BASE_LIFE = 200;
    /** Puff size (block) as it leaves the chimney, before the speed share. */
    static final float BASE_SIZE = 0.2f;
    /** How much bigger, and faster rising, a puff is on an exhaust beat. */
    static final float BEAT = 1.75f;
    /** How many times its starting size a puff spreads to by the end of its life. */
    static final float SPREAD = 17;
    /** Rise per tick (blocks) as a puff leaves the chimney. */
    static final double RISE = 0.25;
    /** Grey of ordinary coal smoke, and of the darker smoke just after firing. */
    static final float SHADE = 0.2f;
    static final float SHADE_FIRED = 0.08f;

    private Plume() {}

    /** One puff from the chimney: its starting and final size, lifetime, opacity, grey and rising speed. */
    public record Puff(float startSize, float endSize, int life, float alpha, float shade, double rise) {}

    /**
     * This tick's puff, if the fire is lit.
     *
     * @param effort how hard the engine works, 0 idle to 1 flat out
     * @param speed  the train's speed, blocks per tick
     */
    public static Optional<Puff> of(boolean fireLit, boolean beat, double effort, double speed, boolean justFired) {
        if (!fireLit) {
            return Optional.empty();
        }
        double share = Math.max(0.2, Math.min(1, Math.abs(speed) * 2));
        double work = Math.max(0, Math.min(1, effort));
        int life = (int) Math.round(BASE_LIFE * (1 + work) * share);
        float alpha = (float) (0.25 + work / 2);
        float size = (float) (BASE_SIZE * (0.8 + share));
        double rise = RISE;
        if (beat) {
            size *= BEAT;
            rise *= BEAT;
        }
        return Optional.of(new Puff(size, size * SPREAD, life, alpha, justFired ? SHADE_FIRED : SHADE, rise));
    }
}
