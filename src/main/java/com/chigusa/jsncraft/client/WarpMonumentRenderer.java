package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.warp.Monuments;
import com.chigusa.jsncraft.warp.WarpMonumentBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** モニュメントのてっぺんで、回りながら浮かぶクリスタル */
public class WarpMonumentRenderer implements BlockEntityRenderer<WarpMonumentBlockEntity> {
    private ItemStack crystal;

    public WarpMonumentRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(WarpMonumentBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        if (crystal == null) crystal = new ItemStack(Monuments.CRYSTAL.get());
        float t = (be.getLevel() == null ? 0 : be.getLevel().getGameTime()) + pt;
        pose.pushPose();
        pose.translate(0.5, Monuments.HEIGHT + 0.55 + Math.sin(t / 12.0) * 0.12, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(t * 2.0F));
        pose.scale(1.4F, 1.4F, 1.4F);
        Minecraft.getInstance().getItemRenderer().renderStatic(crystal, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pose, buf, be.getLevel(), 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WarpMonumentBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(2, 0, 2).expandTowards(0, Monuments.HEIGHT + 2, 0);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(Monuments.BE.get(), WarpMonumentRenderer::new);
        }
    }
}
