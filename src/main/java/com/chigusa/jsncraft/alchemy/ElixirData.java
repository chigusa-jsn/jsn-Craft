package com.chigusa.jsncraft.alchemy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * 薬の中身（アイテムのカスタムデータ jsn_elixir）。
 * quality ★1〜5 / power 効き目の倍率 / time 時間の倍率 / form 0 飲む・1 投げる・2 香 / extras 追加効果
 */
public record ElixirData(int quality, double power, double time, int form, List<String> extras) {
    public static final int DRINK = 0, THROW = 1, DUST = 2;
    public static final ElixirData DEFAULT = new ElixirData(1, 1, 1, DRINK, List.of());
    private static final String KEY = "jsn_elixir";

    public static ElixirData of(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return DEFAULT;
        CompoundTag root = data.getUnsafe();
        if (!root.contains(KEY)) return DEFAULT;
        CompoundTag t = root.getCompound(KEY);
        List<String> ex = new ArrayList<>();
        for (Tag e : t.getList("extras", Tag.TAG_STRING)) ex.add(e.getAsString());
        return new ElixirData(Math.max(1, t.getInt("quality")), t.getDouble("power"), t.getDouble("time"), t.getInt("form"), ex);
    }

    public static int form(ItemStack stack) {
        return of(stack).form();
    }

    public void write(ItemStack stack) {
        CompoundTag t = new CompoundTag();
        t.putInt("quality", quality);
        t.putDouble("power", power);
        t.putDouble("time", time);
        t.putInt("form", form);
        ListTag ex = new ListTag();
        for (String e : extras) ex.add(StringTag.valueOf(e));
        t.put("extras", ex);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> root.put(KEY, t));
    }

    /** 品質の倍率（★1 = 1.0 … ★5 = 1.4） */
    public double qualityMult() {
        return 1 + 0.1 * (quality - 1);
    }
}
