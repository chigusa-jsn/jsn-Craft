package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.ModEntities;
import com.chigusa.jsncraft.npc.ReceptionistEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 受付嬢の見た目。プレイヤーと同じ体（細い腕）に、受付嬢のスキンを貼る。 */
public class ReceptionistRenderer extends HumanoidMobRenderer<ReceptionistEntity, PlayerModel<ReceptionistEntity>> {

    private static final ResourceLocation TEXTURE = JsnCraft.id("textures/entity/receptionist.png");

    public ReceptionistRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ReceptionistEntity entity) {
        return TEXTURE;
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.RECEPTIONIST.get(), ReceptionistRenderer::new);
        }
    }
}
