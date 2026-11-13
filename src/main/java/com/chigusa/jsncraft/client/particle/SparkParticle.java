package com.chigusa.jsncraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** 当たったときの火花。白から金色に変わりながら飛び散る。 */
public class SparkParticle extends TextureSheetParticle {

    protected SparkParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.xd = dx * 1.6;
        this.yd = dy * 1.6 + 0.05;
        this.zd = dz * 1.6;
        this.friction = 0.82F;
        this.gravity = 0.4F;
        this.hasPhysics = false;
        this.lifetime = 6 + random.nextInt(5);
        this.quadSize = 0.12F + random.nextFloat() * 0.08F;
        this.rCol = this.gCol = this.bCol = 1.0F;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) age / lifetime;
        this.gCol = 1.0F - 0.25F * t;
        this.bCol = 1.0F - 0.75F * t;
        this.alpha = 1.0F - t;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new SparkParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
