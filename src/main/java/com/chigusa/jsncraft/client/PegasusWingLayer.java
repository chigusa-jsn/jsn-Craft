package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 天馬の背中の羽。空中では大きく羽ばたき、地面ではたたんでゆっくり揺れる。 */
public class PegasusWingLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEX = JsnCraft.id("textures/entity/pegasus_wing.png");

    public PegasusWingLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbAmount,
                       float pt, float age, float yaw, float pitch) {
        if (!PegasusClient.isPegasus(e) || e.isInvisible()) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0F);
        boolean air = !e.onGround();
        float flap = air ? Mth.sin(age * 0.6F) * 45F + 10F : Mth.sin(age * 0.08F) * 4F - 55F;
        for (int side = -1; side <= 1; side += 2) {
            pose.pushPose();
            // 背中の肩のあたり（馬のモデルの中の位置、1/16 ブロック単位）
            pose.translate(side * 4.5F / 16F, 2.5F / 16F, -6F / 16F);
            pose.mulPose(Axis.ZP.rotationDegrees(side * -flap));
            wing(pose, vc, light, overlay, side);
            pose.popPose();
        }
    }

    /** 羽1枚：根元から横へ 20、後ろへ 16（1/16 ブロック） */
    private static void wing(PoseStack pose, VertexConsumer vc, int light, int overlay, int side) {
        PoseStack.Pose p = pose.last();
        float w = 20F / 16F * side, d = 16F / 16F;
        float u0 = side > 0 ? 0 : 1, u1 = side > 0 ? 1 : 0;
        v(vc, p, 0, 0, 0, u0, 0, light, overlay);
        v(vc, p, w, 0, 0, u1, 0, light, overlay);
        v(vc, p, w, 0, d, u1, 1, light, overlay);
        v(vc, p, 0, 0, d, u0, 1, light, overlay);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int light, int overlay) {
        vc.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(p, 0, -1, 0);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            if (event.getRenderer(EntityType.HORSE) instanceof LivingEntityRenderer r) r.addLayer(new PegasusWingLayer(r));
        }
    }
}
