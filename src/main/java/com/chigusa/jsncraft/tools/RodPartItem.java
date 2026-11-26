package com.chigusa.jsncraft.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 釣り竿の部品。釣り竿カスタム台で、釣り師が竿に取り付ける。 */
public class RodPartItem extends Item {
    public final RodPart part;
    public final int grade;

    public RodPartItem(RodPart part, int grade, Properties p) {
        super(p);
        this.part = part;
        this.grade = grade;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("釣り竿の部品（" + part.name + "・" + grade + "段目）").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal(part.effect + " ＋" + RodParts.fmt(part, grade)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("釣り竿カスタム台で、釣り師が取り付ける").withStyle(ChatFormatting.DARK_GRAY));
    }
}
