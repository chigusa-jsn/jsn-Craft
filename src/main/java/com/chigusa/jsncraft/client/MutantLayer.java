package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.ranch.Mutant;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 突然変異の動物の体に色をかける（金のニワトリは金色に、天馬は白く光る…）。
 * 同じ形・同じ絵をもう一度、色をつけて半透明で重ねて描く。色はゆっくり明るくなったり暗くなったりする。
 */
public class MutantLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public MutantLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Mutant m = RanchHud.mutant(entity);
        if (m == Mutant.NONE || m.tint == 0 || entity.isInvisible()) return;
        float pulse = 0.45F + 0.15F * Mth.sin(ageInTicks * 0.12F);
        int alpha = (int) (pulse * 255) << 24;
        int color = alpha | (m.tint & 0xFFFFFF);
        var vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(getTextureLocation(entity)));
        getParentModel().renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (EntityType<?> type : new EntityType<?>[]{EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.HORSE, EntityType.BEE}) {
                if (event.getRenderer(type) instanceof LivingEntityRenderer r) r.addLayer(new MutantLayer(r));
            }
        }
    }
}
