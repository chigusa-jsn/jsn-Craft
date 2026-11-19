package com.chigusa.jsncraft.armor;

import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ArmorItem;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 戦闘職の服の種類（職業ごとに1つ）。形と絵は tools/gen_armor.py。
 * 能力は「その職業（基本職・上位職）で着たとき」だけ働く。守り（防御力）はだれが着ても同じ。
 */
public enum ArmorStyle {
    MAGE("mage", "星詠みの魔導士", Set.of("mage", "archmage"), Weight.LIGHT,
            new String[]{"見習い", "水晶", "宝玉", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "魔導帽", ArmorItem.Type.CHESTPLATE, "魔導ローブ", ArmorItem.Type.LEGGINGS, "魔導のすそ", ArmorItem.Type.BOOTS, "魔導の靴"),
            "minecraft:white_wool",
            new String[]{"jsn_craft:crystal", "jsn_craft:sapphire"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.MAGIC_POWER, 0.02, "魔法の強さ", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(ModAttributes.MAX_MANA, 5, "最大マナ", false),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.MANA_REGEN, 0.15, "マナの回復", false),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.CAST_SPEED, 0.01, "詠唱の速さ", true)),
            List.of(new Bonus(ModAttributes.MAGIC_POWER, 0.03, "魔法の強さ", true), new Bonus(ModAttributes.COOLDOWN_REDUCTION, 0.015, "待ち時間の短さ", true))),
    WARRIOR("warrior", "蒼刃の剣士", Set.of("warrior", "sword_saint"), Weight.MEDIUM,
            new String[]{"見習い", "鉄", "ダイヤ", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "剣士のはちまき", ArmorItem.Type.CHESTPLATE, "剣士の胴着", ArmorItem.Type.LEGGINGS, "剣士のはかま", ArmorItem.Type.BOOTS, "剣士の足袋"),
            "minecraft:leather",
            new String[]{"minecraft:iron_ingot", "minecraft:diamond"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.CRIT_CHANCE, 0.01, "会心の確率", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(ModAttributes.SKILL_DAMAGE, 0.015, "技のダメージ", true),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.CRIT_DAMAGE, 0.04, "会心のダメージ", true),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.MAX_STAMINA, 4, "最大スタミナ", false)),
            List.of(new Bonus(ModAttributes.CRIT_CHANCE, 0.02, "会心の確率", true), new Bonus(ModAttributes.SKILL_DAMAGE, 0.03, "技のダメージ", true))),
    AXEMAN("axeman", "断鋼の斧使い", Set.of("axeman", "berserker"), Weight.HEAVY,
            new String[]{"見習い", "鉄", "ダイヤ", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "斧使いのかぶと", ArmorItem.Type.CHESTPLATE, "斧使いの胴よろい", ArmorItem.Type.LEGGINGS, "斧使いのすね当て", ArmorItem.Type.BOOTS, "斧使いの長靴"),
            "minecraft:red_wool",
            new String[]{"minecraft:iron_ingot", "minecraft:diamond"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.CRIT_DAMAGE, 0.04, "会心のダメージ", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 0.5, "攻撃力", false),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.DAMAGE_REDUCTION, 0.01, "受けるダメージ減", true),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.MAX_STAMINA, 4, "最大スタミナ", false)),
            List.of(new Bonus(ModAttributes.SKILL_DAMAGE, 0.03, "技のダメージ", true), new Bonus(ModAttributes.LIFESTEAL, 0.01, "吸血", true))),
    ARCHER("archer", "風読みの射手", Set.of("archer", "bow_saint"), Weight.MEDIUM,
            new String[]{"見習い", "骨", "ブレイズ", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "射手のフード", ArmorItem.Type.CHESTPLATE, "射手の上着", ArmorItem.Type.LEGGINGS, "射手のズボン", ArmorItem.Type.BOOTS, "射手の長靴"),
            "minecraft:green_wool",
            new String[]{"minecraft:bone_block", "minecraft:blaze_rod"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.CRIT_CHANCE, 0.01, "会心の確率", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(ModAttributes.SKILL_DAMAGE, 0.015, "技のダメージ", true),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.STAMINA_REGEN, 0.1, "スタミナの回復", false),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.STAMINA_COST_REDUCTION, 0.01, "スタミナの節約", true)),
            List.of(new Bonus(ModAttributes.CRIT_CHANCE, 0.02, "会心の確率", true), new Bonus(ModAttributes.CRIT_DAMAGE, 0.05, "会心のダメージ", true))),
    LANCER("lancer", "潮騒の槍兵", Set.of("lancer", "sea_lancer"), Weight.MEDIUM,
            new String[]{"見習い", "鉄", "海晶", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "槍兵のかぶと", ArmorItem.Type.CHESTPLATE, "槍兵のうろこよろい", ArmorItem.Type.LEGGINGS, "槍兵の腰当て", ArmorItem.Type.BOOTS, "槍兵のすね当て"),
            "minecraft:cyan_wool",
            new String[]{"minecraft:iron_ingot", "minecraft:prismarine_crystals"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.CRIT_CHANCE, 0.01, "会心の確率", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(ModAttributes.SKILL_DAMAGE, 0.015, "技のダメージ", true),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.MAX_STAMINA, 4, "最大スタミナ", false),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.STAMINA_REGEN, 0.1, "スタミナの回復", false)),
            List.of(new Bonus(ModAttributes.SKILL_DAMAGE, 0.03, "技のダメージ", true), new Bonus(ModAttributes.COOLDOWN_REDUCTION, 0.015, "待ち時間の短さ", true))),
    SHIELD_KNIGHT("shield_knight", "不落の盾騎士", Set.of("shield_knight", "guardian_knight"), Weight.HEAVY,
            new String[]{"見習い", "鉄", "ダイヤ", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "盾騎士のかぶと", ArmorItem.Type.CHESTPLATE, "盾騎士の胸よろい", ArmorItem.Type.LEGGINGS, "盾騎士の脚よろい", ArmorItem.Type.BOOTS, "盾騎士の鉄靴"),
            "minecraft:copper_ingot",
            new String[]{"minecraft:iron_block", "minecraft:diamond"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.DAMAGE_REDUCTION, 0.01, "受けるダメージ減", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 1, "最大体力", false),
                    ArmorItem.Type.LEGGINGS, new Bonus(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 0.03, "ふっとびにくさ", true),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.MAX_STAMINA, 4, "最大スタミナ", false)),
            List.of(new Bonus(ModAttributes.DAMAGE_REDUCTION, 0.02, "受けるダメージ減", true), new Bonus(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 1, "最大体力", false))),
    BEAST("beast", "群狼の獣使い", Set.of("beast_tamer", "beast_king"), Weight.MEDIUM,
            new String[]{"見習い", "鉄", "骨", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "獣使いのおおかみずきん", ArmorItem.Type.CHESTPLATE, "獣使いの毛皮の上着", ArmorItem.Type.LEGGINGS, "獣使いのズボン", ArmorItem.Type.BOOTS, "獣使いの毛皮の靴"),
            "minecraft:rabbit_hide",
            new String[]{"minecraft:iron_ingot", "minecraft:bone_block"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.CRIT_CHANCE, 0.01, "会心の確率", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(ModAttributes.SKILL_DAMAGE, 0.02, "技と相棒のダメージ", true),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.MAX_STAMINA, 4, "最大スタミナ", false),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.STAMINA_REGEN, 0.1, "スタミナの回復", false)),
            List.of(new Bonus(ModAttributes.SKILL_DAMAGE, 0.03, "技と相棒のダメージ", true), new Bonus(ModAttributes.COOLDOWN_REDUCTION, 0.015, "待ち時間の短さ", true))),
    BRAWLER("brawler", "鉄拳の拳闘士", Set.of("brawler", "fist_god"), Weight.LIGHT,
            new String[]{"見習い", "鉄", "黒曜石", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "拳闘士のはちまき", ArmorItem.Type.CHESTPLATE, "拳闘士の道着", ArmorItem.Type.LEGGINGS, "拳闘士のズボン", ArmorItem.Type.BOOTS, "拳闘士の足布"),
            "minecraft:orange_wool",
            new String[]{"minecraft:iron_ingot", "minecraft:obsidian"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.CRIT_CHANCE, 0.01, "会心の確率", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 0.5, "攻撃力", false),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.LIFESTEAL, 0.005, "吸血", true),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.STAMINA_COST_REDUCTION, 0.01, "スタミナの節約", true)),
            List.of(new Bonus(ModAttributes.CRIT_DAMAGE, 0.05, "会心のダメージ", true), new Bonus(ModAttributes.SKILL_DAMAGE, 0.03, "技のダメージ", true))),
    ELEMENTALIST("elementalist", "精霊の術師", Set.of("elementalist", "spirit_lord"), Weight.LIGHT,
            new String[]{"見習い", "マンドラゴラ", "精霊石", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "術師のずきん", ArmorItem.Type.CHESTPLATE, "術師のローブ", ArmorItem.Type.LEGGINGS, "術師のすそ", ArmorItem.Type.BOOTS, "術師の靴"),
            "minecraft:lime_wool",
            new String[]{"jsn_craft:mandrake", "jsn_craft:spirit_stone"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.MAGIC_POWER, 0.02, "魔法の強さ", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(ModAttributes.MAX_MANA, 5, "最大マナ", false),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.MANA_REGEN, 0.15, "マナの回復", false),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.COOLDOWN_REDUCTION, 0.008, "待ち時間の短さ", true)),
            List.of(new Bonus(ModAttributes.MAGIC_POWER, 0.03, "魔法の強さ", true), new Bonus(ModAttributes.CAST_SPEED, 0.015, "詠唱の速さ", true))),
    HEALER("healer", "慈光の癒し手", Set.of("healer", "saint"), Weight.LIGHT,
            new String[]{"見習い", "銀", "聖晶", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "癒し手のずきん", ArmorItem.Type.CHESTPLATE, "癒し手の衣", ArmorItem.Type.LEGGINGS, "癒し手のすそ", ArmorItem.Type.BOOTS, "癒し手の靴"),
            "minecraft:yellow_wool",
            new String[]{"jsn_craft:silver_ingot", "jsn_craft:holy_crystal"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.HEALING_POWER, 0.03, "回復の強さ", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(ModAttributes.MAX_MANA, 5, "最大マナ", false),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.MANA_REGEN, 0.15, "マナの回復", false),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.CAST_SPEED, 0.01, "詠唱の速さ", true)),
            List.of(new Bonus(ModAttributes.HEALING_POWER, 0.04, "回復の強さ", true), new Bonus(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 1, "最大体力", false))),
    ASSASSIN("assassin", "影走りの暗殺者", Set.of("assassin", "reaper"), Weight.LIGHT,
            new String[]{"見習い", "鉄", "ダイヤ", "ネザライト", "月銀", "星銀"},
            Map.of(ArmorItem.Type.HELMET, "暗殺者のずきん", ArmorItem.Type.CHESTPLATE, "暗殺者の上着", ArmorItem.Type.LEGGINGS, "暗殺者のズボン", ArmorItem.Type.BOOTS, "暗殺者の足布"),
            "minecraft:black_wool",
            new String[]{"minecraft:iron_ingot", "minecraft:diamond"},
            Map.of(ArmorItem.Type.HELMET, new Bonus(ModAttributes.CRIT_CHANCE, 0.012, "会心の確率", true),
                    ArmorItem.Type.CHESTPLATE, new Bonus(ModAttributes.CRIT_DAMAGE, 0.05, "会心のダメージ", true),
                    ArmorItem.Type.LEGGINGS, new Bonus(ModAttributes.STAMINA_COST_REDUCTION, 0.01, "スタミナの節約", true),
                    ArmorItem.Type.BOOTS, new Bonus(ModAttributes.SKILL_DAMAGE, 0.015, "技のダメージ", true)),
            List.of(new Bonus(ModAttributes.CRIT_CHANCE, 0.02, "会心の確率", true), new Bonus(ModAttributes.LIFESTEAL, 0.01, "吸血", true)));

    /** 能力：amount × 段階 だけ上がる */
    public record Bonus(Holder<Attribute> attribute, double perTier, String name, boolean percent) {
        public String text(int tier) {
            double v = perTier * tier;
            return name + " +" + (percent ? Math.round(v * 1000) / 10.0 + "%" : (v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(Math.round(v * 100) / 100.0)));
        }
    }

    /** 重さ：守りの強さ */
    public enum Weight { LIGHT, MEDIUM, HEAVY }

    public final String id, jobName;
    public final Set<String> jobs;
    public final Weight weight;
    public final String[] tierNames;
    public final Map<ArmorItem.Type, String> pieceNames;
    /** 1段目の材料（作業台）／2段目・3段目の鍛冶の材料 */
    public final String craftItem;
    public final String[] upgradeItems;
    public final Map<ArmorItem.Type, Bonus> pieceBonus;
    /** 4つそろえたとき（いちばん低い段で決まる） */
    public final List<Bonus> setBonus;

    ArmorStyle(String id, String jobName, Set<String> jobs, Weight weight, String[] tierNames, Map<ArmorItem.Type, String> pieceNames,
               String craftItem, String[] upgradeItems, Map<ArmorItem.Type, Bonus> pieceBonus, List<Bonus> setBonus) {
        this.id = id;
        this.jobName = jobName;
        this.jobs = jobs;
        this.weight = weight;
        this.tierNames = tierNames;
        this.pieceNames = pieceNames;
        this.craftItem = craftItem;
        this.upgradeItems = upgradeItems;
        this.pieceBonus = pieceBonus;
        this.setBonus = setBonus;
    }

}
