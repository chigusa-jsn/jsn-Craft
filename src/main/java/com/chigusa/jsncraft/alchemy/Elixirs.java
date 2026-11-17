package com.chigusa.jsncraft.alchemy;

import com.chigusa.jsncraft.farming.ModCrops;
import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 薬の種類（主素材で決まる）と、追加素材の一覧。
 * 調合の腕のレベル（unlock）は、調合師はそのレベル、錬金の賢者は 50＋レベル で数える。
 */
public final class Elixirs {

    /** 能力値アップ1つ（power の倍率がかかる） */
    public record Buff(Holder<Attribute> attr, double amount, AttributeModifier.Operation op, boolean percent, String label) {}

    /**
     * 薬の種類。kind：heal / stamina / mana / cure / elixir / ancient / buff / recall
     * amount：回復量（heal など）、seconds：効く時間（buff など）
     */
    public record Type(int index, String id, String name, String dustName, Supplier<Item> main, int unlock, int color,
                       String kind, double amount, double seconds, List<Buff> buffs) {
        public boolean selfOnly() {
            return kind.equals("recall");
        }

        public boolean timed() {
            return seconds > 0;
        }
    }

    /** 追加素材。kind：strong / long / throw / dust / extra（extra は id ごとの追加効果） */
    public record Additive(String id, Supplier<Item> item, String kind, String desc) {}

    private static final List<Type> TYPES_RW = new ArrayList<>();
    public static final List<Type> TYPES = Collections.unmodifiableList(TYPES_RW);
    private static final List<Additive> ADDS_RW = new ArrayList<>();
    public static final List<Additive> ADDITIVES = Collections.unmodifiableList(ADDS_RW);

    private static Buff add(Holder<Attribute> a, double v, String label) {
        return new Buff(a, v, AttributeModifier.Operation.ADD_VALUE, false, label);
    }

    private static Buff pct(Holder<Attribute> a, double v, String label) {
        return new Buff(a, v, AttributeModifier.Operation.ADD_VALUE, true, label);
    }

    private static Buff mult(Holder<Attribute> a, double v, String label) {
        return new Buff(a, v, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true, label);
    }

    private static void type(String id, String name, String dust, Supplier<Item> main, int unlock, int color, String kind,
                             double amount, double seconds, Buff... buffs) {
        TYPES_RW.add(new Type(TYPES_RW.size(), id, name, dust, main, unlock, color, kind, amount, seconds, List.of(buffs)));
    }

    private static Supplier<Item> crop(String name) {
        return () -> ModCrops.PRODUCE.get(name).get();
    }

    static {
        // ---- 回復 ----
        type("heal", "回復薬", "生命の粉塵", crop("herb"), 1, 0xE03040, "heal", 6, 0);
        type("stamina", "スタミナ回復薬", "活力の粉塵", crop("vigor_herb"), 1, 0xF0B020, "stamina", 40, 0);
        type("mana", "マナ回復薬", "魔力の粉塵", crop("moon_herb"), 5, 0x4080F0, "mana", 30, 0);
        type("cure", "万能薬", "浄化の粉塵", crop("antidote_herb"), 10, 0xA060E0, "cure", 2, 0);
        type("great_heal", "回復薬グレート", "大生命の粉塵", () -> Items.GLISTERING_MELON_SLICE, 20, 0xFF5070, "heal", 14, 0);
        type("elixir", "秘薬", "秘薬の粉塵", crop("mandrake"), 35, 0xFF80C0, "elixir", 40, 20);
        type("ancient", "いにしえの秘薬", "いにしえの粉塵", () -> Items.GOLDEN_APPLE, 65, 0xFFD040, "ancient", 8, 180);
        // ---- 強化 ----
        type("swift", "俊足薬", "疾風の粉塵", () -> Items.SUGAR, 5, 0x80E0F0, "buff", 0, 180,
                mult(Attributes.MOVEMENT_SPEED, 0.15, "移動速度"));
        type("might", "鬼人薬", "鬼人の粉塵", crop("fire_herb"), 10, 0xD03010, "buff", 0, 180,
                add(Attributes.ATTACK_DAMAGE, 2, "攻撃力"));
        type("armor", "硬化薬", "硬化の粉塵", () -> Items.IRON_NUGGET, 10, 0x8A8A9A, "buff", 0, 180,
                add(Attributes.ARMOR, 4, "防御力"), add(Attributes.ARMOR_TOUGHNESS, 2, "防具強度"));
        type("runner", "強走薬", "強走の粉塵", () -> Items.HONEY_BOTTLE, 15, 0xF0D040, "buff", 0, 120,
                add(ModAttributes.STAMINA_REGEN, 4, "スタミナ回復/秒"));
        type("crit", "会心薬", "会心の粉塵", () -> Items.FLINT, 15, 0xE8E8F0, "buff", 0, 180,
                pct(ModAttributes.CRIT_CHANCE, 0.1, "会心率"));
        type("arcane", "魔力増強薬", "魔導の粉塵", () -> Items.AMETHYST_SHARD, 20, 0xB070F0, "buff", 0, 180,
                pct(ModAttributes.MAGIC_POWER, 0.15, "魔法の威力"), add(ModAttributes.MANA_REGEN, 1, "マナ回復/秒"));
        type("focus", "集中薬", "集中の粉塵", crop("coffee"), 20, 0x6A4020, "buff", 0, 180,
                pct(ModAttributes.COOLDOWN_REDUCTION, 0.1, "待ち時間短縮"), pct(ModAttributes.CAST_SPEED, 0.1, "詠唱の速さ"));
        // ---- 生活 ----
        type("miner", "採掘の薬", "採掘の粉塵", () -> Items.RAW_COPPER, 15, 0xC08040, "buff", 0, 300,
                mult(Attributes.BLOCK_BREAK_SPEED, 0.3, "掘る速さ"), pct(ModAttributes.MINING_FORTUNE, 0.2, "採掘の幸運"));
        type("angler", "釣りの薬", "釣りの粉塵", () -> Items.KELP, 15, 0x30A080, "buff", 0, 300,
                pct(ModAttributes.FISHING_BITE, 0.3, "魚のかかる速さ"), pct(ModAttributes.FISHING_TREASURE, 0.05, "お宝の確率"));
        type("training", "修練の薬", "修練の粉塵", () -> Items.EMERALD, 25, 0x40D060, "buff", 0, 600,
                pct(ModAttributes.JOB_XP_BONUS, 0.2, "職業経験値"));
        type("luck", "豪運の薬", "豪運の粉塵", () -> Items.RABBIT_FOOT, 30, 0x50E050, "buff", 0, 300,
                add(Attributes.LUCK, 3, "幸運"), pct(ModAttributes.HARVEST_FORTUNE, 0.2, "収穫の幸運"));
        type("recall", "帰還薬", "", () -> Items.ENDER_PEARL, 25, 0x20A0A0, "recall", 0, 0);

        // ---- 追加素材 ----
        ADDS_RW.add(new Additive("strong", () -> Items.GLOWSTONE_DUST, "strong", "効き目 ×1.5"));
        ADDS_RW.add(new Additive("long", () -> Items.REDSTONE, "long", "効く時間 ×1.5"));
        ADDS_RW.add(new Additive("throw", () -> Items.GUNPOWDER, "throw", "投げる薬になる（当たった周りの仲間に効く）"));
        ADDS_RW.add(new Additive("dust", () -> ModAlchemy.DUST_BASE.get(), "dust", "粉塵になる（周りの仲間みんなに効く）"));
        ADDS_RW.add(new Additive("heal", crop("herb"), "extra", "HPも4回復"));
        ADDS_RW.add(new Additive("stamina", crop("vigor_herb"), "extra", "スタミナも20回復"));
        ADDS_RW.add(new Additive("mana", crop("moon_herb"), "extra", "マナも15回復"));
        ADDS_RW.add(new Additive("cure", crop("antidote_herb"), "extra", "悪い効果も消す"));
        ADDS_RW.add(new Additive("might", crop("fire_herb"), "extra", "攻撃力 +1（60秒）"));
        ADDS_RW.add(new Additive("regen", () -> Items.GHAST_TEAR, "extra", "再生II（10秒）"));
        ADDS_RW.add(new Additive("absorb", () -> Items.GOLDEN_APPLE, "extra", "衝撃吸収（60秒）"));
        ADDS_RW.add(new Additive("night", () -> Items.GOLDEN_CARROT, "extra", "暗視（3分）"));
        ADDS_RW.add(new Additive("fire", () -> Items.MAGMA_CREAM, "extra", "火炎耐性（3分）"));
        ADDS_RW.add(new Additive("water", () -> Items.PUFFERFISH, "extra", "水中呼吸（3分）"));
        ADDS_RW.add(new Additive("feather", () -> Items.PHANTOM_MEMBRANE, "extra", "落下速度低下（1分）"));
    }

    /** 追加効果のポーション効果（id → 効果, 秒, 強さ） */
    public static Object[] extraEffect(String id) {
        return switch (id) {
            case "regen" -> new Object[]{MobEffects.REGENERATION, 10, 1};
            case "absorb" -> new Object[]{MobEffects.ABSORPTION, 60, 0};
            case "night" -> new Object[]{MobEffects.NIGHT_VISION, 180, 0};
            case "fire" -> new Object[]{MobEffects.FIRE_RESISTANCE, 180, 0};
            case "water" -> new Object[]{MobEffects.WATER_BREATHING, 180, 0};
            case "feather" -> new Object[]{MobEffects.SLOW_FALLING, 60, 0};
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    public static Holder<MobEffect> effectOf(Object[] e) {
        return (Holder<MobEffect>) e[0];
    }

    public static Type type(String id) {
        for (Type t : TYPES) if (t.id().equals(id)) return t;
        return null;
    }

    public static Type byMain(Item item) {
        for (Type t : TYPES) if (t.main().get() == item) return t;
        return null;
    }

    public static Additive additive(String id) {
        for (Additive a : ADDITIVES) if (a.id().equals(id)) return a;
        return null;
    }

    public static Additive byAdditive(Item item) {
        for (Additive a : ADDITIVES) if (a.item().get() == item) return a;
        return null;
    }

    public static String num(double v) {
        return Math.abs(v - Math.round(v)) < 1e-6 ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }

    /** 効果の説明（power・time の倍率をかけた数字で） */
    public static List<String> describe(Type t, double power, double time) {
        List<String> out = new ArrayList<>();
        switch (t.kind()) {
            case "heal" -> out.add("HPを" + num(t.amount() * power) + "回復");
            case "stamina" -> out.add("スタミナを" + num(t.amount() * power) + "回復");
            case "mana" -> out.add("マナを" + num(t.amount() * power) + "回復");
            case "cure" -> out.add("悪い効果を消して、HPを" + num(t.amount() * power) + "回復");
            case "elixir" -> out.add("HPを" + num(t.amount() * power) + "回復＋再生II（" + num(t.seconds() * time) + "秒）");
            case "ancient" -> {
                out.add("HP・スタミナ・マナを全回復");
                out.add("最大HP +" + num(t.amount() * power) + "（" + time(t.seconds() * time) + "）");
            }
            case "recall" -> out.add("ベッド（なければ初めの場所）に帰る");
            default -> {
                for (Buff b : t.buffs()) {
                    double v = b.amount() * power;
                    out.add(b.label() + " +" + (b.percent() ? num(v * 100) + "%" : num(v)));
                }
                out.add("（" + time(t.seconds() * time) + "）");
            }
        }
        return out;
    }

    public static String time(double sec) {
        int s = (int) Math.round(sec);
        return s >= 60 ? (s / 60) + "分" + (s % 60 > 0 ? (s % 60) + "秒" : "") : s + "秒";
    }

    private Elixirs() {}
}
