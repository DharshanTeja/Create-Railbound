package dev.railbound.steam;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client side: the latest gauges the server sent for each loco carriage, by its entity id. Create does not sync a
 * train carriage's mounted storage to clients reliably, so the cab HUD and the exhaust read these instead.
 */
public final class LocoGaugeCache {
    private static final Map<Integer, SteamGauges> GAUGES = new ConcurrentHashMap<>();

    private LocoGaugeCache() {}

    public static void put(int entityId, SteamGauges gauges) {
        GAUGES.put(entityId, gauges);
    }

    public static Optional<SteamGauges> get(int entityId) {
        return Optional.ofNullable(GAUGES.get(entityId));
    }

    public static void clear() {
        GAUGES.clear();
    }
}
