package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 斧使い（断鋼の斧使い・破城の狂戦士）の技。ダメージは攻撃力 × power。演出は赤と金。
 * 断鋼：断鋼撃を受けた敵は、しばらく受けるダメージ +20%（AxeEvents）。
 */
public final class AxeSkills {

    public static final String SUNDER = "jsn_sunder";

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "axe_cleave" -> cleave(p, s);
            case "axe_whirl" -> whirl(p, s);
            case "axe_throw" -> throwAxe(p, s);
            case "axe_war_cry" -> warCry(p, s);
            case "axe_fissure" -> fissure(p, s);
            case "axe_sunder" -> sunder(p, s);
            case "axe_rage" -> rage(p, s);
            case "axe_siege" -> siege(p, s);
            case "axe_frenzy" -> frenzy(p, s);
            case "axe_sky_breaker" -> skyBreaker(p, s);
            default -> false;
        };
    }

    // ================= 共通 =================

    static float atk(ServerPlayer p) {
        return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    static Vec3 flatLook(ServerPlayer p) {
        Vec3 f = p.getLookAngle().multiply(1, 0, 1);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    static List<LivingEntity> around(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 2.5, r), e -> SkillEffects.isEnemy(p, e))) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z;
            if (dx * dx + dz * dz <= r * r) out.add(e);
        }
        return out;
    }

    static void sound(ServerPlayer p, Vec3 at, SoundEvent s, float vol, float pitch) {
        p.serverLevel().playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    /** 地面の割れ目の粒（足元のブロックの色） */
    static void crack(ServerLevel level, Vec3 q, int count) {
        BlockPos below = BlockPos.containing(q.x, q.y - 0.5, q.z);
        BlockState st = level.getBlockState(below);
        if (st.isAir()) st = Blocks.DIRT.defaultBlockState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), q.x, q.y + 0.2, q.z, count, 0.3, 0.2, 0.3, 0.15);
    }

    public static boolean sundered(LivingEntity e) {
        return e.getPersistentData().getLong(SUNDER) > e.level().getGameTime();
    }

    // ================= 断鋼の斧使い =================

    /** 兜割り：前の敵に重い振り下ろし。少しのあいだ鈍くする */
    private static boolean cleave(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 look = flatLook(p);
        float dmg = atk(p) * (float) s.power();
        Vec3 front = p.position().add(look.scale(1.6));
        for (LivingEntity e : around(p, front, s.radius())) {
            SkillEffects.hit(p, e, dmg);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) Math.round(s.duration() * 20), 2));
        }
        Vfx.slash(level, front.add(0, 1.4, 0), 2.4F, Vfx.RED, 0);
        Vfx.slash(level, front.add(0, 1.2, 0), 1.7F, Vfx.GOLD, 0);
        crack(level, front, 20);
        sound(p, front, SoundEvents.ANVIL_LAND, 0.6F, 0.7F);
        sound(p, front, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.6F);
        return true;
    }

    /** 回転斬り：その場で大きく回り、周りを何回も斬る */
    private static boolean whirl(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        int hits = Math.max(1, s.amplifier());
        float dmg = atk(p) * (float) s.power();
        for (int n = 0; n < hits; n++) {
            int k = n;
            SkillEffects.later(p, n * 7, () -> {
                if (!p.isAlive()) return;
                Vec3 c = p.position();
                for (LivingEntity e : around(p, c, s.radius())) {
                    SkillEffects.hit(p, e, dmg);
                    Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
                    if (away.lengthSqr() > 0.01) e.knockback(0.5, -away.normalize().x, -away.normalize().z);
                }
                for (int j = 0; j < 4; j++) {
                    double a = Math.toRadians(j * 90 + k * 45);
                    Vfx.slash(level, c.add(Math.cos(a) * s.radius() * 0.6, 0.9, Math.sin(a) * s.radius() * 0.6), 2.0F, j % 2 == 0 ? Vfx.RED : Vfx.GOLD, (float) (j * 90 + k * 45));
                }
                Vfx.ring(level, c, (float) s.radius() * 1.1F, 10, Vfx.RED);
                sound(p, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F + k * 0.1F);
            });
        }
        return true;
    }

    /** 投げ斧：回る斧を投げる。通り道の敵を貫いて、戻ってくる */
    private static boolean throwAxe(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = atk(p) * (float) s.power();
        Vec3 start = p.getEyePosition().add(0, -0.3, 0);
        Vec3 dir = p.getLookAngle();
        double range = s.radius();
        ItemParticleOption axeBits = new ItemParticleOption(ParticleTypes.ITEM, p.getMainHandItem().copyWithCount(1));
        Set<Integer> hit = new HashSet<>();
        var clip = level.clip(new ClipContext(start, start.add(dir.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double reached = clip.getLocation().distanceTo(start);
        int go = Math.max(2, (int) Math.ceil(reached / 1.2));
        for (int i = 1; i <= go * 2; i++) {   // 行って、戻ってくる
            int k = i;
            SkillEffects.later(p, i, () -> {
                double d = k <= go ? reached * k / go : reached * (2 * go - k) / go;
                Vec3 q = start.add(dir.scale(d));
                level.sendParticles(axeBits, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0.02);
                Vfx.slash(level, q, 0.9F, Vfx.GOLD, k * 70);
                for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(q, q).inflate(0.9), e -> SkillEffects.isEnemy(p, e))) {
                    int key = e.getId() * 2 + (k <= go ? 0 : 1);   // 行きと帰りで1回ずつ当たる
                    if (!hit.add(key)) continue;
                    SkillEffects.hit(p, e, dmg);
                }
                if (k % 2 == 0) sound(p, q, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 1.4F);
            });
        }
        sound(p, start, SoundEvents.TRIDENT_THROW.value(), 1.0F, 0.7F);
        return true;
    }

    /** 雄叫び：攻撃力アップ。周りの敵をひるませ、こちらに引きつける */
    private static boolean warCry(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        SkillEffects.attributeBuff(p, Attributes.ATTACK_DAMAGE, "axe_war_cry", s.power(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, s.duration());
        int n = 0;
        for (LivingEntity e : around(p, p.position(), s.radius())) {
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
            if (e instanceof Mob mob) mob.setTarget(p);
            n++;
        }
        Vfx.ring(level, p.position(), (float) s.radius(), 14, Vfx.RED);
        Vfx.ring(level, p.position(), (float) s.radius() * 0.6F, 10, Vfx.GOLD);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getY() + 2, p.getZ(), 6, 0.5, 0.3, 0.5, 0);
        sound(p, p.position(), SoundEvents.RAVAGER_ROAR, 1.2F, 1.1F);
        p.displayClientMessage(Component.literal("雄叫び！ 攻撃力アップ（" + n + "体を引きつけた）").withStyle(ChatFormatting.RED), true);
        return true;
    }

    /** 大地割り：斧を叩きつけ、前へ地割れを走らせる */
    private static boolean fissure(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = flatLook(p);
        Vec3 start = p.position();
        int len = (int) Math.round(s.radius());
        float dmg = atk(p) * (float) s.power();
        Set<Integer> hit = new HashSet<>();
        sound(p, start, SoundEvents.ANVIL_LAND, 0.8F, 0.5F);
        for (int i = 1; i <= len; i++) {
            int k = i;
            SkillEffects.later(p, i, () -> {
                Vec3 q = start.add(dir.scale(k));
                crack(level, q, 14);
                level.sendParticles(ParticleTypes.LAVA, q.x, q.y + 0.2, q.z, 1, 0.2, 0.1, 0.2, 0);
                for (LivingEntity e : around(p, q, 1.4)) {
                    if (!hit.add(e.getId())) continue;
                    SkillEffects.hit(p, e, dmg);
                    e.setDeltaMovement(e.getDeltaMovement().x, 0.75, e.getDeltaMovement().z);
                    e.hurtMarked = true;
                }
                if (k % 3 == 0) sound(p, q, SoundEvents.ROOTED_DIRT_BREAK, 1.5F, 0.5F);
            });
        }
        return true;
    }

    /** 断鋼撃：ためて渾身の一撃。当たった敵はしばらく受けるダメージ +20% */
    private static boolean sunder(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 look = flatLook(p);
        Vec3 front = p.position().add(look.scale(2.0));
        float dmg = atk(p) * (float) s.power();
        int n = 0;
        for (LivingEntity e : around(p, front, s.radius())) {
            SkillEffects.hit(p, e, dmg);
            e.getPersistentData().putLong(SUNDER, e.level().getGameTime() + Math.round(s.duration() * 20));
            e.knockback(1.0, -look.x, -look.z);
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 1, e.getZ(), 20, 0.4, 0.5, 0.4, 0.4);
            n++;
        }
        Vfx.slash(level, front.add(0, 1.3, 0), 3.2F, Vfx.RED, 0);
        Vfx.slash(level, front.add(0, 1.2, 0), 2.6F, Vfx.GOLD, 15);
        Vfx.ring(level, front, 3.0F, 10, Vfx.RED);
        crack(level, front, 30);
        level.sendParticles(ParticleTypes.EXPLOSION, front.x, front.y + 0.6, front.z, 1, 0, 0, 0, 0);
        sound(p, front, SoundEvents.ANVIL_DESTROY, 1.0F, 0.6F);
        sound(p, front, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 1.2F);
        if (n > 0) p.displayClientMessage(Component.literal("断鋼！ 敵の守りを砕いた").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    /** 激昂：しばらく攻撃と速さが大きく上がり、殴ってHPを吸う。ただし受けるダメージも増える */
    private static boolean rage(ServerPlayer p, SkillDefinition s) {
        double sec = s.duration();
        SkillEffects.attributeBuff(p, Attributes.ATTACK_DAMAGE, "axe_rage_atk", s.power(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, sec);
        SkillEffects.attributeBuff(p, Attributes.ATTACK_SPEED, "axe_rage_spd", 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, sec);
        SkillEffects.attributeBuff(p, Attributes.MOVEMENT_SPEED, "axe_rage_move", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, sec);
        SkillEffects.attributeBuff(p, ModAttributes.LIFESTEAL, "axe_rage_ls", s.chance(), AttributeModifier.Operation.ADD_VALUE, sec);
        TimedFlags.set(p, "axe_rage", 0.15, sec);
        ServerLevel level = p.serverLevel();
        com.chigusa.jsncraft.magic.MageSpells.repeat(p, 5, (int) Math.round(sec * 4), i ->
                level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.6, p.getZ(), 3, 0.35, 0.5, 0.35, 0.01));
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getY() + 2, p.getZ(), 5, 0.5, 0.3, 0.5, 0);
        Vfx.ring(level, p.position(), 2.5F, 12, Vfx.RED);
        sound(p, p.position(), SoundEvents.RAVAGER_ROAR, 1.0F, 0.8F);
        p.displayClientMessage(Component.literal("激昂！").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        return true;
    }

    // ================= 破城の狂戦士 =================

    /** 破城槌：前へ突進し、通り道のものを全部はね飛ばす */
    private static boolean siege(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = flatLook(p);
        float dmg = atk(p) * (float) s.power();
        int ticks = 8;
        double per = s.radius() / ticks;
        Set<Integer> hit = new HashSet<>();
        sound(p, p.position(), SoundEvents.RAVAGER_ATTACK, 1.2F, 0.8F);
        for (int i = 0; i < ticks; i++) {
            int k = i;
            SkillEffects.later(p, i, () -> {
                if (!p.isAlive()) return;
                p.setDeltaMovement(dir.x * per * 1.6, Math.min(0, p.getDeltaMovement().y), dir.z * per * 1.6);
                p.hurtMarked = true;
                p.fallDistance = 0;
                Vec3 c = p.position();
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 0.2, c.z, 2, 0.3, 0.1, 0.3, 0.01);
                crack(level, c, 6);
                for (LivingEntity e : around(p, c.add(dir), 1.8)) {
                    if (!hit.add(e.getId())) continue;
                    SkillEffects.hit(p, e, dmg);
                    Vec3 side = new Vec3(-dir.z, 0, dir.x);
                    double sgn = e.position().subtract(c).dot(side) >= 0 ? 1 : -1;
                    e.setDeltaMovement(side.x * sgn * 1.0 + dir.x * 0.6, 0.6, side.z * sgn * 1.0 + dir.z * 0.6);
                    e.hurtMarked = true;
                    Vfx.sparks(level, e.getBoundingBox().getCenter(), 10);
                }
                if (k == ticks - 1) Vfx.ring(level, c, 3.0F, 10, Vfx.RED);
            });
        }
        return true;
    }

    /** 狂乱の大旋風：しばらく回り続けて、動きながら周りを斬り続ける */
    private static boolean frenzy(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = atk(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 5);
        SkillEffects.attributeBuff(p, Attributes.MOVEMENT_SPEED, "axe_frenzy", 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, s.duration());
        sound(p, p.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 0.8F);
        com.chigusa.jsncraft.magic.MageSpells.repeat(p, 4, pulses, i -> {
            Vec3 c = p.position();
            for (LivingEntity e : around(p, c, s.radius())) {
                SkillEffects.hit(p, e, dmg);
                Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
                if (away.lengthSqr() > 0.01) e.knockback(0.25, -away.normalize().x, -away.normalize().z);
            }
            for (int j = 0; j < 3; j++) {
                double a = Math.toRadians(j * 120 + i * 50);
                Vfx.slash(level, c.add(Math.cos(a) * s.radius() * 0.6, 0.9, Math.sin(a) * s.radius() * 0.6), 1.8F, j == 0 ? Vfx.GOLD : Vfx.RED, (float) (j * 120 + i * 50));
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y + 1, c.z, 2, s.radius() * 0.4, 0.2, s.radius() * 0.4, 0);
            if (i % 2 == 0) sound(p, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F + (i % 6) * 0.08F);
        });
        return true;
    }

    /** 奥義・天崩し：高く跳び上がり、天ごと崩すように叩き落とす */
    private static boolean skyBreaker(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        p.setDeltaMovement(p.getDeltaMovement().x * 0.2, 1.5, p.getDeltaMovement().z * 0.2);
        p.hurtMarked = true;
        sound(p, p.position(), SoundEvents.BREEZE_JUMP, 1.2F, 0.6F);
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 20, 0.5, 0.1, 0.5, 0.08);
        waitLanding(p, s, 0);
        return true;
    }

    private static void waitLanding(ServerPlayer p, SkillDefinition s, int t) {
        SkillEffects.later(p, 1, () -> {
            if (!p.isAlive()) return;
            p.fallDistance = 0;
            if (t > 6 && (p.onGround() || t > 60)) {
                crash(p, s);
                return;
            }
            if (t > 10) {   // 頂点から一気に落ちる
                p.setDeltaMovement(p.getDeltaMovement().x, -2.2, p.getDeltaMovement().z);
                p.hurtMarked = true;
            }
            p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.5, p.getZ(), 4, 0.2, 0.3, 0.2, 0.01);
            waitLanding(p, s, t + 1);
        });
    }

    private static void crash(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position();
        double r = s.radius();
        float dmg = atk(p) * (float) s.power();
        int n = 0;
        for (LivingEntity e : around(p, c, r)) {
            SkillEffects.hit(p, e, dmg);
            e.setDeltaMovement(e.getDeltaMovement().x, 1.0, e.getDeltaMovement().z);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            n++;
        }
        // 外へ広がる地割れの輪
        for (int w = 1; w <= 4; w++) {
            int k = w;
            SkillEffects.later(p, w * 2, () -> {
                double rr = r * k / 4.0;
                for (int j = 0; j < 16; j++) {
                    double a = j * Math.PI / 8;
                    crack(level, c.add(Math.cos(a) * rr, 0, Math.sin(a) * rr), 6);
                }
                Vfx.ring(level, c, (float) rr * 1.2F, 10, k % 2 == 0 ? Vfx.GOLD : Vfx.RED);
            });
        }
        Vfx.magicCircle(level, c, (float) r, 30, Circle.FIRE);
        for (int j = 0; j < 8; j++) {
            double a = Math.toRadians(j * 45);
            Vfx.slash(level, c.add(Math.cos(a) * r * 0.5, 0.8, Math.sin(a) * r * 0.5), 2.6F, j % 2 == 0 ? Vfx.RED : Vfx.GOLD, 90);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.3, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.3, c.z, 30, r * 0.4, 0.3, r * 0.4, 0);
        sound(p, c, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.5F);
        sound(p, c, SoundEvents.ANVIL_LAND, 1.0F, 0.4F);
        p.displayClientMessage(Component.literal("天崩し：" + n + "体に命中").withStyle(ChatFormatting.GOLD), true);
    }

    private AxeSkills() {}
}
