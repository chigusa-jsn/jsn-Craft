package com.chigusa.jsncraft.network;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアントが覚えている「各プレイヤーの職業表示」。
 * エンティティIDごとに保存する。Minecraftのクライアント専用クラスは使わないので、
 * サーバーで読み込まれても落ちない。
 */
public final class ClientJobCache {
    private static final Map<Integer, JobSyncPayload> CACHE = new ConcurrentHashMap<>();

    public static void put(JobSyncPayload payload) {
        CACHE.put(payload.entityId(), payload);
    }

    @Nullable
    public static JobSyncPayload get(int entityId) {
        return CACHE.get(entityId);
    }

    public static void clear() {
        CACHE.clear();
    }

    private ClientJobCache() {}
}
