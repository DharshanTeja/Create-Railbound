package dev.railbound.registry;

import dev.railbound.Railbound;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RailboundParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Railbound.MOD_ID);

    /**
     * A particle that never appears. Create spawns a bogey style's smoke puffs from every carriage; our coaches have
     * no steam, so their bogey style uses this instead. Created eagerly: bogey styles are built before registration.
     */
    public static final SimpleParticleType NONE = new SimpleParticleType(false);

    /** White steam (cylinders, safety valve, whistle). */
    public static final SimpleParticleType LOCO_STEAM = new SimpleParticleType(false);
    /** The chimney's smoke plume: each puff carries its own size, lifetime and opacity. */
    public static final ParticleType<PlumeOptions> LOCO_PLUME = new ParticleType<>(false) {
        @Override
        public MapCodec<PlumeOptions> codec() {
            return PlumeOptions.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, PlumeOptions> streamCodec() {
            return PlumeOptions.STREAM_CODEC;
        }
    };

    static {
        PARTICLES.register("none", () -> NONE);
        PARTICLES.register("loco_steam", () -> LOCO_STEAM);
        PARTICLES.register("loco_plume", () -> LOCO_PLUME);
    }

    private RailboundParticles() {}
}
