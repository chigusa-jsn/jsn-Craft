package com.chigusa.jsncraft.job;

import com.chigusa.jsncraft.data.JobProgress;
import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.data.PlayerJobData;
import com.chigusa.jsncraft.network.JobSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Predicate;

/** 経験値・レベルアップ・転職・同期など、職業まわりの処理をまとめた場所。 */
public final class JobService {

    public static PlayerJobData data(Player player) {
        return player.getData(ModAttachments.JOB_DATA);
    }

    /**
     * 今のレベルから次のレベルに上がるのに必要な経験値。
     * 50 ＋ レベル × 増え幅。増え幅は基本職20、上位職40、合体職60。
     * （例：基本職 Lv1→2 は70、上位職 Lv1→2 は90、合体職 Lv1→2 は110）
     */
    public static int xpForNextLevel(JobDefinition def, int level) {
        return 50 + level * def.tier().xpPerLevel();
    }

    /** 行動したときに呼ぶ。今就いている生活・戦闘ジョブそれぞれで、条件に合うルールの経験値を入れる。 */
    public static void onAction(ServerPlayer player, Predicate<JobDefinition.XpRule> matcher) {
        PlayerJobData data = data(player);
        for (JobType type : JobType.values()) {
            ResourceLocation jobId = data.getJob(type);
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (JobDefinition.XpRule rule : def.xp()) {
                if (matcher.test(rule)) {
                    addXp(player, jobId, rule.xp());
                    break; // 最初に当てはまったルールだけ使う
                }
            }
        }
    }

    public static void addXp(ServerPlayer player, ResourceLocation jobId, int amount) {
        JobDefinition def = JobManager.INSTANCE.get(jobId);
        if (def == null || amount <= 0) return;

        JobProgress prog = data(player).progress(jobId);
        if (prog.level >= def.maxLevel()) return;

        prog.xp += amount;
        boolean leveledUp = false;
        while (prog.level < def.maxLevel() && prog.xp >= xpForNextLevel(def, prog.level)) {
            prog.xp -= xpForNextLevel(def, prog.level);
            prog.level++;
            leveledUp = true;
        }
        if (prog.level >= def.maxLevel()) prog.xp = 0;

        if (leveledUp) {
            player.sendSystemMessage(Component.literal("★ ").withStyle(ChatFormatting.GOLD)
                    .append(def.name().copy())
                    .append(Component.literal(" が Lv." + prog.level + " になりました！").withStyle(ChatFormatting.GOLD)));
            if (def.skillPointsPerLevel() > 0 && !def.board().isEmpty()) {
                player.sendSystemMessage(Component.literal("  スキルポイント 残り " + SkillBoardService.pointsAvailable(def, prog)
                        + "（Kキーでスキルボード）").withStyle(ChatFormatting.AQUA));
            }
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75F, 1.0F);
            sync(player);
            SkillBoardService.sendSnapshot(player, false);
        } else {
            String progressText = prog.level >= def.maxLevel()
                    ? " (MAX)"
                    : " (" + prog.xp + "/" + xpForNextLevel(def, prog.level) + ")";
            player.displayClientMessage(Component.literal("+" + amount + " ").withStyle(ChatFormatting.YELLOW)
                    .append(def.name().copy())
                    .append(Component.literal(progressText).withStyle(ChatFormatting.GRAY)), true);
        }
    }

    /** 転職。成功したら null、失敗したら理由を返す。 */
    public static Component changeJob(ServerPlayer player, ResourceLocation jobId) {
        JobDefinition def = JobManager.INSTANCE.get(jobId);
        if (def == null) {
            return Component.literal("その職業は存在しません: " + jobId);
        }
        PlayerJobData data = data(player);
        if (jobId.equals(data.getJob(def.type()))) {
            return Component.literal("すでにその職業です");
        }
        for (JobDefinition.Requirement req : def.requires()) {
            if (data.level(req.job()) < req.level()) {
                JobDefinition reqDef = JobManager.INSTANCE.get(req.job());
                Component reqName = reqDef != null ? reqDef.name() : Component.literal(req.job().toString());
                return Component.literal("転職条件が足りません: ").append(reqName.copy())
                        .append(" Lv." + req.level() + " 以上が必要（今は Lv." + data.level(req.job()) + "）");
            }
        }
        data.setJob(def.type(), jobId);
        data.progress(jobId); // 初めてならLv.1で作られる
        SkillBoardService.applyBonuses(player); // 前の職業のボードはお休み、新しい職業のボードを反映
        sync(player);
        SkillBoardService.sendSnapshot(player, false);
        return null;
    }

    /** このプレイヤーの表示情報を、本人と周りで見ている全員に送る。 */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payloadOf(player));
    }

    /** 指定プレイヤー(target)の表示情報を、受け取る人(receiver)だけに送る。 */
    public static void syncTo(ServerPlayer target, ServerPlayer receiver) {
        PacketDistributor.sendToPlayer(receiver, payloadOf(target));
    }

    public static JobSyncPayload payloadOf(Player player) {
        PlayerJobData data = data(player);
        ResourceLocation combat = data.getJob(JobType.COMBAT);
        ResourceLocation life = data.getJob(JobType.LIFE);
        JobDefinition combatDef = JobManager.INSTANCE.get(combat);
        JobDefinition lifeDef = JobManager.INSTANCE.get(life);
        return new JobSyncPayload(
                player.getId(),
                data.getTitle(),
                combatDef != null ? combatDef.name() : Component.empty(),
                combatDef != null ? data.level(combat) : 0,
                lifeDef != null ? lifeDef.name() : Component.empty(),
                lifeDef != null ? data.level(life) : 0);
    }

    /** /job info で見せる文。 */
    public static MutableComponent describe(Player player) {
        PlayerJobData data = data(player);
        MutableComponent out = Component.literal("=== " + player.getName().getString() + " の職業 ===").withStyle(ChatFormatting.AQUA);
        if (!data.getTitle().isEmpty()) {
            out.append(Component.literal("\n肩書: 《" + data.getTitle() + "》").withStyle(ChatFormatting.GOLD));
        }
        for (JobType type : new JobType[]{JobType.COMBAT, JobType.LIFE}) {
            ResourceLocation id = data.getJob(type);
            JobDefinition def = JobManager.INSTANCE.get(id);
            out.append(Component.literal("\n" + type.displayName() + "ジョブ: ").withStyle(ChatFormatting.WHITE));
            if (def == null) {
                out.append(Component.literal("なし（/job change で就職）").withStyle(ChatFormatting.GRAY));
            } else {
                JobProgress p = data.progress(id);
                String xp = p.level >= def.maxLevel() ? "MAX" : p.xp + "/" + xpForNextLevel(def, p.level);
                out.append(def.name().copy()).append(Component.literal(" Lv." + p.level + "  EXP " + xp
                        + "  SP " + SkillBoardService.pointsAvailable(def, p)).withStyle(ChatFormatting.GRAY));
            }
        }
        return out;
    }

    private JobService() {}
}
