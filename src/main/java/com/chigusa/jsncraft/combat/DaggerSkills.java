package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.magic.MageSpells;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.CircleOptions;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 暗殺者（影走りの暗殺者・夜帳の影王）の技。短剣で使う。ダメージは攻撃力 × power。
 * 背後からの攻撃・毒・出血・隠密からの一撃・死の刻印は DaggerEvents。
 */
public final class DaggerSkills {

    public static final String MARK_OWNER = "jsn_mark_owner", MARK_UNTIL = "jsn_mark_until", MARK_STORE = "jsn_mark_store";
    static final DustParticleOptions SHADOW = new DustParticleOptions(new Vector3f(0.35F, 0.15F, 0.55F), 1.5F),
            BLOOD = new DustParticleOptions(new Vector3f(0.75F, 0.05F, 0.1F), 1.2F);

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "dagger_shadow_step" -> shadowStep(p, s);
            case "dagger_twin" -> twin(p, s);
            case "dagger_poison" -> poison(p, s);
            case "dagger_smoke" -> smoke(p, s);
            case "dagger_knives" -> knives(p, s);
            case "dagger_stealth" -> stealth(p, s);
            case "dagger_assassinate" -> assassinate(p, s);
            case "dagger_dance" -> dance(p, s);
            case "dagger_thousand" -> thousand(p, s);
            case "dagger_death_mark" -> deathMark(p, s);
            case "dagger_netherworld" -> netherworld(p, s);
            default -> false;
        };
    }

    // ================= 共通 =================

    /** 攻撃する側が、相手の背中側にいるか */
    public static boolean behind(LivingEntity attacker, LivingEntity victim) {
        Vec3 to = attacker.position().subtract(victim.position()).multiply(1, 0, 1);
        Vec3 face = Vec3.directionFromRotation(0, victim.getYRot()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-4 || face.lengthSqr() < 1e-4) return false;
        return to.normalize().dot(face.normalize()) < -0.3;
    }

    /** 相手の背中側へ回り込む */
    static void stepBehind(ServerPlayer p, LivingEntity t) {
        Vec3 face = Vec3.directionFromRotation(0, t.getYRot()).multiply(1, 0, 1).normalize();
        Vec3 to = t.position().subtract(face.scale(t.getBbWidth() / 2 + 0.9));
        ServerLevel level = p.serverLevel();
        if (!level.noCollision(p, p.getBoundingBox().move(to.subtract(p.position())))) to = t.position().add(face.scale(t.getBbWidth() / 2 + 0.9));
        level.sendParticles(SHADOW, p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.6, 0.3, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 0.8, p.getZ(), 6, 0.2, 0.4, 0.2, 0.02);
        Vec3 d = t.position().subtract(to);
        float yaw = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
        p.teleportTo(level, to.x, to.y, to.z, yaw, p.getXRot());
        p.fallDistance = 0;
        level.sendParticles(SHADOW, to.x, to.y + 1, to.z, 20, 0.3, 0.6, 0.3, 0);
    }

    static void slashFx(ServerLevel level, Vec3 at, int k) {
        Vfx.slash(level, at, 1.3F, k % 2 == 0 ? Vfx.WHITE : Vfx.RED, k % 2 == 0 ? 35 : 145);
        level.sendParticles(SHADOW, at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0);
    }

    static void swing(ServerPlayer p, int k) {
        p.swing(k % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
    }

    // ================= 影走りの暗殺者 =================

    /** 影渡り：ねらった敵の背後へ回り込む */
    private static boolean shadowStep(ServerPlayer p, SkillDefinition s) {
        LivingEntity t = MageSpells.lookTarget(p, s.radius());
        if (t == null) {
            p.displayClientMessage(Component.literal("ねらう敵がいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        stepBehind(p, t);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }

    /** 双牙：左右の短剣ですばやく2回切る */
    private static boolean twin(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        int n = Math.max(2, s.amplifier());
        for (int i = 0; i < n; i++) {
            int k = i;
            SkillEffects.later(p, i * 3, () -> {
                if (!p.isAlive()) return;
                swing(p, k);
                for (LivingEntity e : AxeSkills.around(p, FistSkills.front(p, 1.4), s.radius())) SkillEffects.hit(p, e, dmg);
                slashFx(level, FistSkills.front(p, 1.5).add(0, 1.1, 0), k);
                level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, 1.6F + k * 0.1F);
            });
        }
        return true;
    }

    /** 毒刃：しばらく攻撃に毒がつく */
    private static boolean poison(ServerPlayer p, SkillDefinition s) {
        TimedFlags.set(p, "dagger_poison", Math.max(0, s.amplifier()) + 1, s.duration());   // 強さ＝毒のレベル+1
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 1, p.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 0.6F);
        p.displayClientMessage(Component.literal("毒刃：" + Math.round(s.duration()) + "秒間、攻撃に毒がつく").withStyle(ChatFormatting.DARK_GREEN), true);
        return true;
    }

    /** 煙玉：周りの敵の目をくらませ、自分は少し姿を消す */
    private static boolean smoke(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        for (LivingEntity e : AxeSkills.around(p, p.position(), s.radius())) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
            if (e instanceof Mob m && m.getTarget() == p) {
                m.setTarget(null);
                m.getNavigation().stop();
            }
        }
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
        MageSpells.repeat(p, 5, ticks / 5, i -> level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 20, s.radius() * 0.4, 0.8, s.radius() * 0.4, 0.01));
        level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    /** 投げナイフ：扇の形にナイフを投げる */
    private static boolean knives(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        int n = Math.max(1, s.amplifier());
        ItemParticleOption bits = new ItemParticleOption(ParticleTypes.ITEM, p.getMainHandItem().copyWithCount(1));
        Vec3 look = p.getLookAngle();
        for (int i = 0; i < n; i++) {
            float ang = (float) Math.toRadians((i - (n - 1) / 2.0) * 9);
            Vec3 v = look.yRot(ang).scale(1.8);
            MageSpells.shoot(p, p.getEyePosition().add(0, -0.2, 0), v, 10, 1, 0, bits, e -> SkillEffects.hit(p, e, dmg), at -> {});
        }
        swing(p, 0);
        swing(p, 1);
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.8F);
        return true;
    }

    /** 隠密：姿を消し、敵に見失わせる。隠れたまま殴った最初の一撃は2倍 */
    private static boolean stealth(ServerPlayer p, SkillDefinition s) {
        int ticks = (int) Math.round(s.duration() * 20);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false));
        TimedFlags.set(p, "dagger_stealth", s.power(), s.duration());
        for (LivingEntity e : AxeSkills.around(p, p.position(), 16)) {
            if (e instanceof Mob m && m.getTarget() == p) {
                m.setTarget(null);
                m.getNavigation().stop();
            }
        }
        ServerLevel level = p.serverLevel();
        level.sendParticles(SHADOW, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.2F);
        p.displayClientMessage(Component.literal("隠密：姿を消した").withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    /** 暗殺：目の前の敵に渾身の一撃。弱った敵（HP30%以下）には2倍、背後からなら1.5倍 */
    private static boolean assassinate(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        LivingEntity t = MageSpells.lookTarget(p, s.radius());
        if (t == null) {
            p.displayClientMessage(Component.literal("ねらう敵がいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        float dmg = AxeSkills.atk(p) * (float) s.power();
        String note = "";
        if (t.getHealth() <= t.getMaxHealth() * 0.3F) {
            dmg *= 2;
            note = "（とどめ）";
        }
        if (behind(p, t)) {
            dmg *= 1.5F;
            note += "（背後）";
        }
        if (p.distanceTo(t) > 3) stepBehind(p, t);
        swing(p, 0);
        SkillEffects.hit(p, t, dmg);
        Vec3 c = t.getBoundingBox().getCenter();
        Vfx.slash(level, c, 2.4F, Vfx.RED, 45);
        Vfx.slash(level, c, 2.0F, Vfx.WHITE, 135);
        level.sendParticles(BLOOD, c.x, c.y, c.z, 30, 0.3, 0.4, 0.3, 0);
        level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 20, 0.3, 0.4, 0.3, 0.4);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2F, 0.6F);
        if (!t.isAlive()) p.displayClientMessage(Component.literal("暗殺" + note).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        return true;
    }

    /** 周りの敵を近い順に（count 体まで） */
    static List<LivingEntity> nearest(ServerPlayer p, double r, int count) {
        List<LivingEntity> list = new ArrayList<>(AxeSkills.around(p, p.position(), r));
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return list.subList(0, Math.min(count, list.size()));
    }

    /** 影舞：周りの敵の背後へ次々と飛び、切りつける */
    private static boolean dance(ServerPlayer p, SkillDefinition s) {
        List<LivingEntity> targets = nearest(p, s.radius(), Math.max(1, s.amplifier()));
        if (targets.isEmpty()) {
            p.displayClientMessage(Component.literal("近くに敵がいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        for (int i = 0; i < targets.size(); i++) {
            int k = i;
            LivingEntity t = targets.get(i);
            SkillEffects.later(p, i * 4, () -> {
                if (!p.isAlive() || !t.isAlive()) return;
                stepBehind(p, t);
                swing(p, k);
                SkillEffects.hit(p, t, dmg * 1.5F);   // 背後から
                slashFx(level, t.getBoundingBox().getCenter(), k);
                level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.5F);
            });
        }
        return true;
    }

    // ================= 夜帳の影王 =================

    /** 千刃：左右の短剣で目の前を切り刻む */
    private static boolean thousand(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        int n = Math.max(1, s.amplifier());
        for (int i = 0; i < n; i++) {
            int k = i;
            SkillEffects.later(p, i * 2, () -> {
                if (!p.isAlive()) return;
                swing(p, k);
                for (LivingEntity e : AxeSkills.around(p, FistSkills.front(p, 1.4), s.radius())) {
                    SkillEffects.hit(p, e, dmg);
                    e.setDeltaMovement(e.getDeltaMovement().multiply(0.2, 1, 0.2));
                }
                if (k % 2 == 0) slashFx(level, FistSkills.front(p, 1.5).add((level.random.nextDouble() - 0.5), 1.0 + level.random.nextDouble() * 0.5, (level.random.nextDouble() - 0.5)), k);
                if (k % 3 == 0) level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.6F + (k % 4) * 0.1F);
            });
        }
        return true;
    }

    /** 死の刻印：刻印した敵が受けたダメージをため、時間がたつと爆ぜて返す */
    private static boolean deathMark(ServerPlayer p, SkillDefinition s) {
        LivingEntity t = MageSpells.lookTarget(p, 20);
        if (t == null) {
            p.displayClientMessage(Component.literal("ねらう敵がいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        t.getPersistentData().putUUID(MARK_OWNER, p.getUUID());
        t.getPersistentData().putLong(MARK_UNTIL, level.getGameTime() + ticks);
        t.getPersistentData().putFloat(MARK_STORE, 0);
        Vfx.circle(level, t.position().add(0, t.getBbHeight() + 0.6, 0), Circle.DARK, 0.8F, ticks, 0);
        MageSpells.repeat(p, 10, ticks / 10, i -> {
            if (t.isAlive()) level.sendParticles(SHADOW, t.getX(), t.getY() + t.getBbHeight() + 0.3, t.getZ(), 4, 0.2, 0.1, 0.2, 0);
        });
        level.playSound(null, t.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.5F, 1.6F);
        SkillEffects.later(p, ticks, () -> {
            if (!t.isAlive()) return;
            float stored = t.getPersistentData().getFloat(MARK_STORE);
            t.getPersistentData().remove(MARK_OWNER);
            t.getPersistentData().remove(MARK_UNTIL);
            t.getPersistentData().remove(MARK_STORE);
            float dmg = stored * (float) s.power() + AxeSkills.atk(p) * 2;
            SkillEffects.hit(p, t, dmg);
            Vec3 c = t.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z, 40, 0.4, 0.6, 0.4, 0.1);
            level.sendParticles(SHADOW, c.x, c.y, c.z, 40, 0.5, 0.6, 0.5, 0);
            level.playSound(null, t.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 0.8F, 1.2F);
            p.displayClientMessage(Component.literal("死の刻印が爆ぜた：" + Math.round(dmg) + "ダメージ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
        });
        return true;
    }

    /** 奥義・冥府送り：姿を消し、周りの敵を次々と背後から切り捨てる */
    private static boolean netherworld(ServerPlayer p, SkillDefinition s) {
        List<LivingEntity> targets = nearest(p, s.radius(), Math.max(1, s.amplifier()));
        if (targets.isEmpty()) {
            p.displayClientMessage(Component.literal("近くに敵がいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        Vec3 start = p.position();
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, targets.size() * 3 + 20, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, targets.size() * 3 + 20, 4, false, false));
        Vfx.magicCircle(level, start, (float) s.radius(), targets.size() * 3 + 20, Circle.DARK);
        Vfx.circle(level, start.add(0, 6, 0), Circle.DARK, (float) s.radius(), targets.size() * 3 + 20, CircleOptions.SKY);
        level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.8F, 1.6F);
        for (int i = 0; i < targets.size(); i++) {
            int k = i;
            LivingEntity t = targets.get(i);
            SkillEffects.later(p, i * 3, () -> {
                if (!p.isAlive() || !t.isAlive()) return;
                stepBehind(p, t);
                swing(p, k);
                SkillEffects.hit(p, t, dmg);
                Vec3 c = t.getBoundingBox().getCenter();
                Vfx.slash(level, c, 2.2F, Vfx.RED, k * 50);
                level.sendParticles(BLOOD, c.x, c.y, c.z, 15, 0.3, 0.4, 0.3, 0);
                level.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 4, 0.2, 0.3, 0.2, 0.03);
                level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 0.8F + k * 0.08F);
            });
        }
        SkillEffects.later(p, targets.size() * 3 + 2, () -> {
            if (!p.isAlive()) return;
            p.removeEffect(MobEffects.INVISIBILITY);
            level.sendParticles(SHADOW, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0);
            p.displayClientMessage(Component.literal("冥府送り：" + targets.size() + "体").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
        });
        return true;
    }

    private DaggerSkills() {}
}
