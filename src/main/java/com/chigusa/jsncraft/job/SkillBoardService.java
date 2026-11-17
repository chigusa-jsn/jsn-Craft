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
import net.minecraft.network.chat.MutableComponent;
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
 * - ステータスのパネルが効くのは「今就いている職業」のボードだけ。
 * - 技・パッシブ・技の強化パネル・ボードで覚えたパッシブは、通ってきた職業のボードからも引き継ぐ。
 *   たとえば蒼天の剣聖なら、蒼刃の剣士で開けた「旋風斬・極」は効くが、「攻撃力+1」は効かない。
 */
public final class SkillBoardService {

    private static final String MODIFIER_PREFIX = "board/";

    /** ステータス画面にいつも出す能力値。 */
    private static final List<ResourceLocation> BASE_STATS = List.of(
            ResourceLocation.withDefaultNamespace("generic.max_health"),
            JsnCraft.id("max_mana"),
            JsnCraft.id("mana_regen"),
            JsnCraft.id("max_stamina"),
            JsnCraft.id("stamina_regen"),
            ResourceLocation.withDefaultNamespace("generic.attack_damage"),
            ResourceLocation.withDefaultNamespace("generic.attack_speed"),
            JsnCraft.id("crit_chance"),
            JsnCraft.id("crit_damage"),
            ResourceLocation.withDefaultNamespace("generic.armor"),
            ResourceLocation.withDefaultNamespace("generic.movement_speed"),
            ResourceLocation.withDefaultNamespace("player.block_break_speed"),
            JsnCraft.id("mining_fortune"),
            JsnCraft.id("hp_regen"),
            ResourceLocation.withDefaultNamespace("generic.luck"));

    // ---------------- 効いているボード ----------------

    /** その枠の職業と、そこに至るまでに通ってきた職業（転職条件をたどる）。今の職業が先頭。 */
    public static List<ResourceLocation> boardChain(PlayerJobData data, JobType type) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        collect(data.getJob(type), out);
        return new ArrayList<>(out);
    }

    private static void collect(@Nullable ResourceLocation jobId, Set<ResourceLocation> out) {
        JobDefinition def = JobManager.INSTANCE.get(jobId);
        if (def == null || !out.add(jobId)) return;
        for (JobDefinition.Requirement req : def.requires()) collect(req.job(), out);
    }

    /** 今就いている職業（生活・戦闘）。ステータスのパネルはこのボードだけ効く。 */
    public static Set<ResourceLocation> currentJobs(PlayerJobData data) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (JobType type : JobType.values()) {
            ResourceLocation id = data.getJob(type);
            if (id != null && JobManager.INSTANCE.get(id) != null) out.add(id);
        }
        return out;
    }

    /** 生活・戦闘の両方で、今効いているボードの職業（重複なし）。 */
    public static Set<ResourceLocation> allActiveBoards(PlayerJobData data) {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        for (JobType type : JobType.values()) out.addAll(boardChain(data, type));
        return out;
    }

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
    public static Component unlock(ServerPlayer player, ResourceLocation jobId, String nodeId) {
        PlayerJobData data = JobService.data(player);
        if (!allActiveBoards(data).contains(jobId)) return Component.literal("今の職業のボードではありません");
        JobDefinition def = JobManager.INSTANCE.get(jobId);
        if (def == null) return Component.literal("その職業は存在しません");

        Optional<BoardNode> found = def.node(nodeId);
        if (found.isEmpty()) return Component.literal("そのマスはありません");
        BoardNode node = found.get();

        JobProgress prog = data.progress(jobId);
        if (prog.unlocked.contains(nodeId)) return Component.literal("もう解放しています");
        if (!isReachable(def, prog.unlocked, node)) return Component.literal("つながっているマスを先に解放してください");
        if (prog.level < node.minLevel()) return Component.literal("このパネルは Lv." + node.minLevel() + " から開けられます");
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

    /** その枠の職業と、通ってきた職業のボードを全部リセットする（忘却の宝珠）。 */
    public static void resetChain(ServerPlayer player, JobType type) {
        PlayerJobData data = JobService.data(player);
        for (ResourceLocation jobId : boardChain(data, type)) data.progress(jobId).unlocked.clear();
        applyBonuses(player);
        sendSnapshot(player, false);
    }

    /** 今就いている職業のボードをリセットして、ポイントを戻す。 */
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

    /** いったん全部外してから、効いているボードの分を付け直す。 */
    public static void applyBonuses(ServerPlayer player) {
        BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
            AttributeInstance inst = player.getAttribute(holder);
            if (inst == null) return;
            for (AttributeModifier m : List.copyOf(inst.getModifiers())) {
                if (isOurs(m)) inst.removeModifier(m.id());
            }
        });

        Map<Key, Double> totals = new LinkedHashMap<>();
        PlayerJobData data = JobService.data(player);
        for (ResourceLocation jobId : currentJobs(data)) {
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (String nodeId : data.progress(jobId).unlocked) {
                def.node(nodeId).ifPresent(node -> {
                    for (BoardNode.Effect e : node.effects()) {
                        e.attribute().ifPresent(a -> totals.merge(new Key(a, e.operation()), e.amount(), Double::sum));
                    }
                });
            }
        }

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

        Set<ResourceLocation> statIds = new LinkedHashSet<>(BASE_STATS);
        for (ResourceLocation jobId : currentJobs(data)) {
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (BoardNode node : def.board()) {
                for (BoardNode.Effect e : node.effects()) e.attribute().ifPresent(statIds::add);
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

        return new BoardSnapshotPayload(open, stats, boardViews(data, JobType.COMBAT), boardViews(data, JobType.LIFE),
                new ArrayList<>(com.chigusa.jsncraft.quest.QuestService.data(player).titles));
    }

    private static List<BoardView> boardViews(PlayerJobData data, JobType type) {
        List<BoardView> out = new ArrayList<>();
        boolean first = true;
        for (ResourceLocation jobId : boardChain(data, type)) {
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            JobProgress prog = data.progress(jobId);
            List<NodeView> nodes = new ArrayList<>();
            for (BoardNode n : def.board()) {
                // ？パネルは、開けたか、隣が開いて手が届くようになるまで中身を見せない
                boolean masked = n.hidden() && !prog.unlocked.contains(n.id()) && !isReachable(def, prog.unlocked, n);
                List<Component> effects = new ArrayList<>();
                if (!masked) for (BoardNode.Effect e : n.effects()) effects.add(describe(e));
                nodes.add(new NodeView(n.id(), n.x(), n.y(),
                        masked ? Component.literal("？？？") : n.name(),
                        masked ? Optional.empty() : n.icon(),
                        n.start(), n.cost(), n.links(), effects, n.minLevel(), n.big(), masked));
            }
            out.add(new BoardView(jobId.toString(), def.name(), prog.level, pointsAvailable(def, prog),
                    !first, nodes, new ArrayList<>(prog.unlocked), def.boardStyle()));
            first = false;
        }
        return out;
    }

    private static Component describe(BoardNode.Effect e) {
        // パッシブを覚える
        if (e.grant().isPresent()) {
            var s = com.chigusa.jsncraft.skill.SkillManager.INSTANCE.get(e.grant().get());
            Component name = s != null ? s.name() : Component.literal(e.grant().get().toString());
            MutableComponent line = Component.literal("新パッシブ「").append(name.copy()).append("」");
            if (s != null && !s.description().getString().isEmpty()) line.append(Component.literal("：").append(s.description().copy()));
            return line.withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        // 技の強化
        if (e.skill().isPresent()) {
            var s = com.chigusa.jsncraft.skill.SkillManager.INSTANCE.get(e.skill().get());
            Component name = s != null ? s.name() : Component.literal(e.skill().get().toString());
            String what = switch (e.field()) {
                case "power_pct" -> "威力 " + signed(e.amount() * 100) + "%";
                case "power" -> "効果 " + (Math.abs(e.amount()) < 1 ? signed(e.amount() * 100) + "%" : signed(e.amount()));
                case "radius" -> "範囲 " + signed(e.amount());
                case "cooldown" -> "待ち時間 " + signed(e.amount()) + "秒";
                case "stamina" -> "消費ST " + signed(e.amount());
                case "amplifier" -> "回数 " + signed(e.amount());
                case "duration" -> "時間 " + signed(e.amount()) + "秒";
                case "cast_time" -> "ため " + signed(e.amount()) + "秒";
                case "chance" -> "確率 " + signed(e.amount() * 100) + "%";
                default -> e.field() + " " + signed(e.amount());
            };
            return Component.literal("").append(name.copy()).append(" " + what).withStyle(ChatFormatting.GOLD);
        }
        // 能力値
        ResourceLocation attr = e.attribute().orElse(ResourceLocation.withDefaultNamespace("unknown"));
        Component name = BuiltInRegistries.ATTRIBUTE.getHolder(attr)
                .<Component>map(h -> Component.translatable(h.value().getDescriptionId()))
                .orElse(Component.literal(attr.toString()));
        boolean pctStat = PERCENT_STATS.contains(attr.getPath());
        String amount = e.operation() == AttributeModifier.Operation.ADD_VALUE
                ? (pctStat ? signed(e.amount() * 100) + "%" : signed(e.amount()))
                : signed(e.amount() * 100) + "%";
        return Component.empty().append(name).append(" " + amount).withStyle(ChatFormatting.BLUE);
    }

    /** 0.05 を「5%」と見せたい能力値 */
    private static final Set<String> PERCENT_STATS = Set.of("crit_chance", "crit_damage", "skill_damage", "cooldown_reduction",
            "stamina_cost_reduction", "damage_reduction", "lifesteal", "job_xp_bonus", "boss_damage", "tool_durability", "generic.knockback_resistance",
            "fishing_zone", "fishing_calm", "fishing_reel", "fishing_treasure", "fishing_bite", "generic.water_movement_efficiency", "harvest_fortune", "cooking_mastery",
            "magic_power", "cast_speed", "healing_power");

    // ---------------- 技の強化・覚えたパッシブ（他から使う） ----------------

    /** 今効いているボードで、このスキルにかかっている強化の合計（field → 量） */
    public static Map<String, Double> skillMods(ServerPlayer player, ResourceLocation skill) {
        Map<String, Double> out = new LinkedHashMap<>();
        PlayerJobData data = JobService.data(player);
        for (ResourceLocation jobId : allActiveBoards(data)) {
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (String nodeId : data.progress(jobId).unlocked) {
                def.node(nodeId).ifPresent(node -> {
                    for (BoardNode.Effect e : node.effects()) {
                        if (e.skill().isPresent() && e.skill().get().equals(skill)) out.merge(e.field(), e.amount(), Double::sum);
                    }
                });
            }
        }
        return out;
    }

    /** 今効いているボードで覚えたパッシブ */
    public static List<ResourceLocation> grantedSkills(ServerPlayer player) {
        List<ResourceLocation> out = new ArrayList<>();
        PlayerJobData data = JobService.data(player);
        for (ResourceLocation jobId : allActiveBoards(data)) {
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (String nodeId : data.progress(jobId).unlocked) {
                def.node(nodeId).ifPresent(node -> {
                    for (BoardNode.Effect e : node.effects()) e.grant().ifPresent(out::add);
                });
            }
        }
        return out;
    }

    private static String fmt(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String signed(double v) {
        return (v >= 0 ? "+" : "") + fmt(v);
    }

    private SkillBoardService() {}
}
