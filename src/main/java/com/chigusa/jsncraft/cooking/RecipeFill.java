package com.chigusa.jsncraft.cooking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** レシピの一覧から選んだとき、持ち物から材料を枠に入れる（入っている材料はいったん持ち物に戻す）。 */
public final class RecipeFill {

    public static boolean fill(ServerPlayer player, Container box, int slots, CookingRecipe recipe) {
        if (recipe == null) return false;
        var inv = player.getInventory();
        // 今の材料を持ち物に戻す
        for (int i = 0; i < slots; i++) {
            ItemStack s = box.getItem(i);
            if (s.isEmpty()) continue;
            if (!inv.add(s)) player.drop(s, false);
            box.setItem(i, ItemStack.EMPTY);
        }
        List<String> need = new ArrayList<>(recipe.ingredients());
        int slot = 0;
        for (String spec : need) {
            if (slot >= slots) break;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty() || !CookingRecipe.matchesOne(spec, s)) continue;
                box.setItem(slot++, s.split(1));
                break;
            }
        }
        box.setChanged();
        inv.setChanged();
        return true;
    }

    /** 隠し味の枠に、持ち物から1つ入れる（入っていた物は持ち物に戻す） */
    public static boolean fillSeasoning(ServerPlayer player, Container box, int slot, Seasoning seasoning) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !CookingRecipe.matchesOne(seasoning.item(), s)) continue;
            ItemStack old = box.getItem(slot);
            if (!old.isEmpty() && CookingRecipe.matchesOne(seasoning.item(), old)) return true;   // もう入っている
            if (!old.isEmpty() && !inv.add(old)) player.drop(old, false);
            box.setItem(slot, s.split(1));
            box.setChanged();
            inv.setChanged();
            return true;
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("持ち物に「" + seasoning.name() + "」の材料がない")
                .withStyle(net.minecraft.ChatFormatting.GRAY), true);
        return false;
    }

    private RecipeFill() {}
}
