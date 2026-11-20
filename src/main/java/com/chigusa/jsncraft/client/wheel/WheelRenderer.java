package com.chigusa.jsncraft.client.wheel;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.wheel.BigWheelBlock;
import com.chigusa.jsncraft.wheel.BigWheelBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** 壁かけのビッグホイール：枠・回る円盤・上の針を、5×5マスの大きさで描く */
public class WheelRenderer implements BlockEntityRenderer<BigWheelBlockEntity> {
    private static final ResourceLocation FACE = ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "textures/block/wheel/face.png");
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "textures/block/wheel/frame.png");
    private static final ResourceLocation POINTER = ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "textures/block/wheel/pointer.png");

    public WheelRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(BigWheelBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Direction f = be.getBlockState().getValue(BigWheelBlock.FACING);
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
        // 壁から少し前に
        ps.translate(0, 0, -0.5 + 0.30);
        int full = LightTexture.FULL_BRIGHT;
        quad(ps, buf.getBuffer(RenderType.entityCutoutNoCull(FRAME)), 2.9F, 0, full);
        ps.translate(0, 0, 0.02);
        double ang = be.level == null ? 0 : be.angle(be.level.getGameTime(), partial);
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees((float) -ang));
        quad(ps, buf.getBuffer(RenderType.entityCutoutNoCull(FACE)), 2.5F, 0, full);
        ps.popPose();
        ps.translate(0, 2.5, 0.03);
        quad(ps, buf.getBuffer(RenderType.entityCutoutNoCull(POINTER)), 0.4F, 0, full);
        ps.popPose();
    }

    /** 真ん中から half の正方形（手前向き） */
    private static void quad(PoseStack ps, VertexConsumer vc, float half, float z, int light) {
        PoseStack.Pose pose = ps.last();
        v(vc, pose, -half, -half, z, 0, 1, light);
        v(vc, pose, half, -half, z, 1, 1, light);
        v(vc, pose, half, half, z, 1, 0, light);
        v(vc, pose, -half, half, z, 0, 0, light);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float vv, int light) {
        vc.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
    }

    public AABB getRenderBoundingBox(BigWheelBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(3.5);
    }

    @Override
    public boolean shouldRenderOffScreen(BigWheelBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
