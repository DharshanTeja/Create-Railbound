package dev.railbound.network;

import dev.railbound.Railbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** A player wrenched the coupler behind carriage {@code gap} of a train: split it there. */
public record UncouplePayload(UUID train, int gap) implements CustomPacketPayload {
    public static final Type<UncouplePayload> TYPE = new Type<>(Railbound.rl("uncouple"));

    public static final StreamCodec<ByteBuf, UncouplePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, UncouplePayload::train,
            ByteBufCodecs.VAR_INT, UncouplePayload::gap,
            UncouplePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
