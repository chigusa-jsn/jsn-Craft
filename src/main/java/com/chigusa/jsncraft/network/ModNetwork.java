package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.client.ClientMenuHandler;
import com.chigusa.jsncraft.job.SkillBoardService;
import com.chigusa.jsncraft.quest.QuestEditorService;
import com.chigusa.jsncraft.quest.QuestService;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("9");
        // サーバー → クライアント
        registrar.playToClient(JobSyncPayload.TYPE, JobSyncPayload.STREAM_CODEC, ModNetwork::handleJobSync);
        registrar.playToClient(BoardSnapshotPayload.TYPE, BoardSnapshotPayload.STREAM_CODEC, ModNetwork::handleSnapshot);
        registrar.playToClient(ResourceSyncPayload.TYPE, ResourceSyncPayload.STREAM_CODEC, ModNetwork::handleResources);
        registrar.playToClient(QuestListPayload.TYPE, QuestListPayload.STREAM_CODEC, ModNetwork::handleQuestList);
        registrar.playToClient(OpenTabPayload.TYPE, OpenTabPayload.STREAM_CODEC, ModNetwork::handleOpenTab);
        registrar.playToClient(SkillPayloads.State.TYPE, SkillPayloads.State.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.ClientSkills.update(p)));
        registrar.playToServer(SkillPayloads.Cast.TYPE, SkillPayloads.Cast.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.skill.SkillService.cast(sp, p.slot());
                }));
        registrar.playToServer(SkillPayloads.Assign.TYPE, SkillPayloads.Assign.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.skill.SkillService.assign(sp, p.slot(), p.skill());
                }));
        registrar.playToClient(JobChangePayloads.Open.TYPE, JobChangePayloads.Open.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> ClientMenuHandler.openJobChange(p)));
        registrar.playToServer(JobChangePayloads.Change.TYPE, JobChangePayloads.Change.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.npc.JobChangeService.change(sp, p.jobId());
                }));
        registrar.playToClient(PartyStatusPayload.TYPE, PartyStatusPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> PartyStatusPayload.client = p.members()));
        registrar.playToClient(QuestEditorPayloads.Open.TYPE, QuestEditorPayloads.Open.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> ClientMenuHandler.openEditor(p)));
        registrar.playToClient(QuestEditorPayloads.Result.TYPE, QuestEditorPayloads.Result.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> ClientMenuHandler.editorResult(p)));
        registrar.playToServer(QuestEditorPayloads.Save.TYPE, QuestEditorPayloads.Save.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) QuestEditorService.save(sp, p.data());
                }));
        // クライアント → サーバー
        registrar.playToServer(OpenMenuPayload.TYPE, OpenMenuPayload.STREAM_CODEC, ModNetwork::handleOpenMenu);
        registrar.playToServer(UnlockNodePayload.TYPE, UnlockNodePayload.STREAM_CODEC, ModNetwork::handleUnlock);
        registrar.playToServer(QuestActionPayload.TYPE, QuestActionPayload.STREAM_CODEC, ModNetwork::handleQuestAction);
    }

    private static void handleJobSync(JobSyncPayload payload, IPayloadContext context) {
        // クライアント側で受け取ったら、表示用の控え（キャッシュ）にしまっておく
        context.enqueueWork(() -> ClientJobCache.put(payload));
    }

    private static void handleSnapshot(BoardSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientMenuHandler.handle(payload));
    }

    private static void handleResources(ResourceSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ResourceSyncPayload.clientMana = payload.mana();
            ResourceSyncPayload.clientStamina = payload.stamina();
        });
    }

    private static void handleOpenMenu(OpenMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                QuestService.leftReceptionist(player); // Kキーは受付嬢の外
                QuestService.sendList(player); // 先に依頼一覧を送ってから、メニューを開かせる
                SkillBoardService.sendSnapshot(player, true);
            }
        });
    }

    private static void handleUnlock(UnlockNodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ResourceLocation jobId = ResourceLocation.tryParse(payload.jobId());
            if (jobId == null) return;
            Component error = SkillBoardService.unlock(player, jobId, payload.nodeId());
            if (error != null) player.displayClientMessage(error.copy().withStyle(net.minecraft.ChatFormatting.RED), true);
        });
    }

    private static void handleOpenTab(OpenTabPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientMenuHandler.startOnTab(payload.tab()));
    }

    private static void handleQuestList(QuestListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> QuestListPayload.client = payload);
    }

    private static void handleQuestAction(QuestActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ResourceLocation id = ResourceLocation.tryParse(payload.questId());
            if (id == null) return;
            Component error = switch (payload.action()) {
                case QuestActionPayload.ACCEPT -> QuestService.accept(player, id);
                case QuestActionPayload.TURN_IN -> QuestService.turnIn(player, id);
                case QuestActionPayload.CLAIM -> QuestService.claim(player, id, payload.picks());
                case QuestActionPayload.ABANDON -> {
                    QuestService.abandon(player, id);
                    yield null;
                }
                default -> null;
            };
            if (error != null) {
                player.displayClientMessage(error.copy().withStyle(net.minecraft.ChatFormatting.RED), true);
                QuestService.sendList(player);
            }
        });
    }

    private ModNetwork() {}
}
