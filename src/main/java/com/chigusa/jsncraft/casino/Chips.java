package com.chigusa.jsncraft.casino;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** カジノチップ（1・10・100・1000）を数える・払う・渡す。 */
public final class Chips {

    public static long count(ServerPlayer p) {
        Inventory inv = p.getInventory();
        long sum = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof ChipItem c) sum += (long) c.value * s.getCount();
        }
        return sum;
    }

    /** n 枚ぶん払う（大きいチップはくずして、おつりを返す）。足りなければ false */
    public static boolean take(ServerPlayer p, long n) {
        if (n <= 0) return true;
        long have = count(p);
        if (have < n) return false;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof ChipItem) inv.setItem(i, ItemStack.EMPTY);
        }
        give(p, have - n);
        return true;
    }

    public static void give(ServerPlayer p, long n) {
        for (int i = ModCasino.CHIPS.size() - 1; i >= 0 && n > 0; i--) {
            ChipItem c = ModCasino.CHIPS.get(i).get();
            long k = n / c.value;
            if (k > 0) {
                Money.giveItems(p, c, k);
                n -= k * c.value;
            }
        }
    }

    private Chips() {}
}
