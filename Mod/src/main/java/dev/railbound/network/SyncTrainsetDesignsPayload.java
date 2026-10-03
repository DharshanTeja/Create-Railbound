package dev.railbound.network;

import dev.railbound.Railbound;
import dev.railbound.trainset.design.TrainsetDesign;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncTrainsetDesignsPayload(Map<ResourceLocation, TrainsetDesign> designs) implements CustomPacketPayload {
    public static final Type<SyncTrainsetDesignsPayload> TYPE = new Type<>(Railbound.rl("sync_trainset_designs"));

    public static final StreamCodec<ByteBuf, SyncTrainsetDesignsPayload> STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.fromCodec(TrainsetDesign.CODEC))
                    .map(SyncTrainsetDesignsPayload::new, payload -> new HashMap<>(payload.designs()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
