package com.chigusa.jsncraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** 星銀のきらめき：丸く光る小さな星が、ふわっと大きくなって、またたいて消える。暗いところでも光る。 */
public class TwinkleParticle extends TextureSheetParticle {
    private final float baseSize;
    private final float phase;

    protected TwinkleParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x, y, z, 0, 0, 0);
        this.xd = dx;
        this.yd = dy + 0.004;
        this.zd = dz;
        this.friction = 0.9F;
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 16 + random.nextInt(14);
        this.baseSize = 0.05F + random.nextFloat() * 0.06F;
        this.quadSize = 0;
        this.phase = random.nextFloat() * 6.28F;
        // 金色と、うすい青の星
        if (random.nextInt(3) == 0) {
            this.rCol = 0.75F; this.gCol = 0.85F; this.bCol = 1.0F;
        } else {
            this.rCol = 1.0F; this.gCol = 0.92F; this.bCol = 0.6F;
        }
        this.roll = this.oRoll = random.nextFloat() * 0.6F - 0.3F;
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) age / lifetime;
        float grow = Mth.sin(t * (float) Math.PI);                       // ふわっと出て消える
        float blink = 0.75F + 0.25F * Mth.sin(age * 1.3F + phase);      // またたき
        this.quadSize = baseSize * grow * blink;
        this.alpha = Math.min(1.0F, grow * 1.4F);
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
            TwinkleParticle p = new TwinkleParticle(level, x, y, z, dx, dy, dz);
            p.pickSprite(sprites);
            return p;
        }
    }
}
