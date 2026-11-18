package com.chigusa.jsncraft.weapon;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.List;

/** 暗殺者の短剣（6段階）。逆手に持つ。両手に持つと二刀流（左右交互に切りつけ、左の短剣の力も乗る）。 */
public class DaggerItem extends TieredItem {

    public record Spec(int tier, String id, String name, int uses, float damage, float speed, double skill, Rarity rarity, boolean fireproof) {}

    public static final List<Spec> SPECS = List.of(
            new Spec(1, "stone_dagger", "石の短剣", 200, 4.0F, 2.2F, 0.03, Rarity.COMMON, false),
            new Spec(2, "iron_dagger", "鉄の短剣", 500, 5.0F, 2.2F, 0.06, Rarity.COMMON, false),
            new Spec(3, "diamond_dagger", "ダイヤの短剣", 1600, 6.0F, 2.3F, 0.09, Rarity.UNCOMMON, false),
            new Spec(4, "netherite_dagger", "ネザライトの短剣", 2100, 7.0F, 2.3F, 0.12, Rarity.RARE, true),
            new Spec(5, "moon_silver_dagger", "月銀の短剣", 2600, 8.5F, 2.4F, 0.16, Rarity.RARE, true),
            new Spec(6, "star_silver_dagger", "星銀の短剣", 3200, 10.0F, 2.5F, 0.20, Rarity.EPIC, true));

    private final Spec spec;

    public DaggerItem(Spec spec) {
        super(new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, spec.uses(), 2.0F, 0, 10 + spec.tier() * 2, () -> Ingredient.of(Items.IRON_INGOT)),
                props(spec));
        this.spec = spec;
    }

    private static Properties props(Spec s) {
        Properties p = new Properties().rarity(s.rarity()).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, s.damage() - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, s.speed() - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build());
        if (s.fireproof()) p = p.fireResistant();
        return p;
    }

    public Spec spec() {
        return spec;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("暗殺者の短剣（" + spec.tier() + "段目）・逆手持ち").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Component.literal("持つとスキルのダメージ +" + Math.round(spec.skill() * 100) + "%").withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("両手に持つと二刀流：左右交互に切りつけ、左の短剣の力も乗る").withStyle(ChatFormatting.BLUE));
        if (spec.tier() < 6) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ（魔紋と強化はそのまま）").withStyle(ChatFormatting.DARK_GRAY));
    }
}
