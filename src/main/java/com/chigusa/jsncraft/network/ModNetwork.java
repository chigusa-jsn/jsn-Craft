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
        PayloadRegistrar registrar = event.registrar("22");
        com.chigusa.jsncraft.casino.CasinoNetwork.register(registrar);
        // サーバー → クライアント
        registrar.playToClient(JobSyncPayload.TYPE, JobSyncPayload.STREAM_CODEC, ModNetwork::handleJobSync);
        registrar.playToClient(BoardSnapshotPayload.TYPE, BoardSnapshotPayload.STREAM_CODEC, ModNetwork::handleSnapshot);
        registrar.playToClient(ResourceSyncPayload.TYPE, ResourceSyncPayload.STREAM_CODEC, ModNetwork::handleResources);
        registrar.playToClient(QuestListPayload.TYPE, QuestListPayload.STREAM_CODEC, ModNetwork::handleQuestList);
        registrar.playToClient(OpenTabPayload.TYPE, OpenTabPayload.STREAM_CODEC, ModNetwork::handleOpenTab);
        registrar.playToClient(SkillPayloads.State.TYPE, SkillPayloads.State.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.ClientSkills.update(p)));
        registrar.playToServer(PegasusFlapPayload.TYPE, PegasusFlapPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp && sp.getVehicle() != null && sp.getVehicle().getId() == p.horseId()
                            && com.chigusa.jsncraft.ranch.RanchService.mutant(sp.getVehicle()) == com.chigusa.jsncraft.ranch.Mutant.PEGASUS) {
                        var h = sp.getVehicle();
                        var level = sp.serverLevel();
                        level.playSound(null, h.getX(), h.getY(), h.getZ(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.NEUTRAL, 0.5F, 1.6F);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, h.getX(), h.getY() + 0.8, h.getZ(), 6, 0.6, 0.1, 0.6, 0.02);
                        h.fallDistance = 0;
                    }
                }));
        registrar.playToServer(DodgePayload.TYPE, DodgePayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.dodge.DodgeService.dodge(sp, p.dir());
                }));
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
        registrar.playToServer(SetTitlePayload.TYPE, SetTitlePayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (!(c.player() instanceof ServerPlayer sp)) return;
                    String t = p.title();
                    if (!t.isEmpty() && !QuestService.data(sp).titles.contains(t)) return; // 持っていない肩書はだめ
                    com.chigusa.jsncraft.job.JobService.data(sp).setTitle(t);
                    com.chigusa.jsncraft.job.JobService.sync(sp);
                    SkillBoardService.sendSnapshot(sp, false);
                }));
        registrar.playToServer(JobChangePayloads.ResetBoard.TYPE, JobChangePayloads.ResetBoard.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.npc.JobChangeService.resetBoard(sp, p.jobType());
                }));
        registrar.playToClient(AnimPayloads.Play.TYPE, AnimPayloads.Play.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.PlayerAnimClient.playOn(p.entityId(), p.anim())));
        registrar.playToServer(AnimPayloads.Swing.TYPE, AnimPayloads.Swing.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.anim.PlayerAnims.onSwing(sp, p.anim());
                }));
        registrar.playToClient(CastBarPayload.TYPE, CastBarPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.CastBarHud.start(p.name(), p.ticks())));
        registrar.playToClient(AnimalRankPayload.TYPE, AnimalRankPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.RanchHud.setRank(p.entityId(), p.rank(), p.mutant())));
        registrar.playToClient(DamageNumberPayload.TYPE, DamageNumberPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.particle.ParticleSetup.spawnDamageNumber(
                        p.x(), p.y(), p.z(), p.amount(), p.style())));
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
        registrar.playToClient(FishingPayloads.Start.TYPE, FishingPayloads.Start.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.FishingScreen.open(p)));
        registrar.playToServer(FishingPayloads.Result.TYPE, FishingPayloads.Result.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.fishing.FishingService.onResult(sp, p.success(), p.perfect());
                }));
        registrar.playToClient(CookingRecipesPayload.TYPE, CookingRecipesPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (p.brewing()) com.chigusa.jsncraft.client.BrewingScreen.BrewingRecipesCache.brewing = p;
                    else CookingRecipesPayload.client = p;
                }));
        registrar.playToClient(SeasoningsPayload.TYPE, SeasoningsPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> SeasoningsPayload.client = p));
        registrar.playToClient(DishDexPayload.TYPE, DishDexPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> DishDexPayload.client = p));
        registrar.playToClient(com.chigusa.jsncraft.loginbonus.LoginBonusPayload.TYPE, com.chigusa.jsncraft.loginbonus.LoginBonusPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.LoginBonusScreen.receive(p)));
        registrar.playToServer(com.chigusa.jsncraft.loginbonus.LoginBonusClaimPayload.TYPE, com.chigusa.jsncraft.loginbonus.LoginBonusClaimPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.loginbonus.LoginBonus.claim(sp);
                }));
        registrar.playToClient(MonsterDexPayload.TYPE, MonsterDexPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> MonsterDexPayload.client = p));
        registrar.playToClient(FishDexPayload.TYPE, FishDexPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> FishDexPayload.client = p));
        registrar.playToClient(OreRevealPayload.TYPE, OreRevealPayload.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.OreHighlight.show(p)));
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
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, FishDexPayload.of(player));
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, DishDexPayload.of(player));
                com.chigusa.jsncraft.dex.MonsterDex.checkRewards(player);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, MonsterDexPayload.of(player));
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
