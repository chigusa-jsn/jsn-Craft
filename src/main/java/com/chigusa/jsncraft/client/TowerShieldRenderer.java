package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.weapon.TowerShieldItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 大盾の見た目：バニラの盾と同じ形（板と持ち手）に、段ごとの絵を貼り、少し大きく描く。 */
public class TowerShieldRenderer extends BlockEntityWithoutLevelRenderer {

    private ShieldModel model;

    public TowerShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** 一人称で、盾騎士（上位職もふくむ）のときだけ */
    private static boolean seeThrough(ItemDisplayContext ctx) {
        if (ctx != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && ctx != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) return false;
        String job = ClientSkills.state().combatJob();
        return job.equals("shield_knight") || job.equals("guardian_knight");
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof TowerShieldItem shield)) return;
        if (model == null) model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        ResourceLocation tex = JsnCraft.id("textures/entity/shield/" + shield.spec().id() + ".png");
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.scale(1.15F, 1.2F, 1.0F);   // 大盾なので、ふつうの盾より大きく
        if (seeThrough(ctx)) {
            // 盾騎士のパッシブ「見通しの大盾」：自分の目から見た盾は半透明（構えているときはもっと薄く）
            var p = Minecraft.getInstance().player;
            int alpha = p != null && p.isUsingItem() && p.getUseItem() == stack ? 0x55 : 0x99;
            int color = (alpha << 24) | 0xFFFFFF;
            VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityTranslucent(tex), true, stack.hasFoil());
            model.handle().render(pose, vc, light, overlay, color);
            model.plate().render(pose, vc, light, overlay, color);
        } else {
            VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffers, model.renderType(tex), true, stack.hasFoil());
            model.handle().render(pose, vc, light, overlay);
            model.plate().render(pose, vc, light, overlay);
        }
        pose.popPose();
    }
}
