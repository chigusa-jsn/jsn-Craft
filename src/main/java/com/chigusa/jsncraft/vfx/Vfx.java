package com.chigusa.jsncraft.vfx;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * サーバーからエフェクトを出すための道具箱。
 * 色の番号：0 = 蒼、1 = 白、2 = 金、3 = 赤、4 = 緑
 */
public final class Vfx {
    public static final int AZURE = 0, WHITE = 1, GOLD = 2, RED = 3, GREEN = 4;

    /** 三日月の斬撃。size はブロック単位の大きさ、roll は傾き（度） */
    public static void slash(ServerLevel level, Vec3 p, float size, int color, float roll) {
        level.sendParticles(ModParticles.SLASH.get(), p.x, p.y, p.z, 0, size, color, roll, 1.0);
    }

    /** 足元の魔法陣（色の番号から柄を選ぶ。職業がわかるときは Circle を渡す方を使う） */
    public static void magicCircle(ServerLevel level, Vec3 p, float size, int ticks, int color) {
        Circle c = switch (color) {
            case AZURE -> Circle.SWORD;
            case RED -> Circle.FIRE;
            case GREEN -> Circle.NATURE;
            default -> Circle.HOLY;
        };
        magicCircle(level, p, size, ticks, c);
    }

    /** 足元の魔法陣。ticks のあいだ出ていて、ゆっくり回る */
    public static void magicCircle(ServerLevel level, Vec3 p, float size, int ticks, Circle c) {
        circle(level, p.add(0, 0.03, 0), c, size, ticks, 0);
        circle(level, p.add(0, 0.05, 0), c, size * 0.55F, ticks, CircleOptions.REVERSE);
    }

    /** 魔法陣を1枚出す（style は CircleOptions.CHARGE などの組み合わせ） */
    public static void circle(ServerLevel level, Vec3 p, Circle c, float size, int ticks, int style) {
        level.sendParticles(new CircleOptions(c.ordinal(), size, ticks, style), p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }

    /** 魔法陣の枚数：ためが長いほど重なる（1〜1.2秒未満は1枚、2.5秒未満は2枚、それ以上は3枚＋空） */
    public static int layers(int castTicks) {
        return castTicks >= 50 ? 3 : castTicks >= 24 ? 2 : 1;
    }

    /** ため（詠唱）の魔法陣：足元に重ねて、大技は空にも出す。ticks で育ちきる */
    public static void castCircles(ServerLevel level, Vec3 feet, Circle c, int ticks, int layers) {
        circle(level, feet.add(0, 0.03, 0), c, 1.8F, ticks, CircleOptions.CHARGE);
        if (layers >= 2) circle(level, feet.add(0, 0.06, 0), c, 2.8F, ticks, CircleOptions.CHARGE | CircleOptions.REVERSE);
        if (layers >= 3) {
            circle(level, feet.add(0, 0.09, 0), c, 0.9F, ticks, CircleOptions.CHARGE | CircleOptions.REVERSE);
            circle(level, feet.add(0, 5.0, 0), c, 3.4F, ticks, CircleOptions.CHARGE | CircleOptions.SKY | CircleOptions.REVERSE);
        }
    }

    /** 発動の瞬間：魔法陣がパッと光って広がる */
    public static void releaseCircles(ServerLevel level, Vec3 feet, Circle c, int layers) {
        circle(level, feet.add(0, 0.03, 0), c, 1.8F, 12, CircleOptions.FLASH);
        if (layers >= 2) circle(level, feet.add(0, 0.06, 0), c, 2.8F, 14, CircleOptions.FLASH | CircleOptions.REVERSE);
        if (layers >= 3) circle(level, feet.add(0, 5.0, 0), c, 3.4F, 16, CircleOptions.FLASH | CircleOptions.SKY | CircleOptions.REVERSE);
        ring(level, feet, 2.5F + layers, 10, WHITE);
    }

    /** 広がる光の輪 */
    public static void ring(ServerLevel level, Vec3 p, float size, int ticks, int color) {
        level.sendParticles(ModParticles.RING.get(), p.x, p.y + 0.05, p.z, 0, size, ticks, color, 1.0);
    }

    /** 火花をパッと散らす */
    public static void sparks(ServerLevel level, Vec3 p, int count) {
        level.sendParticles(ModParticles.SPARK.get(), p.x, p.y, p.z, count, 0.15, 0.15, 0.15, 0.35);
    }

    public static float[] color(int index) {
        return switch (index) {
            case WHITE -> new float[]{1.0F, 1.0F, 1.0F};
            case GOLD -> new float[]{1.0F, 0.82F, 0.35F};
            case RED -> new float[]{1.0F, 0.35F, 0.3F};
            case GREEN -> new float[]{0.55F, 1.0F, 0.55F};
            default -> new float[]{0.45F, 0.78F, 1.0F};
        };
    }

    private Vfx() {}
}
