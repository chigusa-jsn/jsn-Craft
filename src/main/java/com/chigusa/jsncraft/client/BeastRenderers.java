package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.beast.ModBeasts;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Parrot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 獣使いの相棒の見た目：オオカミとミツバチはそのまま、ゴーレムは半分の大きさ、鷹（オウム）は大きめ。 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BeastRenderers {

    static final class MiniGolem extends IronGolemRenderer {
        MiniGolem(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.35F;
        }

        @Override
        protected void scale(IronGolem golem, PoseStack pose, float partial) {
            pose.scale(0.5F, 0.5F, 0.5F);
        }
    }

    static final class Hawk extends ParrotRenderer {
        Hawk(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 0.4F;
        }

        @Override
        protected void scale(Parrot parrot, PoseStack pose, float partial) {
            pose.scale(1.5F, 1.5F, 1.5F);
        }
    }

    /** 百獣の王の相棒：jsn-boss と同じモデルを小さくして描く（光る部分も光る） */
    static final class Mini<T extends com.chigusa.jsncraft.beast.GeoPet> extends software.bernie.geckolib.renderer.GeoEntityRenderer<T> {
        Mini(EntityRendererProvider.Context ctx, String model, float scale, float shadow) {
            super(ctx, new software.bernie.geckolib.model.DefaultedEntityGeoModel<>(JsnCraft.id(model), false));
            addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));
            withScale(scale);
            this.shadowRadius = shadow;
        }

        @Override
        protected float getDeathMaxRotation(T entity) {
            return 0.0F;
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModBeasts.WOLF.get(), WolfRenderer::new);
        event.registerEntityRenderer(ModBeasts.GOLEM.get(), MiniGolem::new);
        event.registerEntityRenderer(ModBeasts.HAWK.get(), Hawk::new);
        event.registerEntityRenderer(ModBeasts.BEE.get(), BeeRenderer::new);
        event.registerEntityRenderer(ModBeasts.FOX.get(), c -> new Mini<com.chigusa.jsncraft.beast.PetFox>(c, "white_fox", 0.4F, 0.5F));
        event.registerEntityRenderer(ModBeasts.CAT.get(), c -> new Mini<com.chigusa.jsncraft.beast.PetCat>(c, "wind_cat", 0.4F, 0.5F));
        event.registerEntityRenderer(ModBeasts.LION.get(), c -> new Mini<com.chigusa.jsncraft.beast.PetLion>(c, "thunder_lion", 0.45F, 0.7F));
        event.registerEntityRenderer(ModBeasts.DRAGON.get(), c -> new Mini<com.chigusa.jsncraft.beast.PetDragon>(c, "blade_dragon", 0.55F, 0.9F));
    }

    private BeastRenderers() {}
}
