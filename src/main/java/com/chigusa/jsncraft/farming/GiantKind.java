package com.chigusa.jsncraft.farming;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** 巨大作物の種類。 */
public enum GiantKind implements StringRepresentable {
    WHEAT("wheat", "巨大小麦", 0xDCBE5A),
    CARROT("carrot", "巨大ニンジン", 0xF08228),
    POTATO("potato", "巨大じゃがいも", 0xC8A05A),
    BEETROOT("beetroot", "巨大ビーツ", 0x961E32),
    RICE("rice", "巨大稲", 0xF0EBD2),
    TOMATO("tomato", "巨大トマト", 0xDC3228),
    CABBAGE("cabbage", "巨大キャベツ", 0x82C85A),
    ONION("onion", "巨大玉ねぎ", 0xDCB478);

    public final String id, name;
    public final int color;

    GiantKind(String id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** 作物のブロック → 巨大作物の種類（なれない作物は null） */
    public static GiantKind of(Block b) {
        if (b == Blocks.WHEAT) return WHEAT;
        if (b == Blocks.CARROTS) return CARROT;
        if (b == Blocks.POTATOES) return POTATO;
        if (b == Blocks.BEETROOTS) return BEETROOT;
        for (GiantKind k : new GiantKind[]{RICE, TOMATO, CABBAGE, ONION}) {
            var cb = ModCrops.CROP_BLOCKS.get(k.id);
            if (cb != null && cb.get() == b) return k;
        }
        return null;
    }

    /** 取れる物 */
    public Item produce() {
        return switch (this) {
            case WHEAT -> Items.WHEAT;
            case CARROT -> Items.CARROT;
            case POTATO -> Items.POTATO;
            case BEETROOT -> Items.BEETROOT;
            default -> ModCrops.PRODUCE.get(id).get();
        };
    }

    /** 別に種がある作物の種（なければ null） */
    public Item seed() {
        return switch (this) {
            case WHEAT -> Items.WHEAT_SEEDS;
            case BEETROOT -> Items.BEETROOT_SEEDS;
            case TOMATO, CABBAGE -> ModCrops.SEEDS.get(id).get();
            default -> null;
        };
    }
}
