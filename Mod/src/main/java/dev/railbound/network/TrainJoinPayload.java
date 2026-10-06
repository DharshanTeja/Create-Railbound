package dev.railbound.network;

import dev.railbound.Railbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * The server joined train {@code drop} onto train {@code keep}: clients do the same in their copies. The flags say
 * which train was turned round first and whether {@code keep}'s carriages come first; {@code gap} is the spacing Create
 * keeps between the bogeys either side of the new join.
 */
public record TrainJoinPayload(UUID keep, UUID drop, int flags, int gap, boolean doubleEnded) implements CustomPacketPayload {
    public static final Type<TrainJoinPayload> TYPE = new Type<>(Railbound.rl("train_join"));
    private static final int REVERSE_KEEP = 1, REVERSE_DROP = 2, KEEP_FIRST = 4;

    public static final StreamCodec<ByteBuf, TrainJoinPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TrainJoinPayload::keep,
            UUIDUtil.STREAM_CODEC, TrainJoinPayload::drop,
            ByteBufCodecs.VAR_INT, TrainJoinPayload::flags,
            ByteBufCodecs.VAR_INT, TrainJoinPayload::gap,
            ByteBufCodecs.BOOL, TrainJoinPayload::doubleEnded,
            TrainJoinPayload::new);

    public static int flags(boolean reverseKeep, boolean reverseDrop, boolean keepFirst) {
        return (reverseKeep ? REVERSE_KEEP : 0) | (reverseDrop ? REVERSE_DROP : 0) | (keepFirst ? KEEP_FIRST : 0);
    }

    public boolean reverseKeep() {
        return (flags & REVERSE_KEEP) != 0;
    }

    public boolean reverseDrop() {
        return (flags & REVERSE_DROP) != 0;
    }

    public boolean keepFirst() {
        return (flags & KEEP_FIRST) != 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
