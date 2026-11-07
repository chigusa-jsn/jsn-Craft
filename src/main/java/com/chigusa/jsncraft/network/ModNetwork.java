package com.chigusa.jsncraft.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(JobSyncPayload.TYPE, JobSyncPayload.STREAM_CODEC, ModNetwork::handleJobSync);
    }

    private static void handleJobSync(JobSyncPayload payload, IPayloadContext context) {
        // クライアント側で受け取ったら、表示用の控え（キャッシュ）にしまっておく
        context.enqueueWork(() -> ClientJobCache.put(payload));
    }

    private ModNetwork() {}
}
