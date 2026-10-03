package dev.railbound.network;

import dev.railbound.trainset.load.TrainsetDesigns;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class RailboundNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private RailboundNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION).playToClient(
                SyncTrainsetDesignsPayload.TYPE,
                SyncTrainsetDesignsPayload.STREAM_CODEC,
                (payload, context) -> TrainsetDesigns.replace(payload.designs()));
    }

    /** Fires when a player joins and after /reload; sends the current designs to the affected players. */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SyncTrainsetDesignsPayload payload = new SyncTrainsetDesignsPayload(TrainsetDesigns.all());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }
}
