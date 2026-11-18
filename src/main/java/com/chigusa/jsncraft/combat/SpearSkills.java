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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * 槍兵（潮騒の槍兵・海神の槍騎士）の技。ダメージは攻撃力 × power。水の技は敵を濡らす（魔導士の雷で感電）。
 */
public final class SpearSkills {

    static final DustParticleOptions WATER = new DustParticleOptions(new Vector3f(0.3F, 0.65F, 1.0F), 1.4F),
            FOAM = new DustParticleOptions(new Vector3f(0.85F, 0.95F, 1.0F), 1.1F);

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "spear_thrust" -> thrust(p, s);
            case "spear_sweep" -> sweep(p, s);
            case "spear_throw" -> throwSpear(p, s);
            case "spear_leap" -> leap(p, s);
            case "spear_triple" -> triple(p, s);
            case "spear_whirlpool" -> whirlpool(p, s, false);
            case "spear_wall" -> wall(p, s);
            case "spear_thousand" -> thousand(p, s);
            case "spear_maelstrom" -> whirlpool(p, s, true);
            case "spear_poseidon" -> poseidon(p, s);
            default -> false;
        };
    }

    static void splash(ServerLevel level, Vec3 q, int n) {
        level.sendParticles(ParticleTypes.SPLASH, q.x, q.y, q.z, n, 0.3, 0.2, 0.3, 0.2);
        level.sendParticles(WATER, q.x, q.y, q.z, n / 2, 0.3, 0.3, 0.3, 0);
    }

    /** 槍の一撃の筋（白い線） */
    static void streak(ServerLevel level, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        for (double t = 0; t < len; t += 0.35) {
            Vec3 q = a.add(d.scale(t / len));
            level.sendParticles(FOAM, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.CRIT, b.x, b.y, b.z, 6, 0.1, 0.1, 0.1, 0.2);
    }

    // ================= 潮騒の槍兵 =================

    /** 一閃突き：前へ駆け抜けながら、一直線に突き抜ける */
    private static boolean thrust(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = AxeSkills.flatLook(p);
        Vec3 start = p.position();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        Vec3 dest = start.add(dir.scale(s.radius()));
        var clip = level.clip(new ClipContext(start.add(0, 1, 0), dest.add(0, 1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = clip.getLocation().subtract(0, 1, 0).subtract(dir.scale(0.6));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.2, 1.5, 1.2), e -> SkillEffects.isEnemy(p, e))) {
            if (e.getBoundingBox().inflate(0.9).clip(start.add(0, 1, 0), end.add(0, 1, 0)).isEmpty()) continue;
            SkillEffects.hit(p, e, dmg);
        }
        streak(level, start.add(0, 1.1, 0), end.add(0, 1.1, 0).add(dir.scale(1.5)));
        p.teleportTo(end.x, end.y, end.z);
        p.fallDistance = 0;
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
        return true;
    }

    /** 薙ぎ払い：槍を大きく振って周りを払う */
    private static boolean sweep(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        for (LivingEntity e : AxeSkills.around(p, c, s.radius())) {
            SkillEffects.hit(p, e, dmg);
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
            if (away.lengthSqr() > 0.01) e.knockback(0.9, -away.normalize().x, -away.normalize().z);
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45 + p.getYRot());
            Vfx.slash(level, c.add(Math.cos(a) * s.radius() * 0.65, 1.0, Math.sin(a) * s.radius() * 0.65), 1.6F, k % 2 == 0 ? Vfx.AZURE : Vfx.WHITE, k * 45);
        }
        Vfx.ring(level, c, (float) s.radius() * 1.1F, 10, Vfx.AZURE);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);
        return true;
    }

    /** 投げ槍：槍を投げる。通り道を貫いて、手元に戻ってくる */
    private static boolean throwSpear(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        Vec3 start = p.getEyePosition().add(0, -0.3, 0);
        Vec3 dir = p.getLookAngle();
        var clip = level.clip(new ClipContext(start, start.add(dir.scale(s.radius())), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double reached = clip.getLocation().distanceTo(start);
        int go = Math.max(2, (int) Math.ceil(reached / 1.6));
        ItemParticleOption bits = new ItemParticleOption(ParticleTypes.ITEM, p.getMainHandItem().copyWithCount(1));
        Set<Integer> hit = new HashSet<>();
        for (int i = 1; i <= go * 2; i++) {
            int k = i;
            SkillEffects.later(p, i, () -> {
                double d = k <= go ? reached * k / go : reached * (2 * go - k) / go;
                Vec3 q = start.add(dir.scale(d));
                level.sendParticles(bits, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0.02);
                level.sendParticles(FOAM, q.x, q.y, q.z, 2, 0.1, 0.1, 0.1, 0);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(q, q).inflate(1.0), e -> SkillEffects.isEnemy(p, e))) {
                    if (!hit.add(e.getId() * 2 + (k <= go ? 0 : 1))) continue;
                    SkillEffects.hit(p, e, dmg);
                }
            });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        SkillEffects.later(p, go * 2, () -> level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 1.0F));
        return true;
    }

    /** 跳び突き：高く跳び、目線の先へ真下から突き刺す */
    private static boolean leap(ServerPlayer p, SkillDefinition s) {
        Vec3 look = AxeSkills.flatLook(p);
        p.setDeltaMovement(look.x * 0.9, 1.1, look.z * 0.9);
        p.hurtMarked = true;
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 12, 0.4, 0.1, 0.4, 0.05);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
        waitLanding(p, s, 0);
        return true;
    }

    private static void waitLanding(ServerPlayer p, SkillDefinition s, int t) {
        SkillEffects.later(p, 1, () -> {
            if (!p.isAlive()) return;
            p.fallDistance = 0;
            if (t > 5 && (p.onGround() || t > 50)) {
                ServerLevel level = p.serverLevel();
                Vec3 c = p.position();
                float dmg = AxeSkills.atk(p) * (float) s.power();
                for (LivingEntity e : AxeSkills.around(p, c, s.radius())) {
                    SkillEffects.hit(p, e, dmg);
                    e.setDeltaMovement(e.getDeltaMovement().x, 0.6, e.getDeltaMovement().z);
                    e.hurtMarked = true;
                }
                splash(level, c.add(0, 0.3, 0), 30);
                AxeSkills.crack(level, c, 20);
                Vfx.ring(level, c, (float) s.radius() * 1.2F, 10, Vfx.AZURE);
                level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1.2F, 0.7F);
                return;
            }
            if (t > 8 && p.getDeltaMovement().y < 0.2) {
                p.setDeltaMovement(p.getDeltaMovement().x * 0.5, -1.6, p.getDeltaMovement().z * 0.5);
                p.hurtMarked = true;
            }
            p.serverLevel().sendParticles(FOAM, p.getX(), p.getY() + 0.5, p.getZ(), 2, 0.2, 0.2, 0.2, 0);
            waitLanding(p, s, t + 1);
        });
    }

    /** 三段突き：すばやく3回、前を突く */
    private static boolean triple(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        int n = Math.max(1, s.amplifier());
        for (int i = 0; i < n; i++) {
            int k = i;
            SkillEffects.later(p, i * 4, () -> {
                if (!p.isAlive()) return;
                Vec3 dir = p.getLookAngle();
                Vec3 from = p.getEyePosition().add(0, -0.3, 0);
                Vec3 to = from.add(dir.scale(s.radius()));
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0), e -> SkillEffects.isEnemy(p, e))) {
                    if (e.getBoundingBox().inflate(0.7).clip(from, to).isEmpty()) continue;
                    SkillEffects.hit(p, e, k == n - 1 ? dmg * 1.5F : dmg);
                }
                streak(level, from.add(dir.scale(0.8)), to);
                level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 1.2F + k * 0.15F);
            });
        }
        return true;
    }

    /** 渦潮突き／大渦：渦を作って敵を引き込み、濡らす */
    private static boolean whirlpool(ServerPlayer p, SkillDefinition s, boolean big) {
        ServerLevel level = p.serverLevel();
        Vec3 at = big ? BowSkills.ground(p, 24).add(0, 0.3, 0) : p.position().add(AxeSkills.flatLook(p).scale(4)).add(0, 0.3, 0);
        double r = s.radius();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 2);
        if (!big) streak(level, p.getEyePosition().add(0, -0.3, 0), at.add(0, 0.7, 0));
        Vfx.magicCircle(level, at, (float) r, pulses * 10, Circle.WATER);
        if (big) Vfx.circle(level, at.add(0, 6, 0), Circle.WATER, (float) r, pulses * 10, CircleOptions.SKY);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 2.0F, big ? 0.5F : 0.8F);
        MageSpells.repeat(p, 10, pulses, i -> {
            for (int k = 0; k < (big ? 36 : 20); k++) {
                double a = i * 0.7 + k * Math.PI * 2 / (big ? 36 : 20), rr = r * (1 - (k % 4) * 0.22);
                level.sendParticles(ParticleTypes.SPLASH, at.x + Math.cos(a) * rr, at.y + 0.2, at.z + Math.sin(a) * rr, 1, 0, 0, 0, 0);
                level.sendParticles(WATER, at.x + Math.cos(a + 0.3) * rr, at.y + 0.4 + (big ? (k % 3) * 0.5 : 0), at.z + Math.sin(a + 0.3) * rr, 1, 0, 0, 0, 0);
            }
            for (LivingEntity e : AxeSkills.around(p, at, r + 1)) {
                Vec3 d = at.subtract(e.position()).multiply(1, 0, 1);
                Vec3 tangent = new Vec3(-d.z, 0, d.x);
                Vec3 v = d.lengthSqr() > 0.25 ? d.normalize().scale(0.3).add(tangent.normalize().scale(0.25)) : Vec3.ZERO;
                e.setDeltaMovement(v.x, big ? 0.1 : -0.05, v.z);
                e.hurtMarked = true;
                SkillEffects.hit(p, e, dmg);
                MageSpells.wet(e, 160);
            }
        });
        return true;
    }

    /** 槍衾：構えている間、近づいた敵を自動で突く */
    private static boolean wall(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        float dmg = AxeSkills.atk(p) * (float) s.power();
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2, false, false));
        TimedFlags.set(p, "spear_wall", 1, s.duration());
        MageSpells.repeat(p, 5, ticks / 5, i -> {
            Vec3 c = p.position();
            for (LivingEntity e : AxeSkills.around(p, c, s.radius())) {
                SkillEffects.hit(p, e, dmg);
                Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
                if (away.lengthSqr() > 0.01) e.knockback(0.6, -away.normalize().x, -away.normalize().z);
                streak(level, c.add(0, 1.1, 0), e.getBoundingBox().getCenter());
            }
            for (int k = 0; k < 6; k++) {
                double a = i * 0.5 + k * Math.PI / 3;
                level.sendParticles(FOAM, c.x + Math.cos(a) * s.radius() * 0.7, c.y + 1.0, c.z + Math.sin(a) * s.radius() * 0.7, 1, 0, 0, 0, 0);
            }
        });
        Vfx.ring(level, p.position(), (float) s.radius(), 12, Vfx.AZURE);
        level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
        p.displayClientMessage(Component.literal("槍衾！").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        return true;
    }

    // ================= 海神の槍騎士 =================

    /** 千本槍：空から光の槍の雨を降らせる */
    private static boolean thousand(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 at = BowSkills.ground(p, 28);
        double r = s.radius();
        float dmg = AxeSkills.atk(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 4);
        Vfx.magicCircle(level, at, (float) r, pulses * 5 + 10, Circle.WATER);
        Vfx.circle(level, at.add(0, 10, 0), Circle.WATER, (float) r, pulses * 5 + 10, CircleOptions.SKY);
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
        MageSpells.repeat(p, 5, pulses, i -> {
            var rnd = level.random;
            for (int k = 0; k < 3; k++) {
                double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
                Vec3 q = at.add(Math.cos(a) * d, 0, Math.sin(a) * d);
                for (int h = 0; h < 10; h++) level.sendParticles(FOAM, q.x, q.y + 1 + h * 0.6, q.z, 1, 0.02, 0.1, 0.02, 0);
                splash(level, q.add(0, 0.2, 0), 6);
                for (LivingEntity e : AxeSkills.around(p, q, 1.6)) SkillEffects.hit(p, e, dmg);
            }
            if (i % 3 == 0) level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.0F + rnd.nextFloat() * 0.4F);
        });
        return true;
    }

    /** 奥義・海神降臨：前へ大波を走らせ、周りの敵に雷を落とす */
    private static boolean poseidon(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = AxeSkills.flatLook(p);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        Vec3 start = p.position();
        int len = 14;
        float wave = AxeSkills.atk(p) * (float) s.power();
        Set<Integer> hit = new HashSet<>();
        Vfx.magicCircle(level, start, 3.5F, 40, Circle.WATER);
        Vfx.circle(level, start.add(0, 8, 0), Circle.THUNDER, 5.0F, 40, CircleOptions.SKY);
        level.playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.6F, 0.6F);
        for (int i = 1; i <= len; i++) {
            int k = i;
            SkillEffects.later(p, i, () -> {
                Vec3 c = start.add(dir.scale(k));
                double h = 3.5 * Math.sin(Math.PI * Math.min(1, k / (double) len * 1.2));
                for (double w = -4.5; w <= 4.5; w += 0.7) {
                    Vec3 q = c.add(side.scale(w));
                    level.sendParticles(ParticleTypes.SPLASH, q.x, q.y + h, q.z, 4, 0.2, 0.3, 0.2, 0.2);
                    level.sendParticles(WATER, q.x, q.y + h * 0.5, q.z, 3, 0.2, h * 0.3, 0.2, 0);
                }
                for (LivingEntity e : AxeSkills.around(p, c, 5.5)) {
                    if (Math.abs(e.position().subtract(c).dot(dir)) > 1.5 || !hit.add(e.getId())) continue;
                    SkillEffects.hit(p, e, wave);
                    MageSpells.wet(e, 200);
                    e.setDeltaMovement(dir.x * 1.6, 0.5, dir.z * 1.6);
                    e.hurtMarked = true;
                }
                if (k % 4 == 0) level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.0F, 0.5F);
            });
        }
        // 雷
        float bolt = AxeSkills.atk(p) * (float) s.chance();
        int strikes = Math.max(1, s.amplifier());
        MageSpells.repeat(p, 5, strikes, i -> SkillEffects.later(p, len, () -> {
            var list = AxeSkills.around(p, p.position(), s.radius());
            if (list.isEmpty()) return;
            LivingEntity t = list.get(level.random.nextInt(list.size()));
            var lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(t.position());
                lightning.setVisualOnly(true);
                level.addFreshEntity(lightning);
            }
            MageSpells.magicHit(p, t, bolt, MageSpells.Elem.THUNDER);   // 濡れていれば感電
        }));
        p.displayClientMessage(Component.literal("海神降臨！").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
        return true;
    }

    private SpearSkills() {}
}
