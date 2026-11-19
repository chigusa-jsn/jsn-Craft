package com.chigusa.jsncraft.casino.pachi;

import java.util.ArrayList;
import java.util.List;

/**
 * パチスロのリール：21コマ×3、5ライン、役、すべり（最大4コマ）の止め方。
 * 絵柄：0=7  1=BAR  2=ベル  3=ぶどう  4=チェリー  5=リプレイ  6=スイカ
 * 画面の見え方：上の段＝中段の1つ前（index-1）、下の段＝index+1。リールは上から下へ流れるので、
 * 押したあとは index が 1 ずつ減る方向にすべる。
 */
public final class Reels {
    public static final int SEVEN = 0, BAR = 1, BELL = 2, GRAPE = 3, CHERRY = 4, REPLAY = 5, MELON = 6;
    public static final int SIZE = 21, SLIP = 4;

    public static final int[][] STRIP = {
            {3, 0, 5, 4, 3, 1, 5, 4, 3, 2, 5, 0, 3, 4, 5, 1, 3, 6, 5, 2, 6},
            {3, 0, 6, 5, 3, 1, 2, 5, 3, 0, 6, 5, 3, 2, 1, 5, 3, 6, 2, 5, 1},
            {3, 5, 0, 1, 3, 5, 6, 2, 3, 5, 0, 6, 3, 5, 2, 1, 3, 5, 0, 6, 2}};

    /** ライン（左・中・右の段。0=上 1=中 2=下）。かけ枚数で 1/3/5 本 */
    public static final int[][] LINES = {{1, 1, 1}, {0, 0, 0}, {2, 2, 2}, {0, 1, 2}, {2, 1, 0}};

    public static int lines(int bet) {
        return bet >= 3 ? 5 : bet == 2 ? 3 : 1;
    }

    /** 役 */
    public enum Role {
        NONE, REPLAY, GRAPE, BELL, MELON, CHERRY, BIG, REG;

        /** この役の絵柄（左・中・右）。チェリーは左だけ */
        int[] symbols() {
            return switch (this) {
                case REPLAY -> new int[]{Reels.REPLAY, Reels.REPLAY, Reels.REPLAY};
                case GRAPE -> new int[]{Reels.GRAPE, Reels.GRAPE, Reels.GRAPE};
                case BELL -> new int[]{Reels.BELL, Reels.BELL, Reels.BELL};
                case MELON -> new int[]{Reels.MELON, Reels.MELON, Reels.MELON};
                case BIG -> new int[]{Reels.SEVEN, Reels.SEVEN, Reels.SEVEN};
                case REG -> new int[]{Reels.SEVEN, Reels.SEVEN, Reels.BAR};
                default -> null;
            };
        }
    }

    public static int at(int reel, int index) {
        return STRIP[reel][Math.floorMod(index, SIZE)];
    }

    /** 見えている絵柄（段 row の） */
    public static int visible(int reel, int stop, int row) {
        return at(reel, stop + row - 1);
    }

    /** 全部止まったときにそろった役（ライン番号つき） */
    public record Win(Role role, int line) {}

    public static List<Win> wins(int[] stops, int bet) {
        List<Win> out = new ArrayList<>();
        boolean cherry = false;
        for (int l = 0; l < lines(bet); l++) {
            int[] rows = LINES[l];
            int a = visible(0, stops[0], rows[0]), b = visible(1, stops[1], rows[1]), c = visible(2, stops[2], rows[2]);
            for (Role r : Role.values()) {
                int[] s = r.symbols();
                if (s != null && s[0] == a && s[1] == b && s[2] == c) out.add(new Win(r, l));
            }
            if (a == Reels.CHERRY && !cherry) {
                cherry = true;
                out.add(new Win(Role.CHERRY, l));
            }
        }
        return out;
    }

    /**
     * すべりの止め方：押した位置 pressed から最大4コマすべって、どこで止めるかを決める。
     * stops は止まっているリール（-1 はまだ）。target は今回ねらう役（小役 or 持ち越しのボーナス）、
     * allowed はそろってよい役。
     */
    public static int control(int reel, int pressed, int[] stops, int bet, Role target, Role bonusHeld) {
        return control(reel, pressed, stops, bet, target, bonusHeld, SLIP);
    }

    /** slip を大きくすると、どこで押してもそろう（オート用） */
    public static int control(int reel, int pressed, int[] stops, int bet, Role target, Role bonusHeld, int slip) {
        int best = pressed, bestScore = Integer.MIN_VALUE;
        boolean last = true;
        for (int r = 0; r < 3; r++) if (r != reel && stops[r] < 0) last = false;
        for (int k = 0; k <= slip; k++) {
            int cand = Math.floorMod(pressed - k, SIZE);
            int[] s = stops.clone();
            s[reel] = cand;
            int score = -k;   // すべりは少ないほうがいい
            if (last) {
                List<Win> w = wins(s, bet);
                boolean gotTarget = false, gotBonus = false, bad = false;
                for (Win x : w) {
                    if (x.role() == target) gotTarget = true;
                    else if (x.role() == bonusHeld && bonusHeld != Role.NONE) gotBonus = true;
                    else bad = true;
                }
                if (bad) score -= 1000;
                if (gotTarget) score += 200;
                if (gotBonus) score += 150;
            } else {
                // チェリーは左リールだけで決まる
                if (reel == 0) {
                    boolean cherryOn = false;
                    for (int l = 0; l < lines(bet); l++) if (visible(0, cand, LINES[l][0]) == CHERRY) cherryOn = true;
                    if (cherryOn && target != Role.CHERRY) score -= 1000;
                    if (cherryOn && target == Role.CHERRY) score += 200;
                }
                if (target != Role.NONE && target != Role.CHERRY && reachable(s, bet, target)) score += 100;
                if (bonusHeld != Role.NONE && (target == Role.NONE || target == Role.CHERRY) && reachable(s, bet, bonusHeld)) score += 60;
            }
            if (score > bestScore) {
                bestScore = score;
                best = cand;
            }
        }
        return best;
    }

    /** 止まっているリールだけ見て、その役がまだそろう見込みがあるか */
    private static boolean reachable(int[] stops, int bet, Role role) {
        int[] sym = role.symbols();
        if (sym == null) return false;
        for (int l = 0; l < lines(bet); l++) {
            boolean ok = true;
            for (int r = 0; r < 3 && ok; r++) if (stops[r] >= 0 && visible(r, stops[r], LINES[l][r]) != sym[r]) ok = false;
            if (ok) return true;
        }
        return false;
    }

    private Reels() {}
}
