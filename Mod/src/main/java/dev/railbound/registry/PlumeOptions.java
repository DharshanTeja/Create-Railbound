package dev.railbound.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.railbound.steam.Plume;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** One chimney puff as a particle: its own size, spread, lifetime, opacity and grey (see {@link Plume}). */
public record PlumeOptions(float startSize, float endSize, int life, float alpha, float shade) implements ParticleOptions {
    public static final MapCodec<PlumeOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("start_size").forGetter(PlumeOptions::startSize),
            Codec.FLOAT.fieldOf("end_size").forGetter(PlumeOptions::endSize),
            Codec.INT.fieldOf("life").forGetter(PlumeOptions::life),
            Codec.FLOAT.fieldOf("alpha").forGetter(PlumeOptions::alpha),
            Codec.FLOAT.fieldOf("shade").forGetter(PlumeOptions::shade)
    ).apply(i, PlumeOptions::new));

    public static final StreamCodec<ByteBuf, PlumeOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PlumeOptions::startSize,
            ByteBufCodecs.FLOAT, PlumeOptions::endSize,
            ByteBufCodecs.VAR_INT, PlumeOptions::life,
            ByteBufCodecs.FLOAT, PlumeOptions::alpha,
            ByteBufCodecs.FLOAT, PlumeOptions::shade,
            PlumeOptions::new);

    public static PlumeOptions of(Plume.Puff puff) {
        return new PlumeOptions(puff.startSize(), puff.endSize(), puff.life(), puff.alpha(), puff.shade());
    }

    @Override
    public ParticleType<?> getType() {
        return RailboundParticles.LOCO_PLUME;
    }
}
