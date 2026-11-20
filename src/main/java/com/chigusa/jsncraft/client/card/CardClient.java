package com.chigusa.jsncraft.client.card;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.card.CardItem;
import com.chigusa.jsncraft.card.CardList;
import com.chigusa.jsncraft.card.ModCards;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CardClient {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ModCards.openAlbum = h -> Minecraft.getInstance().setScreen(new AlbumScreen(h));
        ModCards.openBinder = h -> Minecraft.getInstance().setScreen(new BinderScreen(h));
        event.enqueueWork(() -> ItemProperties.register(ModCards.CARD.get(), ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "card_rarity"), (stack, level, e, seed) -> {
            CardList.Card c = CardItem.def(stack);
            return c == null ? 0 : c.rarity() / 4F;
        }));
    }

    private CardClient() {}
}
