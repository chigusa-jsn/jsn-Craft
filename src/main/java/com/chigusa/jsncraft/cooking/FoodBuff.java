package com.chigusa.jsncraft.cooking;

import com.chigusa.jsncraft.skill.SkillEffects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Locale;
import java.util.Optional;

/**
 * 料理のバフ1つ。effect（ポーション効果）・attribute（能力値）・special（特別な効果）のどれか。
 * special … cleanse（悪い効果を消す）/ sober（酔いを amount だけ覚ます）/ warm（凍えない）/ awake（眠気覚まし：ファントムが来なくなる）
 * { "effect": "minecraft:regeneration", "amplifier": 0, "seconds": 20 }
 * { "attribute": "jsn_craft:max_stamina", "amount": 20, "operation": "add_value", "seconds": 180 }
 */
public record FoodBuff(String effect, String attribute, int amplifier, double amount, String operation, double seconds, String special) {

    public static final Codec<FoodBuff> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("effect", "").forGetter(FoodBuff::effect),
            Codec.STRING.optionalFieldOf("attribute", "").forGetter(FoodBuff::attribute),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(FoodBuff::amplifier),
            Codec.DOUBLE.optionalFieldOf("amount", 0.0).forGetter(FoodBuff::amount),
            Codec.STRING.optionalFieldOf("operation", "add_value").forGetter(FoodBuff::operation),
            Codec.DOUBLE.optionalFieldOf("seconds", 60.0).forGetter(FoodBuff::seconds),
            Codec.STRING.optionalFieldOf("special", "").forGetter(FoodBuff::special)
    ).apply(i, FoodBuff::new));

    public boolean isSpecial() {
        return !special.isEmpty();
    }

    /** 品質などで強くした版（時間 × timeMult、能力値の量 × amountMult） */
    public FoodBuff scaled(double timeMult, double amountMult) {
        return new FoodBuff(effect, attribute, amplifier, special.equals("sober") ? amount : amount * amountMult, operation, seconds * timeMult, special);
    }

    public boolean isEffect() {
        return !effect.isEmpty();
    }

    /** 同じバフかどうか（同じ物は強いほう・長いほうにまとめる） */
    public String key() {
        if (isSpecial()) return "s:" + special;
        return isEffect() ? "e:" + effect : "a:" + attribute + ":" + operation;
    }

    public Optional<Holder.Reference<MobEffect>> effectHolder() {
        ResourceLocation id = ResourceLocation.tryParse(effect);
        return id == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, id));
    }

    public Optional<Holder.Reference<Attribute>> attributeHolder() {
        ResourceLocation id = ResourceLocation.tryParse(attribute);
        return id == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getHolder(ResourceKey.create(Registries.ATTRIBUTE, id));
    }

    public AttributeModifier.Operation op() {
        return switch (operation) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    /** 食べた人にかける。timeMult で時間をのばせる */
    public void apply(ServerPlayer player, double timeMult) {
        int ticks = (int) Math.round(seconds * timeMult * 20);
        if (isSpecial()) {
            CookingService.special(player, special, amount, seconds * timeMult);
            return;
        }
        if (isEffect()) {
            effectHolder().ifPresent(h -> player.addEffect(new MobEffectInstance(h, ticks, amplifier)));
        } else {
            attributeHolder().ifPresent(h -> SkillEffects.attributeBuff(player, h,
                    "food_" + attribute.replace(':', '_').replace('.', '_'), amount, op(), seconds * timeMult));
        }
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        if (isSpecial()) t.putString("special", special);
        else if (isEffect()) t.putString("effect", effect);
        else t.putString("attribute", attribute);
        t.putInt("amplifier", amplifier);
        t.putDouble("amount", amount);
        t.putString("operation", operation);
        t.putDouble("seconds", seconds);
        return t;
    }

    public static FoodBuff load(CompoundTag t) {
        return new FoodBuff(t.getString("effect"), t.getString("attribute"), t.getInt("amplifier"), t.getDouble("amount"),
                t.getString("operation").isEmpty() ? "add_value" : t.getString("operation"), t.getDouble("seconds"), t.getString("special"));
    }

    /** 説明文（例：再生 I 0:30 ／ 最大体力 +4 3:00） */
    public Component describe() {
        String time = String.format(Locale.ROOT, "%d:%02d", (int) seconds / 60, (int) seconds % 60);
        MutableComponent c;
        if (isSpecial()) {
            return switch (special) {
                case "cleanse" -> Component.literal("悪い効果を消す").withStyle(ChatFormatting.AQUA);
                case "sober" -> Component.literal("酔い覚まし").withStyle(ChatFormatting.AQUA);
                case "warm" -> Component.literal("ぽかぽか（凍えない） ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(time).withStyle(ChatFormatting.GRAY));
                case "awake" -> Component.literal("眠気覚まし（ファントムが来なくなる）").withStyle(ChatFormatting.AQUA);
                default -> Component.literal(special).withStyle(ChatFormatting.AQUA);
            };
        }
        if (isEffect()) {
            c = effectHolder().map(h -> h.value().getDisplayName().copy()).orElse(Component.literal(effect));
            if (amplifier > 0) c.append(" " + roman(amplifier + 1));
            c.withStyle(effectHolder().map(h -> h.value().isBeneficial()).orElse(true) ? ChatFormatting.BLUE : ChatFormatting.RED);
        } else {
            c = attributeHolder().map(h -> Component.translatable(h.value().getDescriptionId()).copy()).orElse(Component.literal(attribute));
            String v;
            if (op() == AttributeModifier.Operation.ADD_VALUE && !isPercentStat()) v = trim(amount);
            else v = trim(amount * 100) + "%";
            c.append(" +" + v).withStyle(ChatFormatting.DARK_GREEN);
        }
        return c.append(Component.literal(" " + time).withStyle(ChatFormatting.GRAY));
    }

    private boolean isPercentStat() {
        return attribute.startsWith("jsn_craft:fishing_") || attribute.endsWith("_reduction") || attribute.endsWith("crit_chance")
                || attribute.endsWith("skill_damage") || attribute.endsWith("mining_fortune") || attribute.endsWith("harvest_fortune")
                || attribute.endsWith("job_xp_bonus") || attribute.endsWith("lifesteal") || attribute.endsWith("boss_damage")
                || attribute.endsWith("cooking_mastery");
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    private static String roman(int n) {
        return switch (n) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
