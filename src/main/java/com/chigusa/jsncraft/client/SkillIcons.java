package com.chigusa.jsncraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * スキルのアイコンを描く。
 * "jsn_craft:skill_icon/…" なら専用の絵（textures/gui/skill_icon/….png、32×32 を 16×16 で）、
 * それ以外はアイテムの絵。絵は tools/gen_skill_icons.py で作る。
 */
public final class SkillIcons {

    public static boolean isCustom(ResourceLocation icon) {
        return icon != null && icon.getPath().startsWith("skill_icon/");
    }

    public static void draw(GuiGraphics g, ResourceLocation icon, int x, int y) {
        if (icon == null) return;
        if (isCustom(icon)) {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(icon.getNamespace(), "textures/gui/" + icon.getPath() + ".png");
            g.blit(tex, x, y, 16, 16, 0, 0, 32, 32, 32, 32);
        } else {
            g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(icon)), x, y);
        }
    }

    private SkillIcons() {}
}
