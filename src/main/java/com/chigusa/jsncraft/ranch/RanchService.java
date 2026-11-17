package com.chigusa.jsncraft.ranch;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.network.AnimalRankPayload;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** 牧場：家畜の★（血統）と、獣飼いのスキル。 */
public final class RanchService {

    public static final ResourceLocation RANCHER = JsnCraft.id("rancher");
    public static final ResourceLocation LORD = JsnCraft.id("ranch_lord");
    private static final String KEY = "jsn_rank";

    // ================= 職業 =================

    /** 牧場の腕のレベル（獣飼いならそのレベル、牧主なら 50 + レベル、それ以外は -1） */
    public static int ranchLevel(ServerPlayer player) {
        var data = JobService.data(player);
        ResourceLocation life = data.getJob(JobType.LIFE);
        if (RANCHER.equals(life)) return data.level(life);
        if (LORD.equals(life)) return 50 + data.level(life);
        return -1;
    }

    public static boolean isRancher(ServerPlayer player) {
        return ranchLevel(player) >= 0;
    }

    // ================= ★ =================

    /** ★1〜5（まだ決まっていなければ 0） */
    public static int rank(Entity e) {
        return e.getPersistentData().getInt(KEY);
    }

    public static int rankOr1(Entity e) {
        return Math.max(1, rank(e));
    }

    public static void setRank(Animal a, int rank) {
        rank = Math.max(1, Math.min(5, rank));
        a.getPersistentData().putInt(KEY, rank);
        if (a instanceof AbstractHorse horse) horseBonus(horse, rank);
        if (a.level() instanceof ServerLevel) PacketDistributor.sendToPlayersTrackingEntity(a, new AnimalRankPayload(a.getId(), rank));
    }

    public static void sync(ServerPlayer to, Entity e) {
        int r = rank(e);
        if (r > 0) PacketDistributor.sendToPlayer(to, new AnimalRankPayload(e.getId(), r));
    }

    /** 野生の動物：だいたい★1、ときどき★2、まれに★3 */
    public static int wildRank(RandomSource r) {
        double x = r.nextDouble();
        return x < 0.02 ? 3 : x < 0.15 ? 2 : 1;
    }

    /** 子どもの★：両親の平均 ＋ ゆらぎ ＋ 牧場の腕 ＋ スキル */
    public static int childRank(ServerPlayer breeder, Animal a, Animal b, RandomSource r) {
        double avg = (rankOr1(a) + rankOr1(b)) / 2.0;
        double score = avg + r.nextDouble() * 1.5 - 0.4;
        if (breeder != null) {
            score += Math.max(0, ranchLevel(breeder)) / 150.0 * 0.8;
            SkillDefinition g1 = SkillService.passive(breeder, "good_breed");
            SkillDefinition g2 = SkillService.passive(breeder, "good_breed2");
            if (g1 != null) score += g1.power();
            if (g2 != null) score += g2.power();
        }
        int rank = (int) Math.floor(score);
        if (breeder != null) {
            SkillDefinition pure = SkillService.passive(breeder, "pure_blood");
            if (pure != null && rank == 4 && r.nextDouble() < pure.power()) rank = 5;
            if (TimedFlags.get(breeder, "ark_blessing") > 0) rank++;
        }
        return Math.max(1, Math.min(5, rank));
    }

    /** ウマ（ロバ・ラバ・ラマも）は★で足の速さ・ジャンプ力・体力が上がる */
    private static void horseBonus(AbstractHorse horse, int rank) {
        int k = rank - 1;
        put(horse, Attributes.MOVEMENT_SPEED, "speed", 0.05 * k, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        put(horse, Attributes.JUMP_STRENGTH, "jump", 0.04 * k, AttributeModifier.Operation.ADD_VALUE);
        put(horse, Attributes.MAX_HEALTH, "health", 2.0 * k, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void put(LivingEntity e, Holder<Attribute> attr, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        ResourceLocation id = JsnCraft.id("ranch_rank_" + name);
        inst.removeModifier(id);
        if (amount != 0) inst.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    /** ★の表示（★★★☆☆） */
    public static String stars(int rank) {
        return "★".repeat(rank) + "☆".repeat(5 - rank);
    }

    // ================= スキル =================

    /** 呼び寄せ：周りの家畜を自分のそばへ */
    public static boolean animalCall(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int n = 0;
        for (Animal a : level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(s.radius()))) {
            if (a.isLeashed() || a.isVehicle()) continue;
            double ang = player.getRandom().nextDouble() * Math.PI * 2;
            Vec3 to = player.position().add(Math.cos(ang) * 2.5, 0, Math.sin(ang) * 2.5);
            a.teleportTo(to.x, player.getY(), to.z);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, a.getX(), a.getY() + 0.8, a.getZ(), 4, 0.3, 0.3, 0.3, 0);
            n++;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        player.displayClientMessage(Component.literal("呼び寄せ：" + n + "頭が集まった").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    /** 群れの恵み：周りの家畜を癒して、子どもはすぐ大人に */
    public static boolean herdBlessing(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (Animal a : level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(s.radius()))) {
            a.heal(a.getMaxHealth());
            if (a.isBaby()) a.setAge(0);
            level.sendParticles(ParticleTypes.HEART, a.getX(), a.getY() + a.getBbHeight() + 0.3, a.getZ(), 2, 0.3, 0.2, 0.3, 0);
        }
        Vfx.ring(level, player.position().add(0, 0.1, 0), (float) s.radius(), 16, Vfx.GREEN);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 群れの突進：周りの敵にダメージを与えて吹き飛ばす */
    public static boolean stampede(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(s.radius()))) {
            if (!SkillEffects.isEnemy(player, e) || e.distanceTo(player) > s.radius()) continue;
            SkillEffects.hit(player, e, (float) s.power());
            Vec3 push = e.position().subtract(player.position()).normalize().scale(1.2);
            e.push(push.x, 0.45, push.z);
            e.hurtMarked = true;
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, e.getX(), e.getY() + 0.3, e.getZ(), 4, 0.3, 0.1, 0.3, 0.02);
        }
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 60, s.radius() * 0.4, 0.1, s.radius() * 0.4, 0.05);
        Vfx.ring(level, player.position().add(0, 0.1, 0), (float) s.radius(), 20, Vfx.GOLD);
        level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8F, 1.3F);
        level.playSound(null, player.blockPosition(), SoundEvents.COW_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /** 方舟の祝福：しばらくの間、生まれる子どもの★がひとつ上がる */
    public static boolean arkBlessing(ServerPlayer player, SkillDefinition s) {
        TimedFlags.set(player, "ark_blessing", 1, s.duration());
        ServerLevel level = player.serverLevel();
        Vfx.magicCircle(level, player.position(), 3.0F, 40, Vfx.GOLD);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 50, 1.5, 0.8, 1.5, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        player.displayClientMessage(Component.literal("方舟の祝福：" + Math.round(s.duration()) + "秒間、生まれる子の★がひとつ上がる！")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    private RanchService() {}
}
