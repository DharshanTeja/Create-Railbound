package dev.railbound.registry;

import dev.railbound.Railbound;
import net.minecraft.core.particles.ParticleType;
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

    /** Chimney smoke, darker smoke just after the fireman has put coal on, and white steam (cylinders, whistle). */
    public static final SimpleParticleType LOCO_SMOKE = new SimpleParticleType(false);
    public static final SimpleParticleType LOCO_SMOKE_DARK = new SimpleParticleType(false);
    public static final SimpleParticleType LOCO_STEAM = new SimpleParticleType(false);

    static {
        PARTICLES.register("none", () -> NONE);
        PARTICLES.register("loco_smoke", () -> LOCO_SMOKE);
        PARTICLES.register("loco_smoke_dark", () -> LOCO_SMOKE_DARK);
        PARTICLES.register("loco_steam", () -> LOCO_STEAM);
    }

    private RailboundParticles() {}
}
