package com.chigusa.jsncraft.job;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.data.JobProgress;
import com.chigusa.jsncraft.data.PlayerJobData;
import com.chigusa.jsncraft.network.BoardSnapshotPayload;
import com.chigusa.jsncraft.network.BoardSnapshotPayload.BoardView;
import com.chigusa.jsncraft.network.BoardSnapshotPayload.NodeView;
import com.chigusa.jsncraft.network.BoardSnapshotPayload.StatLine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * スキルボードまわり：ポイントの計算、マスの解放、能力値への反映、画面に送る情報づくり。
 *
 * - スキルポイントは職業ごと。(レベル - 1) × skill_points_per_level がもらえる分で、
 *   解放したマスのコストの合計を引いた残りが使える分。
 * - 能力が上がるのは、今就いている生活ジョブ・戦闘ジョブのボードだけ。
 */
public final class SkillBoardService {

    private static final String MODIFIER_PREFIX = "board/";

    /** ステータス画面にいつも出す能力値（1.21.1 のID）。 */
    private static final List<ResourceLocation> BASE_STATS = List.of(
            ResourceLocation.withDefaultNamespace("generic.max_health"),
            ResourceLocation.withDefaultNamespace("generic.attack_damage"),
            ResourceLocation.withDefaultNamespace("generic.attack_speed"),
            ResourceLocation.withDefaultNamespace("generic.armor"),
            ResourceLocation.withDefaultNamespace("generic.movement_speed"),
            ResourceLocation.withDefaultNamespace("player.block_break_speed"),
            ResourceLocation.withDefaultNamespace("generic.luck"));

    // ---------------- ポイント ----------------

    public static int pointsEarned(JobDefinition def, JobProgress prog) {
        return Math.max(0, prog.level - 1) * def.skillPointsPerLevel();
    }

    public static int pointsSpent(JobDefinition def, JobProgress prog) {
        int spent = 0;
        for (String id : prog.unlocked) {
            spent += def.node(id).map(BoardNode::cost).orElse(0);
        }
        return spent;
    }

    public static int pointsAvailable(JobDefinition def, JobProgress prog) {
        return pointsEarned(def, prog) - pointsSpent(def, prog);
    }

    private static boolean isReachable(JobDefinition def, Set<String> unlocked, BoardNode node) {
        if (node.start()) return true;
        for (String link : node.links()) if (unlocked.contains(link)) return true;
        for (BoardNode other : def.board()) {
            if (unlocked.contains(other.id()) && other.links().contains(node.id())) return true;
        }
        return false;
    }

    // ---------------- 解放・リセット ----------------

    /** マスを解放する。成功したら null、失敗したら理由を返す。 */
    @Nullable
    public static Component unlock(ServerPlayer player, JobType type, String nodeId) {
        PlayerJobData data = JobService.data(player);
        ResourceLocation jobId = data.getJob(type);
        JobDefinition def = JobManager.INSTANCE.get(jobId);
        if (def == null) return Component.literal(type.displayName() + "ジョブに就いていません");

        Optional<BoardNode> found = def.node(nodeId);
        if (found.isEmpty()) return Component.literal("そのマスはありません");
        BoardNode node = found.get();

        JobProgress prog = data.progress(jobId);
        if (prog.unlocked.contains(nodeId)) return Component.literal("もう解放しています");
        if (!isReachable(def, prog.unlocked, node)) return Component.literal("つながっているマスを先に解放してください");
        int points = pointsAvailable(def, prog);
        if (points < node.cost()) {
            return Component.literal("スキルポイントが足りません（必要 " + node.cost() + " / 残り " + points + "）");
        }

        prog.unlocked.add(nodeId);
        applyBonuses(player);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.literal("解放: ").withStyle(ChatFormatting.AQUA).append(node.name().copy()), true);
        sendSnapshot(player, false);
        return null;
    }

    /** ボードを全部リセットして、ポイントを戻す。 */
    public static void reset(ServerPlayer player, JobType type) {
        PlayerJobData data = JobService.data(player);
        ResourceLocation jobId = data.getJob(type);
        if (jobId == null) return;
        data.progress(jobId).unlocked.clear();
        applyBonuses(player);
        sendSnapshot(player, false);
    }

    // ---------------- 能力値への反映 ----------------

    private record Key(ResourceLocation attribute, AttributeModifier.Operation operation) {}

    /** いったん全部外してから、今の職業のボードの分を付け直す。 */
    public static void applyBonuses(ServerPlayer player) {
        // 外す
        BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
            AttributeInstance inst = player.getAttribute(holder);
            if (inst == null) return;
            for (AttributeModifier m : List.copyOf(inst.getModifiers())) {
                if (isOurs(m)) inst.removeModifier(m.id());
            }
        });

        // 合計する
        Map<Key, Double> totals = new LinkedHashMap<>();
        PlayerJobData data = JobService.data(player);
        for (JobType type : JobType.values()) {
            ResourceLocation jobId = data.getJob(type);
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (String nodeId : data.progress(jobId).unlocked) {
                def.node(nodeId).ifPresent(node -> {
                    for (BoardNode.Effect e : node.effects()) {
                        totals.merge(new Key(e.attribute(), e.operation()), e.amount(), Double::sum);
                    }
                });
            }
        }

        // 付ける
        totals.forEach((key, amount) -> {
            Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(key.attribute());
            if (holder.isEmpty()) {
                JsnCraft.LOGGER.warn("[jsn-Craft] 知らない能力値です: {}", key.attribute());
                return;
            }
            AttributeInstance inst = player.getAttribute(holder.get());
            if (inst == null) return;
            ResourceLocation id = JsnCraft.id(MODIFIER_PREFIX + key.attribute().getNamespace() + "/"
                    + key.attribute().getPath() + "/" + key.operation().getSerializedName());
            inst.addTransientModifier(new AttributeModifier(id, amount, key.operation()));
        });

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static boolean isOurs(AttributeModifier m) {
        return m.id().getNamespace().equals(JsnCraft.MODID) && m.id().getPath().startsWith(MODIFIER_PREFIX);
    }

    // ---------------- 画面に送る情報 ----------------

    public static void sendSnapshot(ServerPlayer player, boolean open) {
        PacketDistributor.sendToPlayer(player, buildSnapshot(player, open));
    }

    public static BoardSnapshotPayload buildSnapshot(ServerPlayer player, boolean open) {
        PlayerJobData data = JobService.data(player);

        // ステータスに出す能力値：いつもの7つ＋ボードで上がるもの
        Set<ResourceLocation> statIds = new LinkedHashSet<>(BASE_STATS);
        for (JobType type : JobType.values()) {
            JobDefinition def = JobManager.INSTANCE.get(data.getJob(type));
            if (def == null) continue;
            for (BoardNode node : def.board()) {
                for (BoardNode.Effect e : node.effects()) statIds.add(e.attribute());
            }
        }
        List<StatLine> stats = new ArrayList<>();
        for (ResourceLocation id : statIds) {
            BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(holder -> {
                AttributeInstance inst = player.getAttribute(holder);
                if (inst == null) return;
                double flat = 0, percent = 0;
                for (AttributeModifier m : inst.getModifiers()) {
                    if (!isOurs(m)) continue;
                    if (m.operation() == AttributeModifier.Operation.ADD_VALUE) flat += m.amount();
                    else percent += m.amount() * 100;
                }
                StringBuilder bonus = new StringBuilder();
                if (flat != 0) bonus.append(signed(flat));
                if (percent != 0) bonus.append(bonus.isEmpty() ? "" : " ").append(signed(percent)).append("%");
                stats.add(new StatLine(Component.translatable(holder.value().getDescriptionId()), fmt(inst.getValue()), bonus.toString()));
            });
        }

        return new BoardSnapshotPayload(open, stats, boardView(data, JobType.COMBAT), boardView(data, JobType.LIFE));
    }

    private static Optional<BoardView> boardView(PlayerJobData data, JobType type) {
        ResourceLocation jobId = data.getJob(type);
        JobDefinition def = JobManager.INSTANCE.get(jobId);
        if (def == null) return Optional.empty();
        JobProgress prog = data.progress(jobId);

        List<NodeView> nodes = new ArrayList<>();
        for (BoardNode n : def.board()) {
            List<Component> effects = new ArrayList<>();
            for (BoardNode.Effect e : n.effects()) effects.add(describe(e));
            nodes.add(new NodeView(n.id(), n.x(), n.y(), n.name(), n.icon(), n.start(), n.cost(), n.links(), effects));
        }
        return Optional.of(new BoardView(type.getSerializedName(), def.name(), prog.level,
                pointsAvailable(def, prog), nodes, new ArrayList<>(prog.unlocked)));
    }

    private static Component describe(BoardNode.Effect e) {
        Component name = BuiltInRegistries.ATTRIBUTE.getHolder(e.attribute())
                .<Component>map(h -> Component.translatable(h.value().getDescriptionId()))
                .orElse(Component.literal(e.attribute().toString()));
        String amount = e.operation() == AttributeModifier.Operation.ADD_VALUE
                ? signed(e.amount())
                : signed(e.amount() * 100) + "%";
        return Component.empty().append(name).append(" " + amount).withStyle(ChatFormatting.BLUE);
    }

    private static String fmt(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String signed(double v) {
        return (v >= 0 ? "+" : "") + fmt(v);
    }

    private SkillBoardService() {}
}
