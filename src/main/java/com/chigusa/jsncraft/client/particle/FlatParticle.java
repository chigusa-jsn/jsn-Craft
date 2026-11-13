package com.chigusa.jsncraft.client.particle;

import com.chigusa.jsncraft.vfx.Vfx;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 地面に平らに描くエフェクト。
 * 魔法陣：ふわっと出て、ゆっくり回り、最後に消える。
 * 光の輪：小さい輪から一気に広がって消える。
 */
public class FlatParticle extends TextureSheetParticle {

    private final boolean ring;
    private final float maxSize;
    private float angle, oAngle;

    protected FlatParticle(ClientLevel level, double x, double y, double z, double size, double ticks, double color,
                           SpriteSet sprites, boolean ring) {
        super(level, x, y, z);
        this.ring = ring;
        this.xd = this.yd = this.zd = 0;
        this.hasPhysics = false;
        this.gravity = 0;
        this.maxSize = size > 0 ? (float) size : 1.5F;
        this.lifetime = Math.max(4, (int) Math.round(ticks));
        float[] c = Vfx.color((int) Math.round(color));
        this.rCol = c[0];
        this.gCol = c[1];
        this.bCol = c[2];
        this.quadSize = ring ? maxSize * 0.2F : maxSize;
        this.alpha = ring ? 1.0F : 0.0F;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float t = (float) age / lifetime;
        oAngle = angle;
        if (ring) {
            // 一気に広がって、だんだん薄く
            float e = 1 - (1 - t) * (1 - t);
            quadSize = maxSize * (0.2F + 0.8F * e);
            alpha = 1.0F - t;
        } else {
            angle += 0.06F;
            // 最初の5tickで現れて、最後の5tickで消える
            float in = Math.min(1, age / 5F), out = Math.min(1, (lifetime - age) / 5F);
            alpha = 0.9F * Math.min(in, out);
            quadSize = maxSize * (0.85F + 0.15F * in);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, xo, this.x) - cam.x());
        float y = (float) (Mth.lerp(partialTick, yo, this.y) - cam.y());
        float z = (float) (Mth.lerp(partialTick, zo, this.z) - cam.z());
        float s = getQuadSize(partialTick);
        float a = Mth.lerp(partialTick, oAngle, angle);
        float c = Mth.cos(a) * s, sn = Mth.sin(a) * s;
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        int light = getLightColor(partialTick);
        // 上から見える面と、下から見える面の両方を描く
        float[][] corners = {{-1, -1, u0, v0}, {-1, 1, u0, v1}, {1, 1, u1, v1}, {1, -1, u1, v0}};
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 4; i++) {
                float[] k = corners[pass == 0 ? i : 3 - i];
                float px = k[0] * c - k[1] * sn;
                float pz = k[0] * sn + k[1] * c;
                buffer.addVertex(x + px, y, z + pz).setUv(k[2], k[3]).setColor(rCol, gCol, bCol, alpha).setLight(light);
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public record CircleProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new FlatParticle(level, x, y, z, dx, dy, dz, sprites, false);
        }
    }

    public record RingProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new FlatParticle(level, x, y, z, dx, dy, dz, sprites, true);
        }
    }
}
