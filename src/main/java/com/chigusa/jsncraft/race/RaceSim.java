package com.chigusa.jsncraft.race;

/**
 * 競馬のレースの計算。サーバーと画面でまったく同じ結果になるように、自前の乱数と StrictMath だけを使う。
 * 1000m を 20ティック/秒 で走る（だいたい25秒）。
 */
public final class RaceSim {
    public static final int HORSES = 8;
    public static final double LENGTH = 1000;
    public static final int MAX_TICKS = 900;
    static final double SPEED_MIN = 39.2, SPEED_RANGE = 1.6, FORM = 0.03, NOISE = 0.05;

    /** speed：速さ(m/s)、stamina：ばてにくさ(0〜1)、burst：末脚(0〜0.3)、gate：出だし(0〜1) */
    public record Horse(double speed, double stamina, double burst, double gate) {}

    public record Result(double[][] pos, int[] order, int ticks) {}

    public static final class Rng {
        long s;

        public Rng(long seed) {
            s = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
        }

        public double next() {
            s ^= s << 13;
            s ^= s >>> 7;
            s ^= s << 17;
            return (s >>> 11) * 0x1.0p-53;
        }
    }

    public static Result run(Horse[] h, long seed) {
        Rng r = new Rng(seed);
        int n = h.length;
        double[] p = new double[n], noise = new double[n], finish = new double[n], form = new double[n];
        // その日の調子（走ってみないとわからない）
        for (int i = 0; i < n; i++) form[i] = 1 + FORM * (r.next() * 2 - 1);
        java.util.Arrays.fill(finish, -1);
        double[][] out = new double[MAX_TICKS + 1][n];
        int t = 0, done = 0;
        while (t < MAX_TICKS && done < n) {
            t++;
            for (int i = 0; i < n; i++) {
                if (t % 8 == 1) noise[i] = noise[i] * 0.85 + (r.next() * 2 - 1) * 0.5;
                if (finish[i] >= 0) {
                    p[i] += h[i].speed() * 0.03;   // ゴールのあとはゆっくり
                    continue;
                }
                double f = p[i] / LENGTH;
                double v = h[i].speed() * form[i] * (1 + NOISE * noise[i]);
                if (t < 30) v *= StrictMath.min(1.0, t / 30.0 + h[i].gate() * 0.25);
                if (f > 0.55) v *= 1 - (1 - h[i].stamina()) * (f - 0.55) * 1.2;
                if (f > 0.8) v *= 1 + h[i].burst() * (f - 0.8) * 1.5;
                double before = p[i];
                p[i] += v * 0.05;
                if (p[i] >= LENGTH) {
                    finish[i] = t - 1 + (LENGTH - before) / (p[i] - before);
                    done++;
                }
            }
            System.arraycopy(p, 0, out[t], 0, n);
        }
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        final double[] fin = finish;
        java.util.Arrays.sort(idx, (a, b) -> {
            double fa = fin[a] < 0 ? 1e9 - p[a] : fin[a], fb = fin[b] < 0 ? 1e9 - p[b] : fin[b];
            return Double.compare(fa, fb);
        });
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = idx[i];
        double[][] trimmed = java.util.Arrays.copyOf(out, t + 1);
        return new Result(trimmed, order, t);
    }

    /** 新しいレースの馬（seed から作る：画面でも同じものが作れる） */
    public static Horse[] horses(long seed) {
        Rng r = new Rng(seed * 31 + 7);
        Horse[] h = new Horse[HORSES];
        for (int i = 0; i < HORSES; i++)
            h[i] = new Horse(SPEED_MIN + r.next() * SPEED_RANGE, 0.55 + r.next() * 0.45, r.next() * 0.28, r.next());
        return h;
    }

    /** 調子（1〜5）：強さのめやす */
    public static int stars(Horse h) {
        double score = (h.speed() - SPEED_MIN) / SPEED_RANGE * 0.55 + (h.stamina() - 0.55) / 0.45 * 0.25 + h.burst() / 0.28 * 0.15 + h.gate() * 0.05;
        return (int) StrictMath.max(1, StrictMath.min(5, StrictMath.round(score * 4 + 1)));
    }

    private RaceSim() {}
}
