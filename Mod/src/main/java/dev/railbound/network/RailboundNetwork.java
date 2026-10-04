package dev.railbound.network;

import dev.railbound.steam.LocoGaugeCache;
import dev.railbound.Railbound;
import dev.railbound.trainset.design.DesignValidator;
import dev.railbound.trainset.design.TrainsetDesign;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class RailboundNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private RailboundNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).playToClient(
                SyncTrainsetDesignsPayload.TYPE,
                SyncTrainsetDesignsPayload.STREAM_CODEC,
                (payload, context) -> TrainsetDesigns.replace(acceptValid(payload.designs())))
                .playToClient(LocoGaugesPayload.TYPE, LocoGaugesPayload.STREAM_CODEC,
                        (payload, context) -> LocoGaugeCache.put(payload.entityId(), payload.gauges()));
    }

    /** Fires when a player joins and after /reload; sends the current designs to the affected players. */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncTrainsetDesignsPayload payload = new SyncTrainsetDesignsPayload(TrainsetDesigns.rawDesigns());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    /** Clients re-check designs so a mismatched server can never crash them. */
    static Map<ResourceLocation, TrainsetDesign> acceptValid(Map<ResourceLocation, TrainsetDesign> received) {
        Map<ResourceLocation, TrainsetDesign> accepted = new TreeMap<>();
        received.forEach((id, design) -> {
            List<String> problems = DesignValidator.validate(design);
            if (problems.isEmpty()) {
                accepted.put(id, design);
            } else {
                Railbound.LOGGER.warn("Ignoring synced trainset design {}: {}", id, problems);
            }
        });
        return accepted;
    }
}
