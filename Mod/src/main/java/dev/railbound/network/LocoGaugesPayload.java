package dev.railbound.network;

import dev.railbound.Railbound;
import dev.railbound.steam.SteamGauges;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A loco carriage's gauges, sent to the players who can see it. */
public record LocoGaugesPayload(int entityId, SteamGauges gauges) implements CustomPacketPayload {
    public static final Type<LocoGaugesPayload> TYPE = new Type<>(Railbound.rl("loco_gauges"));

    public static final StreamCodec<ByteBuf, LocoGaugesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LocoGaugesPayload::entityId,
            SteamGauges.STREAM_CODEC, LocoGaugesPayload::gauges,
            LocoGaugesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
