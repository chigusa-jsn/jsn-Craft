package com.chigusa.jsncraft.npc;

import com.chigusa.jsncraft.data.PlayerJobData;
import com.chigusa.jsncraft.job.JobDefinition;
import com.chigusa.jsncraft.job.JobManager;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.network.JobChangePayloads;
import com.chigusa.jsncraft.network.JobChangePayloads.JobEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 転職の神官：画面に出す職業の一覧と、転職の受付（神官のそばでだけ）。 */
public final class JobChangeService {

    private static final double RANGE = 8.0;
    private static final Map<UUID, Integer> LAST_MASTER = new ConcurrentHashMap<>();

    public static void talkedTo(ServerPlayer player, JobMasterEntity master) {
        LAST_MASTER.put(player.getUUID(), master.getId());
    }

    public static boolean isNearMaster(ServerPlayer player) {
        Integer id = LAST_MASTER.get(player.getUUID());
        if (id == null) return false;
        Entity e = player.level().getEntity(id);
        return e instanceof JobMasterEntity && e.isAlive() && e.distanceToSqr(player) <= RANGE * RANGE;
    }

    public static void open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new JobChangePayloads.Open(build(player)));
    }

    public static void change(ServerPlayer player, String jobIdText) {
        if (!isNearMaster(player)) {
            player.displayClientMessage(Component.literal("転職は転職の神官のそばでしかできません").withStyle(ChatFormatting.RED), true);
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(jobIdText);
        if (id == null) return;
        Component error = JobService.changeJob(player, id);
        if (error != null) {
            player.displayClientMessage(error.copy().withStyle(ChatFormatting.RED), true);
        } else {
            JobDefinition def = JobManager.INSTANCE.get(id);
            player.sendSystemMessage(Component.literal("✦ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(def.type().displayName() + "ジョブを ")).append(def.name().copy())
                    .append(Component.literal(" に転職しました！").withStyle(ChatFormatting.GOLD)));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.2);
        }
        open(player); // 画面を新しくする
    }

    /** 忘却の宝珠を1つ使って、ボードをリセットする（神官のそばでだけ） */
    public static void resetBoard(ServerPlayer player, String typeText) {
        if (!isNearMaster(player)) {
            player.displayClientMessage(Component.literal("リセットは転職の神官のそばでしかできません").withStyle(ChatFormatting.RED), true);
            return;
        }
        com.chigusa.jsncraft.job.JobType type = "life".equals(typeText)
                ? com.chigusa.jsncraft.job.JobType.LIFE : com.chigusa.jsncraft.job.JobType.COMBAT;
        if (JobService.data(player).getJob(type) == null) {
            player.displayClientMessage(Component.literal(type.displayName() + "ジョブに就いていません").withStyle(ChatFormatting.RED), true);
            return;
        }
        var inv = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModEntities.OBLIVION_ORB.get())) {
                slot = i;
                break;
            }
        }
        if (slot < 0 && !player.isCreative()) {
            player.displayClientMessage(Component.literal("忘却の宝珠を持っていません").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (slot >= 0 && !player.isCreative()) inv.getItem(slot).shrink(1);
        com.chigusa.jsncraft.job.SkillBoardService.resetChain(player, type);
        player.sendSystemMessage(Component.literal("✦ " + type.displayName() + "ジョブのスキルボードをリセットしました。SPがすべて戻りました")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.8, 0.5, 0.1);
        open(player);
    }

    private static List<JobEntry> build(ServerPlayer player) {
        PlayerJobData data = JobService.data(player);
        List<JobEntry> out = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JobDefinition> e : JobManager.INSTANCE.all().entrySet()) {
            ResourceLocation id = e.getKey();
            JobDefinition def = e.getValue();
            boolean current = id.equals(data.getJob(def.type()));
            List<Component> reqs = new ArrayList<>();
            boolean ok = true;
            for (JobDefinition.Requirement r : def.requires()) {
                JobDefinition rd = JobManager.INSTANCE.get(r.job());
                int have = data.level(r.job());
                boolean met = have >= r.level();
                ok &= met;
                reqs.add(Component.empty().append(rd != null ? rd.name().copy() : Component.literal(r.job().toString()))
                        .append(" Lv." + r.level() + " 以上（今 Lv." + have + "）")
                        .withStyle(met ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            Component reason = current ? Component.literal("今この職業です")
                    : ok ? Component.literal("転職できます") : Component.literal("条件が足りません");
            out.add(new JobEntry(id.toString(), def.name(), def.type().getSerializedName(), def.tier().getSerializedName(),
                    data.level(id), def.maxLevel(), current, !current && ok, reason, reqs));
        }
        return out;
    }

    private JobChangeService() {}
}
