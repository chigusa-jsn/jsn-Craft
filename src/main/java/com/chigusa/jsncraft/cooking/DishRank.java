package com.chigusa.jsncraft.cooking;

import net.minecraft.ChatFormatting;

/** 料理のランク（料理図鑑用）。 */
public enum DishRank {
    NORMAL("ふつう", ChatFormatting.WHITE, 0xFF8A6A4A),
    FEAST("ごちそう", ChatFormatting.GREEN, 0xFF3A9A3A),
    FINE("逸品", ChatFormatting.AQUA, 0xFF30A0D0),
    LEGEND("伝説", ChatFormatting.GOLD, 0xFFE0B020);

    public final String label;
    public final ChatFormatting color;
    public final int frameColor;

    DishRank(String label, ChatFormatting color, int frameColor) {
        this.label = label;
        this.color = color;
        this.frameColor = frameColor;
    }

    public static DishRank byName(String name) {
        for (DishRank r : values()) if (r.name().equalsIgnoreCase(name)) return r;
        return NORMAL;
    }

    public static DishRank byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : NORMAL;
    }
}
