package com.chigusa.jsncraft.skill;

/**
 * 「今のダメージはスキルによるもの／会心」という目印。ダメージの数字の色を決めるのに使う。
 * ダメージの処理は同じ流れの中で続けて起きるので、その間だけ立てておく。
 */
public final class HitFlags {
    public static final ThreadLocal<Boolean> SKILL = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> CRIT = ThreadLocal.withInitial(() -> false);

    private HitFlags() {}
}
