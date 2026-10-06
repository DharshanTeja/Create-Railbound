package dev.railbound.network;

import dev.railbound.Railbound;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * The server split a train behind carriage {@code gap}: clients make the same cut in their copy, so the carriages they
 * draw stay with their trains. The cut-off half becomes train {@code newTrain}.
 */
public record TrainSplitPayload(UUID train, int gap, boolean frontKeeps, UUID newTrain, boolean keptDoubleEnded,
                                boolean cutDoubleEnded) implements CustomPacketPayload {
    public static final Type<TrainSplitPayload> TYPE = new Type<>(Railbound.rl("train_split"));

    public static final StreamCodec<ByteBuf, TrainSplitPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TrainSplitPayload::train,
            ByteBufCodecs.VAR_INT, TrainSplitPayload::gap,
            ByteBufCodecs.BOOL, TrainSplitPayload::frontKeeps,
            UUIDUtil.STREAM_CODEC, TrainSplitPayload::newTrain,
            ByteBufCodecs.BOOL, TrainSplitPayload::keptDoubleEnded,
            ByteBufCodecs.BOOL, TrainSplitPayload::cutDoubleEnded,
            TrainSplitPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
