package com.chigusa.jsncraft.smithing.arrow;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** 鍛冶師が作った矢・投げ物の品質（★1〜3）。 */
public final class Quality {
    public static int of(ItemStack stack) {
        int q = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("jsn_q");
        return Math.max(1, Math.min(3, q));
    }

    public static void set(ItemStack stack, int q) {
        if (q <= 1) return;   // ★1 は印なし（ふつうの物と重なる）
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putInt("jsn_q", q));
    }

    public static double mul(int q) {
        return 1.0 + 0.15 * (q - 1);
    }

    /** 鍛冶のレベルで品質を決める */
    public static int roll(int smithLevel, RandomSource r) {
        double p3 = Math.min(0.5, Math.max(0, (smithLevel - 15) * 0.01));
        double p2 = Math.min(0.6, 0.2 + smithLevel * 0.008);
        double x = r.nextDouble();
        if (x < p3) return 3;
        if (x < p3 + p2) return 2;
        return 1;
    }

    public static Component line(ItemStack stack) {
        int q = of(stack);
        ChatFormatting c = q == 3 ? ChatFormatting.GOLD : q == 2 ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        return Component.literal("品質 " + "★".repeat(q) + "☆".repeat(3 - q) + "（威力 ×" + String.format(java.util.Locale.ROOT, "%.2f", mul(q)) + "）").withStyle(c);
    }

    private Quality() {}
}
