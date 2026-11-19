package com.chigusa.jsncraft.casino;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * お金の出入り口（jsn-Craft の中で、お金にさわるのはここだけ）。
 * Lightman's Currency が入っていれば、そのコイン（銅・鉄・金・エメラルド・ダイヤ・ネザライト、10枚で1つ上）を使う。
 * 入っていなければ、エメラルド（1個＝チップ10枚）で代わりにする。
 * 値段は「チップの枚数」で数える。チップ1枚 ＝ 鉄コイン1枚（銅コイン10枚）。
 */
public final class Money {

    private static final String LC = "lightmanscurrency";
    /** ライトマンのコイン（銅コインいくつ分か） */
    private static final String[] COINS = {"coin_copper", "coin_iron", "coin_gold", "coin_emerald", "coin_diamond", "coin_netherite"};
    private static final long[] COIN_VALUE = {1, 10, 100, 1_000, 10_000, 100_000};
    /** チップ1枚 ＝ 銅コイン10枚 */
    public static final long COPPER_PER_CHIP = 10;
    /** ライトマンがないとき：エメラルド1個 ＝ チップ10枚 */
    public static final int CHIPS_PER_EMERALD = 10;

    public static boolean hasLightman() {
        return BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(LC, COINS[0]));
    }

    private static Item coin(int i) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(LC, COINS[i]));
    }

    /** 持っているお金（チップの枚数で。ライトマンなら銅コインの端数は切り捨て） */
    public static long walletInChips(ServerPlayer p) {
        if (hasLightman()) return copper(p.getInventory()) / COPPER_PER_CHIP;
        return (long) p.getInventory().countItem(Items.EMERALD) * CHIPS_PER_EMERALD;
    }

    private static long copper(Inventory inv) {
        long sum = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            for (int c = 0; c < COINS.length; c++) if (!s.isEmpty() && s.is(coin(c))) sum += COIN_VALUE[c] * s.getCount();
        }
        return sum;
    }

    /** チップ chips 枚分のお金を払う。足りなければ何もせず false */
    public static boolean pay(ServerPlayer p, long chips) {
        if (chips <= 0) return true;
        Inventory inv = p.getInventory();
        if (hasLightman()) {
            long cost = chips * COPPER_PER_CHIP, have = copper(inv);
            if (have < cost) return false;
            // いったんコインを全部とって、おつりを大きいコインから返す
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                for (int c = 0; c < COINS.length; c++) if (!s.isEmpty() && s.is(coin(c))) inv.setItem(i, ItemStack.EMPTY);
            }
            giveCopper(p, have - cost);
            return true;
        }
        if (chips % CHIPS_PER_EMERALD != 0) return false;
        int need = (int) (chips / CHIPS_PER_EMERALD);
        if (inv.countItem(Items.EMERALD) < need) return false;
        for (int i = 0; i < inv.getContainerSize() && need > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.EMERALD)) {
                int t = Math.min(need, s.getCount());
                s.shrink(t);
                need -= t;
            }
        }
        return true;
    }

    /** チップ chips 枚分のお金を渡す（ライトマンがないときは10枚単位） */
    public static boolean give(ServerPlayer p, long chips) {
        if (chips <= 0) return true;
        if (hasLightman()) {
            giveCopper(p, chips * COPPER_PER_CHIP);
            return true;
        }
        if (chips % CHIPS_PER_EMERALD != 0) return false;
        giveItems(p, Items.EMERALD, chips / CHIPS_PER_EMERALD);
        return true;
    }

    private static void giveCopper(ServerPlayer p, long copper) {
        for (int c = COINS.length - 1; c >= 0 && copper > 0; c--) {
            long n = copper / COIN_VALUE[c];
            if (n > 0) {
                giveItems(p, coin(c), n);
                copper -= n * COIN_VALUE[c];
            }
        }
    }

    static void giveItems(ServerPlayer p, Item item, long count) {
        while (count > 0) {
            int n = (int) Math.min(count, item.getDefaultMaxStackSize());
            ItemStack s = new ItemStack(item, n);
            if (!p.getInventory().add(s)) p.drop(s, false);
            count -= n;
        }
    }

    /** 両替1回で動かせるチップの単位（ライトマンがないときはエメラルド1個＝10枚） */
    public static int unit() {
        return hasLightman() ? 1 : CHIPS_PER_EMERALD;
    }

    private Money() {}
}
