package com.chigusa.jsncraft.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 職業武器の鍛冶型（全職業共通）。鍛冶台で「鍛冶型＋武器＋素材」→ 次の段。 */
public class TemplateItem extends Item {
    private final String desc;

    public TemplateItem(Properties props, String desc) {
        super(props);
        this.desc = desc;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(desc).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("鍛冶台：鍛冶型＋職業武器＋素材 → 次の段").withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("魔紋・強化（+）・エンチャントは引き継ぐ").withStyle(ChatFormatting.DARK_GRAY));
    }
}
