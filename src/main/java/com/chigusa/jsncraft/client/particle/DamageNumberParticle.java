package com.chigusa.jsncraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * ダメージの数字。敵の上にポンと出て、ふわっと浮かんで消える。
 * style：0 = ふつう（白）、1 = 会心（金・大きめ）、2 = スキル（蒼）
 */
public class DamageNumberParticle extends Particle {

    /** 数字0〜9の絵（RegisterParticleProvidersEvent で入る） */
    public static SpriteSet SPRITES;

    private final String text;
    private final float size;
    private final float r, g, b;

    public DamageNumberParticle(ClientLevel level, double x, double y, double z, float amount, int style) {
        super(level, x, y, z);
        this.text = String.valueOf(Math.max(1, Math.round(amount)));
        this.size = style == 1 ? 0.2F : 0.14F;
        this.lifetime = 22;
        this.hasPhysics = false;
        this.gravity = 0;
        this.xd = (random.nextFloat() - 0.5F) * 0.04;
        this.zd = (random.nextFloat() - 0.5F) * 0.04;
        this.yd = 0.09;
        switch (style) {
            case 1 -> { r = 1.0F; g = 0.85F; b = 0.25F; }
            case 2 -> { r = 0.5F; g = 0.85F; b = 1.0F; }
            default -> { r = 1.0F; g = 1.0F; b = 1.0F; }
        }
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        x += xd;
        y += yd;
        z += zd;
        yd *= 0.82;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (SPRITES == null) return;
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - cam.x());
        float py = (float) (Mth.lerp(partialTick, yo, y) - cam.y());
        float pz = (float) (Mth.lerp(partialTick, zo, z) - cam.z());
        Quaternionf rot = camera.rotation();
        float life = (age + partialTick) / lifetime;
        float alpha = life < 0.7F ? 1.0F : Math.max(0, 1 - (life - 0.7F) / 0.3F);
        // 出た瞬間だけ少し大きく（ポンと出る感じ）
        float pop = life < 0.15F ? 1.0F + (0.15F - life) * 3.0F : 1.0F;
        float s = size * pop;
        float step = 1.6F;
        float start = -(text.length() - 1) * step / 2F;
        for (int i = 0; i < text.length(); i++) {
            int digit = text.charAt(i) - '0';
            TextureAtlasSprite sp = SPRITES.get(digit, 9);
            float cx = start + i * step;
            quad(buffer, rot, px, py, pz, cx, s, sp, alpha);
        }
    }

    private void quad(VertexConsumer buffer, Quaternionf rot, float px, float py, float pz, float cx, float s,
                      TextureAtlasSprite sp, float alpha) {
        float[][] corners = {{1, -1}, {1, 1}, {-1, 1}, {-1, -1}};
        float[][] uv = {{sp.getU1(), sp.getV1()}, {sp.getU1(), sp.getV0()}, {sp.getU0(), sp.getV0()}, {sp.getU0(), sp.getV1()}};
        for (int i = 0; i < 4; i++) {
            Vector3f v = new Vector3f(corners[i][0] + cx, corners[i][1], 0).rotate(rot).mul(s);
            buffer.addVertex(px + v.x(), py + v.y(), pz + v.z()).setUv(uv[i][0], uv[i][1]).setColor(r, g, b, alpha).setLight(0xF000F0);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
