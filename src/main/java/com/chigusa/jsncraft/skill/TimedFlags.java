package com.chigusa.jsncraft.skill;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 「しばらくの間○○」の印（広域掘りなど）。key ごとに強さと終わる時刻を持つ。 */
public final class TimedFlags {
    private record Flag(double power, long until) {}

    private static final Map<UUID, Map<String, Flag>> FLAGS = new ConcurrentHashMap<>();

    public static void set(ServerPlayer player, String key, double power, double seconds) {
        FLAGS.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(key, new Flag(power, player.level().getGameTime() + Math.round(seconds * 20)));
    }

    /** 効いていれば強さ、切れていれば 0 */
    public static double get(ServerPlayer player, String key) {
        Map<String, Flag> m = FLAGS.get(player.getUUID());
        if (m == null) return 0;
        Flag f = m.get(key);
        return f != null && f.until() > player.level().getGameTime() ? f.power() : 0;
    }

    public static void clear(UUID player) {
        FLAGS.remove(player);
    }

    private TimedFlags() {}
}
