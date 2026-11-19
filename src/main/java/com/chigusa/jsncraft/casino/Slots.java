package com.chigusa.jsncraft.casino;

import net.minecraft.util.RandomSource;

/**
 * スロットの絵柄と配当（還元率は約94%。tools で計算）。
 * 0:7  1:ダイヤ  2:エメラルド  3:金リンゴ  4:スイカ  5:ベリー  6:骨（はずれ）
 */
public final class Slots {
    public static final String[] NAMES = {"7", "ダイヤ", "エメラルド", "金リンゴ", "スイカ", "ベリー", "骨"};
    static final int[] WEIGHT = {2, 4, 6, 8, 12, 12, 20};
    /** 3つそろったときの倍率 */
    public static final int[] THREE = {500, 100, 40, 20, 10, 8, 0};
    public static final int BERRY = 5, BONE = 6;
    /** 宝石（7・ダイヤ・エメラルド）ばかりでバラバラ */
    public static final int MIXED_GEMS = 15;
    public static final int[] BETS = {1, 5, 10, 50, 100};

    static int roll(RandomSource r) {
        int total = 0;
        for (int w : WEIGHT) total += w;
        int x = r.nextInt(total);
        for (int i = 0; i < WEIGHT.length; i++) {
            x -= WEIGHT[i];
            if (x < 0) return i;
        }
        return BONE;
    }

    /** 倍率（0ならはずれ） */
    public static int multiplier(int a, int b, int c) {
        if (a == b && b == c) return THREE[a];
        int berries = (a == BERRY ? 1 : 0) + (b == BERRY ? 1 : 0) + (c == BERRY ? 1 : 0);
        if (berries == 1) return 1;
        if (berries == 2) return 3;
        if (a <= 2 && b <= 2 && c <= 2) return MIXED_GEMS;
        return 0;
    }

    private Slots() {}
}
