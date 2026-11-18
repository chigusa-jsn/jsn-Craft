package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.weapon.ModWeapons;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** 職業武器の見た目：弓の引き（pull / pulling）、大盾の構え（blocking）と立体の描き方。 */
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
            for (var shield : ModWeapons.TOWER_SHIELDS) {
                ItemProperties.register(shield.get(), ResourceLocation.withDefaultNamespace("blocking"), (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
        });
    }

    private static TowerShieldRenderer shieldRenderer;

    @SubscribeEvent
    public static void onClientExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (shieldRenderer == null) shieldRenderer = new TowerShieldRenderer();
                return shieldRenderer;
            }
        };
        event.registerItem(ext, ModWeapons.TOWER_SHIELDS.stream().map(h -> (net.minecraft.world.item.Item) h.get()).toArray(net.minecraft.world.item.Item[]::new));
    }

    private WeaponClient() {}
}
