package com.chigusa.jsncraft.smithing;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/** 強化値（+）と特性を、アイテムのカスタムデータに読み書きする。 */
public final class SmithData {

    public record TraitRoll(String id, double value, double ratio) {}

    /** 種類：W 武器 / A 防具 / T 道具（斧は WT）。強化できない物は空 */
    public static String kinds(ItemStack stack) {
        var item = stack.getItem();
        StringBuilder sb = new StringBuilder();
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem
                || item instanceof ProjectileWeaponItem
                || item instanceof com.chigusa.jsncraft.weapon.StaffItem) sb.append('W');
        if (item instanceof ArmorItem) sb.append('A');
        if (item instanceof DiggerItem) sb.append('T');
        return sb.toString();
    }

    public static boolean ranged(ItemStack stack) {
        return stack.getItem() instanceof ProjectileWeaponItem;
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static int plus(ItemStack stack) {
        return tag(stack).getInt("jsn_plus");
    }

    public static List<TraitRoll> traits(ItemStack stack) {
        List<TraitRoll> out = new ArrayList<>();
        for (Tag t : tag(stack).getList("jsn_traits", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            out.add(new TraitRoll(c.getString("id"), c.getDouble("value"), c.getDouble("ratio")));
        }
        return out;
    }

    public static void setPlus(ItemStack stack, int plus) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt("jsn_plus", plus));
        refreshName(stack);
    }

    public static void setTrait(ItemStack stack, int slot, TraitRoll roll) {
        List<TraitRoll> list = traits(stack);
        while (list.size() <= slot) list.add(null);
        list.set(slot, roll);
        ListTag lt = new ListTag();
        for (TraitRoll r : list) {
            if (r == null) continue;
            CompoundTag c = new CompoundTag();
            c.putString("id", r.id());
            c.putDouble("value", r.value());
            c.putDouble("ratio", r.ratio());
            lt.add(c);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.put("jsn_traits", lt));
    }

    public static ChatFormatting plusColor(int plus) {
        return plus >= 13 ? ChatFormatting.GOLD : plus >= 10 ? ChatFormatting.LIGHT_PURPLE : plus >= 7 ? ChatFormatting.AQUA
                : plus >= 4 ? ChatFormatting.GREEN : ChatFormatting.WHITE;
    }

    /** 名前を「+7 ダイヤモンドの剣」にして、+7 からは光らせる */
    public static void refreshName(ItemStack stack) {
        int p = plus(stack);
        if (p <= 0) return;
        stack.set(DataComponents.ITEM_NAME, Component.literal("+" + p + " ").withStyle(plusColor(p), ChatFormatting.BOLD)
                .append(Component.translatable(stack.getItem().getDescriptionId()).withStyle(plusColor(p))));
        if (p >= 7) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    private SmithData() {}
}
