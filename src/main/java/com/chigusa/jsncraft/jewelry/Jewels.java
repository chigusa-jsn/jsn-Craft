package com.chigusa.jsncraft.jewelry;

import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 細工の表：装飾品の種類（枠）・台座（金属のランク）・宝石・特別な装飾品。
 * 細工の腕のレベル（unlock）は、細工師はそのレベル、宝飾師は 50＋レベル で数える。
 */
public final class Jewels {

    /** 能力値1つ（percent は表示を％にするか） */
    public record Stat(Holder<Attribute> attr, double amount, AttributeModifier.Operation op, boolean percent, String label) {}

    private static Stat add(Holder<Attribute> a, double v, String label) {
        return new Stat(a, v, AttributeModifier.Operation.ADD_VALUE, false, label);
    }

    private static Stat pct(Holder<Attribute> a, double v, String label) {
        return new Stat(a, v, AttributeModifier.Operation.ADD_VALUE, true, label);
    }

    private static Stat mult(Holder<Attribute> a, double v, String label) {
        return new Stat(a, v, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true, label);
    }

    // ================= 装飾品の種類（Curios の枠） =================

    /** slot は Curios の枠の名前。base は宝石なしでもつく能力（台座の倍率がかかる） */
    public record Kind(int index, String id, String name, String slot, Stat base) {}

    public static final List<Kind> KINDS = List.of(
            new Kind(0, "ring", "指輪", "ring", add(Attributes.ATTACK_DAMAGE, 0.4, "攻撃力")),
            new Kind(1, "necklace", "首飾り", "necklace", add(ModAttributes.MAX_MANA, 5, "最大マナ")),
            new Kind(2, "bracelet", "腕輪", "bracelet", add(Attributes.ARMOR, 1, "防御力")),
            new Kind(3, "earring", "耳飾り", "earring", pct(ModAttributes.COOLDOWN_REDUCTION, 0.01, "待ち時間短縮")),
            new Kind(4, "charm", "お守り", "charm", add(Attributes.LUCK, 0.5, "幸運")));

    // ================= 台座（金属のランク） =================

    /** metal と count：作るのに使う金属。sockets：宝石の穴。power：効果の倍率 */
    public record Tier(int index, String id, String name, Supplier<Item> metal, int count, int sockets, double power,
                       int unlock, int color) {}

    public static final List<Tier> TIERS = List.of(
            new Tier(0, "copper", "銅", () -> Items.COPPER_INGOT, 4, 1, 1.0, 1, 0xC8784A),
            new Tier(1, "iron", "鉄", () -> Items.IRON_INGOT, 4, 1, 1.2, 5, 0xD8D8D8),
            new Tier(2, "gold", "金", () -> Items.GOLD_INGOT, 4, 2, 1.4, 15, 0xF0C840),
            new Tier(3, "diamond", "ダイヤ", () -> Items.DIAMOND, 2, 2, 1.7, 25, 0x5AE0D8),
            new Tier(4, "netherite", "ネザライト", () -> Items.NETHERITE_INGOT, 1, 2, 2.0, 40, 0x5A4A50),
            new Tier(5, "moon_silver", "月銀", () -> ModJewelry.MOON_SILVER.get(), 2, 3, 2.4, 60, 0xB8C8F0),
            new Tier(6, "star_silver", "星銀", () -> ModJewelry.STAR_SILVER.get(), 2, 3, 2.8, 80, 0xF8F0D0));

    // ================= 宝石 =================

    /** rarity：0 よく出る / 1 ふつう / 2 めずらしい。affinity：相性のいい装飾品（効果 ×1.25） */
    public record Gem(int index, String id, String name, int color, int rarity, String affinity, List<Stat> stats) {}

    public static final List<Gem> GEMS = List.of(
            new Gem(0, "garnet", "ガーネット", 0x9A1A2A, 0, "bracelet", List.of(add(Attributes.MAX_HEALTH, 1.5, "最大HP"))),
            new Gem(1, "peridot", "ペリドット", 0x9AD040, 0, "earring", List.of(add(ModAttributes.MAX_STAMINA, 6, "最大スタミナ"))),
            new Gem(2, "aquamarine", "アクアマリン", 0x70D8E8, 0, "earring", List.of(mult(Attributes.MOVEMENT_SPEED, 0.015, "移動速度"))),
            new Gem(3, "onyx", "オニキス", 0x2A2A30, 0, "bracelet", List.of(pct(ModAttributes.DAMAGE_REDUCTION, 0.01, "受けるダメージ減少"))),
            new Gem(4, "ruby", "ルビー", 0xE02040, 1, "ring", List.of(add(Attributes.ATTACK_DAMAGE, 0.4, "攻撃力"))),
            new Gem(5, "sapphire", "サファイア", 0x2050E0, 1, "necklace", List.of(add(ModAttributes.MAX_MANA, 6, "最大マナ"))),
            new Gem(6, "topaz", "トパーズ", 0xF0A020, 1, "ring", List.of(pct(ModAttributes.CRIT_CHANCE, 0.015, "会心率"))),
            new Gem(7, "crystal", "水晶", 0xE8E0F8, 1, "necklace", List.of(pct(ModAttributes.MAGIC_POWER, 0.025, "魔法の威力"))),
            new Gem(8, "moonstone", "ムーンストーン", 0xC8D8F8, 2, "necklace",
                    List.of(add(ModAttributes.MANA_REGEN, 0.2, "マナ回復/秒"), pct(ModAttributes.HEALING_POWER, 0.02, "回復の力"))),
            new Gem(9, "opal", "オパール", 0xF0E0F0, 2, "charm",
                    List.of(add(Attributes.LUCK, 0.3, "幸運"), pct(ModAttributes.JOB_XP_BONUS, 0.02, "職業経験値"))));

    // ================= 特別な装飾品（宝飾師だけ） =================

    /** special：能力値のほかに持つ効果（totem 死を一度防ぐ / night 暗視 / water 水中呼吸 / fall 落下ダメージなし） */
    public record Unique(int index, String id, String name, int kind, Supplier<Item> a, int aCount, Supplier<Item> b, int bCount,
                         int unlock, String special, String specialText, List<Stat> stats) {}

    public static final List<Unique> UNIQUES = List.of(
            new Unique(0, "wanderer_ring", "旅人の指輪", 0, () -> Items.ENDER_PEARL, 8, () -> Items.GOLD_INGOT, 4, 10,
                    "fall", "落下ダメージを受けない", List.of(mult(Attributes.MOVEMENT_SPEED, 0.1, "移動速度"))),
            new Unique(1, "harvest_charm", "豊穣のお守り", 4, () -> Items.GOLDEN_CARROT, 16, () -> Items.EMERALD, 8, 15,
                    "", "", List.of(pct(ModAttributes.HARVEST_FORTUNE, 0.3, "収穫の幸運"), pct(ModAttributes.MINING_FORTUNE, 0.2, "採掘の幸運"))),
            new Unique(2, "night_earring", "夜鳴きの耳飾り", 3, () -> Items.GLOW_INK_SAC, 8, () -> Items.PHANTOM_MEMBRANE, 4, 20,
                    "night", "暗いところでもよく見える", List.of(pct(ModAttributes.CRIT_CHANCE, 0.08, "会心率"))),
            new Unique(3, "sea_bracelet", "海神の腕輪", 2, () -> Items.HEART_OF_THE_SEA, 1, () -> Items.PRISMARINE_CRYSTALS, 16, 25,
                    "water", "水の中で息ができる", List.of(add(Attributes.ARMOR, 4, "防御力"), mult(Attributes.WATER_MOVEMENT_EFFICIENCY, 0.5, "水中の移動"))),
            new Unique(4, "phoenix_charm", "不死鳥のお守り", 4, () -> Items.TOTEM_OF_UNDYING, 1, () -> Items.BLAZE_ROD, 8, 35,
                    "totem", "倒れそうになっても一度だけ助かる（10分に1回）", List.of(add(Attributes.MAX_HEALTH, 4, "最大HP"))),
            new Unique(5, "echo_earring", "残響の耳飾り", 3, () -> Items.ECHO_SHARD, 4, () -> ModJewelry.MOON_SILVER.get(), 2, 45,
                    "", "", List.of(pct(ModAttributes.COOLDOWN_REDUCTION, 0.1, "待ち時間短縮"), pct(ModAttributes.CAST_SPEED, 0.1, "詠唱の速さ"))),
            new Unique(6, "dragon_necklace", "竜息の首飾り", 1, () -> Items.DRAGON_BREATH, 8, () -> ModJewelry.STAR_SILVER.get(), 2, 60,
                    "", "", List.of(pct(ModAttributes.MAGIC_POWER, 0.2, "魔法の威力"), add(ModAttributes.MAX_MANA, 40, "最大マナ"))),
            new Unique(7, "sovereign_ring", "覇者の指輪", 0, () -> Items.NETHER_STAR, 1, () -> ModJewelry.STAR_SILVER.get(), 2, 70,
                    "", "", List.of(add(Attributes.ATTACK_DAMAGE, 4, "攻撃力"), pct(ModAttributes.CRIT_CHANCE, 0.1, "会心率"), add(Attributes.MAX_HEALTH, 6, "最大HP"))),
            // ---- 追加：弱い物から強い物まで ----
            new Unique(8, "life_ring", "命の指輪", 0, () -> Items.APPLE, 16, () -> Items.IRON_INGOT, 4, 1,
                    "", "", List.of(add(Attributes.MAX_HEALTH, 4, "最大HP"))),
            new Unique(9, "gale_earring", "疾風の耳飾り", 3, () -> Items.FEATHER, 16, () -> Items.GOLD_INGOT, 2, 5,
                    "", "", List.of(mult(Attributes.MOVEMENT_SPEED, 0.08, "移動速度"), add(ModAttributes.MAX_STAMINA, 15, "最大スタミナ"))),
            new Unique(10, "miner_charm", "鉱夫のお守り", 4, () -> Items.REDSTONE, 16, () -> Items.IRON_INGOT, 4, 8,
                    "", "", List.of(mult(Attributes.BLOCK_BREAK_SPEED, 0.2, "掘る速さ"), pct(ModAttributes.MINING_FORTUNE, 0.15, "採掘の幸運"))),
            new Unique(11, "guardian_bracelet", "守りの腕輪", 2, () -> Items.OBSIDIAN, 8, () -> Items.IRON_BLOCK, 2, 12,
                    "", "", List.of(add(Attributes.ARMOR, 3, "防御力"), add(Attributes.ARMOR_TOUGHNESS, 2, "防具強度"), pct(ModAttributes.DAMAGE_REDUCTION, 0.05, "受けるダメージ減少"))),
            new Unique(12, "spring_necklace", "泉の首飾り", 1, () -> Items.LAPIS_LAZULI, 16, () -> Items.AMETHYST_SHARD, 8, 18,
                    "", "", List.of(add(ModAttributes.MAX_MANA, 30, "最大マナ"), add(ModAttributes.MANA_REGEN, 0.5, "マナ回復/秒"))),
            new Unique(13, "fury_ring", "狂戦の指輪", 0, () -> Items.BLAZE_POWDER, 16, () -> Items.GOLD_INGOT, 4, 25,
                    "", "", List.of(add(Attributes.ATTACK_DAMAGE, 3, "攻撃力"), pct(ModAttributes.CRIT_CHANCE, 0.05, "会心率"))),
            new Unique(14, "titan_bracelet", "巨人の腕輪", 2, () -> Items.GOLDEN_APPLE, 4, () -> ModJewelry.MOON_SILVER.get(), 2, 40,
                    "", "", List.of(add(Attributes.MAX_HEALTH, 20, "最大HP"), add(ModAttributes.HP_REGEN, 0.5, "HP回復/秒"))),
            new Unique(15, "star_earring", "星詠みの耳飾り", 3, () -> Items.ECHO_SHARD, 8, () -> ModJewelry.STAR_SILVER.get(), 2, 65,
                    "", "", List.of(pct(ModAttributes.COOLDOWN_REDUCTION, 0.15, "待ち時間短縮"), pct(ModAttributes.MAGIC_POWER, 0.15, "魔法の威力"), add(ModAttributes.MAX_MANA, 30, "最大マナ"))),
            new Unique(16, "world_tree_charm", "世界樹のお守り", 4, () -> Items.ENCHANTED_GOLDEN_APPLE, 1, () -> ModJewelry.STAR_SILVER.get(), 3, 85,
                    "", "", List.of(add(Attributes.MAX_HEALTH, 50, "最大HP"), add(ModAttributes.HP_REGEN, 1.0, "HP回復/秒")))));

    // ================= 便利 =================

    public static Kind kind(String id) {
        for (Kind k : KINDS) if (k.id().equals(id)) return k;
        return null;
    }

    public static Tier tierByMetal(Item item) {
        for (Tier t : TIERS) if (t.metal().get() == item) return t;
        return null;
    }

    public static Gem gem(String id) {
        for (Gem g : GEMS) if (g.id().equals(id)) return g;
        return null;
    }

    public static String num(double v) {
        return Math.abs(v - Math.round(v)) < 1e-6 ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }

    public static String format(Stat s, double mult) {
        double v = s.amount() * mult;
        return s.label() + " +" + (s.percent() ? num(v * 100) + "%" : num(v));
    }

    private Jewels() {}
}
