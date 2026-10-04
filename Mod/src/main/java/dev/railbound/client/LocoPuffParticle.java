package dev.railbound.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/**
 * A puff of loco smoke or steam: it shoots out with its exhaust speed, slows, rises and spreads, and thins away.
 * Smoke lingers grey; steam is white and quicker to vanish.
 */
public class LocoPuffParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startSize;
    private final float endSize;

    LocoPuffParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites,
                     float shade, int life, float startSize, float endSize) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.lifetime = life + random.nextInt(life / 3 + 1);
        this.startSize = startSize;
        this.endSize = endSize;
        this.quadSize = startSize;
        this.gravity = -0.01f;
        this.friction = 0.92f;
        float jitter = shade > 0.9f ? 0 : random.nextFloat() * 0.06f;
        setColor(Math.min(1, shade + jitter), Math.min(1, shade + jitter), Math.min(1, shade + jitter));
        setAlpha(1f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float t = (float) age / lifetime;
        quadSize = startSize + (endSize - startSize) * (float) Math.sqrt(t);
        // solid while it billows, thinning away only in the last part of its life
        setAlpha(t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f);
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites, float shade, int life, float startSize, float endSize)
            implements ParticleProvider<SimpleParticleType> {
        @Override
        public @Nullable Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                                 double dx, double dy, double dz) {
            return new LocoPuffParticle(level, x, y, z, dx, dy, dz, sprites, shade, life, startSize, endSize);
        }
    }
}
