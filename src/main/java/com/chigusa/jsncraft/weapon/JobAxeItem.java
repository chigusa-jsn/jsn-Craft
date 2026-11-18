package com.chigusa.jsncraft.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.List;

/** 斧使いの斧（6段階）。ふつうの斧として使え、持つとスキルのダメージも上がる。 */
public class JobAxeItem extends AxeItem {

    /** 段階ごとの性能（damage は攻撃力、skill はスキルダメージ +○%） */
    public record Spec(int tier, String id, String name, int uses, float digSpeed, float damage, float speed, double skill,
                       Rarity rarity, boolean fireproof) {}

    public static final List<Spec> SPECS = List.of(
            new Spec(1, "stone_job_axe", "石の大斧", 200, 4.0F, 9.5F, 0.85F, 0.03, Rarity.COMMON, false),
            new Spec(2, "iron_job_axe", "鉄の大斧", 500, 6.0F, 10.0F, 0.9F, 0.06, Rarity.COMMON, false),
            new Spec(3, "diamond_job_axe", "ダイヤの大斧", 1600, 8.0F, 10.5F, 1.0F, 0.09, Rarity.UNCOMMON, false),
            new Spec(4, "netherite_job_axe", "ネザライトの大斧", 2100, 9.0F, 11.5F, 1.0F, 0.12, Rarity.RARE, true),
            new Spec(5, "moon_silver_job_axe", "月銀の大斧", 2600, 10.0F, 13.0F, 1.05F, 0.16, Rarity.RARE, true),
            new Spec(6, "star_silver_job_axe", "星銀の大斧", 3200, 11.0F, 15.0F, 1.1F, 0.20, Rarity.EPIC, true));

    private final Spec spec;

    public JobAxeItem(Spec spec) {
        super(tier(spec), props(spec));
        this.spec = spec;
    }

    private static SimpleTier tier(Spec s) {
        var incorrect = s.tier() <= 1 ? BlockTags.INCORRECT_FOR_STONE_TOOL : s.tier() == 2 ? BlockTags.INCORRECT_FOR_IRON_TOOL
                : s.tier() == 3 ? BlockTags.INCORRECT_FOR_DIAMOND_TOOL : BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        return new SimpleTier(incorrect, s.uses(), s.digSpeed(), 0, 10 + s.tier() * 2, () -> Ingredient.of(Items.IRON_INGOT));
    }

    private static Properties props(Spec s) {
        Properties p = new Properties().rarity(s.rarity()).attributes(AxeItem.createAttributes(tier(s), s.damage() - 1.0F, s.speed() - 4.0F));
        if (s.fireproof()) p = p.fireResistant();
        return p;
    }

    public Spec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("斧使いの大斧（" + spec.tier() + "段目）").withStyle(ChatFormatting.DARK_RED));
        lines.add(Component.literal("持つとスキルのダメージ +" + Math.round(spec.skill() * 100) + "%").withStyle(ChatFormatting.BLUE));
        if (spec.tier() < 6) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ（魔紋と強化はそのまま）").withStyle(ChatFormatting.DARK_GRAY));
    }
}
