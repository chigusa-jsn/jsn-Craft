package com.chigusa.jsncraft.quest;

import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.data.PlayerJobData;
import com.chigusa.jsncraft.job.JobDefinition;
import com.chigusa.jsncraft.job.JobManager;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.network.QuestListPayload;
import com.chigusa.jsncraft.npc.ReceptionistEntity;
import com.chigusa.jsncraft.network.QuestListPayload.ObjectiveView;
import com.chigusa.jsncraft.network.QuestListPayload.QuestView;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 依頼の受注・進行・報告・ごほうび。
 * 受注と報酬の受け取りは受付嬢のそばでだけ。報告（納品）はどこでもできる。
 */
public final class QuestService {

    /** 同時に受けられる依頼の数 */
    public static final int MAX_ACTIVE = 5;
    /** 受付嬢と話せる距離（ブロック） */
    private static final double RECEPTIONIST_RANGE = 8.0;

    /** プレイヤーが最後に話しかけた受付嬢（エンティティID）。保存はしない。 */
    private static final Map<UUID, Integer> LAST_RECEPTIONIST = new ConcurrentHashMap<>();

    public static void talkedTo(ServerPlayer player, ReceptionistEntity receptionist) {
        LAST_RECEPTIONIST.put(player.getUUID(), receptionist.getId());
    }

    /** 今、受付嬢のそばにいるか（最後に話しかけた受付嬢が近くにいる）。 */
    public static boolean isAtReceptionist(ServerPlayer player) {
        Integer id = LAST_RECEPTIONIST.get(player.getUUID());
        if (id == null) return false;
        Entity e = player.level().getEntity(id);
        return e instanceof ReceptionistEntity && e.isAlive()
                && e.distanceToSqr(player) <= RECEPTIONIST_RANGE * RECEPTIONIST_RANGE;
    }

    /** Kキーでメニューを開いたときは「受付嬢から離れている」扱いにする。 */
    public static void leftReceptionist(ServerPlayer player) {
        LAST_RECEPTIONIST.remove(player.getUUID());
    }

    public static PlayerQuestData data(ServerPlayer player) {
        return player.getData(ModAttachments.QUESTS);
    }

    // ---------------- 受けられるか ----------------

    /** 受けられないなら理由、受けられるなら null。 */
    @Nullable
    public static Component whyUnavailable(ServerPlayer player, ResourceLocation id, QuestDefinition def) {
        PlayerQuestData q = data(player);
        if (q.active.containsKey(id)) return Component.literal("受注中です");
        if (q.reported.contains(id)) return Component.literal("報酬の受け取り待ちです");
        if (!def.isOpen(LocalDateTime.now())) return Component.literal("今は受けられない期間です");

        Long last = q.completed.get(id);
        if (last != null) {
            if (def.repeat().equals("once")) return Component.literal("達成済みです");
            if (def.repeat().equals("daily") && last == LocalDate.now().toEpochDay()) {
                return Component.literal("今日はもう達成しました（明日また受けられます）");
            }
        }

        if (def.requiredJob().isPresent()) {
            ResourceLocation job = def.requiredJob().get();
            PlayerJobData jobs = JobService.data(player);
            boolean holding = job.equals(jobs.getJob(JobType.LIFE)) || job.equals(jobs.getJob(JobType.COMBAT));
            JobDefinition jobDef = JobManager.INSTANCE.get(job);
            Component jobName = jobDef != null ? jobDef.name() : Component.literal(job.toString());
            if (!holding) return Component.literal("").append(jobName.copy()).append(" に就いている人だけが受けられます");
            if (jobs.level(job) < def.minLevel()) {
                return Component.literal("").append(jobName.copy()).append(" Lv." + def.minLevel() + " 以上が必要です");
            }
        }
        return null;
    }

    // ---------------- 受ける・あきらめる・報告 ----------------

    @Nullable
    public static Component accept(ServerPlayer player, ResourceLocation id) {
        QuestDefinition def = QuestManager.INSTANCE.get(id);
        if (def == null) return Component.literal("その依頼はありません");
        if (!isAtReceptionist(player)) return Component.literal("依頼は受付嬢から受けてください");
        Component why = whyUnavailable(player, id, def);
        if (why != null) return why;
        PlayerQuestData q = data(player);
        if (q.active.size() >= MAX_ACTIVE) return Component.literal("同時に受けられるのは " + MAX_ACTIVE + " 件までです");

        List<Integer> progress = new ArrayList<>();
        for (int i = 0; i < def.objectives().size(); i++) progress.add(0);
        q.active.put(id, progress);
        player.sendSystemMessage(Component.literal("依頼「").withStyle(ChatFormatting.YELLOW)
                .append(def.title().copy()).append(Component.literal("」を受けました").withStyle(ChatFormatting.YELLOW)));
        sendList(player);
        return null;
    }

    public static void abandon(ServerPlayer player, ResourceLocation id) {
        if (data(player).active.remove(id) != null) sendList(player);
    }

    @Nullable
    public static Component turnIn(ServerPlayer player, ResourceLocation id) {
        QuestDefinition def = QuestManager.INSTANCE.get(id);
        PlayerQuestData q = data(player);
        List<Integer> progress = q.active.get(id);
        if (def == null || progress == null) return Component.literal("その依頼は受けていません");
        if (def.endTime().map(LocalDateTime.now()::isAfter).orElse(false)) return Component.literal("期限が過ぎています");
        if (!isComplete(player, def, progress)) return Component.literal("まだ目標を達成していません");

        // 納品アイテムを回収して、報酬待ちにする
        for (QuestDefinition.Objective o : def.objectives()) {
            if (o.type().equals("deliver_item")) removeItems(player.getInventory(), o.target(), o.count());
        }
        q.active.remove(id);
        q.reported.add(id);
        player.sendSystemMessage(Component.literal("依頼「").withStyle(ChatFormatting.AQUA)
                .append(def.title().copy())
                .append(Component.literal("」を報告しました！ 報酬は受付嬢から受け取ってください").withStyle(ChatFormatting.AQUA)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        sendList(player);
        return null;
    }

    /** 受付嬢のところで報酬を受け取る。 */
    @Nullable
    public static Component claim(ServerPlayer player, ResourceLocation id, List<Integer> picks) {
        QuestDefinition def = QuestManager.INSTANCE.get(id);
        PlayerQuestData q = data(player);
        if (def == null || !q.reported.contains(id)) return Component.literal("受け取れる報酬はありません");
        if (!isAtReceptionist(player)) return Component.literal("報酬は受付嬢から受け取ってください");

        QuestDefinition.Rewards r = def.rewards();
        // 選択報酬のチェック：ちょうど決められた数を、重複なく選んでいるか
        java.util.Set<Integer> chosen = new java.util.LinkedHashSet<>(picks);
        if (chosen.size() != picks.size() || chosen.size() != r.pickCount()
                || chosen.stream().anyMatch(i -> i < 0 || i >= r.choices().size())) {
            return Component.literal("選択報酬を " + r.pickCount() + " 個選んでください");
        }
        for (int i : chosen) {
            ItemStack c = r.choices().get(i);
            give(player, c);
            player.sendSystemMessage(Component.literal("  + ").withStyle(ChatFormatting.GRAY)
                    .append(c.getHoverName()).append(" ×" + c.getCount()));
        }

        for (QuestDefinition.JobXp xp : r.jobXp()) {
            ResourceLocation job = resolveJob(player, xp.job());
            if (job != null) JobService.addXp(player, job, xp.amount());
        }
        for (ItemStack item : r.items()) {
            give(player, item);
            player.sendSystemMessage(Component.literal("  + ").withStyle(ChatFormatting.GRAY)
                    .append(item.getHoverName()).append(" ×" + item.getCount()));
        }
        r.title().ifPresent(title -> {
            q.titles.add(title);
            JobService.data(player).setTitle(title);
            JobService.sync(player);
            player.sendSystemMessage(Component.literal("肩書《" + title + "》を手に入れました！").withStyle(ChatFormatting.GOLD));
        });

        q.reported.remove(id);
        q.completed.put(id, LocalDate.now().toEpochDay());
        player.sendSystemMessage(Component.literal("★ 依頼「").withStyle(ChatFormatting.GOLD)
                .append(def.title().copy()).append(Component.literal("」を達成しました！").withStyle(ChatFormatting.GOLD)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6F, 1.0F);
        sendList(player);
        return null;
    }

    private static void give(ServerPlayer player, ItemStack item) {
        ItemStack left = item.copy();
        while (!left.isEmpty()) {
            ItemStack part = left.split(left.getMaxStackSize());
            if (!player.getInventory().add(part)) player.drop(part, false);
        }
    }

    @Nullable
    private static ResourceLocation resolveJob(ServerPlayer player, String job) {
        PlayerJobData jobs = JobService.data(player);
        if (job.equals("life")) return jobs.getJob(JobType.LIFE);
        if (job.equals("combat")) return jobs.getJob(JobType.COMBAT);
        return ResourceLocation.tryParse(job);
    }

    // ---------------- 進み具合 ----------------

    public static void onBreak(ServerPlayer player, BlockState state) {
        onAction(player, "break_block", target -> matchesBlock(target, state));
    }

    public static void onKill(ServerPlayer player, Entity victim) {
        onAction(player, "kill_entity", target -> matchesEntity(target, victim));
    }

    private static void onAction(ServerPlayer player, String type, Predicate<String> matcher) {
        PlayerQuestData q = data(player);
        if (q.active.isEmpty()) return;
        boolean changed = false;
        for (Map.Entry<ResourceLocation, List<Integer>> e : q.active.entrySet()) {
            QuestDefinition def = QuestManager.INSTANCE.get(e.getKey());
            if (def == null) continue;
            List<Integer> progress = e.getValue();
            for (int i = 0; i < def.objectives().size() && i < progress.size(); i++) {
                QuestDefinition.Objective o = def.objectives().get(i);
                if (!o.type().equals(type) || progress.get(i) >= o.count() || !matcher.test(o.target())) continue;
                progress.set(i, progress.get(i) + 1);
                changed = true;
                if (progress.get(i) >= o.count()) {
                    player.sendSystemMessage(Component.literal("依頼「").withStyle(ChatFormatting.AQUA)
                            .append(def.title().copy())
                            .append(Component.literal("」の目標を達成：").withStyle(ChatFormatting.AQUA))
                            .append(label(o)));
                    if (isComplete(player, def, progress)) {
                        player.sendSystemMessage(Component.literal("  → Kキーの「依頼」タブから報告できます（報酬は受付嬢から）").withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }
        if (changed) sendList(player);
    }

    private static int currentProgress(ServerPlayer player, QuestDefinition.Objective o, int counted) {
        return switch (o.type()) {
            case "deliver_item" -> countItems(player.getInventory(), o.target());
            case "reach_level" -> {
                ResourceLocation job = ResourceLocation.tryParse(o.target());
                yield job == null ? 0 : JobService.data(player).level(job);
            }
            default -> counted;
        };
    }

    public static boolean isComplete(ServerPlayer player, QuestDefinition def, List<Integer> progress) {
        for (int i = 0; i < def.objectives().size(); i++) {
            QuestDefinition.Objective o = def.objectives().get(i);
            int counted = i < progress.size() ? progress.get(i) : 0;
            if (currentProgress(player, o, counted) < o.count()) return false;
        }
        return true;
    }

    // ---------------- 画面に送る ----------------

    public static void sendList(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildList(player));
    }

    public static QuestListPayload buildList(ServerPlayer player) {
        PlayerQuestData q = data(player);
        List<QuestView> views = new ArrayList<>();
        // 報酬待ち
        for (ResourceLocation id : q.reported) {
            QuestDefinition def = QuestManager.INSTANCE.get(id);
            if (def != null) views.add(view(player, id, def, List.of(), "reported"));
        }
        // 受注中
        for (Map.Entry<ResourceLocation, List<Integer>> e : q.active.entrySet()) {
            QuestDefinition def = QuestManager.INSTANCE.get(e.getKey());
            if (def != null) views.add(view(player, e.getKey(), def, e.getValue(), "active"));
        }
        // 受けられる依頼（特別 → 職業 → 通常の順）
        for (String cat : List.of("special", "job", "normal")) {
            for (Map.Entry<ResourceLocation, QuestDefinition> e : QuestManager.INSTANCE.all().entrySet()) {
                QuestDefinition def = e.getValue();
                if (!def.category().equals(cat)) continue;
                if (whyUnavailable(player, e.getKey(), def) != null) continue;
                views.add(view(player, e.getKey(), def, List.of(), "available"));
            }
        }
        return new QuestListPayload(views, q.active.size(), MAX_ACTIVE, isAtReceptionist(player));
    }

    private static QuestView view(ServerPlayer player, ResourceLocation id, QuestDefinition def, List<Integer> progress, String state) {
        boolean active = state.equals("active");
        boolean reported = state.equals("reported");
        List<ObjectiveView> objs = new ArrayList<>();
        for (int i = 0; i < def.objectives().size(); i++) {
            QuestDefinition.Objective o = def.objectives().get(i);
            int counted = i < progress.size() ? progress.get(i) : 0;
            int now = reported ? o.count() : active || !o.isCounted() ? currentProgress(player, o, counted) : 0;
            objs.add(new ObjectiveView(label(o), Math.min(now, o.count()), o.count()));
        }
        boolean hide = def.hideRewards();
        List<Component> rewards = new ArrayList<>();
        for (QuestDefinition.JobXp xp : def.rewards().jobXp()) {
            Component job = switch (xp.job()) {
                case "life" -> Component.literal("生活ジョブ");
                case "combat" -> Component.literal("戦闘ジョブ");
                default -> {
                    JobDefinition jd = JobManager.INSTANCE.get(ResourceLocation.tryParse(xp.job()));
                    yield jd != null ? jd.name() : Component.literal(xp.job());
                }
            };
            rewards.add(Component.empty().append(job.copy()).append(" 経験値 +" + (hide ? "？？？" : xp.amount())));
        }
        for (ItemStack item : def.rewards().items()) {
            rewards.add(Component.empty().append(hide ? Component.literal("？？？？？") : item.getHoverName())
                    .append(" ×" + item.getCount()));
        }
        def.rewards().title().ifPresent(t -> rewards.add(
                Component.literal("肩書《" + (hide ? "？？？？？" : t) + "》").withStyle(ChatFormatting.GOLD)));
        // 隠すときは、中身をクライアントに送らない（個数だけ伝える）
        List<ItemStack> choices = new ArrayList<>();
        for (ItemStack c : def.rewards().choices()) {
            choices.add(hide ? new ItemStack(net.minecraft.world.item.Items.PAPER, c.getCount()) : c);
        }

        boolean canTurnIn = active && isComplete(player, def, progress)
                && !def.endTime().map(LocalDateTime.now()::isAfter).orElse(false);
        return new QuestView(id.toString(), def.title(), def.description(), def.category(), state, canTurnIn,
                periodText(def), objs, rewards, choices, def.rewards().choose(), hide);
    }

    private static String periodText(QuestDefinition def) {
        String repeat = switch (def.repeat()) {
            case "daily" -> "1日1回";
            case "always" -> "何度でも";
            default -> "1回のみ";
        };
        return def.endTime().map(end -> {
            Duration left = Duration.between(LocalDateTime.now(), end);
            if (left.isNegative()) return repeat + " / 期限切れ";
            long days = left.toDays();
            String rest = days > 0 ? "あと" + days + "日" : "あと" + Math.max(1, left.toHours()) + "時間";
            return repeat + " / " + end.format(DateTimeFormatter.ofPattern("M/d HH:mm")) + " まで（" + rest + "）";
        }).orElse(repeat);
    }

    /** 目標の説明。JSONに label があればそれ、なければ自動で作る。 */
    public static Component label(QuestDefinition.Objective o) {
        if (o.label().isPresent()) return o.label().get();
        String t = o.target();
        return switch (o.type()) {
            case "break_block" -> Component.empty().append(blockName(t)).append(" を壊す");
            case "kill_entity" -> Component.empty().append(entityName(t)).append(" を倒す");
            case "deliver_item" -> Component.empty().append(itemName(t)).append(" を納品");
            case "reach_level" -> {
                JobDefinition jd = JobManager.INSTANCE.get(ResourceLocation.tryParse(t));
                yield Component.empty().append(jd != null ? jd.name() : Component.literal(t)).append(" のレベル");
            }
            default -> Component.literal(o.type() + " " + t);
        };
    }

    private static Component blockName(String t) {
        if (t.equals("*")) return Component.literal("ブロック");
        if (t.startsWith("#")) return Component.literal(t);
        ResourceLocation id = ResourceLocation.tryParse(t);
        return id == null ? Component.literal(t) : BuiltInRegistries.BLOCK.get(id).getName();
    }

    private static Component entityName(String t) {
        if (t.equals("*")) return Component.literal("モンスター");
        if (t.startsWith("#")) return Component.literal(t);
        ResourceLocation id = ResourceLocation.tryParse(t);
        return id == null ? Component.literal(t) : BuiltInRegistries.ENTITY_TYPE.get(id).getDescription();
    }

    private static Component itemName(String t) {
        if (t.startsWith("#")) return Component.literal(t);
        ResourceLocation id = ResourceLocation.tryParse(t);
        return id == null ? Component.literal(t) : new ItemStack(BuiltInRegistries.ITEM.get(id)).getHoverName();
    }

    // ---------------- 判定の小道具 ----------------

    private static boolean matchesBlock(String target, BlockState state) {
        if (target.equals("*")) return true;
        if (target.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
            return tag != null && state.is(TagKey.create(Registries.BLOCK, tag));
        }
        ResourceLocation id = ResourceLocation.tryParse(target);
        return id != null && id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static boolean matchesEntity(String target, Entity entity) {
        if (target.equals("*")) return true;
        if (target.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
            return tag != null && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, tag));
        }
        ResourceLocation id = ResourceLocation.tryParse(target);
        return id != null && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    private static boolean matchesItem(String target, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (target.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        ResourceLocation id = ResourceLocation.tryParse(target);
        return id != null && id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static int countItems(Inventory inv, String target) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (matchesItem(target, s)) n += s.getCount();
        }
        return n;
    }

    private static void removeItems(Inventory inv, String target, int count) {
        for (int i = 0; i < inv.getContainerSize() && count > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!matchesItem(target, s)) continue;
            int take = Math.min(count, s.getCount());
            s.shrink(take);
            count -= take;
        }
        inv.setChanged();
    }

    private QuestService() {}
}
