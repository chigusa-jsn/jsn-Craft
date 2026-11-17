package com.chigusa.jsncraft.event;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

/** ログインしたとき、jsn-Craft のクラフトのレシピを全部レシピ本に出す（材料を拾わなくても見えるように） */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class RecipeUnlock {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        List<RecipeHolder<?>> ours = p.getServer().getRecipeManager().getRecipes().stream()
                .filter(r -> r.id().getNamespace().equals(JsnCraft.MODID)).toList();
        p.awardRecipes(ours);
    }

    private RecipeUnlock() {}
}
