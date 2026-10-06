package dev.railbound.steam;

/**
 * A steam loco's boiler, ticked while the loco is in service. Coal fed from the bunker keeps the fire lit; the fire
 * raises steam pressure, and pressure is what lets the engine pull. Coal and water go faster the harder the engine
 * works. When the water gets too low the safety (fusible) plug melts and drops the fire until the tank is refilled.
 * Coal is counted in coal items (a coal block is 9); water in millibuckets.
 */
public final class Boiler {
    /** Coal burnt per tick at full effort, one coal in 80 s as in a furnace; idling burns {@link #IDLE_SHARE} of that. */
    static final double FULL_BURN = 1 / 1600.0;
    /** Water boiled off per tick at full effort, in millibuckets. */
    static final double FULL_WATER = 1.0;
    /** How much of the full rate an idling engine still burns and boils. */
    static final double IDLE_SHARE = 0.25;
    /** Pressure gained per tick with the fire lit: cold to full in about 55 s. */
    static final double WARM = 1 / 1100.0;
    /** Pressure lost per tick with the fire out. */
    static final double COOL = 1 / 1500.0;
    /** Extra pressure drawn per tick at full effort. */
    static final double DRAW = 1 / 3000.0;
    /** Below this pressure the engine cannot move the train. */
    static final double MOVING_PRESSURE = 0.6;
    /** The pull at just-moving pressure, rising to 1 at full pressure. */
    static final double MIN_PULL = 0.3;
    /** The plug melts when the water falls under this share of the tank... */
    static final double PLUG_LEVEL = 0.05;
    /** ...and the fire can be relit once it is back above this share. */
    static final double RELIGHT_LEVEL = 0.25;

    /** Something that hands the fireman one more lump of fuel, returning how many coal it is worth (0 when empty). */
    @FunctionalInterface
    public interface FuelSource {
        double take();
    }

    /** Everything a boiler needs to be saved and restored. */
    public record Snapshot(double pressure, double fire, double water, boolean plugMelted) {}

    private final int capacity;
    private double pressure;
    /** Coal still burning in the firebox. */
    private double fire;
    private double water;
    private boolean plugMelted;

    public Boiler(int capacity) {
        this.capacity = capacity;
    }

    public static Boiler restore(int capacity, Snapshot snapshot) {
        Boiler boiler = new Boiler(capacity);
        boiler.pressure = snapshot.pressure();
        boiler.fire = snapshot.fire();
        boiler.water = Math.min(capacity, snapshot.water());
        boiler.plugMelted = snapshot.plugMelted();
        return boiler;
    }

    public Snapshot snapshot() {
        return new Snapshot(pressure, fire, water, plugMelted);
    }

    /** One tick in service; effort is how hard the engine is working, 0 (idle) to 1 (flat out). */
    public void tick(double effort, FuelSource fuel) {
        effort = Math.max(0, Math.min(1, effort));
        if (plugMelted && water >= RELIGHT_LEVEL * capacity) {
            plugMelted = false;
        }
        boolean low = water < PLUG_LEVEL * capacity;
        // the plug only melts in a hot boiler; a cold, empty one is just waiting to be filled
        if (!plugMelted && low && (fireLit() || pressure > 0)) {
            plugMelted = true;
            fire = 0;
        }
        if (!plugMelted && !low && fire <= 0) {
            fire += fuel.take();
        }

        double rate = IDLE_SHARE + (1 - IDLE_SHARE) * effort;
        if (fireLit()) {
            fire = Math.max(0, fire - FULL_BURN * rate);
            water = Math.max(0, water - FULL_WATER * rate);
            pressure += WARM - DRAW * effort;
        } else {
            pressure -= COOL + DRAW * effort;
        }
        pressure = Math.max(0, Math.min(1, pressure));
    }

    /** Fills the tank, returning how much water it took. */
    public int addWater(int amount) {
        int accepted = (int) Math.max(0, Math.min(amount, Math.floor(capacity - water)));
        water += accepted;
        return accepted;
    }

    /** How hard the engine can pull: 0 below moving pressure, then from {@link #MIN_PULL} up to 1 at full pressure. */
    public double pull() {
        if (pressure < MOVING_PRESSURE) {
            return 0;
        }
        return MIN_PULL + (1 - MIN_PULL) * (pressure - MOVING_PRESSURE) / (1 - MOVING_PRESSURE);
    }

    /** What the gauge shows: a melted plug reads as fixed once the tank is back above the relighting level. */
    public static boolean plugStillMelted(boolean melted, double water, int capacity) {
        return melted && water < RELIGHT_LEVEL * capacity;
    }

    public boolean fireLit() {
        return fire > 0;
    }

    public double pressure() {
        return pressure;
    }

    public double water() {
        return water;
    }

    public int capacity() {
        return capacity;
    }

    public boolean plugMelted() {
        return plugMelted;
    }
}
