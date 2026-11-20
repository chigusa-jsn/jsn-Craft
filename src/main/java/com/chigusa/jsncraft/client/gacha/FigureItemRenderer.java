package com.chigusa.jsncraft.client.gacha;

import com.chigusa.jsncraft.gacha.FigureItem;
import com.chigusa.jsncraft.gacha.ModGacha;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;

/** フィギュアのアイテムの見た目：台＋モンスター */
public class FigureItemRenderer extends BlockEntityWithoutLevelRenderer {
    public FigureItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer().renderSingleBlock(ModGacha.FIGURE.get().defaultBlockState(), ps, buf, light, overlay, ModelData.EMPTY, null);
        FigureRenderer.draw(FigureItem.entityId(stack), ps, buf, light, 180, 0.8F, mc.getTimer().getGameTimeDeltaPartialTick(true));
    }
}
