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

    /** 足元の魔法陣。ticks のあいだ出ている */
    public static void magicCircle(ServerLevel level, Vec3 p, float size, int ticks, int color) {
        level.sendParticles(ModParticles.MAGIC_CIRCLE.get(), p.x, p.y + 0.03, p.z, 0, size, ticks, color, 1.0);
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
