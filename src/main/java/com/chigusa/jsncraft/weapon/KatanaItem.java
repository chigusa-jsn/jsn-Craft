package com.chigusa.jsncraft.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.List;

/** 剣士の刀（6段階）。剣より少し速く振れる。持つとスキルのダメージも上がる。 */
public class KatanaItem extends SwordItem {

    /** 段階ごとの性能（damage は攻撃力、skill はスキルダメージ +○%） */
    public record Spec(int tier, String id, String name, int uses, int damage, float speed, double skill, Rarity rarity, boolean fireproof) {}

    public static final List<Spec> SPECS = List.of(
            new Spec(1, "stone_katana", "石の刀", 200, 5, 1.9F, 0.03, Rarity.COMMON, false),
            new Spec(2, "iron_katana", "鉄の刀", 500, 6, 1.9F, 0.06, Rarity.COMMON, false),
            new Spec(3, "diamond_katana", "ダイヤの刀", 1600, 7, 1.95F, 0.09, Rarity.UNCOMMON, false),
            new Spec(4, "netherite_katana", "ネザライトの刀", 2100, 8, 1.95F, 0.12, Rarity.RARE, true),
            new Spec(5, "moon_silver_katana", "月銀の刀", 2600, 9, 2.0F, 0.16, Rarity.RARE, true),
            new Spec(6, "star_silver_katana", "星銀の刀", 3200, 11, 2.1F, 0.20, Rarity.EPIC, true));

    private final Spec spec;

    public KatanaItem(Spec spec) {
        super(tier(spec), props(spec));
        this.spec = spec;
    }

    private static SimpleTier tier(Spec s) {
        return new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, s.uses(), 4.0F, 0, 10 + s.tier() * 2, () -> Ingredient.of(Items.IRON_INGOT));
    }

    private static Properties props(Spec s) {
        Properties p = new Properties().rarity(s.rarity()).attributes(SwordItem.createAttributes(tier(s), s.damage() - 1, s.speed() - 4.0F));
        if (s.fireproof()) p = p.fireResistant();
        return p;
    }

    public Spec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("剣士の刀（" + spec.tier() + "段目）").withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.literal("持つとスキルのダメージ +" + Math.round(spec.skill() * 100) + "%").withStyle(ChatFormatting.BLUE));
        if (spec.tier() < 6) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ（魔紋と強化はそのまま）").withStyle(ChatFormatting.DARK_GRAY));
    }
}
