package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.magic.HolySpells;
import com.chigusa.jsncraft.magic.MageSpells;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.CircleOptions;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * 盾騎士（不落の盾騎士・金剛の守護騎士）の技。敵を引きつけ、仲間を守る。演出は金と白。
 * 守りの効果（鉄壁・かばう・裁きのため・金剛不壊）は GuardEvents で受けるダメージを変える。
 */
public final class GuardSkills {

    public static final String STORE = "jsn_guard_store";
    static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.4F), 1.4F),
            WHITE = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 0.95F), 1.2F);

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "guard_bash" -> bash(p, s);
            case "guard_taunt" -> taunt(p, s);
            case "guard_charge" -> charge(p, s);
            case "guard_iron_wall" -> ironWall(p, s);
            case "guard_cover" -> cover(p, s);
            case "guard_formation" -> formation(p, s);
            case "guard_rampart" -> rampart(p, s);
            case "guard_judgment" -> judgment(p, s);
            case "guard_adamant" -> adamant(p, s);
            default -> false;
        };
    }

    /** 周りの敵を自分に引きつける */
    static int provoke(ServerPlayer p, Vec3 c, double r) {
        int n = 0;
        for (LivingEntity e : AxeSkills.around(p, c, r)) {
            if (e instanceof Mob mob) {
                mob.setTarget(p);
                n++;
            }
        }
        return n;
    }

    // ================= 不落の盾騎士 =================

    /** シールドバッシュ：盾で殴って、少しのあいだ動けなくする */
    private static boolean bash(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 look = AxeSkills.flatLook(p);
        Vec3 front = p.position().add(look.scale(1.5));
        float dmg = AxeSkills.atk(p) * (float) s.power();
        int ticks = (int) Math.round(s.duration() * 20);
        for (LivingEntity e : AxeSkills.around(p, front, s.radius())) {
            SkillEffects.hit(p, e, dmg);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, false));
            e.knockback(1.2, -look.x, -look.z);
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 10, 0.3, 0.1, 0.3, 0.1);
        }
        Vfx.ring(level, front, 2.0F, 8, Vfx.GOLD);
        level.sendParticles(GOLD, front.x, front.y + 1, front.z, 20, 0.5, 0.5, 0.5, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 0.4F, 1.4F);
        return true;
    }

    /** 挑発：周りの敵を引きつけ、しばらく受けるダメージが減る */
    private static boolean taunt(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        int pulses = (int) Math.round(s.duration());
        TimedFlags.set(p, "guard_taunt", s.power(), s.duration());
        int n = provoke(p, p.position(), s.radius());
        MageSpells.repeat(p, 20, pulses, i -> provoke(p, p.position(), s.radius()));
        Vfx.ring(level, p.position(), (float) s.radius(), 14, Vfx.RED);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getY() + 2.2, p.getZ(), 5, 0.4, 0.2, 0.4, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.4F);
        p.displayClientMessage(Component.literal("挑発：" + n + "体を引きつけた").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** シールドチャージ：盾を構えて突進、ぶつかった敵を吹き飛ばす */
    private static boolean charge(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = AxeSkills.flatLook(p);
        float dmg = AxeSkills.atk(p) * (float) s.power();
        int ticks = 7;
        double per = s.radius() / ticks;
        Set<Integer> hit = new HashSet<>();
        level.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.6F);
        for (int i = 0; i < ticks; i++) {
            SkillEffects.later(p, i, () -> {
                if (!p.isAlive()) return;
                p.setDeltaMovement(dir.x * per * 1.6, Math.min(0, p.getDeltaMovement().y), dir.z * per * 1.6);
                p.hurtMarked = true;
                p.fallDistance = 0;
                Vec3 c = p.position();
                level.sendParticles(WHITE, c.x, c.y + 1, c.z, 3, 0.3, 0.4, 0.3, 0);
                for (LivingEntity e : AxeSkills.around(p, c.add(dir), 1.6)) {
                    if (!hit.add(e.getId())) continue;
                    SkillEffects.hit(p, e, dmg);
                    e.setDeltaMovement(dir.x * 1.4, 0.5, dir.z * 1.4);
                    e.hurtMarked = true;
                    level.playSound(null, e.blockPosition(), SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F);
                }
            });
        }
        return true;
    }

    /** 鉄壁の構え：しばらくほとんどダメージを受けない。ただし動けない */
    private static boolean ironWall(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        int ticks = (int) Math.round(s.duration() * 20);
        TimedFlags.set(p, "guard_iron_wall", s.power(), s.duration());
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false));
        MageSpells.repeat(p, 4, ticks / 4, i -> {
            for (int k = 0; k < 10; k++) {
                double a = i * 0.4 + k * Math.PI / 5;
                level.sendParticles(GOLD, p.getX() + Math.cos(a) * 1.0, p.getY() + 0.2 + (k % 5) * 0.45, p.getZ() + Math.sin(a) * 1.0, 1, 0, 0, 0, 0);
            }
        });
        Vfx.magicCircle(level, p.position(), 1.6F, ticks, Circle.HOLY);
        level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 0.8F);
        p.displayClientMessage(Component.literal("鉄壁の構え！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    /** かばう：しばらく、近くの仲間が受けるダメージの半分を代わりに受ける */
    private static boolean cover(ServerPlayer p, SkillDefinition s) {
        TimedFlags.set(p, "guard_cover", s.power(), s.duration());
        ServerLevel level = p.serverLevel();
        for (ServerPlayer o : HolySpells.allies(p, p.position(), s.radius())) {
            if (o == p) continue;
            level.sendParticles(GOLD, o.getX(), o.getY() + 1, o.getZ(), 15, 0.4, 0.6, 0.4, 0);
            o.displayClientMessage(Component.literal(p.getName().getString() + " がかばってくれている").withStyle(ChatFormatting.GOLD), true);
        }
        Vfx.ring(level, p.position(), (float) s.radius(), 14, Vfx.GOLD);
        level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 守護の陣：周りの仲間の守りを上げる */
    private static boolean formation(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        for (ServerPlayer o : HolySpells.allies(p, p.position(), s.radius())) {
            SkillEffects.attributeBuff(o, ModAttributes.DAMAGE_REDUCTION, "guard_formation", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            SkillEffects.attributeBuff(o, Attributes.ARMOR, "guard_formation_armor", s.amplifier(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            level.sendParticles(GOLD, o.getX(), o.getY() + 1, o.getZ(), 15, 0.4, 0.6, 0.4, 0);
        }
        Vfx.magicCircle(level, p.position(), (float) s.radius(), 30, Circle.HOLY);
        level.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    // ================= 金剛の守護騎士 =================

    /** 城壁：目の前に光の壁を出す。敵は通れず、矢や火の玉は消える */
    private static boolean rampart(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = AxeSkills.flatLook(p);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        Vec3 c = p.position().add(dir.scale(2.5));
        double half = s.radius() / 2;
        int pulses = (int) Math.round(s.duration() * 10);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        MageSpells.repeat(p, 2, pulses, i -> {
            if (i % 2 == 0) {
                for (double w = -half; w <= half; w += 0.6) {
                    Vec3 q = c.add(side.scale(w));
                    level.sendParticles(WHITE, q.x, q.y + 0.3 + (i % 5) * 0.6, q.z, 1, 0.05, 0.05, 0.05, 0);
                    if (((int) (w * 10) + i) % 7 == 0) level.sendParticles(GOLD, q.x, q.y + 1.5, q.z, 1, 0.05, 1.2, 0.05, 0);
                }
            }
            // 壁のこちら側に来た敵は押し返す
            for (LivingEntity e : AxeSkills.around(p, c, half + 1.5)) {
                Vec3 d = e.position().subtract(c);
                double along = d.dot(side), depth = d.dot(dir);
                if (Math.abs(along) > half + 0.5 || Math.abs(depth) > 1.3) continue;
                Vec3 push = dir.scale(depth >= 0 ? 0.7 : -0.7);
                e.setDeltaMovement(push.x, 0.15, push.z);
                e.hurtMarked = true;
            }
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(half + 1, 3, half + 1))) {
                Entity owner = pr.getOwner();
                if (owner instanceof ServerPlayer op && HolySpells.ally(p, op)) continue;
                Vec3 d = pr.position().subtract(c);
                if (Math.abs(d.dot(side)) > half + 0.5 || Math.abs(d.dot(dir)) > 1.5 || d.y > 3.5 || d.y < -0.5) continue;
                level.sendParticles(ParticleTypes.END_ROD, pr.getX(), pr.getY(), pr.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                pr.discard();
            }
        });
        return true;
    }

    /** 聖盾の裁き：しばらく受けたダメージをため、最後に光の爆発で返す */
    private static boolean judgment(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        p.getPersistentData().putFloat(STORE, 0);
        TimedFlags.set(p, "guard_store", 1, s.duration());
        int ticks = (int) Math.round(s.duration() * 20);
        Vfx.magicCircle(level, p.position(), 2.0F, ticks, Circle.HOLY);
        MageSpells.repeat(p, 5, ticks / 5, i ->
                level.sendParticles(GOLD, p.getX(), p.getY() + 1, p.getZ(), 4, 0.6, 0.8, 0.6, 0));
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
        p.displayClientMessage(Component.literal("聖盾の裁き：受けたダメージをためている…").withStyle(ChatFormatting.GOLD), true);
        SkillEffects.later(p, ticks, () -> {
            if (!p.isAlive()) return;
            float stored = p.getPersistentData().getFloat(STORE);
            p.getPersistentData().remove(STORE);
            float dmg = AxeSkills.atk(p) + stored * (float) s.power();
            int n = 0;
            for (LivingEntity e : AxeSkills.around(p, p.position(), s.radius())) {
                SkillEffects.hit(p, e, dmg);
                Vec3 push = e.position().subtract(p.position()).normalize().scale(1.0);
                e.push(push.x, 0.5, push.z);
                e.hurtMarked = true;
                n++;
            }
            level.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 100, s.radius() * 0.3, 0.8, s.radius() * 0.3, 0.4);
            Vfx.ring(level, p.position(), (float) s.radius() * 1.4F, 16, Vfx.GOLD);
            level.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.4F);
            p.displayClientMessage(Component.literal("聖盾の裁き：" + Math.round(stored) + "ダメージを返した（" + n + "体）").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        });
        return true;
    }

    /** 奥義・金剛不壊：自分と仲間の受けるダメージを半分に、周りの敵をすべて引きつける */
    private static boolean adamant(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        int pulses = (int) Math.round(s.duration());
        for (ServerPlayer o : HolySpells.allies(p, p.position(), s.radius())) {
            TimedFlags.set(o, "guard_adamant", s.power(), s.duration());
            level.sendParticles(GOLD, o.getX(), o.getY() + 1, o.getZ(), 25, 0.4, 0.8, 0.4, 0);
        }
        provoke(p, p.position(), s.radius() + 6);
        MageSpells.repeat(p, 20, pulses, i -> {
            provoke(p, p.position(), s.radius() + 6);
            for (int k = 0; k < 12; k++) {
                double a = i * 0.3 + k * Math.PI / 6;
                level.sendParticles(GOLD, p.getX() + Math.cos(a) * 1.4, p.getY() + 2.4, p.getZ() + Math.sin(a) * 1.4, 1, 0, 0, 0, 0);
            }
        });
        Vfx.magicCircle(level, p.position(), (float) s.radius(), pulses * 20, Circle.HOLY);
        Vfx.circle(level, p.position().add(0, 6, 0), Circle.HOLY, (float) s.radius() * 0.8F, pulses * 20, CircleOptions.SKY);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.8F);
        level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
        p.displayClientMessage(Component.literal("金剛不壊！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    private GuardSkills() {}
}
