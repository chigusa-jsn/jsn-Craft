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

    private RecipeFill() {}
}
