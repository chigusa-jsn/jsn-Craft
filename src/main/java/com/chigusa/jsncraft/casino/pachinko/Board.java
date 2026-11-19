package com.chigusa.jsncraft.casino.pachinko;

import java.util.ArrayList;
import java.util.List;

/**
 * パチンコの盤と玉の動き。サーバー（結果を決める）と画面（玉を動かす）で同じ計算をするので、
 * StrictMath と自前の乱数だけを使う（どのパソコンでも同じ結果になる）。
 * 座標は 幅200×高さ230、y は下向き。
 */
public final class Board {
    public static final int W = 200, H = 230;
    public static final double CX = 100, CY = 118, R = 110, BR = 2.0, PR = 1.2, G = 0.06, REST = 0.45;
    /** 右の道を分ける壁（x=150、y 70〜200） */
    public static final double WALL_X = 150, WALL_Y0 = 70, WALL_Y1 = 200;
    /** 結果 */
    public static final int OUT = 0, HESO = 1, LEFT = 2, RIGHT = 3, ATTACKER = 4, DENCHU = 5;
    /** ポケット：x0, x1, y */
    public static final double[][] POCKET = {{0, 0, 0}, {97.5, 102.5, 150}, {30, 40, 175}, {178, 186, 175}, {156, 186, 195}, {170, 176, 150}};

    /** 真ん中の液晶（玉ははね返る） */
    /** 液晶の上に乗った玉が、ステージから真ん中に落ちる確率 */
    public static final double STAGE = 0.4;
    public static final double LCD_X0 = 64, LCD_Y0 = 50, LCD_X1 = 136, LCD_Y1 = 114;

    public static final List<double[]> PEGS = makePegs();

    private static List<double[]> makePegs() {
        List<double[]> p = new ArrayList<>();
        for (int row = 0; row < 9; row++) {
            int y = 40 + row * 11, off = row % 2 == 1 ? 7 : 0;
            for (int x = 14 + off; x < 190; x += 14) {
                if (x > 62 && x < 138 && y > 45 && y < 120) continue;   // 液晶のところは空ける
                p.add(new double[]{x, y});
            }
        }
        for (int i = 0; i < 14; i++) {   // 液晶のまわり
            double a = StrictMath.PI * (1.05 + 0.9 * i / 13.0);
            p.add(new double[]{100 + 44 * StrictMath.cos(a), 82 + 38 * StrictMath.sin(a)});
        }
        for (int row = 0; row < 5; row++) {
            int y = 128 + row * 10, off = row % 2 == 1 ? 7 : 0;
            for (int x = 14 + off; x < 190; x += 14) {
                if (Math.abs(x - 100) < 10 && y > 140) continue;
                p.add(new double[]{x, y});
            }
        }
        p.add(new double[]{95.5, 143});   // 命釘
        p.add(new double[]{104.5, 143});
        for (double[] c : new double[][]{{40, 125}, {160, 125}}) {   // 風車
            for (int a = 0; a < 360; a += 90) p.add(new double[]{c[0] + 4 * StrictMath.cos(StrictMath.toRadians(a)), c[1] + 4 * StrictMath.sin(StrictMath.toRadians(a))});
        }
        return p;
    }

    /** 自前の乱数（xorshift） */
    static final class Rng {
        long s;
        Rng(long seed) { s = seed == 0 ? 0x9E3779B97F4A7C15L : seed; }
        double next() {
            s ^= s << 13; s ^= s >>> 7; s ^= s << 17;
            return (s >>> 11) * 0x1.0p-53;
        }
    }

    /**
     * 玉を1つ打つ。strength は 0〜1（強いほど右へ）。path があれば、1ティックごとの位置を入れる。
     */
    public static int shoot(long seed, double strength, boolean attackerOpen, boolean denchuOpen, List<float[]> path) {
        Rng rnd = new Rng(seed);
        double th = StrictMath.toRadians(205 + strength * 130 + (rnd.next() - 0.5) * 6);
        double r = R - BR - 1;
        double x = CX + r * StrictMath.cos(th), y = CY + r * StrictMath.sin(th);
        boolean onStage = false;
        double sp = 1.6, vx = -StrictMath.sin(th) * sp, vy = StrictMath.cos(th) * sp;
        for (int step = 0; step < 4000; step++) {
            vy += G;
            x += vx;
            y += vy;
            double dx = x - CX, dy = y - CY, d = StrictMath.sqrt(dx * dx + dy * dy);
            if (d > R - BR && y < CY) {
                double nx = dx / d, ny = dy / d;
                x = CX + nx * (R - BR);
                y = CY + ny * (R - BR);
                double dot = vx * nx + vy * ny;
                if (dot > 0) { vx -= 2 * dot * nx * 0.8; vy -= 2 * dot * ny * 0.8; }
            }
            if (y > WALL_Y0 && y < WALL_Y1 && Math.abs(x - WALL_X) < BR + 1) {
                if (vx > 0 && x < WALL_X) { x = WALL_X - BR - 1; vx = -Math.abs(vx) * 0.4; }
                else if (vx < 0 && x > WALL_X) { x = WALL_X + BR + 1; vx = Math.abs(vx) * 0.4; }
            }
            // 液晶
            if (x > LCD_X0 - BR && x < LCD_X1 + BR && y > LCD_Y0 - BR && y < LCD_Y1 + BR) {
                double dl = x - (LCD_X0 - BR), dr = (LCD_X1 + BR) - x, dt = y - (LCD_Y0 - BR), db = (LCD_Y1 + BR) - y;
                double m = Math.min(Math.min(dl, dr), Math.min(dt, db));
                if (m == dt) {
                    // ステージ：液晶の上に乗った玉は、ときどき真ん中から落ちてヘソへ向かう
                    boolean first = !onStage;
                    onStage = true;
                    if (vy > 0 && first && rnd.next() < STAGE) {
                        x = 100 + (rnd.next() - 0.5) * 4;
                        y = LCD_Y1 + BR + 1;
                        vx = (rnd.next() - 0.5) * 1.6;
                        vy = 0.4;
                        if (path != null) path.add(new float[]{(float) x, (float) y});
                    } else {
                        y = LCD_Y0 - BR;
                        if (vy > 0) vy = -vy * REST;
                        vx += x < 100 ? -0.15 : 0.15;   // 液晶の上からは左右に転がり落ちる
                    }
                }
                else if (m == db) { y = LCD_Y1 + BR; if (vy < 0) vy = -vy * REST; }
                else if (m == dl) { x = LCD_X0 - BR; if (vx > 0) vx = -vx * REST; }
                else { x = LCD_X1 + BR; if (vx < 0) vx = -vx * REST; }
            }
            if (x < 6) { x = 6; vx = Math.abs(vx) * 0.5; }
            if (x > 194) { x = 194; vx = -Math.abs(vx) * 0.5; }
            for (double[] pg : PEGS) {
                double ddx = x - pg[0], ddy = y - pg[1], dd = ddx * ddx + ddy * ddy;
                if (dd < (BR + PR) * (BR + PR)) {
                    dd = StrictMath.sqrt(dd);
                    if (dd == 0) dd = 0.01;
                    double nx = ddx / dd, ny = ddy / dd;
                    x = pg[0] + nx * (BR + PR);
                    y = pg[1] + ny * (BR + PR);
                    double dot = vx * nx + vy * ny;
                    if (dot < 0) {
                        vx -= (1 + REST) * dot * nx;
                        vy -= (1 + REST) * dot * ny;
                        vx += (rnd.next() - 0.5) * 0.25;
                    }
                }
            }
            if (path != null && step % 2 == 0) path.add(new float[]{(float) x, (float) y});
            for (int k = 1; k < POCKET.length; k++) {
                if (k == ATTACKER && !attackerOpen) continue;
                if (k == DENCHU && !denchuOpen) continue;
                double[] pk = POCKET[k];
                if (x >= pk[0] && x <= pk[1] && y >= pk[2] - 2 && y <= pk[2] + 2 && vy > 0) {
                    if (path != null) path.add(new float[]{(float) x, (float) y});
                    return k;
                }
            }
            if (y > 222) return OUT;
            vx *= 0.999;
        }
        return OUT;
    }

    private Board() {}
}
