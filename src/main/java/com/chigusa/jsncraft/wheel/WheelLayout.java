package com.chigusa.jsncraft.wheel;

/** ビッグホイールの並び：25マス（1×12、3×6、5×4、10×2、20×1）。上から時計回り */
public final class WheelLayout {
    public static final int[] VALUES = {1, 3, 5, 10, 20};
    public static final int[] SEGMENTS = {20, 1, 3, 1, 5, 1, 3, 1, 10, 1, 3, 1, 5, 1, 3, 1, 5, 1, 10, 1, 3, 1, 5, 3, 1};
    public static final float STEP = 360F / SEGMENTS.length;
    /** 色（1=灰 3=緑 5=青 10=紫 20=金） */
    public static final int[] COLORS = {0xFFB0B0B0, 0xFF40C060, 0xFF4080F0, 0xFFB050E0, 0xFFF0C030};

    public static int index(int value) {
        for (int i = 0; i < VALUES.length; i++) if (VALUES[i] == value) return i;
        return 0;
    }

    private WheelLayout() {}
}
