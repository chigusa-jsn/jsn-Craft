package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.weapon.WhipItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 伸びる鞭：鞭を振った瞬間、手元から本物の鞭がしなりながら前へ伸びて、先がパシッと鳴り、くるっと戻る。
 * 手に持った鞭の絵は形を変えられないので、そのかわりに線（帯）で描く。
 */
@EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
public final class WhipLash {

    private static final int OUT = 3, HOLD = 1, BACK = 5, LIFE = OUT + HOLD + BACK;
    private static final int SEGMENTS = 24;

    private record Lash(WeakReference<AbstractClientPlayer> player, long start, float length, int[] colors) {}

    private static final List<Lash> LASHES = new ArrayList<>();

    /** 鞭を伸ばす（length ブロック） */
    public static void start(AbstractClientPlayer p, float length) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        LASHES.removeIf(l -> l.player().get() == p);
        LASHES.add(new Lash(new WeakReference<>(p), mc.level.getGameTime(), length, colors(p)));
    }

    /** 鞭の色（段ごと）：［濃い色, 明るい色］ */
    private static int[] colors(AbstractClientPlayer p) {
        if (p.getMainHandItem().getItem() instanceof WhipItem w) {
            return switch (w.spec().tier()) {
                case 2 -> new int[]{0x2a2420, 0x6a6a72};
                case 3 -> new int[]{0x8a7a5a, 0xe8dcc0};
                case 4 -> new int[]{0x2a2228, 0xc02018};
                case 5 -> new int[]{0x4a5a88, 0xd0dcff};
                case 6 -> new int[]{0x1c1c48, 0xffd040};
                default -> new int[]{0x4a2c14, 0x9a6234};
            };
        }
        return new int[]{0x4a2c14, 0x9a6234};
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || LASHES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            LASHES.clear();
            return;
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = mc.level.getGameTime() + pt;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.leash());
        LASHES.removeIf(l -> {
            AbstractClientPlayer p = l.player().get();
            double age = now - l.start();
            if (p == null || p.isRemoved() || age > LIFE) return true;
            draw(vc, m, p, l, (float) age, pt, cam);
            return false;
        });
        buffers.endBatch(RenderType.leash());
        pose.popPose();
    }

    /** 体の動きのとおりに描いた右手の位置（HandLayer が毎フレーム書く） */
    private record HandPos(Vec3 pos, long nanos) {}
    private static final java.util.Map<java.util.UUID, HandPos> HANDS = new java.util.HashMap<>();

    static boolean wants(AbstractClientPlayer p) {
        for (Lash l : LASHES) if (l.player().get() == p) return true;
        return false;
    }

    /**
     * プレイヤーの絵にのせる「層」：右腕の向き（振りの動きをふくむ）から、手の位置を求めて覚えておく。
     * 何も描かない。
     */
    public static final class HandLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<AbstractClientPlayer, net.minecraft.client.model.PlayerModel<AbstractClientPlayer>> {
        public HandLayer(net.minecraft.client.renderer.entity.RenderLayerParent<AbstractClientPlayer, net.minecraft.client.model.PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbAmount,
                           float pt, float age, float headYaw, float headPitch) {
            if (!wants(p)) return;
            pose.pushPose();
            boolean rightHanded = p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
            (rightHanded ? getParentModel().rightArm : getParentModel().leftArm).translateAndRotate(pose);
            // 腕の先（手に持った物の位置）。ItemInHandLayer と同じ場所
            org.joml.Vector3f v = pose.last().pose().transformPosition(rightHanded ? 0.0625F : -0.0625F, 0.625F, -0.125F, new org.joml.Vector3f());
            pose.popPose();
            Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            HANDS.put(p.getUUID(), new HandPos(cam.add(v.x, v.y, v.z), System.nanoTime()));
        }
    }

    /** 手の位置（まわりから見るときは、体の動きのとおりの右手。一人称は画面の右下） */
    private static Vec3 hand(AbstractClientPlayer p, float pt) {
        Minecraft mc = Minecraft.getInstance();
        HandPos h = HANDS.get(p.getUUID());
        boolean firstPerson = p == mc.player && mc.options.getCameraType().isFirstPerson();
        if (!firstPerson && h != null && System.nanoTime() - h.nanos() < 100_000_000L) return h.pos();
        Vec3 look = p.getViewVector(pt);
        if (p == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
            return p.getEyePosition(pt).add(look.scale(0.45)).add(side.scale(0.32)).add(0, -0.28, 0);
        }
        double yaw = Math.toRadians(Mth.lerp(pt, p.yBodyRotO, p.yBodyRot));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return p.getPosition(pt).add(right.scale(0.38)).add(0, 1.3, 0).add(look.scale(0.4));
    }

    private static void draw(VertexConsumer vc, Matrix4f m, AbstractClientPlayer p, Lash l, float age, float pt, Vec3 cam) {
        // 伸びぐあい：すばやく伸びて、少し止まって、しなりながら戻る
        float e = age < OUT ? Mth.sin((age / OUT) * Mth.HALF_PI) : age < OUT + HOLD ? 1 : 1 - (age - OUT - HOLD) / BACK;
        e = Mth.clamp(e, 0, 1);
        if (e <= 0.02F) return;
        boolean back = age >= OUT + HOLD;
        Vec3 start = hand(p, pt);
        Vec3 dir = p.getViewVector(pt);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 side = dir.cross(up);
        if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
        side = side.normalize();
        up = side.cross(dir).normalize();
        float len = l.length() * e;
        Vec3[] pts = new Vec3[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            float s = (float) i / SEGMENTS;
            float wave = Mth.sin((s * 1.6F - age * 0.45F) * Mth.TWO_PI) * 0.35F * (1 - e) * s;  // しなる波
            float droop = back ? -0.6F * s * s * (1 - e) : -0.12F * s * s;                      // 戻るときは垂れる
            pts[i] = start.add(dir.scale(len * s)).add(up.scale(wave + droop)).add(side.scale(wave * 0.4F));
        }
        for (int i = 0; i <= SEGMENTS; i++) {
            float s = (float) i / SEGMENTS;
            Vec3 a = pts[Math.max(0, i - 1)], b = pts[Math.min(SEGMENTS, i + 1)];
            Vec3 seg = b.subtract(a);
            Vec3 toCam = cam.subtract(pts[i]);
            Vec3 n = seg.cross(toCam);
            if (n.lengthSqr() < 1e-6) n = up;
            n = n.normalize().scale(0.028 * (1 - 0.75 * s) + 0.006);
            int c = (i / 2) % 2 == 0 ? l.colors()[0] : l.colors()[1];
            if (i == SEGMENTS) c = l.colors()[1];
            float r = (c >> 16 & 255) / 255F, g = (c >> 8 & 255) / 255F, bl = (c & 255) / 255F;
            int light = LightTexture.FULL_BRIGHT;
            Vec3 v1 = pts[i].add(n), v2 = pts[i].subtract(n);
            vc.addVertex(m, (float) v1.x, (float) v1.y, (float) v1.z).setColor(r, g, bl, 1.0F).setLight(light);
            vc.addVertex(m, (float) v2.x, (float) v2.y, (float) v2.z).setColor(r, g, bl, 1.0F).setLight(light);
        }
        // いちばん伸びたところで、先から火花
        if (!back && age >= OUT - 0.5F && age < OUT + 0.5F && p.level().random.nextInt(2) == 0) {
            Vec3 tip = pts[SEGMENTS];
            p.level().addParticle(ParticleTypes.CRIT, tip.x, tip.y, tip.z, 0, 0.05, 0);
            p.level().addParticle(ParticleTypes.SWEEP_ATTACK, tip.x, tip.y, tip.z, 0, 0, 0);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void onAddLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
            for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer r) r.addLayer(new HandLayer(r));
            }
        }
    }

    private WhipLash() {}
}
