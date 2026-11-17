package com.chigusa.jsncraft.enchanting;

import com.chigusa.jsncraft.smithing.SmithData;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 魔紋をアイテムのカスタムデータ（jsn_runes）に読み書きする。 */
public final class RuneData {
    public static final String KEY = "jsn_runes";

    /** 刻まれている魔紋（id → レベル、刻んだ順） */
    public static Map<String, Integer> runes(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return Map.of();
        CompoundTag tag = data.getUnsafe();
        if (!tag.contains(KEY)) return Map.of();
        CompoundTag r = tag.getCompound(KEY);
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String k : r.getAllKeys()) if (Runes.get(k) != null && r.getInt(k) > 0) out.put(k, r.getInt(k));
        return out;
    }

    public static int level(ItemStack stack, String id) {
        if (stack.isEmpty()) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0;
        CompoundTag tag = data.getUnsafe();
        return tag.contains(KEY) ? tag.getCompound(KEY).getInt(id) : 0;
    }

    public static void set(ItemStack stack, String id, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
            CompoundTag r = t.getCompound(KEY);
            r.putInt(id, level);
            t.put(KEY, r);
        });
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    /** 刻める種類：W 近接武器 / B 弓・クロスボウ / A 防具 / T 道具（斧は WT） */
    public static String kinds(ItemStack stack) {
        String k = SmithData.kinds(stack);
        if (k.isEmpty()) return k;
        if (SmithData.ranged(stack)) k = k.replace("W", "B");
        return k;
    }

    public static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem;
    }

    /** バニラのエンチャント（本なら中身）を、名前順に並べたもの（サーバーと画面で順番をそろえるため） */
    public static List<Map.Entry<Holder<Enchantment>, Integer>> enchantments(ItemStack stack) {
        List<Map.Entry<Holder<Enchantment>, Integer>> out = new ArrayList<>();
        for (var e : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            out.add(Map.entry(e.getKey(), e.getIntValue()));
        }
        out.sort(Comparator.comparing(e -> e.getKey().unwrapKey().map(k -> k.location().toString()).orElse("")));
        return out;
    }

    private RuneData() {}
}
