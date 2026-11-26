package com.chigusa.jsncraft.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 段階のある釣り竿（1 鉄 / 2 ダイヤ / 3 ネザライト / 4 月銀 / 5 星銀）。
 * 段階が上がるほど、当たり判定の幅・巻く速さ・食いつき・お宝・幸運が上がる（ToolEvents で付ける）。
 */
public class JsnRodItem extends FishingRodItem {
    public final int tier;

    public JsnRodItem(int tier, Properties p) {
        super(p);
        this.tier = tier;
    }

    @Override
    public int getEnchantmentValue() {
        return 1 + tier * 3;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("段階 " + tier + "：釣りの腕が上がる竿").withStyle(ChatFormatting.AQUA));
    }
}
