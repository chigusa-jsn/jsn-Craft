package com.chigusa.jsncraft.client.gacha;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.gacha.ModGacha;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GachaClient {
    private static FigureItemRenderer itemRenderer;

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModGacha.FIGURE_BE.get(), FigureRenderer::new);
    }

    @SubscribeEvent
    public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (itemRenderer == null) itemRenderer = new FigureItemRenderer();
                return itemRenderer;
            }
        }, ModGacha.FIGURE_ITEM.get());
    }

    private GachaClient() {}
}
