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
            List.of(new Bonus(ModAttributes.CRIT_CHANCE, 0.02, "会心の確率", true), new Bonus(ModAttributes.SKILL_DAMAGE, 0.03, "技のダメージ", true)));

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
