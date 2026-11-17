package com.chigusa.jsncraft.jewelry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/** 装飾品の中身（カスタムデータ jsn_jewel）：品質★・穴の数・はまっている宝石 */
public record JewelData(int quality, int sockets, List<String> gems) {
    private static final String KEY = "jsn_jewel";

    public static JewelData of(ItemStack stack, int defaultSockets) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.getUnsafe().contains(KEY)) return new JewelData(1, defaultSockets, List.of());
        CompoundTag t = data.getUnsafe().getCompound(KEY);
        List<String> gems = new ArrayList<>();
        for (Tag g : t.getList("gems", Tag.TAG_STRING)) if (Jewels.gem(g.getAsString()) != null) gems.add(g.getAsString());
        return new JewelData(Math.max(1, t.getInt("quality")), Math.max(1, t.getInt("sockets")), gems);
    }

    public void write(ItemStack stack) {
        CompoundTag t = new CompoundTag();
        t.putInt("quality", quality);
        t.putInt("sockets", sockets);
        ListTag l = new ListTag();
        for (String g : gems) l.add(StringTag.valueOf(g));
        t.put("gems", l);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> root.put(KEY, t));
    }

    /** 品質の倍率（★1 = 1.0 … ★5 = 1.2） */
    public double qualityMult() {
        return 1 + 0.05 * (quality - 1);
    }

    public JewelData withGems(List<String> g) {
        return new JewelData(quality, sockets, List.copyOf(g));
    }
}
