package com.chigusa.jsncraft.client.particle;

import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.CircleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 回る魔法陣（地面や空に平らに描く）。
 * ふつう：ふわっと出て回り、消える。
 * ため：小さく出て時間いっぱいで育ち、回るのがだんだん速くなる。
 * 発動の光：パッと広がりながら消える。
 */
public class SpellCircleParticle extends TextureSheetParticle {

    private final float maxSize;
    private final int style;
    private final float dir;
    private float angle, oAngle;

    protected SpellCircleParticle(ClientLevel level, double x, double y, double z, CircleOptions o, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = this.yd = this.zd = 0;
        this.hasPhysics = false;
        this.gravity = 0;
        this.style = o.style();
        this.maxSize = o.size() > 0 ? o.size() : 1.5F;
        this.lifetime = Math.max(4, o.ticks());
        this.dir = has(CircleOptions.REVERSE) ? -1 : 1;
        this.angle = this.oAngle = level.random.nextFloat() * Mth.TWO_PI;
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.alpha = 0;
        this.quadSize = has(CircleOptions.CHARGE) ? maxSize * 0.25F : maxSize;
        setSize(maxSize * 3.0F, 0.2F);   // 大きい魔法陣が画面の端で消えないように
        setSprite(sprites.get(Math.max(0, Math.min(Circle.values().length - 1, o.pattern())), Circle.values().length - 1));
    }

    private boolean has(int flag) {
        return (style & flag) != 0;
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float t = (float) age / lifetime;
        oAngle = angle;
        float spin = has(CircleOptions.SKY) ? 0.025F : 0.04F;
        if (has(CircleOptions.FLASH)) {
            float e = 1 - (1 - t) * (1 - t);
            quadSize = maxSize * (1.0F + 0.45F * e);
            alpha = (float) Math.pow(1 - t, 1.5);
            spin *= 4.0F * (1 - t) + 0.5F;
        } else if (has(CircleOptions.CHARGE)) {
            float e = 1 - (1 - t) * (1 - t) * (1 - t);
            quadSize = maxSize * (0.25F + 0.75F * e);
            alpha = 0.95F * Math.min(1, age / 4F);
            spin *= 1 + 3.5F * t;
        } else {
            float in = Math.min(1, age / 5F), out = Math.min(1, (lifetime - age) / 5F);
            alpha = 0.9F * Math.min(in, out);
            quadSize = maxSize * (0.85F + 0.15F * in);
        }
        angle += spin * dir;
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
        int light = 0xF000F0;
        float[][] corners = {{-1, -1, u0, v0}, {-1, 1, u0, v1}, {1, 1, u1, v1}, {1, -1, u1, v0}};
        for (int pass = 0; pass < 2; pass++) {   // 上からも下からも見える
            for (int i = 0; i < 4; i++) {
                float[] k = corners[pass == 0 ? i : 3 - i];
                float px = k[0] * c - k[1] * sn;
                float pz = k[0] * sn + k[1] * c;
                buffer.addVertex(x + px, y, z + pz).setUv(k[2], k[3]).setColor(rCol, gCol, bCol, alpha).setLight(light);
            }
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        return quadSize;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<CircleOptions> {
        @Override
        public Particle createParticle(CircleOptions o, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new SpellCircleParticle(level, x, y, z, o, sprites);
        }
    }
}
