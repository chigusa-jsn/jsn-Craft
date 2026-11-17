package com.chigusa.jsncraft.alchemy;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 錬金術（錬金の賢者だけ）：素材を別の素材に変える。
 * unlock は錬金の賢者のレベル。格上げは「損をする交換」にしてある。
 */
public final class Transmutes {

    public record Recipe(int index, String group, Supplier<Item> a, int aCount, Supplier<Item> b, int bCount,
                         Supplier<Item> out, int outCount, int unlock) {
        public boolean hasB() {
            return b != null;
        }

        /** 使う経験値レベル */
        public int xp() {
            return 1 + unlock / 10;
        }
    }

    private static final List<Recipe> LIST_RW = new ArrayList<>();
    public static final List<Recipe> LIST = Collections.unmodifiableList(LIST_RW);

    private static Supplier<Item> v(Item i) {
        return () -> i;
    }

    private static Supplier<Item> jsn(String name) {
        return () -> BuiltInRegistries.ITEM.get(JsnCraft.id(name));
    }

    private static void r(String group, Supplier<Item> a, int an, Supplier<Item> out, int on, int unlock) {
        LIST_RW.add(new Recipe(LIST_RW.size(), group, a, an, null, 0, out, on, unlock));
    }

    private static void r2(String group, Supplier<Item> a, int an, Supplier<Item> b, int bn, Supplier<Item> out, int on, int unlock) {
        LIST_RW.add(new Recipe(LIST_RW.size(), group, a, an, b, bn, out, on, unlock));
    }

    static {
        // ---- 鉱石の格上げ・格下げ ----
        r("格上げ", v(Items.COAL), 4, v(Items.COPPER_INGOT), 1, 1);
        r("格上げ", v(Items.COPPER_INGOT), 6, v(Items.IRON_INGOT), 1, 1);
        r("格下げ", v(Items.IRON_INGOT), 1, v(Items.COPPER_INGOT), 3, 1);
        r("格上げ", v(Items.REDSTONE), 12, v(Items.LAPIS_LAZULI), 4, 5);
        r("格上げ", v(Items.IRON_INGOT), 8, v(Items.GOLD_INGOT), 1, 10);
        r("格下げ", v(Items.GOLD_INGOT), 1, v(Items.IRON_INGOT), 4, 10);
        r("格上げ", v(Items.LAPIS_LAZULI), 10, v(Items.EMERALD), 1, 20);
        r("格下げ", v(Items.EMERALD), 1, v(Items.LAPIS_LAZULI), 4, 20);
        r("格上げ", v(Items.GOLD_INGOT), 8, v(Items.DIAMOND), 1, 30);
        r("格下げ", v(Items.DIAMOND), 1, v(Items.GOLD_INGOT), 4, 30);
        r("格上げ", v(Items.DIAMOND), 4, v(Items.NETHERITE_SCRAP), 1, 60);
        // ---- 薬草と料理 ----
        r("薬草", jsn("herb"), 3, jsn("vigor_herb"), 1, 1);
        r("薬草", jsn("herb"), 3, jsn("antidote_herb"), 1, 1);
        r("薬草", jsn("herb"), 4, jsn("moon_herb"), 1, 5);
        r("薬草", jsn("herb"), 4, jsn("fire_herb"), 1, 5);
        r("薬草", jsn("herb"), 16, jsn("mandrake"), 1, 50);
        r2("薬草", v(Items.BONE_MEAL), 4, jsn("herb"), 1, jsn("dust_base"), 3, 1);
        r("料理", v(Items.SAND), 4, jsn("salt"), 2, 1);
        // ---- 細工師の宝石（原石） ----
        r("宝石", v(Items.COAL_BLOCK), 2, jsn("raw_onyx"), 1, 15);
        r("宝石", v(Items.NETHER_WART), 8, jsn("raw_garnet"), 1, 15);
        r("宝石", v(Items.EMERALD), 1, jsn("raw_peridot"), 2, 15);
        r("宝石", v(Items.PRISMARINE_CRYSTALS), 8, jsn("raw_aquamarine"), 1, 15);
        r("宝石", v(Items.REDSTONE_BLOCK), 1, jsn("raw_ruby"), 1, 20);
        r("宝石", v(Items.LAPIS_BLOCK), 1, jsn("raw_sapphire"), 1, 20);
        r("宝石", v(Items.GOLD_INGOT), 4, jsn("raw_topaz"), 1, 20);
        r("宝石", v(Items.AMETHYST_SHARD), 8, jsn("raw_crystal"), 1, 20);
        r2("宝石", v(Items.QUARTZ), 12, v(Items.GLOWSTONE_DUST), 4, jsn("raw_moonstone"), 1, 30);
        r2("宝石", v(Items.GLOW_INK_SAC), 2, v(Items.QUARTZ), 8, jsn("raw_opal"), 1, 35);
        // ---- 鍛冶師・付与術師の素材 ----
        r("鍛冶", jsn("enhance_stone_iron"), 3, jsn("enhance_stone_gold"), 1, 15);
        r("鍛冶", jsn("enhance_stone_gold"), 3, jsn("enhance_stone_diamond"), 1, 25);
        r("鍛冶", jsn("enhance_stone_diamond"), 3, jsn("enhance_stone_netherite"), 1, 45);
        r2("鍛冶", jsn("enhance_stone_netherite"), 3, v(Items.NETHERITE_SCRAP), 1, jsn("enhance_stone_star"), 1, 80);
        r2("付与", v(Items.QUARTZ), 8, v(Items.SOUL_SAND), 4, v(Items.GHAST_TEAR), 1, 35);
        r("付与", v(Items.WIND_CHARGE), 8, v(Items.BREEZE_ROD), 1, 40);
        r2("付与", v(Items.AMETHYST_SHARD), 16, v(Items.SCULK), 8, v(Items.ECHO_SHARD), 1, 70);
    }

    public static Recipe get(int i) {
        return i >= 0 && i < LIST.size() ? LIST.get(i) : null;
    }

    private Transmutes() {}
}
