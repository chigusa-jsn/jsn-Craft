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

/** 獣使いの鞭（6段階）。遠くまで届き、叩いた敵を相棒みんなが狙う。 */
public class WhipItem extends TieredItem {

    public record Spec(int tier, String id, String name, int uses, float damage, float speed, double reach, double skill, Rarity rarity, boolean fireproof) {}

    public static final List<Spec> SPECS = List.of(
            new Spec(1, "leather_whip", "革の鞭", 200, 3.0F, 1.6F, 1.5, 0.03, Rarity.COMMON, false),
            new Spec(2, "iron_whip", "鉄の鞭", 500, 4.0F, 1.6F, 1.75, 0.06, Rarity.COMMON, false),
            new Spec(3, "bone_whip", "獣骨の鞭", 1400, 5.0F, 1.7F, 2.0, 0.09, Rarity.UNCOMMON, false),
            new Spec(4, "netherite_whip", "ネザライトの鞭", 2100, 6.0F, 1.7F, 2.25, 0.12, Rarity.RARE, true),
            new Spec(5, "moon_silver_whip", "月銀の鞭", 2600, 7.5F, 1.8F, 2.5, 0.16, Rarity.RARE, true),
            new Spec(6, "star_silver_whip", "星銀の鞭", 3200, 9.0F, 1.9F, 3.0, 0.20, Rarity.EPIC, true));

    private final Spec spec;

    public WhipItem(Spec spec) {
        super(new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, spec.uses(), 2.0F, 0, 10 + spec.tier() * 2, () -> Ingredient.of(Items.LEATHER)),
                props(spec));
        this.spec = spec;
    }

    private static Properties props(Spec s) {
        Properties p = new Properties().rarity(s.rarity()).attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, s.damage() - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, s.speed() - 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(JsnCraft.id("whip/reach"), s.reach(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
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
        lines.add(Component.literal("獣使いの鞭（" + spec.tier() + "段目）").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("持つとスキルと相棒のダメージ +" + Math.round(spec.skill() * 100) + "%").withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("攻撃が " + spec.reach() + " ブロック遠くまで届く").withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("伸びた鞭の線の上の敵に、まとめて当たる").withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("叩いた敵を、相棒たちがいっせいに狙う").withStyle(ChatFormatting.BLUE));
        if (spec.tier() < 6) lines.add(Component.literal("鍛冶台で鍛冶型を使うと次の段へ（魔紋と強化はそのまま）").withStyle(ChatFormatting.DARK_GRAY));
    }
}
