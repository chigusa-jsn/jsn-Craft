package com.chigusa.jsncraft.smithing;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 鍛え直しでつく特性。触媒（鉱石など）で「系統」が決まり、その中から1つと強さがランダムで決まる。
 * 種類：W 武器 / A 防具 / T 道具
 */
public final class SmithTraits {

    public record Trait(String id, String name, String attribute, AttributeModifier.Operation op, double min, double max,
                        String kinds, boolean percent) {
        public Optional<Holder.Reference<Attribute>> holder() {
            return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceKey.create(Registries.ATTRIBUTE, ResourceLocation.parse(attribute)));
        }

        public boolean fits(String kind) {
            return kinds.contains(kind);
        }
    }

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final AttributeModifier.Operation MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;

    public static final Map<String, Trait> TRAITS = new LinkedHashMap<>();

    private static void t(String id, String name, String attr, AttributeModifier.Operation op, double min, double max, String kinds, boolean pct) {
        TRAITS.put(id, new Trait(id, name, attr, op, min, max, kinds, pct));
    }

    static {
        t("sharp", "鋭利", "minecraft:generic.attack_damage", ADD, 0.5, 2.0, "W", false);
        t("critical", "会心", "jsn_craft:crit_chance", ADD, 0.02, 0.08, "WA", true);
        t("swift", "迅速", "minecraft:generic.attack_speed", MUL, 0.03, 0.12, "W", true);
        t("agile", "軽快", "minecraft:generic.movement_speed", MUL, 0.02, 0.06, "A", true);
        t("efficient", "効率", "minecraft:player.block_break_speed", MUL, 0.05, 0.20, "T", true);
        t("arcane", "魔性", "jsn_craft:skill_damage", ADD, 0.03, 0.10, "WAT", true);
        t("focus", "集中", "jsn_craft:cooldown_reduction", ADD, 0.02, 0.06, "WAT", true);
        t("wise", "知恵", "jsn_craft:job_xp_bonus", ADD, 0.03, 0.10, "WAT", true);
        t("lucky", "幸運", "minecraft:generic.luck", ADD, 0.5, 2.0, "WAT", false);
        t("prospector", "鉱脈", "jsn_craft:mining_fortune", ADD, 0.05, 0.20, "T", true);
        t("sturdy", "頑丈", "minecraft:generic.armor_toughness", ADD, 0.5, 2.0, "A", false);
        t("durable", "長持ち", "jsn_craft:tool_durability", ADD, 0.05, 0.15, "WT", true);
        t("guard", "守護", "jsn_craft:damage_reduction", ADD, 0.01, 0.04, "A", true);
        t("vital", "生命", "minecraft:generic.max_health", ADD, 1.0, 4.0, "A", false);
        t("regen", "再生", "jsn_craft:hp_regen", ADD, 0.1, 0.4, "WA", false);
        t("vampire", "吸血", "jsn_craft:lifesteal", ADD, 0.01, 0.04, "W", true);
        t("slayer", "討伐", "jsn_craft:boss_damage", ADD, 0.05, 0.15, "W", true);
    }

    /** 触媒 → 出る特性の系統 */
    public static final Map<Item, List<String>> CATALYSTS = new LinkedHashMap<>();

    static {
        CATALYSTS.put(Items.QUARTZ, List.of("sharp", "critical"));
        CATALYSTS.put(Items.REDSTONE, List.of("swift", "agile", "efficient"));
        CATALYSTS.put(Items.AMETHYST_SHARD, List.of("arcane", "focus"));
        CATALYSTS.put(Items.LAPIS_LAZULI, List.of("wise", "lucky"));
        CATALYSTS.put(Items.EMERALD, List.of("lucky", "prospector"));
        CATALYSTS.put(Items.IRON_INGOT, List.of("sturdy", "durable", "guard"));
        CATALYSTS.put(Items.GHAST_TEAR, List.of("vital", "regen", "vampire"));
        CATALYSTS.put(Items.BLAZE_POWDER, List.of("slayer", "sharp"));
        CATALYSTS.put(Items.ECHO_SHARD, new ArrayList<>(TRAITS.keySet())); // どれでも・強めに出る
    }

    /** この触媒とこの種類の道具で出る特性 */
    public static List<Trait> pool(Item catalyst, String kinds) {
        List<Trait> out = new ArrayList<>();
        List<String> ids = CATALYSTS.get(catalyst);
        if (ids == null) return out;
        for (String id : ids) {
            Trait t = TRAITS.get(id);
            if (t == null) continue;
            for (char k : kinds.toCharArray()) {
                if (t.fits(String.valueOf(k))) {
                    out.add(t);
                    break;
                }
            }
        }
        return out;
    }

    /** 強さの割合（0〜1）から評価 S/A/B/C */
    public static String grade(double ratio) {
        return ratio >= 0.9 ? "S" : ratio >= 0.7 ? "A" : ratio >= 0.4 ? "B" : "C";
    }

    public static ChatFormatting gradeColor(String g) {
        return switch (g) {
            case "S" -> ChatFormatting.GOLD;
            case "A" -> ChatFormatting.LIGHT_PURPLE;
            case "B" -> ChatFormatting.AQUA;
            default -> ChatFormatting.GRAY;
        };
    }

    private SmithTraits() {}
}
