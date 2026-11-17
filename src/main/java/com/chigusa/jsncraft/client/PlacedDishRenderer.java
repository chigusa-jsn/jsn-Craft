package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.cooking.PlacedDishBlock;
import com.chigusa.jsncraft.cooking.PlacedDishBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 置いた料理：料理のアイテムを、台の上に寝かせて描く。 */
public class PlacedDishRenderer implements BlockEntityRenderer<PlacedDishBlockEntity> {

    public PlacedDishRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(PlacedDishBlockEntity be, float pt, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ItemStack dish = be.dish();
        if (dish.isEmpty()) return;
        float yaw = be.getBlockState().getValue(PlacedDishBlock.FACING).toYRot();
        ps.pushPose();
        ps.translate(0.5, 0.04, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.mulPose(Axis.XP.rotationDegrees(90));
        ps.scale(0.85F, 0.85F, 0.85F);
        Minecraft.getInstance().getItemRenderer().renderStatic(dish, ItemDisplayContext.FIXED, light, overlay, ps, buffers, be.getLevel(), 0);
        ps.popPose();
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModCooking.PLACED_DISH_BE.get(), PlacedDishRenderer::new);
        }
    }
}
