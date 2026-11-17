package com.chigusa.jsncraft.enchanting;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * 魔紋（jsn-Craft オリジナルのエンチャント）の一覧。
 * kinds：刻める物（W 近接武器 / B 弓・クロスボウ / A 防具 / T 道具）。
 * unlock：刻めるようになる付与の腕のレベル（付与術師はそのレベル、刻印師は 50＋レベル）。
 * レベル n を刻むには unlock + (n-1)×20 が必要。
 */
public final class Runes {

    public record Rune(int index, String id, String name, String kinds, Item catalyst, int maxLevel, int unlock,
                       IntFunction<String> effect) {
        public int requiredLevel(int level) {
            return unlock + (level - 1) * 20;
        }

        public String category() {
            return switch (kinds) {
                case "W" -> "近接武器";
                case "B" -> "弓・クロスボウ";
                case "A" -> "防具";
                case "T" -> "道具";
                case "WBA" -> "武器・防具（魔法）";
                default -> "なんでも";
            };
        }

        public boolean fits(String itemKinds) {
            for (char c : itemKinds.toCharArray()) if (kinds.indexOf(c) >= 0) return true;
            return false;
        }
    }

    public static final Map<String, Rune> BY_ID = new LinkedHashMap<>();
    private static final List<Rune> LIST_RW = new ArrayList<>();
    public static final List<Rune> LIST = Collections.unmodifiableList(LIST_RW);

    private static String pct(double v) {
        return (Math.abs(v - Math.round(v)) < 1e-6) ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }

    private static void add(String id, String name, String kinds, Item catalyst, int max, int unlock, IntFunction<String> effect) {
        Rune r = new Rune(LIST_RW.size(), id, name, kinds, catalyst, max, unlock, effect);
        LIST_RW.add(r);
        BY_ID.put(id, r);
    }

    static {
        // ---- 近接武器 ----
        add("frost", "氷結", "W", Items.BLUE_ICE, 3, 1, l -> "当てた敵を" + (2 + l) + "秒間 鈍くする" + (l >= 3 ? "（鈍足II）" : ""));
        add("vampire", "吸血", "W", Items.FERMENTED_SPIDER_EYE, 3, 5, l -> "与えたダメージの" + 4 * l + "%だけHPが回復");
        add("inferno", "業火", "W", Items.BLAZE_POWDER, 3, 10, l -> "敵を燃やし、燃えている敵へのダメージ +" + 10 * l + "%");
        add("thunder", "雷撃", "W", Items.LIGHTNING_ROD, 3, 15, l -> "攻撃の" + 8 * l + "%で落雷（+" + 4 * l + "ダメージ）");
        add("execute", "処刑", "W", Items.FLINT, 3, 20, l -> "HPが30%以下の敵へのダメージ +" + 20 * l + "%");
        add("combo", "連撃", "W", Items.GOLD_INGOT, 3, 25, l -> "攻撃の" + 7 * l + "%で、もう一撃（ダメージ50%）");
        add("wave", "斬撃波", "W", Items.BREEZE_ROD, 3, 30, l -> "当てた敵の周りの敵にも" + 20 * l + "%のダメージ");
        add("reaper", "魂狩り", "W", Items.SOUL_SAND, 3, 40, l -> "倒した敵の経験値 +" + 50 * l + "%、倒すとHPが" + l + "回復");
        // ---- 弓・クロスボウ ----
        add("recovery", "回収", "B", Items.STRING, 3, 1, l -> "当たった矢（敵でも地面でも）が" + 30 * l + "%で持ち物に戻る。光の矢・効果つきの矢も");
        add("homing", "追尾", "B", Items.ENDER_EYE, 3, 10, l -> "矢が近くの敵（" + (8 + 4 * l) + "ブロック）を追いかける");
        add("thunder_arrow", "雷矢", "B", Items.GLOWSTONE_DUST, 3, 15, l -> "敵に当たると" + 20 * l + "%で落雷（+" + 4 * l + "ダメージ）");
        add("split", "分裂矢", "B", Items.PRISMARINE_SHARD, 3, 25, l -> "少しずつ時間をずらして、矢がもう" + l + "本飛ぶ");
        add("blast", "爆裂矢", "B", Items.TNT, 3, 30, l -> "当たると小さく爆発する（威力" + pct(0.6 + 0.4 * l) + "、地形は壊れない）");
        // ---- 防具 ----
        add("swift", "疾風", "A", Items.SUGAR, 3, 1, l -> "移動速度 +" + 4 * l + "%");
        add("night_eye", "夜目", "A", Items.GOLDEN_CARROT, 1, 1, l -> "暗いところでもよく見える（暗視）");
        add("feather", "羽根", "A", Items.FEATHER, 3, 5, l -> "落下ダメージ -" + 20 * l + "%");
        add("leap", "跳躍", "A", Items.SLIME_BLOCK, 3, 10, l -> "ジャンプ力アップ、落下ダメージを受けない高さ +" + l);
        add("regen", "再生", "A", Items.GHAST_TEAR, 3, 15, l -> "HPが毎秒" + pct(0.1 * l) + "回復");
        add("tenacity", "不屈", "A", Items.OBSIDIAN, 3, 20, l -> "ノックバック耐性 +" + 10 * l + "%、受けるダメージ -" + 2 * l + "%");
        add("counter", "反撃", "A", Items.CACTUS, 3, 25, l -> "近接攻撃を受けると、" + 15 * l + "%のダメージを返す");
        // ---- 道具 ----
        add("magnet", "磁力", "T", Items.REDSTONE, 2, 1, l -> l >= 2 ? "掘った物が直接持ち物へ＋手に持つと周りの物を引き寄せる" : "掘った物と経験値が直接持ち物へ");
        add("wisdom", "経験値増幅", "T", Items.EXPERIENCE_BOTTLE, 3, 5, l -> "掘ったときの経験値 +" + 50 * l + "%");
        add("smelt", "自動精錬", "T", Items.MAGMA_BLOCK, 1, 10, l -> "掘った物が焼かれて出てくる（鉄鉱石→鉄インゴットなど）");
        add("area", "範囲掘り", "T", Items.PISTON, 3, 20, l -> (l == 1 ? "十字に5マス" : l == 2 ? "3×3" : "5×5") + "まとめて掘る（しゃがむと1マス）");
        add("vein", "鉱脈", "T", Items.RAW_IRON, 3, 30, l -> "しゃがんで鉱石を掘ると、つながった鉱石を" + (4 << (l - 1)) + "個まで掘る");
        add("self_repair", "自己修復", "WBAT", Items.EMERALD, 3, 30, l -> "持っているだけで2秒ごとに耐久が" + l + "回復");
        // ---- 魔法（武器・防具） ----
        add("mana", "魔力", "WBA", Items.PRISMARINE_CRYSTALS, 3, 1, l -> "最大マナ +" + 10 * l);
        add("meditation", "瞑想", "WBA", Items.GLOW_BERRIES, 3, 5, l -> "マナ回復 +" + pct(0.4 * l) + "/秒");
        add("arcane", "魔導", "WBA", Items.AMETHYST_SHARD, 3, 10, l -> "魔法（スキル）の威力 +" + 6 * l + "%");
        add("resonance", "共鳴", "WBA", Items.CLOCK, 3, 15, l -> "スキルの待ち時間 -" + 3 * l + "%");
        add("chant", "詠唱", "WBA", Items.BOOK, 3, 20, l -> "ため（詠唱）時間 -" + 8 * l + "%");
        add("mercy", "慈愛", "WBA", Items.GLISTERING_MELON_SLICE, 3, 25, l -> "回復スキルの回復量 +" + 8 * l + "%");
        // ---- なんでも ----
        add("luck", "幸運", "WBAT", Items.RABBIT_FOOT, 3, 15, l -> "幸運 +" + l + "、採掘の幸運 +" + 5 * l + "%");
        add("soulbound", "魂結び", "WBAT", Items.ECHO_SHARD, 1, 45, l -> "死んでもなくならず、手元に残る");
    }

    public static Rune get(String id) {
        return BY_ID.get(id);
    }

    public static Rune get(int index) {
        return index >= 0 && index < LIST.size() ? LIST.get(index) : null;
    }

    /** その触媒で刻める魔紋（ツールチップ用） */
    public static List<Rune> byCatalyst(Item item) {
        List<Rune> out = new ArrayList<>();
        for (Rune r : LIST) if (r.catalyst() == item) out.add(r);
        return out;
    }

    public static String roman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    // ---- 刻むのに必要な物（スキルで減る前の値） ----

    public static int lapisCost(int level) {
        return 2 + 3 * level;
    }

    public static int xpCost(int level) {
        return 1 + 2 * level;
    }

    public static int catalystCost(int level) {
        return level;
    }

    private Runes() {}
}
