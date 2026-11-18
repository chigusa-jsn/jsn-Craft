package com.chigusa.jsncraft.skill;

import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.data.PlayerJobData;
import com.chigusa.jsncraft.job.JobDefinition;
import com.chigusa.jsncraft.job.JobManager;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.SkillBoardService;
import com.chigusa.jsncraft.network.SkillPayloads;
import com.chigusa.jsncraft.network.SkillPayloads.SkillView;
import com.chigusa.jsncraft.stat.ResourceService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * スキルの習得・スキルバー・発動。
 * 覚えているスキル＝今の職業と、そこに至るまでに通ってきた職業のスキルのうち、レベルが足りているもの。
 */
public final class SkillService {

    public static PlayerSkillData data(ServerPlayer player) {
        return player.getData(ModAttachments.SKILLS);
    }

    /** 職業ごとのスキル一覧（覚えていないものも含む）。キーはスキルID、値は職業ID。 */
    private static Map<ResourceLocation, ResourceLocation> allJobSkills(ServerPlayer player) {
        Map<ResourceLocation, ResourceLocation> out = new LinkedHashMap<>();
        PlayerJobData jobs = JobService.data(player);
        for (ResourceLocation jobId : SkillBoardService.allActiveBoards(jobs)) {
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (JobDefinition.SkillUnlock u : def.skills()) out.putIfAbsent(u.skill(), jobId);
        }
        return out;
    }

    public static boolean knows(ServerPlayer player, ResourceLocation skill) {
        PlayerJobData jobs = JobService.data(player);
        for (ResourceLocation jobId : SkillBoardService.allActiveBoards(jobs)) {
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (JobDefinition.SkillUnlock u : def.skills()) {
                if (u.skill().equals(skill) && jobs.level(jobId) >= u.level()) return true;
            }
        }
        return false;
    }

    /** 覚えているパッシブのうち、この効果のもの（なければ null）。 */
    @Nullable
    public static SkillDefinition passive(ServerPlayer player, String effect) {
        PlayerJobData jobs = JobService.data(player);
        for (ResourceLocation jobId : SkillBoardService.allActiveBoards(jobs)) {
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (JobDefinition.SkillUnlock u : def.skills()) {
                if (jobs.level(jobId) < u.level()) continue;
                SkillDefinition s = SkillManager.INSTANCE.get(u.skill());
                if (s != null && !s.isActive() && s.effect().equals(effect)) return effective(player, u.skill(), s);
            }
        }
        // ボードのパネルで覚えたパッシブ
        for (ResourceLocation id : SkillBoardService.grantedSkills(player)) {
            SkillDefinition s = SkillManager.INSTANCE.get(id);
            if (s != null && !s.isActive() && s.effect().equals(effect)) return effective(player, id, s);
        }
        return null;
    }

    /**
     * ボードの「技の強化」パネルと、待ち時間短縮・消費ST軽減を反映したスキルの中身。
     * 元の設定は変えずに、強化済みのコピーを返す。
     */
    public static SkillDefinition effective(ServerPlayer player, ResourceLocation id, SkillDefinition s) {
        Map<String, Double> m = SkillBoardService.skillMods(player, id);
        double cdr = player.getAttributeValue(com.chigusa.jsncraft.stat.ModAttributes.COOLDOWN_REDUCTION);
        double scr = player.getAttributeValue(com.chigusa.jsncraft.stat.ModAttributes.STAMINA_COST_REDUCTION);
        float stamina = (float) Math.max(0, (s.stamina() + m.getOrDefault("stamina", 0.0)) * (1 - Math.min(0.5, scr)));
        float cooldown = (float) Math.max(0, (s.cooldown() + m.getOrDefault("cooldown", 0.0)) * (1 - Math.min(0.5, cdr)));
        return new SkillDefinition(s.name(), s.description(), s.icon(), s.type(), s.effect(),
                stamina, s.mana(), cooldown,
                s.power() * (1 + m.getOrDefault("power_pct", 0.0)) + m.getOrDefault("power", 0.0),
                s.bonus(),
                s.radius() + m.getOrDefault("radius", 0.0),
                s.duration() + m.getOrDefault("duration", 0.0),
                s.chance() + m.getOrDefault("chance", 0.0),
                s.target(),
                (int) Math.round(s.amplifier() + m.getOrDefault("amplifier", 0.0)),
                (float) (Math.max(0, s.castTime() + m.getOrDefault("cast_time", 0.0))
                        * (1 - Math.min(0.6, player.getAttributeValue(com.chigusa.jsncraft.stat.ModAttributes.CAST_SPEED)))));
    }

    // ---------------- 発動 ----------------

    public static void cast(ServerPlayer player, int slot) {
        PlayerSkillData d = data(player);
        if (slot < 0 || slot >= PlayerSkillData.SLOTS) return;
        ResourceLocation id = ResourceLocation.tryParse(d.slots.get(slot));
        SkillDefinition base = SkillManager.INSTANCE.get(id);
        SkillDefinition skill = base == null ? null : effective(player, id, base);
        if (skill == null) {
            player.displayClientMessage(Component.literal((slot + 1) + "番にはスキルがセットされていません（Kキー →「スキル」）")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!skill.isActive()) return;
        if (!knows(player, id)) {
            player.displayClientMessage(Component.literal("今の職業ではそのスキルは使えません").withStyle(ChatFormatting.RED), true);
            return;
        }
        long now = player.level().getGameTime();
        if (d.casting != null) {
            player.displayClientMessage(Component.literal("ため中…").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long ready = d.readyAt.getOrDefault(id.toString(), 0L);
        if (now < ready) {
            player.displayClientMessage(Component.literal("待ち時間 あと " + String.format("%.1f", (ready - now) / 20.0) + " 秒")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (needsStaff(id) && !(player.getMainHandItem().getItem() instanceof com.chigusa.jsncraft.weapon.StaffItem)) {
            fail(player, "魔法は杖を持っていないと使えません");
            return;
        }
        var res = ResourceService.get(player);
        if (res.stamina < skill.stamina()) {
            fail(player, "スタミナが足りません");
            return;
        }
        if (res.mana < skill.mana()) {
            fail(player, "マナが足りません");
            return;
        }
        if (skill.castTime() > 0 && !instantCast(player)) {
            // ため：コストと待ち時間は先に払って、時間がたったら技が出る
            startCast(player, d, id, skill, now);
        } else if (!SkillEffects.activate(player, skill)) {
            return; // 使えない場面（対象がいないなど）
        }

        ResourceService.tryUseStamina(player, skill.stamina());
        ResourceService.tryUseMana(player, skill.mana());
        d.readyAt.put(id.toString(), now + Math.round(skill.cooldown() * 20));
        player.displayClientMessage(Component.empty().append(skill.name().copy()).withStyle(ChatFormatting.AQUA), true);
        sync(player);
    }

    /** 魔導士の魔法は杖が必要 */
    public static boolean needsStaff(ResourceLocation id) {
        String path = id == null ? "" : id.getPath();
        return path.startsWith("mage/") || path.startsWith("archmage/");
    }

    /** 詠唱破棄（ボードのパッシブ）：ためずにすぐ出る */
    private static boolean instantCast(ServerPlayer player) {
        SkillDefinition s = passive(player, "mage_instant_cast");
        if (s == null || player.getRandom().nextDouble() >= s.chance()) return false;
        player.displayClientMessage(Component.literal("詠唱破棄！").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        return true;
    }

    private static final ResourceLocation CAST_SLOW = com.chigusa.jsncraft.JsnCraft.id("casting_slow");

    private static void startCast(ServerPlayer player, PlayerSkillData d, ResourceLocation id, SkillDefinition skill, long now) {
        int ticks = Math.max(1, Math.round(skill.castTime() * 20));
        d.casting = id.toString();
        d.castEnd = now + ticks;
        var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(CAST_SLOW);
            speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(CAST_SLOW, -0.6,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        com.chigusa.jsncraft.vfx.Vfx.castCircles(player.serverLevel(), player.position(), com.chigusa.jsncraft.vfx.Circle.forSkill(id),
                ticks, com.chigusa.jsncraft.vfx.Vfx.layers(ticks));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 1.8F);
        PacketDistributor.sendToPlayer(player, new com.chigusa.jsncraft.network.CastBarPayload(skill.name(), ticks));
    }

    /** ため中の人を見て、時間になったら技を出す（毎tick） */
    public static void tickCasts(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerSkillData d = data(player);
            if (d.casting == null) continue;
            if (!player.isAlive() || player.level().getGameTime() >= d.castEnd) {
                String id = d.casting;
                d.casting = null;
                var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                if (speed != null) speed.removeModifier(CAST_SLOW);
                SkillDefinition skill = SkillManager.INSTANCE.get(ResourceLocation.tryParse(id));
                if (skill != null && player.isAlive()) {
                    ResourceLocation rid = ResourceLocation.tryParse(id);
                    SkillDefinition eff = effective(player, rid, skill);
                    com.chigusa.jsncraft.vfx.Vfx.releaseCircles(player.serverLevel(), player.position(), com.chigusa.jsncraft.vfx.Circle.forSkill(rid),
                            com.chigusa.jsncraft.vfx.Vfx.layers(Math.max(1, Math.round(eff.castTime() * 20))));
                    SkillEffects.activate(player, eff);
                }
            }
        }
    }

    private static void fail(ServerPlayer player, String msg) {
        player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.RED), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5F, 0.6F);
    }

    // ---------------- スキルバー ----------------

    public static void assign(ServerPlayer player, int slot, String skill) {
        if (slot < 0 || slot >= PlayerSkillData.SLOTS) return;
        PlayerSkillData d = data(player);
        if (!skill.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(skill);
            SkillDefinition def = SkillManager.INSTANCE.get(id);
            if (def == null || !def.isActive() || !knows(player, id)) return;
            // 同じスキルが別の枠にあったら外す（入れ替え）
            int old = d.slots.indexOf(skill);
            if (old >= 0) d.slots.set(old, d.slots.get(slot));
        }
        d.slots.set(slot, skill);
        sync(player);
    }

    /** 新しく覚えたアクティブスキルを、空いている枠に自動でセットする。 */
    public static void autoFill(ServerPlayer player) {
        PlayerSkillData d = data(player);
        for (ResourceLocation id : allJobSkills(player).keySet()) {
            SkillDefinition def = SkillManager.INSTANCE.get(id);
            if (def == null || !def.isActive() || !knows(player, id) || d.slots.contains(id.toString())) continue;
            int free = d.slots.indexOf("");
            if (free < 0) return;
            d.slots.set(free, id.toString());
        }
    }

    /** 職業レベルが上がったとき：新しく覚えたスキルを知らせる。 */
    public static void onLevelUp(ServerPlayer player, ResourceLocation jobId, int oldLevel, int newLevel) {
        JobDefinition def = JobManager.INSTANCE.get(jobId);
        if (def == null) return;
        boolean any = false;
        for (JobDefinition.SkillUnlock u : def.skills()) {
            if (u.level() > oldLevel && u.level() <= newLevel) {
                SkillDefinition s = SkillManager.INSTANCE.get(u.skill());
                if (s == null) continue;
                any = true;
                player.sendSystemMessage(Component.literal("✦ 新しいスキル「").withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(s.name().copy())
                        .append(Component.literal("」を覚えました！" + (s.isActive() ? "（Rでスキルモード）" : "（常に効果あり）"))
                                .withStyle(ChatFormatting.LIGHT_PURPLE)));
            }
        }
        if (any) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
            autoFill(player);
        }
        sync(player);
    }

    // ---------------- 画面に送る ----------------

    public static void sync(ServerPlayer player) {
        PlayerSkillData d = data(player);
        PlayerJobData jobs = JobService.data(player);
        List<SkillView> views = new ArrayList<>();
        for (ResourceLocation jobId : SkillBoardService.allActiveBoards(jobs)) {
            JobDefinition def = JobManager.INSTANCE.get(jobId);
            if (def == null) continue;
            for (JobDefinition.SkillUnlock u : def.skills()) {
                SkillDefinition s = SkillManager.INSTANCE.get(u.skill());
                if (s == null) continue;
                SkillDefinition e = effective(player, u.skill(), s); // ボードの強化を反映した値を見せる
                views.add(new SkillView(u.skill().toString(), s.name(), s.description(), s.icon(), s.isActive(), u.level(),
                        def.name(), jobs.level(jobId) >= u.level(), e.stamina(), e.mana(), e.cooldown()));
            }
        }
        long now = player.level().getGameTime();
        List<Float> left = new ArrayList<>();
        for (String id : d.slots) left.add(Math.max(0, d.readyAt.getOrDefault(id, 0L) - now) / 20F);
        PacketDistributor.sendToPlayer(player, new SkillPayloads.State(views, new ArrayList<>(d.slots), left));
    }

    private SkillService() {}
}
