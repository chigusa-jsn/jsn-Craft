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

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModBeasts.WOLF.get(), WolfRenderer::new);
        event.registerEntityRenderer(ModBeasts.GOLEM.get(), MiniGolem::new);
        event.registerEntityRenderer(ModBeasts.HAWK.get(), Hawk::new);
        event.registerEntityRenderer(ModBeasts.BEE.get(), BeeRenderer::new);
    }

    private BeastRenderers() {}
}
