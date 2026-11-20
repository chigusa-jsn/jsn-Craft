package com.chigusa.jsncraft.chinchiro;

import java.util.Arrays;

/** チンチロの役：rank が大きいほど強い。mult は勝ったときの倍率 */
public record Hand(int rank, int mult, String name) {
    public static final Hand NONE = new Hand(0, 1, "目なし");

    public static Hand of(int a, int b, int c) {
        int[] d = {a, b, c};
        Arrays.sort(d);
        if (d[0] == 1 && d[1] == 1 && d[2] == 1) return new Hand(100, 5, "ピンゾロ");
        if (d[0] == d[1] && d[1] == d[2]) return new Hand(90 + d[0], 3, "ゾロ目（" + d[0] + "）");
        if (d[0] == 4 && d[1] == 5 && d[2] == 6) return new Hand(80, 2, "シゴロ");
        if (d[0] == 1 && d[1] == 2 && d[2] == 3) return new Hand(-10, 2, "ヒフミ");
        if (d[0] == d[1]) return new Hand(d[2], 1, d[2] + "の目");
        if (d[1] == d[2]) return new Hand(d[0], 1, d[0] + "の目");
        return null;   // 役なし：ふり直し
    }

    /** 親の即決：勝ち(1)・負け(-1)・勝負(0) */
    public int parentInstant() {
        if (rank >= 6) return 1;
        if (rank <= 1) return -1;
        return 0;
    }
}
