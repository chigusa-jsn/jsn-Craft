package com.chigusa.jsncraft.weapon;

import com.chigusa.jsncraft.JsnCraft;
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

/** 槍兵の槍（6段階）。リーチが長い（+1.5ブロック）。持つとスキルのダメージも上がる。 */
public class JobSpearItem extends TieredItem {

    public record Spec(int tier, String id, String name, int uses, float damage, float speed, double skill, Rarity rarity, boolean fireproof) {}

    public static final List<Spec> SPECS = List.of(
            new Spec(1, "stone_spear", "石の槍", 200, 6.5F, 1.1F, 0.03, Rarity.COMMON, false),
            new Spec(2, "iron_spear", "鉄の槍", 500, 7.5F, 1.1F, 0.06, Rarity.COMMON, false),
            new Spec(3, "prismarine_spear", "プリズマリンの槍", 1600, 8.5F, 1.15F, 0.09, Rarity.UNCOMMON, false),
            new Spec(4, "netherite_spear", "ネザライトの槍", 2100, 9.5F, 1.15F, 0.12, Rarity.RARE, true),
            new Spec(5, "moon_silver_spear", "月銀の槍", 2600, 11.0F, 1.2F, 0.16, Rarity.RARE, true),
            new Spec(6, "star_silver_spear", "星銀の槍", 3200, 13.0F, 1.25F, 0.20, Rarity.EPIC, true));

    public static final double REACH = 1.5;

    private final Spec spec;

    public JobSpearItem(Spec spec) {
        super(new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, spec.uses(), 2.0F, 0, 10 + spec.tier() * 2, () -> Ingredient.of(Items.IRON_INGOT)),
                props(spec));
        this.spec = spec;
    }

    private static Properties props(Spec s) {
        Properties p = new Properties().rarity(s.rarity()).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, s.damage() - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, s.speed() - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(JsnCraft.id("spear_reach"), REACH, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
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
        lines.add(Component.literal("槍兵の槍（" + spec.tier() + "段目）").withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.literal("リーチ +" + REACH + "ブロック・持つとスキルのダメージ +" + Math.round(spec.skill() * 100) + "%").withStyle(ChatFormatting.BLUE));
        if (spec.tier() < 6) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ（魔紋と強化はそのまま）").withStyle(ChatFormatting.DARK_GRAY));
    }
}
