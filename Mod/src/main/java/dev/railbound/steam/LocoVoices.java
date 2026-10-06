package dev.railbound.steam;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Client: where each train's locos are, so every loco sounds from itself. Create plays a train's chuffing and whistle
 * from its first carriage only; on a train with locos of ours, each loco chuffs from its own chimney instead, and the
 * whistle sounds from whichever loco is nearest the listener. A loco tells this its whistle's place every tick it moves
 * with its train; one not heard for a few ticks (uncoupled, unloaded, gone) is forgotten.
 */
public final class LocoVoices {
    /** Ticks a loco's last place is trusted. */
    static final int FORGET_AFTER = 5;

    private record Voice(Vec3 whistle, long tick) {}

    private static final Map<UUID, Map<Integer, Voice>> TRAINS = new HashMap<>();

    private LocoVoices() {}

    /** Loco {@code loco} (its entity id) on {@code train} has its whistle here this tick. */
    public static void heard(UUID train, int loco, Vec3 whistle, long gameTime) {
        TRAINS.computeIfAbsent(train, id -> new HashMap<>()).put(loco, new Voice(whistle, gameTime));
    }

    public static boolean hasLoco(UUID train, long gameTime) {
        return !current(train, gameTime).isEmpty();
    }

    /** The whistle of the train's loco nearest the listener. */
    public static Optional<Vec3> nearestWhistle(UUID train, Vec3 listener, long gameTime) {
        return current(train, gameTime).values().stream().map(Voice::whistle)
                .min((a, b) -> Double.compare(a.distanceToSqr(listener), b.distanceToSqr(listener)));
    }

    /** The train's locos heard lately, forgetting the rest (and the train, once none are left). */
    private static Map<Integer, Voice> current(UUID train, long gameTime) {
        Map<Integer, Voice> voices = TRAINS.get(train);
        if (voices == null) {
            return Map.of();
        }
        voices.values().removeIf(voice -> gameTime - voice.tick() > FORGET_AFTER);
        if (voices.isEmpty()) {
            TRAINS.remove(train);
        }
        return voices;
    }

    /** Leaving a world: no train is heard any more. */
    public static void clear() {
        TRAINS.clear();
    }
}
