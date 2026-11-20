package com.chigusa.jsncraft.client.wheel;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.wheel.ModWheel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WheelClient {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModWheel.WHEEL_BE.get(), WheelRenderer::new);
    }

    private WheelClient() {}
}
