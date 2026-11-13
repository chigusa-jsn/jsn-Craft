package com.chigusa.jsncraft.client.particle;

import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** 三日月の斬撃。こちらを向いて一瞬光り、少し広がりながら消える。 */
public class SlashParticle extends TextureSheetParticle {

    private final float baseSize;

    protected SlashParticle(ClientLevel level, double x, double y, double z, double size, double color, double roll, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = this.yd = this.zd = 0;
        this.hasPhysics = false;
        this.gravity = 0;
        this.lifetime = 7;
        this.baseSize = size > 0 ? (float) size : 1.2F;
        this.quadSize = baseSize;
        float[] c = Vfx.color((int) Math.round(color));
        this.rCol = c[0];
        this.gCol = c[1];
        this.bCol = c[2];
        this.roll = this.oRoll = (float) Math.toRadians(roll);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) age / lifetime;
        this.alpha = 1.0F - t * t;
        this.quadSize = baseSize * (1.0F + t * 0.35F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0; // 暗い場所でも光って見える
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new SlashParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }
}
