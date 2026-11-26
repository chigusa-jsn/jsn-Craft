package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.tools.ModTools;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** 段階のある釣り竿：投げているときの見た目（cast）。 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RodClient {
    @SubscribeEvent
    public static void onSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var rod : ModTools.RODS) {
                ItemProperties.register(rod.get(), ResourceLocation.withDefaultNamespace("cast"), (stack, level, entity, seed) -> {
                    if (!(entity instanceof Player p) || p.fishing == null) return 0;
                    boolean main = p.getMainHandItem() == stack, off = p.getOffhandItem() == stack;
                    if (p.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem) off = false;
                    return main || off ? 1 : 0;
                });
            }
        });
    }

    private RodClient() {}
}
