package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.weapon.ModWeapons;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** 射手の弓：引いている間の絵（pull / pulling）をバニラの弓と同じように切り替える。 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WeaponClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var bow : ModWeapons.JOB_BOWS) {
                ItemProperties.register(bow.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
                        entity == null || entity.getUseItem() != stack ? 0.0F
                                : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F);
                ItemProperties.register(bow.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
        });
    }

    private WeaponClient() {}
}
