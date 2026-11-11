package com.chigusa.jsncraft.quest;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobManager;
import com.chigusa.jsncraft.network.QuestEditorPayloads;
import com.chigusa.jsncraft.network.QuestEditorPayloads.JobOption;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 依頼エディター：開く・保存する・入力内容と依頼の変換。OPだけが使える。 */
public final class QuestEditorService {

    public static final int PERMISSION = 2;

    public static boolean canEdit(ServerPlayer player) {
        return player.hasPermissions(PERMISSION);
    }

    /** エディターを開く。id が null なら新規。 */
    public static void open(ServerPlayer player, @Nullable ResourceLocation id) {
        QuestEditorData data = QuestEditorData.blank();
        if (id != null) {
            CustomQuestStore store = CustomQuestStore.get();
            QuestDefinition def = store == null ? null : store.find(id);
            if (def != null) data = toEditor(id, def);
        }
        List<JobOption> jobs = new ArrayList<>();
        JobManager.INSTANCE.all().forEach((jobId, def) -> jobs.add(new JobOption(jobId.toString(), def.name())));
        PacketDistributor.sendToPlayer(player, new QuestEditorPayloads.Open(data, jobs));
    }

    public static void save(ServerPlayer player, QuestEditorData data) {
        if (!canEdit(player)) {
            reply(player, false, "依頼を作る権限がありません");
            return;
        }
        CustomQuestStore store = CustomQuestStore.get();
        if (store == null) return;

        // 入力チェック
        if (data.title().isBlank()) {
            reply(player, false, "タイトルを入力してください");
            return;
        }
        List<QuestDefinition.Objective> objectives = new ArrayList<>();
        for (QuestEditorData.Obj o : data.objectives()) {
            if (o.target().isBlank()) continue;
            if (o.count() < 1) {
                reply(player, false, "目標の数は1以上にしてください");
                return;
            }
            String target = o.type().equals("reach_level") ? jobId(o.target()) : o.target().trim();
            objectives.add(new QuestDefinition.Objective(o.type(), target, o.count(), Optional.empty()));
        }
        if (objectives.isEmpty()) {
            reply(player, false, "目標を1つ以上入れてください");
            return;
        }
        Optional<String> start = date(data.start(), false);
        Optional<String> end = date(data.end(), true);
        if (start == null || end == null) {
            reply(player, false, "日付は 2026-12-24 か 2026-12-24 18:00 の形で入れてください");
            return;
        }

        List<QuestDefinition.JobXp> xp = data.xpAmount() > 0
                ? List.of(new QuestDefinition.JobXp(data.xpJob(), data.xpAmount())) : List.of();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack s : data.items()) if (!s.isEmpty()) items.add(s.copy());
        Optional<String> title = data.rewardTitle().isBlank() ? Optional.empty() : Optional.of(data.rewardTitle().trim());

        QuestDefinition def = new QuestDefinition(
                Component.literal(data.title().trim()),
                Component.literal(data.description().trim()),
                data.category(),
                data.requiredJob().isBlank() ? Optional.empty() : Optional.ofNullable(ResourceLocation.tryParse(jobId(data.requiredJob()))),
                Math.max(0, data.minLevel()),
                data.repeat(),
                start, end,
                objectives,
                new QuestDefinition.Rewards(xp, items, title));

        ResourceLocation id = data.id().isBlank() ? null : ResourceLocation.tryParse(data.id());
        if (id != null && store.find(id) != null) {
            store.replace(id, def);
            reply(player, true, "依頼「" + data.title().trim() + "」を更新しました（" + id.getPath() + "）");
        } else {
            id = store.add(def);
            reply(player, true, "依頼「" + data.title().trim() + "」を作りました（" + id.getPath() + "）");
        }
        // みんなの依頼一覧を新しくする
        if (player.getServer() != null) {
            for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) QuestService.sendList(p);
        }
    }

    private static void reply(ServerPlayer player, boolean ok, String message) {
        Component msg = Component.literal(message).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED);
        PacketDistributor.sendToPlayer(player, new QuestEditorPayloads.Result(ok, msg));
        if (ok) player.sendSystemMessage(msg);
    }

    /** "lumberjack" だけ書かれたら "jsn_craft:lumberjack" にする。 */
    private static String jobId(String s) {
        s = s.trim();
        return s.contains(":") ? s : JsnCraft.MODID + ":" + s;
    }

    /**
     * 日付の入力を "2026-12-24T18:00" の形にそろえる。空なら Optional.empty()、読めなければ null。
     * 時刻を省くと、開始は 00:00、終了は 23:59 にする。
     */
    @Nullable
    private static Optional<String> date(String input, boolean isEnd) {
        String s = input.trim().replace('/', '-');
        if (s.isEmpty()) return Optional.empty();
        s = s.replace(' ', 'T');
        if (!s.contains("T")) s += isEnd ? "T23:59" : "T00:00";
        try {
            LocalDateTime.parse(s);
            return Optional.of(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static QuestEditorData toEditor(ResourceLocation id, QuestDefinition def) {
        List<QuestEditorData.Obj> objs = new ArrayList<>();
        for (QuestDefinition.Objective o : def.objectives()) {
            if (objs.size() >= QuestEditorData.MAX_OBJECTIVES) break;
            objs.add(new QuestEditorData.Obj(o.type(), o.target(), o.count()));
        }
        while (objs.size() < QuestEditorData.MAX_OBJECTIVES) objs.add(new QuestEditorData.Obj("break_block", "", 1));
        QuestDefinition.JobXp xp = def.rewards().jobXp().isEmpty() ? null : def.rewards().jobXp().get(0);
        return new QuestEditorData(
                id.toString(),
                def.title().getString(),
                def.description().getString(),
                def.category(),
                def.repeat(),
                def.requiredJob().map(ResourceLocation::toString).orElse(""),
                def.minLevel(),
                def.start().map(s -> s.replace('T', ' ')).orElse(""),
                def.end().map(s -> s.replace('T', ' ')).orElse(""),
                objs,
                xp != null ? xp.job() : "life",
                xp != null ? xp.amount() : 0,
                def.rewards().title().orElse(""),
                def.rewards().items().stream().limit(QuestEditorData.MAX_ITEMS).map(ItemStack::copy).toList());
    }

    private QuestEditorService() {}
}
