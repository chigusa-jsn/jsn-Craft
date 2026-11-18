package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.magic.MageSpells;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.CircleOptions;
import com.chigusa.jsncraft.vfx.Vfx;
import com.chigusa.jsncraft.weapon.GauntletItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * 拳闘士（鉄拳の拳闘士・覇拳の武神）の技。素手で使う。
 * 拳の強さ ＝ 攻撃力 ＋ 籠手のパンチ ＋ 素手の心得。殴るたびに「気」がたまり、満タン（10）で次の技が強くなる。
 */
public final class FistSkills {

    public static final String KI = "jsn_ki";
    public static final int KI_MAX = 10;
    static final DustParticleOptions KI_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.7F, 0.25F), 1.4F),
            WHITE = new DustParticleOptions(new Vector3f(1.0F, 0.95F, 0.85F), 1.1F);

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "fist_rush" -> rush(p, s, 4);
            case "fist_straight" -> straight(p, s);
            case "fist_dash" -> dash(p, s);
            case "fist_uppercut" -> uppercut(p, s);
            case "fist_ki_gather" -> kiGather(p, s);
            case "fist_whirl_kick" -> whirlKick(p, s);
            case "fist_double" -> doubleFist(p, s);
            case "fist_qigong" -> qigong(p, s);
            case "fist_hundred" -> rush(p, s, 2);
            case "fist_stomp" -> stomp(p, s);
            case "fist_overlord" -> overlord(p, s);
            default -> false;
        };
    }

    // ================= 共通 =================

    /** 拳の強さ */
    public static float fist(ServerPlayer p) {
        float v = AxeSkills.atk(p);
        ItemStack g = GauntletItem.equipped(p);
        if (g.getItem() instanceof GauntletItem gi) v += gi.spec().punch();
        SkillDefinition bare = SkillService.passive(p, "fist_bare");
        if (bare != null) v += (float) bare.power();
        return v;
    }

    public static int ki(ServerPlayer p) {
        return p.getPersistentData().getInt(KI);
    }

    public static void addKi(ServerPlayer p, int n) {
        int k = Math.min(KI_MAX, ki(p) + n);
        p.getPersistentData().putInt(KI, k);
        p.displayClientMessage(Component.literal("気 " + "●".repeat(k / 2) + "○".repeat((KI_MAX - k) / 2) + (k >= KI_MAX ? "  満タン！" : ""))
                .withStyle(k >= KI_MAX ? ChatFormatting.GOLD : ChatFormatting.YELLOW), true);
    }

    /** 気が満タンなら使い切って、技が強くなる（ふつう1.3倍、気の極意で1.6倍） */
    static float kiBonus(ServerPlayer p) {
        if (ki(p) < KI_MAX) return 1.0F;
        p.getPersistentData().putInt(KI, 0);
        SkillDefinition master = SkillService.passive(p, "fist_ki_master");
        p.serverLevel().sendParticles(KI_DUST, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.7, 0.5, 0);
        p.displayClientMessage(Component.literal("気を解き放った！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return master != null ? 1.0F + (float) master.power() : 1.3F;
    }

    static void punchFx(ServerLevel level, Vec3 at, boolean big) {
        level.sendParticles(big ? ParticleTypes.EXPLOSION : ParticleTypes.CRIT, at.x, at.y, at.z, big ? 1 : 8, 0.15, 0.15, 0.15, 0.3);
        level.sendParticles(KI_DUST, at.x, at.y, at.z, big ? 12 : 4, 0.2, 0.2, 0.2, 0);
    }

    static Vec3 front(ServerPlayer p, double d) {
        return p.position().add(AxeSkills.flatLook(p).scale(d));
    }

    // ================= 鉄拳の拳闘士 =================

    /** ラッシュ・百裂拳：左右の拳で何度も前を殴る */
    private static boolean rush(ServerPlayer p, SkillDefinition s, int interval) {
        ServerLevel level = p.serverLevel();
        float dmg = fist(p) * (float) s.power() * kiBonus(p);
        int n = Math.max(1, s.amplifier());
        for (int i = 0; i < n; i++) {
            int k = i;
            SkillEffects.later(p, i * interval, () -> {
                if (!p.isAlive()) return;
                p.swing(k % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
                Vec3 f = front(p, 1.6).add(0, 1.1, 0);
                for (LivingEntity e : AxeSkills.around(p, front(p, 1.5), s.radius())) {
                    SkillEffects.hit(p, e, dmg);
                    e.setDeltaMovement(e.getDeltaMovement().multiply(0.3, 1, 0.3));
                }
                Vec3 side = new Vec3(-AxeSkills.flatLook(p).z, 0, AxeSkills.flatLook(p).x).scale(k % 2 == 0 ? 0.35 : -0.35);
                punchFx(level, f.add(side), false);
                if (k % 2 == 0) level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.8F, 1.2F + (k % 5) * 0.1F);
            });
        }
        return true;
    }

    /** 正拳突き：重い一撃で遠くへ吹き飛ばす */
    private static boolean straight(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = fist(p) * (float) s.power() * kiBonus(p);
        Vec3 look = AxeSkills.flatLook(p);
        p.swing(InteractionHand.MAIN_HAND, true);
        for (LivingEntity e : AxeSkills.around(p, front(p, 1.6), s.radius())) {
            SkillEffects.hit(p, e, dmg);
            e.setDeltaMovement(look.x * 2.2, 0.5, look.z * 2.2);
            e.hurtMarked = true;
        }
        Vec3 f = front(p, 1.8).add(0, 1.1, 0);
        punchFx(level, f, true);
        level.sendParticles(ParticleTypes.SONIC_BOOM, f.x, f.y, f.z, 1, 0, 0, 0, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4F, 1.6F);
        return true;
    }

    /** 踏み込み拳：前へ飛び込んで殴る */
    private static boolean dash(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = AxeSkills.flatLook(p);
        float dmg = fist(p) * (float) s.power() * kiBonus(p);
        Vec3 start = p.position();
        var clip = level.clip(new ClipContext(start.add(0, 1, 0), start.add(0, 1, 0).add(dir.scale(s.radius())), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = clip.getLocation().subtract(0, 1, 0).subtract(dir.scale(0.6));
        Set<Integer> hit = new HashSet<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.3, 1.5, 1.3), e -> SkillEffects.isEnemy(p, e))) {
            if (e.getBoundingBox().inflate(1.0).clip(start.add(0, 1, 0), end.add(0, 1, 0).add(dir)).isEmpty() || !hit.add(e.getId())) continue;
            SkillEffects.hit(p, e, dmg);
            e.setDeltaMovement(dir.x * 1.2, 0.4, dir.z * 1.2);
            e.hurtMarked = true;
            punchFx(level, e.getBoundingBox().getCenter(), false);
        }
        for (double t = 0; t < start.distanceTo(end); t += 0.6) {
            Vec3 q = start.add(dir.scale(t));
            level.sendParticles(ParticleTypes.CLOUD, q.x, q.y + 0.3, q.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
        p.teleportTo(end.x, end.y, end.z);
        p.fallDistance = 0;
        p.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 昇龍拳：前の敵を突き上げて、自分も跳ぶ */
    private static boolean uppercut(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = fist(p) * (float) s.power() * kiBonus(p);
        for (LivingEntity e : AxeSkills.around(p, front(p, 1.4), s.radius())) {
            SkillEffects.hit(p, e, dmg);
            e.setDeltaMovement(e.getDeltaMovement().x * 0.2, 1.2, e.getDeltaMovement().z * 0.2);
            e.hurtMarked = true;
        }
        p.setDeltaMovement(p.getDeltaMovement().x * 0.3, 0.9, p.getDeltaMovement().z * 0.3);
        p.hurtMarked = true;
        p.swing(InteractionHand.MAIN_HAND, true);
        for (int h = 0; h < 8; h++) {
            Vec3 q = front(p, 1.2).add(0, h * 0.5, 0);
            level.sendParticles(ParticleTypes.FLAME, q.x, q.y, q.z, 3, 0.2, 0.1, 0.2, 0.02);
            level.sendParticles(KI_DUST, q.x, q.y, q.z, 2, 0.2, 0.1, 0.2, 0);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.4F);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    /** 気合い：気をためて、しばらく攻撃が速くなる */
    private static boolean kiGather(ServerPlayer p, SkillDefinition s) {
        addKi(p, Math.max(1, s.amplifier()));
        SkillEffects.attributeBuff(p, Attributes.ATTACK_SPEED, "fist_ki_gather", s.power(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, s.duration());
        SkillEffects.attributeBuff(p, Attributes.MOVEMENT_SPEED, "fist_ki_gather_move", 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, s.duration());
        ServerLevel level = p.serverLevel();
        level.sendParticles(KI_DUST, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0);
        level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.2, p.getZ(), 20, 0.6, 0.1, 0.6, 0.03);
        Vfx.ring(level, p.position(), 2.5F, 12, Vfx.GOLD);
        level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    /** 旋風脚：回し蹴りで周りをまとめて蹴り飛ばす */
    private static boolean whirlKick(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = fist(p) * (float) s.power() * kiBonus(p);
        Vec3 c = p.position();
        for (LivingEntity e : AxeSkills.around(p, c, s.radius())) {
            SkillEffects.hit(p, e, dmg);
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
            if (away.lengthSqr() > 0.01) e.knockback(1.0, -away.normalize().x, -away.normalize().z);
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45);
            Vfx.slash(level, c.add(Math.cos(a) * s.radius() * 0.6, 0.5, Math.sin(a) * s.radius() * 0.6), 1.5F, k % 2 == 0 ? Vfx.GOLD : Vfx.WHITE, k * 45);
        }
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.3, c.z, 20, s.radius() * 0.4, 0.1, s.radius() * 0.4, 0.05);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.0F);
        return true;
    }

    /** 双拳打ち：両方の拳を同時に打ち込み、前へ衝撃波を走らせる */
    private static boolean doubleFist(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = fist(p) * (float) s.power() * kiBonus(p);
        Vec3 dir = AxeSkills.flatLook(p);
        p.swing(InteractionHand.MAIN_HAND, true);
        p.swing(InteractionHand.OFF_HAND, true);
        Set<Integer> hit = new HashSet<>();
        for (int i = 1; i <= (int) s.radius(); i++) {
            int k = i;
            SkillEffects.later(p, i, () -> {
                Vec3 q = p.position().add(dir.scale(k));
                level.sendParticles(ParticleTypes.EXPLOSION, q.x, q.y + 1, q.z, 1, 0, 0, 0, 0);
                level.sendParticles(KI_DUST, q.x, q.y + 1, q.z, 8, 0.4, 0.4, 0.4, 0);
                AxeSkills.crack(level, q, 6);
                for (LivingEntity e : AxeSkills.around(p, q, 1.6)) {
                    if (!hit.add(e.getId())) continue;
                    SkillEffects.hit(p, e, dmg);
                    e.setDeltaMovement(dir.x * 1.6, 0.4, dir.z * 1.6);
                    e.hurtMarked = true;
                }
            });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    /** 気功：気でHPを回復する（気が多いほどよく治る） */
    private static boolean qigong(ServerPlayer p, SkillDefinition s) {
        int k = ki(p);
        float heal = p.getMaxHealth() * (float) (s.power() + 0.02 * k);
        p.heal(heal);
        p.getPersistentData().putInt(KI, 0);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) Math.round(s.duration() * 20), 0));
        ServerLevel level = p.serverLevel();
        level.sendParticles(KI_DUST, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.7, 0.4, 0);
        level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 2, p.getZ(), 4, 0.4, 0.2, 0.4, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        p.displayClientMessage(Component.literal("気功：HPが" + Math.round(heal) + "回復").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    // ================= 覇拳の武神 =================

    /** 震脚：地面を踏み鳴らし、周りの敵を打ち上げる */
    private static boolean stomp(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = fist(p) * (float) s.power() * kiBonus(p);
        Vec3 c = p.position();
        for (LivingEntity e : AxeSkills.around(p, c, s.radius())) {
            if (!e.onGround()) continue;
            SkillEffects.hit(p, e, dmg);
            e.setDeltaMovement(e.getDeltaMovement().x * 0.2, 0.9, e.getDeltaMovement().z * 0.2);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
        }
        for (int w = 1; w <= 3; w++) {
            int k = w;
            SkillEffects.later(p, w * 2, () -> {
                double rr = s.radius() * k / 3.0;
                for (int j = 0; j < 16; j++) AxeSkills.crack(level, c.add(Math.cos(j * Math.PI / 8) * rr, 0, Math.sin(j * Math.PI / 8) * rr), 5);
                Vfx.ring(level, c, (float) rr * 1.2F, 10, Vfx.GOLD);
            });
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.2, c.z, 1, 0, 0, 0, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.4F);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    }

    /** 奥義・覇王拳：気の大きな拳を撃ち出し、通り道の敵を貫いて最後に爆ぜる */
    private static boolean overlord(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        float dmg = fist(p) * (float) s.power() * kiBonus(p);
        Vec3 from = p.getEyePosition().add(0, -0.3, 0);
        Vec3 dir = p.getLookAngle();
        var clip = level.clip(new ClipContext(from, from.add(dir.scale(s.radius())), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 to = clip.getLocation();
        int steps = Math.max(2, (int) Math.ceil(to.distanceTo(from) / 2.0));
        Set<Integer> hit = new HashSet<>();
        p.swing(InteractionHand.MAIN_HAND, true);
        p.swing(InteractionHand.OFF_HAND, true);
        Vfx.circle(level, from.add(dir.scale(1.5)), Circle.FIRE, 1.6F, 20, CircleOptions.FLASH);
        level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.2F);
        for (int i = 1; i <= steps; i++) {
            int k = i;
            SkillEffects.later(p, i, () -> {
                Vec3 q = from.add(to.subtract(from).scale((double) k / steps));
                level.sendParticles(KI_DUST, q.x, q.y, q.z, 30, 0.6, 0.6, 0.6, 0);
                level.sendParticles(ParticleTypes.FLAME, q.x, q.y, q.z, 12, 0.5, 0.5, 0.5, 0.02);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(q, q).inflate(1.6), e -> SkillEffects.isEnemy(p, e))) {
                    if (!hit.add(e.getId())) continue;
                    SkillEffects.hit(p, e, dmg);
                }
                if (k == steps) {
                    for (LivingEntity e : AxeSkills.around(p, q, 4.0)) {
                        SkillEffects.hit(p, e, dmg * 0.5F);
                        Vec3 push = e.position().subtract(q).normalize().scale(1.2);
                        e.push(push.x, 0.6, push.z);
                        e.hurtMarked = true;
                    }
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                    Vfx.ring(level, q, 5.0F, 14, Vfx.GOLD);
                    level.playSound(null, q.x, q.y, q.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5F, 0.7F);
                }
            });
        }
        p.displayClientMessage(Component.literal("覇王拳！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    private FistSkills() {}
}
