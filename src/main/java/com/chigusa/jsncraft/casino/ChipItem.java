package com.chigusa.jsncraft.casino;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** カジノチップ。value は1枚の値打ち。 */
public class ChipItem extends Item {
    public final int value;

    public ChipItem(Properties props, int value) {
        super(props);
        this.value = value;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("チップ " + value + " 枚ぶん").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("カジノの台で遊べる。両替機でお金に戻せる").withStyle(ChatFormatting.GRAY));
    }
}
