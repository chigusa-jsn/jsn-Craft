package com.chigusa.jsncraft.fishing;

import net.minecraft.ChatFormatting;

/** 魚のランク。難しさ・出やすさ・職業経験値の倍率・ミニゲームの枠の色が決まる。 */
public enum FishRank {
    COMMON("コモン", ChatFormatting.WHITE, 20, 10, 1.0, 0xFF6A4A2A),
    UNCOMMON("アンコモン", ChatFormatting.GREEN, 35, 5, 1.5, 0xFF3A9A3A),
    RARE("レア", ChatFormatting.BLUE, 50, 2.5, 2.5, 0xFF3070E0),
    EPIC("エピック", ChatFormatting.LIGHT_PURPLE, 65, 1.0, 4.0, 0xFFA040D0),
    LEGEND("レジェンド", ChatFormatting.GOLD, 85, 0.4, 8.0, 0xFFE0B020);

    public final String label;
    public final ChatFormatting color;
    public final int difficulty;
    public final double weight;
    public final double xpMultiplier;
    public final int frameColor;

    FishRank(String label, ChatFormatting color, int difficulty, double weight, double xpMultiplier, int frameColor) {
        this.label = label;
        this.color = color;
        this.difficulty = difficulty;
        this.weight = weight;
        this.xpMultiplier = xpMultiplier;
        this.frameColor = frameColor;
    }

    public static FishRank byName(String name) {
        for (FishRank r : values()) if (r.name().equalsIgnoreCase(name)) return r;
        return COMMON;
    }

    public static FishRank byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : COMMON;
    }
}
