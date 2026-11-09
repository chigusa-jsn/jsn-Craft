package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.client.ClientMenuHandler;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.job.SkillBoardService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        // サーバー → クライアント
        registrar.playToClient(JobSyncPayload.TYPE, JobSyncPayload.STREAM_CODEC, ModNetwork::handleJobSync);
        registrar.playToClient(BoardSnapshotPayload.TYPE, BoardSnapshotPayload.STREAM_CODEC, ModNetwork::handleSnapshot);
        // クライアント → サーバー
        registrar.playToServer(OpenMenuPayload.TYPE, OpenMenuPayload.STREAM_CODEC, ModNetwork::handleOpenMenu);
        registrar.playToServer(UnlockNodePayload.TYPE, UnlockNodePayload.STREAM_CODEC, ModNetwork::handleUnlock);
    }

    private static void handleJobSync(JobSyncPayload payload, IPayloadContext context) {
        // クライアント側で受け取ったら、表示用の控え（キャッシュ）にしまっておく
        context.enqueueWork(() -> ClientJobCache.put(payload));
    }

    private static void handleSnapshot(BoardSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientMenuHandler.handle(payload));
    }

    private static void handleOpenMenu(OpenMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) SkillBoardService.sendSnapshot(player, true);
        });
    }

    private static void handleUnlock(UnlockNodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            JobType type = "life".equals(payload.jobType()) ? JobType.LIFE : JobType.COMBAT;
            Component error = SkillBoardService.unlock(player, type, payload.nodeId());
            if (error != null) player.displayClientMessage(error.copy().withStyle(net.minecraft.ChatFormatting.RED), true);
        });
    }

    private ModNetwork() {}
}
