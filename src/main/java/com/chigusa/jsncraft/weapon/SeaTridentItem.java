package com.chigusa.jsncraft.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * 海神の三叉槍（トライデントを進化させた特別な槍、4段階）。投げられる。
 * 投げて当たると敵を濡らし、雨の中なら雷が落ちる。投げたときのダメージは throwMult 倍（SpearEvents）。
 */
public class SeaTridentItem extends TridentItem {

    public record Spec(int tier, String id, String name, int uses, float melee, double throwMult, Rarity rarity, boolean fireproof) {}

    public static final List<Spec> SPECS = List.of(
            new Spec(1, "sea_trident", "海神の三叉槍", 600, 9.5F, 1.3, Rarity.RARE, false),
            new Spec(2, "netherite_sea_trident", "ネザライトの三叉槍", 2100, 10.5F, 1.5, Rarity.RARE, true),
            new Spec(3, "moon_silver_sea_trident", "月銀の三叉槍", 2600, 12.0F, 1.75, Rarity.EPIC, true),
            new Spec(4, "star_silver_sea_trident", "星銀の三叉槍", 3200, 14.0F, 2.0, Rarity.EPIC, true));

    private final Spec spec;

    public SeaTridentItem(Spec spec) {
        super(props(spec));
        this.spec = spec;
    }

    private static Properties props(Spec s) {
        Properties p = new Properties().durability(s.uses()).rarity(s.rarity()).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, s.melee() - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.9, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build());
        if (s.fireproof()) p = p.fireResistant();
        return p;
    }

    public Spec spec() {
        return spec;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("海神の三叉槍（" + spec.tier() + "段目）").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("投げたときのダメージ ×" + spec.throwMult() + "、当たると敵を濡らす").withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("雨の中で当てると雷が落ちる").withStyle(ChatFormatting.BLUE));
        if (spec.tier() < 4) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ").withStyle(ChatFormatting.DARK_GRAY));
    }
}
