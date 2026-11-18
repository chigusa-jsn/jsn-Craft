package com.chigusa.jsncraft.magic;

import com.chigusa.jsncraft.party.PartyManager;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.stat.PlayerResources;
import com.chigusa.jsncraft.stat.ResourceService;
import com.chigusa.jsncraft.vfx.Circle;
import com.chigusa.jsncraft.vfx.Vfx;
import com.chigusa.jsncraft.weapon.StaffItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 魔導士（星詠みの魔導士・星界の大魔導）の魔法。
 * ダメージは「持っている杖のマナ弾の威力 × power」。魔法の威力（magic_power）で上がる。
 * 属性の組み合わせ：凍結に雷＝砕氷、燃焼に氷＝蒸気爆発、帯電に火＝プラズマ、星の刻印は全属性 +20%。
 */
public final class MageSpells {

    public enum Elem { FIRE, ICE, THUNDER, STAR, WIND, EARTH, WATER, DARK, LIGHT, NONE }

    private static final String FROZEN = "jsn_frozen", SHOCK = "jsn_shock", MARK = "jsn_star_mark", WET = "jsn_wet", CURSE = "jsn_curse";

    static final DustParticleOptions FIRE_DUST = dust(0xFF7030, 1.6F), ICE_DUST = dust(0x9AE0FF, 1.4F),
            BOLT_DUST = dust(0xFFE860, 1.2F), STAR_DUST = dust(0xC8A0FF, 1.4F), DARK_DUST = dust(0x4A2A80, 2.0F);

    private static DustParticleOptions dust(int c, float s) {
        return new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), s);
    }

    /** 魔法を使う（effect が mage_ で始まるもの） */
    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        return switch (s.effect()) {
            case "mage_fire_ball" -> fireBall(p, s);
            case "mage_fire_wall" -> fireWall(p, s);
            case "mage_fire_armor" -> fireArmor(p, s);
            case "mage_explosion" -> explosion(p, s);
            case "mage_inferno" -> inferno(p, s);
            case "mage_ice_lance" -> iceLance(p, s);
            case "mage_frost_field" -> frostField(p, s);
            case "mage_ice_barrier" -> iceBarrier(p, s);
            case "mage_blizzard" -> blizzard(p, s);
            case "mage_absolute_zero" -> absoluteZero(p, s);
            case "mage_lightning" -> lightning(p, s);
            case "mage_thunder_cloud" -> thunderCloud(p, s);
            case "mage_bolt_step" -> boltStep(p, s);
            case "mage_chain_lightning" -> chainLightning(p, s);
            case "mage_judgment" -> judgment(p, s);
            case "mage_star_dust" -> starDust(p, s);
            case "mage_gravity" -> gravity(p, s);
            case "mage_star_light" -> starLight(p, s);
            case "mage_stellar_ray" -> stellarRay(p, s);
            case "mage_meteor" -> meteor(p, s);
            case "mage_teleport" -> teleport(p, s);
            case "mage_mana_shield" -> manaShield(p, s);
            case "mage_meditation" -> meditation(p, s);
            default -> false;
        };
    }

    // ================= 共通 =================

    /** 魔法の基本の威力：手に持った杖（魔導書）のマナ弾 */
    public static float base(ServerPlayer p) {
        return p.getMainHandItem().getItem() instanceof StaffItem st ? st.tier().bolt() : 3.0F;
    }

    private static long now(LivingEntity e) {
        return e.level().getGameTime();
    }

    public static boolean frozen(LivingEntity e) {
        return e.getPersistentData().getLong(FROZEN) > now(e);
    }

    public static boolean shocked(LivingEntity e) {
        return e.getPersistentData().getLong(SHOCK) > now(e);
    }

    public static boolean marked(LivingEntity e) {
        return e.getPersistentData().getLong(MARK) > now(e);
    }

    public static boolean wet(LivingEntity e) {
        return e.getPersistentData().getLong(WET) > now(e) || e.isInWaterRainOrBubble();
    }

    public static boolean cursed(LivingEntity e) {
        return e.getPersistentData().getLong(CURSE) > now(e);
    }

    /** 呪う（ticks のあいだ魔法のダメージ +25%、弱体） */
    public static void curse(LivingEntity e, int ticks) {
        e.getPersistentData().putLong(CURSE, now(e) + ticks);
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
    }

    /** 宙に浮いている（風で巻き上げられた）か */
    public static boolean airborne(LivingEntity e) {
        return !e.onGround() && !e.isInWater() && !(e instanceof net.minecraft.world.entity.FlyingMob);
    }

    /** 凍らせる（hard なら完全に止める） */
    public static void freeze(LivingEntity e, int ticks, boolean hard) {
        e.getPersistentData().putLong(FROZEN, now(e) + ticks);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, hard ? 9 : 3, false, false));
        if (hard) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2, false, false));
        e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + ticks / 2));
        ((ServerLevel) e.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
    }

    /** 魔法でダメージ。属性の組み合わせと状態（燃焼・冷気・帯電・刻印）もここで */
    public static void magicHit(ServerPlayer p, LivingEntity e, float dmg, Elem el) {
        if (!SkillEffects.isEnemy(p, e)) return;
        ServerLevel level = p.serverLevel();
        String combo = null;
        if (el == Elem.THUNDER && wet(e) && !frozen(e)) {
            dmg *= 1.5F;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1, e.getZ(), 40, 0.5, 0.7, 0.5, 0.4);
            level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + 1, e.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            combo = "感電！";
        } else if (el == Elem.ICE && wet(e) && !frozen(e)) {
            e.getPersistentData().remove(WET);
            freeze(e, 60, false);
            combo = "瞬間凍結！";
        } else if (el == Elem.EARTH && wet(e)) {
            SkillDefinition mud = SkillService.passive(p, "spirit_mud");
            dmg *= 1.4F + (mud != null ? (float) mud.power() : 0);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, mud != null ? 100 : 60, mud != null ? 5 : 3));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), e.getX(), e.getY() + 0.3, e.getZ(), 30, 0.5, 0.3, 0.5, 0.1);
            combo = "泥沼！";
        } else if (el == Elem.EARTH && airborne(e)) {
            dmg *= 1.6F;
            e.setDeltaMovement(e.getDeltaMovement().x * 0.3, -1.6, e.getDeltaMovement().z * 0.3);
            e.hurtMarked = true;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), e.getX(), e.getY() + 0.5, e.getZ(), 25, 0.4, 0.4, 0.4, 0.1);
            combo = "叩きつけ！";
        } else if (el == Elem.LIGHT && cursed(e)) {
            SkillDefinition purge = SkillService.passive(p, "holy_purge");
            dmg *= 1.8F + (purge != null ? (float) purge.power() : 0);
            e.getPersistentData().remove(CURSE);
            e.removeEffect(MobEffects.WEAKNESS);
            level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 30, 0.4, 0.7, 0.4, 0.15);
            level.sendParticles(ParticleTypes.FLASH, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, e.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
            combo = "浄化！";
        } else if (el == Elem.THUNDER && frozen(e)) {
            SkillDefinition sh = SkillService.passive(p, "mage_shatter");
            dmg *= 1.8F + (sh != null ? (float) sh.power() : 0);
            e.getPersistentData().remove(FROZEN);
            e.setTicksFrozen(0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                    e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 40, 0.4, 0.5, 0.4, 0.15);
            level.playSound(null, e.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
            combo = "砕氷！";
        } else if (el == Elem.ICE && e.isOnFire()) {
            dmg *= 1.5F;
            e.clearFire();
            level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 1, e.getZ(), 30, 0.6, 0.6, 0.6, 0.08);
            level.playSound(null, e.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.8F);
            combo = "蒸気爆発！";
        } else if (el == Elem.FIRE && shocked(e)) {
            dmg *= 1.3F;
            e.getPersistentData().remove(SHOCK);
            float splash = dmg * 0.5F;
            for (LivingEntity o : enemies(p, e.position(), 2.5)) if (o != e) SkillEffects.hit(p, o, splash);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1, e.getZ(), 40, 1.0, 0.8, 1.0, 0.4);
            level.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + 1, e.getZ(), 30, 1.0, 0.8, 1.0, 0.1);
            level.playSound(null, e.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 0.8F);
            combo = "プラズマ！";
        }
        if (marked(e) && el != Elem.STAR) dmg *= 1.2F;
        if (el == Elem.LIGHT && e.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) {   // 光はアンデッドに2倍
            SkillDefinition bane = SkillService.passive(p, "holy_undead_bane");
            dmg *= 2.0F + (bane != null ? (float) bane.power() : 0);
        }
        if (cursed(e)) {
            SkillDefinition deep = SkillService.passive(p, "spirit_deep_curse");
            dmg *= 1.25F + (deep != null ? (float) deep.power() : 0);
        }
        if (wet(e)) {
            SkillDefinition tide = SkillService.passive(p, "spirit_tide");
            if (tide != null) dmg *= 1 + (float) tide.power();
        }
        if (el == Elem.WIND && airborne(e)) {
            SkillDefinition tail = SkillService.passive(p, "spirit_tailwind");
            if (tail != null) dmg *= 1 + (float) tail.power();
        }
        if (el == Elem.FIRE && e.isOnFire()) {
            SkillDefinition b = SkillService.passive(p, "mage_burn_boost");
            if (b != null) dmg *= 1 + (float) b.power();
        }
        if (frozen(e)) {
            SkillDefinition b = SkillService.passive(p, "mage_ice_heart");
            if (b != null) dmg *= 1 + (float) b.power();
        }
        if (shocked(e)) {
            SkillDefinition b = SkillService.passive(p, "mage_overcharge");
            if (b != null) dmg *= 1 + (float) b.power();
        }
        SkillDefinition flow = SkillService.passive(p, "mage_overflow");
        PlayerResources r = ResourceService.get(p);
        if (flow != null && r.mana >= ResourceService.maxMana(p) * 0.8F) dmg *= 1 + (float) flow.power();

        SkillEffects.hit(p, e, dmg);
        if (combo != null) p.displayClientMessage(Component.literal(combo).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);

        // 状態をつける
        switch (el) {
            case FIRE -> e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 80));
            case ICE -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            case THUNDER -> e.getPersistentData().putLong(SHOCK, now(e) + 80);
            case STAR -> e.getPersistentData().putLong(MARK, now(e) + 200);
            case WATER -> {
                e.getPersistentData().putLong(WET, now(e) + 160);
                if (e.isOnFire()) e.clearFire();
            }
            case EARTH -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            default -> {}
        }
        // 魔力循環：魔法で倒すとマナが戻る
        if (!e.isAlive()) {
            SkillDefinition mf = SkillService.passive(p, "mage_mana_flow");
            if (mf != null) r.mana = Math.min(ResourceService.maxMana(p), r.mana + (float) mf.power());
            if (el == Elem.DARK) {   // 魂食い：闇の魔法で倒すとHP回復
                SkillDefinition soul = SkillService.passive(p, "spirit_soul_eater");
                if (soul != null) p.heal((float) soul.power());
            }
        }
    }

    static List<LivingEntity> enemies(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, r * 0.6 + 2, r),
                e -> SkillEffects.isEnemy(p, e))) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z;
            if (dx * dx + dz * dz <= r * r) out.add(e);
        }
        return out;
    }

    /** 目線の先の地面（range ブロックまで） */
    static Vec3 groundTarget(ServerPlayer p, double range) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 t = hit.getType() == HitResult.Type.MISS ? hit.getLocation() : hit.getLocation().subtract(p.getLookAngle().scale(0.1));
        for (int i = 0; i < 12; i++) {
            BlockPos below = BlockPos.containing(t.x, t.y - 0.5, t.z);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) break;
            t = t.subtract(0, 1, 0);
        }
        return new Vec3(t.x, Math.floor(t.y + 0.01), t.z);
    }

    /** 目線の先の敵（なければ前方の近い敵） */
    static LivingEntity lookTarget(ServerPlayer p, double range) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult bh = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(level, p, eye, end, new AABB(eye, end).inflate(1.5),
                e -> e instanceof LivingEntity le && SkillEffects.isEnemy(p, le), 0.8F);
        if (eh != null) return (LivingEntity) eh.getEntity();
        Vec3 look = p.getLookAngle();
        return level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range * 0.6), e -> SkillEffects.isEnemy(p, e)
                        && e.position().subtract(p.position()).normalize().dot(look) > 0.8 && p.hasLineOfSight(e))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
    }

    static void sound(ServerPlayer p, Vec3 at, SoundEvent s, float vol, float pitch) {
        p.serverLevel().playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    /** interval tick ごとに times 回くり返す（i は 0 から） */
    static void repeat(ServerPlayer p, int interval, int times, java.util.function.IntConsumer run) {
        for (int i = 0; i < times; i++) {
            int k = i;
            SkillEffects.later(p, i * interval, () -> {
                if (p.isAlive() && !p.isRemoved()) run.accept(k);
            });
        }
    }

    /** 稲妻の線（見た目だけ） */
    static void zap(ServerLevel level, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        int n = Math.max(4, (int) (d.length() * 3));
        var rnd = level.random;
        for (int i = 0; i <= n; i++) {
            Vec3 q = a.add(d.scale((double) i / n)).add((rnd.nextDouble() - 0.5) * 0.3, (rnd.nextDouble() - 0.5) * 0.3, (rnd.nextDouble() - 0.5) * 0.3);
            level.sendParticles(BOLT_DUST, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            if (i % 3 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.05);
        }
    }

    static void strike(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(at);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    /**
     * 飛ぶ魔法（エンティティなし、毎tick進む）。pierce 体まで貫く。
     * homing > 0 なら近くの敵へ曲がる。onHit は敵に当たるたび、onEnd は最後の場所。
     */
    static void shoot(ServerPlayer p, Vec3 pos, Vec3 vel, int life, int pierce, double homing, ParticleOptions trail,
                      Consumer<LivingEntity> onHit, Consumer<Vec3> onEnd) {
        step(p, pos, vel, life, pierce, homing, trail, onHit, onEnd, new HashSet<>());
    }

    private static void step(ServerPlayer p, Vec3 pos, Vec3 vel, int life, int pierce, double homing, ParticleOptions trail,
                             Consumer<LivingEntity> onHit, Consumer<Vec3> onEnd, Set<Integer> done) {
        if (!p.isAlive() || p.isRemoved()) return;
        ServerLevel level = p.serverLevel();
        if (homing > 0) {
            LivingEntity t = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(10),
                            e -> SkillEffects.isEnemy(p, e) && !done.contains(e.getId()))
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(pos))).orElse(null);
            if (t != null) {
                Vec3 want = t.getBoundingBox().getCenter().subtract(pos).normalize().scale(vel.length());
                vel = vel.scale(1 - homing).add(want.scale(homing));
            }
        }
        Vec3 next = pos.add(vel);
        BlockHitResult bh = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = bh.getType() == HitResult.Type.MISS ? next : bh.getLocation();
        for (int i = 0; i < 3; i++) {
            Vec3 q = pos.add(end.subtract(pos).scale(i / 3.0));
            level.sendParticles(trail, q.x, q.y, q.z, 1, 0.03, 0.03, 0.03, 0.002);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, end).inflate(0.5), e -> SkillEffects.isEnemy(p, e))) {
            if (done.contains(e.getId())) continue;
            if (e.getBoundingBox().inflate(0.4).clip(pos, end).isEmpty() && !e.getBoundingBox().inflate(0.4).contains(end)) continue;
            done.add(e.getId());
            onHit.accept(e);
            if (done.size() >= pierce) {
                onEnd.accept(e.getBoundingBox().getCenter());
                return;
            }
        }
        if (bh.getType() != HitResult.Type.MISS || life <= 0) {
            onEnd.accept(end);
            return;
        }
        Vec3 v = vel;
        SkillEffects.later(p, 1, () -> step(p, next, v, life - 1, pierce, homing, trail, onHit, onEnd, done));
    }

    static Vec3 hand(ServerPlayer p) {
        return p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.2, 0);
    }

    // ================= 火 =================

    private static boolean fireBall(ServerPlayer p, SkillDefinition s) {
        float dmg = base(p) * (float) s.power();
        double r = Math.max(1.5, s.radius());
        sound(p, p.position(), SoundEvents.BLAZE_SHOOT, 0.8F, 1.1F);
        shoot(p, hand(p), p.getLookAngle().scale(1.3), 30, 1, 0, ParticleTypes.FLAME, e -> {}, at -> {
            ServerLevel level = p.serverLevel();
            for (LivingEntity e : enemies(p, at, r)) magicHit(p, e, dmg, Elem.FIRE);
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.3, 0.3, 0.3, 0);
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 30, 0.5, 0.5, 0.5, 0.12);
            level.sendParticles(FIRE_DUST, at.x, at.y, at.z, 15, 0.6, 0.6, 0.6, 0);
            sound(p, at, SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 1.3F);
        });
        return true;
    }

    private static boolean fireWall(ServerPlayer p, SkillDefinition s) {
        Vec3 flat = p.getLookAngle().multiply(1, 0, 1);
        Vec3 look = flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        Vec3 center = groundTarget(p, 4.5);
        double half = Math.max(2, s.radius()) / 2.0;
        float dmg = base(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 2);
        ServerLevel level = p.serverLevel();
        sound(p, center, SoundEvents.FIRECHARGE_USE, 1.0F, 0.7F);
        Vfx.magicCircle(level, center, (float) half, pulses * 10, Circle.FIRE);
        repeat(p, 10, pulses, i -> {
            for (double t = -half; t <= half; t += 0.5) {
                Vec3 q = center.add(side.scale(t));
                level.sendParticles(ParticleTypes.FLAME, q.x, q.y + 0.3, q.z, 3, 0.15, 0.6, 0.15, 0.02);
                if (level.random.nextInt(3) == 0) level.sendParticles(ParticleTypes.LAVA, q.x, q.y + 0.2, q.z, 1, 0.1, 0.1, 0.1, 0);
            }
            for (LivingEntity e : enemies(p, center, half + 1)) {
                Vec3 d = e.position().subtract(center);
                if (Math.abs(d.dot(look)) < 1.2 && Math.abs(d.dot(side)) <= half + 0.5) magicHit(p, e, dmg, Elem.FIRE);
            }
            if (i % 4 == 0) sound(p, center, SoundEvents.FIRE_AMBIENT, 1.0F, 1.0F);
        });
        return true;
    }

    private static boolean fireArmor(ServerPlayer p, SkillDefinition s) {
        TimedFlags.set(p, "mage_fire_armor", base(p) * s.power(), s.duration());
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, (int) Math.round(s.duration() * 20), 0, false, false, true));
        ServerLevel level = p.serverLevel();
        repeat(p, 5, (int) Math.round(s.duration() * 4), i ->
                level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 3, 0.4, 0.6, 0.4, 0.01));
        Vfx.ring(level, p.position(), 2.5F, 12, Vfx.RED);
        sound(p, p.position(), SoundEvents.BLAZE_AMBIENT, 1.0F, 1.2F);
        return true;
    }

    private static boolean explosion(ServerPlayer p, SkillDefinition s) {
        Vec3 at = groundTarget(p, 24).add(0, 0.5, 0);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : enemies(p, at, r)) {
            magicHit(p, e, dmg, Elem.FIRE);
            Vec3 push = e.position().subtract(at).normalize().scale(1.0);
            e.push(push.x, 0.5, push.z);
            e.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 120, r * 0.4, 1.0, r * 0.4, 0.25);
        level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 25, r * 0.3, 0.5, r * 0.3, 0);
        level.sendParticles(FIRE_DUST, at.x, at.y + 1, at.z, 60, r * 0.5, 1.2, r * 0.5, 0);
        Vfx.ring(level, at, (float) r * 1.5F, 14, Vfx.RED);
        Vfx.magicCircle(level, at, (float) r, 20, Circle.FIRE);
        sound(p, at, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.8F);
        sound(p, at, SoundEvents.DRAGON_FIREBALL_EXPLODE, 1.0F, 0.9F);
        return true;
    }

    private static boolean inferno(ServerPlayer p, SkillDefinition s) {
        Vec3 at = groundTarget(p, 24);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 2);
        ServerLevel level = p.serverLevel();
        Vfx.magicCircle(level, at, (float) r, pulses * 10 + 10, Circle.FIRE);
        Vfx.circle(level, at.add(0, 6, 0), Circle.FIRE, (float) r * 0.8F, pulses * 10 + 10, com.chigusa.jsncraft.vfx.CircleOptions.SKY);
        sound(p, at, SoundEvents.BLAZE_SHOOT, 2.0F, 0.5F);
        repeat(p, 10, pulses, i -> {
            var rnd = level.random;
            for (int k = 0; k < 6; k++) {   // 火柱
                double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
                Vec3 q = at.add(Math.cos(a) * d, 0, Math.sin(a) * d);
                for (int h = 0; h < 8; h++) level.sendParticles(ParticleTypes.FLAME, q.x, q.y + h * 0.5, q.z, 2, 0.15, 0.2, 0.15, 0.02);
                level.sendParticles(ParticleTypes.LAVA, q.x, q.y + 0.3, q.z, 2, 0.2, 0.1, 0.2, 0);
            }
            level.sendParticles(FIRE_DUST, at.x, at.y + 1, at.z, 30, r * 0.5, 0.8, r * 0.5, 0);
            for (LivingEntity e : enemies(p, at, r)) magicHit(p, e, dmg, Elem.FIRE);
            if (i % 2 == 0) sound(p, at, SoundEvents.FIRECHARGE_USE, 1.2F, 0.6F + rnd.nextFloat() * 0.3F);
        });
        return true;
    }

    // ================= 氷 =================

    private static boolean iceLance(ServerPlayer p, SkillDefinition s) {
        float dmg = base(p) * (float) s.power();
        int pierce = Math.max(1, s.amplifier());
        sound(p, p.position(), SoundEvents.GLASS_PLACE, 1.0F, 1.6F);
        shoot(p, hand(p), p.getLookAngle().scale(2.2), 14, pierce, 0, ParticleTypes.SNOWFLAKE, e -> {
            magicHit(p, e, dmg, Elem.ICE);
            if (e.isAlive() && p.getRandom().nextDouble() < s.chance()) freeze(e, 60, false);
            sound(p, e.position(), SoundEvents.GLASS_BREAK, 0.6F, 1.8F);
        }, at -> p.serverLevel().sendParticles(ICE_DUST, at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0));
        return true;
    }

    private static boolean frostField(ServerPlayer p, SkillDefinition s) {
        Vec3 at = groundTarget(p, 20);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration());
        ServerLevel level = p.serverLevel();
        Vfx.magicCircle(level, at, (float) r, pulses * 20, Circle.ICE);
        sound(p, at, SoundEvents.POWDER_SNOW_PLACE, 1.5F, 0.8F);
        repeat(p, 20, pulses, i -> {
            level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.3, at.z, 40, r * 0.5, 0.2, r * 0.5, 0.01);
            for (LivingEntity e : enemies(p, at, r)) {
                magicHit(p, e, dmg, Elem.ICE);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                if (e.isAlive() && level.random.nextDouble() < s.chance()) freeze(e, 50, false);
            }
        });
        return true;
    }

    private static boolean iceBarrier(ServerPlayer p, SkillDefinition s) {
        float amount = base(p) * (float) s.power();
        SkillEffects.attributeBuff(p, Attributes.MAX_ABSORPTION, "mage_ice_barrier", amount, AttributeModifier.Operation.ADD_VALUE, s.duration());
        SkillEffects.attributeBuff(p, Attributes.KNOCKBACK_RESISTANCE, "mage_ice_barrier_kb", 0.5, AttributeModifier.Operation.ADD_VALUE, s.duration());
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), amount));
        ServerLevel level = p.serverLevel();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.8, 0.6, 0.05);
        level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.7, 0.9, 0.7, 0.02);
        Vfx.ring(level, p.position(), 2.0F, 12, Vfx.AZURE);
        sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_PLACE, 1.2F, 0.8F);
        return true;
    }

    private static boolean blizzard(ServerPlayer p, SkillDefinition s) {
        Vec3 at = p.position();
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 2);
        ServerLevel level = p.serverLevel();
        Vfx.magicCircle(level, at, (float) r, pulses * 10, Circle.ICE);
        repeat(p, 10, pulses, i -> {
            level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 5, at.z, 80, r * 0.6, 1.5, r * 0.6, 0.05);
            level.sendParticles(ICE_DUST, at.x, at.y + 3, at.z, 30, r * 0.6, 2.0, r * 0.6, 0);
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 6, at.z, 15, r * 0.6, 0.3, r * 0.6, 0.01);
            for (LivingEntity e : enemies(p, at, r)) {
                magicHit(p, e, dmg, Elem.ICE);
                if (e.isAlive() && level.random.nextDouble() < s.chance()) freeze(e, 60, false);
            }
            if (i % 3 == 0) sound(p, at, SoundEvents.SNOW_GOLEM_SHOOT, 1.2F, 0.5F);
        });
        return true;
    }

    private static boolean absoluteZero(ServerPlayer p, SkillDefinition s) {
        Vec3 at = p.position();
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int ticks = (int) Math.round(s.duration() * 20);
        ServerLevel level = p.serverLevel();
        int n = 0;
        for (LivingEntity e : enemies(p, at, r)) {
            magicHit(p, e, dmg, Elem.ICE);
            if (e.isAlive()) freeze(e, ticks, true);
            n++;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), at.x, at.y + 1, at.z, 200, r * 0.5, 1.0, r * 0.5, 0.1);
        level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1, at.z, 200, r * 0.6, 1.5, r * 0.6, 0.05);
        Vfx.ring(level, at, (float) r * 1.4F, 18, Vfx.AZURE);
        Vfx.ring(level, at, (float) r, 14, Vfx.WHITE);
        Vfx.magicCircle(level, at, (float) r, 40, Circle.ICE);
        sound(p, at, SoundEvents.GLASS_BREAK, 2.0F, 0.4F);
        sound(p, at, SoundEvents.PLAYER_HURT_FREEZE, 1.5F, 0.6F);
        p.displayClientMessage(Component.literal("アブソリュートゼロ：" + n + "体を凍らせた").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    // ================= 雷 =================

    private static boolean lightning(ServerPlayer p, SkillDefinition s) {
        float dmg = base(p) * (float) s.power();
        LivingEntity t = lookTarget(p, 24);
        ServerLevel level = p.serverLevel();
        Vec3 at = t != null ? t.position() : groundTarget(p, 24);
        strike(level, at);
        zap(level, at.add(0, 8, 0), at);
        if (t != null) magicHit(p, t, dmg, Elem.THUNDER);
        else for (LivingEntity e : enemies(p, at, 1.5)) magicHit(p, e, dmg, Elem.THUNDER);
        return true;
    }

    private static boolean thunderCloud(ServerPlayer p, SkillDefinition s) {
        Vec3 at = groundTarget(p, 20);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int strikes = (int) Math.round(s.duration() / 0.75);
        ServerLevel level = p.serverLevel();
        Vfx.magicCircle(level, at, (float) r, strikes * 15, Circle.THUNDER);
        sound(p, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.5F);
        repeat(p, 15, strikes, i -> {
            Vec3 cloud = at.add(0, 6, 0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, cloud.x, cloud.y, cloud.z, 25, r * 0.5, 0.3, r * 0.5, 0.01);
            level.sendParticles(ParticleTypes.CLOUD, cloud.x, cloud.y, cloud.z, 15, r * 0.5, 0.3, r * 0.5, 0.01);
            List<LivingEntity> list = enemies(p, at, r);
            Vec3 hitAt;
            if (!list.isEmpty()) {
                LivingEntity e = list.get(level.random.nextInt(list.size()));
                hitAt = e.position();
                magicHit(p, e, dmg, Elem.THUNDER);
            } else {
                double a = level.random.nextDouble() * Math.PI * 2, d = level.random.nextDouble() * r;
                hitAt = at.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            }
            zap(level, new Vec3(hitAt.x, cloud.y, hitAt.z), hitAt);
            sound(p, hitAt, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.6F);
        });
        return true;
    }

    /** 安全な行き先（look の方向へ最大 range、壁の手前で止まる） */
    private static Vec3 blinkTarget(ServerPlayer p, double range) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        BlockHitResult bh = level.clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 dest = bh.getLocation().subtract(p.getLookAngle().scale(0.7)).subtract(0, p.getEyeHeight(), 0);
        for (int up = 0; up <= 2; up++) {
            Vec3 d = dest.add(0, up, 0);
            if (level.noCollision(p, p.getBoundingBox().move(d.subtract(p.position())))) return d;
        }
        return null;
    }

    private static boolean boltStep(ServerPlayer p, SkillDefinition s) {
        Vec3 from = p.position();
        Vec3 to = blinkTarget(p, s.radius());
        if (to == null) return false;
        ServerLevel level = p.serverLevel();
        float dmg = base(p) * (float) s.power();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.2), e -> SkillEffects.isEnemy(p, e))) {
            if (e.getBoundingBox().inflate(1.0).clip(from.add(0, 1, 0), to.add(0, 1, 0)).isPresent()) magicHit(p, e, dmg, Elem.THUNDER);
        }
        zap(level, from.add(0, 1, 0), to.add(0, 1, 0));
        p.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y + 1, to.z, 30, 0.4, 0.6, 0.4, 0.3);
        sound(p, to, SoundEvents.TRIDENT_THUNDER.value(), 0.6F, 1.6F);
        return true;
    }

    private static boolean chainLightning(ServerPlayer p, SkillDefinition s) {
        LivingEntity first = lookTarget(p, 24);
        if (first == null) {
            p.displayClientMessage(Component.literal("ねらう敵がいない").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel level = p.serverLevel();
        float dmg = base(p) * (float) s.power();
        int jumps = Math.max(1, s.amplifier());
        Set<Integer> done = new HashSet<>();
        Vec3 from = hand(p);
        LivingEntity cur = first;
        for (int i = 0; i <= jumps && cur != null; i++) {
            LivingEntity target = cur;
            Vec3 a = from;
            float d = dmg * (float) Math.pow(0.85, i);
            SkillEffects.later(p, i * 3, () -> {
                if (!target.isAlive()) return;
                zap(level, a, target.getBoundingBox().getCenter());
                magicHit(p, target, d, Elem.THUNDER);
                sound(p, target.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4F, 1.8F);
            });
            done.add(cur.getId());
            from = cur.getBoundingBox().getCenter();
            Vec3 c = from;
            cur = level.getEntitiesOfClass(LivingEntity.class, cur.getBoundingBox().inflate(s.radius()), e -> SkillEffects.isEnemy(p, e) && !done.contains(e.getId()))
                    .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(c))).orElse(null);
        }
        return true;
    }

    private static boolean judgment(ServerPlayer p, SkillDefinition s) {
        Vec3 at = p.position();
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int strikes = Math.max(1, s.amplifier());
        ServerLevel level = p.serverLevel();
        Vfx.magicCircle(level, at, (float) r, strikes * 5 + 20, Circle.THUNDER);
        Vfx.circle(level, at.add(0, 8, 0), Circle.THUNDER, (float) r, strikes * 5 + 20, com.chigusa.jsncraft.vfx.CircleOptions.SKY);
        repeat(p, 5, strikes, i -> {
            List<LivingEntity> list = enemies(p, at, r);
            Vec3 hitAt;
            if (!list.isEmpty()) hitAt = list.get(level.random.nextInt(list.size())).position();
            else {
                double a = level.random.nextDouble() * Math.PI * 2, d = level.random.nextDouble() * r;
                hitAt = at.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            }
            strike(level, hitAt);
            for (LivingEntity e : enemies(p, hitAt, 2.0)) magicHit(p, e, dmg, Elem.THUNDER);
        });
        return true;
    }

    // ================= 星 =================

    private static boolean starDust(ServerPlayer p, SkillDefinition s) {
        float dmg = base(p) * (float) s.power();
        int n = Math.max(1, s.amplifier());
        Vec3 look = p.getLookAngle();
        sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.6F);
        for (int i = 0; i < n; i++) {
            double spread = (i - (n - 1) / 2.0) * 0.18;
            Vec3 v = look.yRot((float) spread).add(0, 0.15, 0).normalize().scale(1.0);
            int k = i;
            SkillEffects.later(p, i * 2, () -> shoot(p, hand(p), v, 30, 1, 0.25, ParticleTypes.END_ROD,
                    e -> magicHit(p, e, dmg, Elem.STAR),
                    at -> p.serverLevel().sendParticles(STAR_DUST, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0)));
        }
        return true;
    }

    private static boolean gravity(ServerPlayer p, SkillDefinition s) {
        Vec3 at = groundTarget(p, 20).add(0, 0.5, 0);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        int pulses = (int) Math.round(s.duration() * 2);
        ServerLevel level = p.serverLevel();
        Vfx.magicCircle(level, at, (float) r, pulses * 10, Circle.STAR);
        sound(p, at, SoundEvents.PORTAL_TRIGGER, 0.5F, 1.8F);
        repeat(p, 10, pulses, i -> {
            for (int k = 0; k < 16; k++) {
                double a = (i * 0.6 + k * Math.PI / 8);
                double d = r * (1 - (k % 4) * 0.22);
                level.sendParticles(STAR_DUST, at.x + Math.cos(a) * d, at.y + 0.3, at.z + Math.sin(a) * d, 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.5, at.z, 20, r * 0.4, 0.4, r * 0.4, 0.05);
            for (LivingEntity e : enemies(p, at, r + 1)) {
                Vec3 pull = at.subtract(e.position()).multiply(1, 0, 1);
                if (pull.lengthSqr() > 0.25) pull = pull.normalize().scale(0.35);
                e.setDeltaMovement(e.getDeltaMovement().scale(0.3).add(pull.x, -0.05, pull.z));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                magicHit(p, e, dmg, Elem.STAR);
            }
        });
        return true;
    }

    private static boolean starLight(ServerPlayer p, SkillDefinition s) {
        PartyManager.Party party = PartyManager.partyOf(p);
        ServerLevel level = p.serverLevel();
        for (ServerPlayer o : level.players()) {
            boolean ally = o == p || (party != null && party.members.contains(o.getUUID()));
            if (!ally || o.distanceTo(p) > s.radius()) continue;
            SkillEffects.attributeBuff(o, ModAttributes.MAGIC_POWER, "mage_star_light", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            PlayerResources r = ResourceService.get(o);
            r.mana = Math.min(ResourceService.maxMana(o), r.mana + (float) s.amplifier());
            level.sendParticles(ParticleTypes.END_ROD, o.getX(), o.getY() + 3, o.getZ(), 25, 0.5, 1.0, 0.5, 0.02);
            Vfx.ring(level, o.position(), 1.6F, 12, Vfx.WHITE);
        }
        Vfx.magicCircle(level, p.position(), (float) s.radius() * 0.6F, 30, Circle.STAR);
        sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.4F);
        return true;
    }

    private static boolean stellarRay(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 from = hand(p);
        Vec3 look = p.getLookAngle();
        BlockHitResult bh = level.clip(new ClipContext(from, from.add(look.scale(s.radius())), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 to = bh.getLocation();
        float dmg = base(p) * (float) s.power();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0), e -> SkillEffects.isEnemy(p, e))) {
            if (e.getBoundingBox().inflate(0.8).clip(from, to).isPresent()) magicHit(p, e, dmg, Elem.STAR);
        }
        double len = to.distanceTo(from);
        for (double t = 0; t < len; t += 0.4) {
            Vec3 q = from.add(look.scale(t));
            level.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(STAR_DUST, q.x, q.y, q.z, 2, 0.15, 0.15, 0.15, 0);
        }
        level.sendParticles(ParticleTypes.FLASH, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        sound(p, from, SoundEvents.BEACON_DEACTIVATE, 1.2F, 2.0F);
        sound(p, to, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 0.6F);
        return true;
    }

    private static boolean meteor(ServerPlayer p, SkillDefinition s) {
        Vec3 at = groundTarget(p, 28);
        double r = s.radius();
        float dmg = base(p) * (float) s.power();
        ServerLevel level = p.serverLevel();
        int fall = 30;
        Vec3 start = at.add(-10, 30, -6);
        Vfx.magicCircle(level, at, (float) r, fall + 20, Circle.STAR);
        sound(p, at, SoundEvents.WITHER_SPAWN, 0.6F, 1.5F);
        repeat(p, 1, fall, i -> {
            Vec3 q = start.add(at.subtract(start).scale((i + 1) / (double) fall));
            level.sendParticles(ParticleTypes.FLAME, q.x, q.y, q.z, 25, 0.8, 0.8, 0.8, 0.05);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, q.x, q.y, q.z, 8, 0.6, 0.6, 0.6, 0.02);
            level.sendParticles(STAR_DUST, q.x, q.y, q.z, 15, 1.0, 1.0, 1.0, 0);
            level.sendParticles(ParticleTypes.LAVA, q.x, q.y, q.z, 3, 0.5, 0.5, 0.5, 0);
        });
        SkillEffects.later(p, fall, () -> {
            if (!p.isAlive()) return;
            int n = 0;
            for (LivingEntity e : enemies(p, at, r)) {
                float d = marked(e) ? dmg * 1.5F : dmg;
                magicHit(p, e, d, Elem.STAR);
                e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 100));
                Vec3 push = e.position().subtract(at).normalize().scale(1.4);
                e.push(push.x, 0.8, push.z);
                e.hurtMarked = true;
                n++;
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.5, at.z, 3, r * 0.3, 0.3, r * 0.3, 0);
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 200, r * 0.5, 1.0, r * 0.5, 0.3);
            level.sendParticles(STAR_DUST, at.x, at.y + 1, at.z, 120, r * 0.6, 1.5, r * 0.6, 0);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 80, r * 0.4, 1.0, r * 0.4, 0.3);
            Vfx.ring(level, at, (float) r * 1.6F, 20, Vfx.GOLD);
            Vfx.ring(level, at, (float) r, 14, Vfx.WHITE);
            sound(p, at, SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.5F);
            sound(p, at, SoundEvents.DRAGON_FIREBALL_EXPLODE, 2.0F, 0.6F);
            p.displayClientMessage(Component.literal("メテオフォール：" + n + "体に命中").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        });
        return true;
    }

    // ================= 共通の魔法 =================

    private static boolean teleport(ServerPlayer p, SkillDefinition s) {
        Vec3 from = p.position();
        Vec3 to = blinkTarget(p, s.radius());
        if (to == null) return false;
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 40, 0.4, 0.8, 0.4, 0.3);
        p.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 40, 0.4, 0.8, 0.4, 0.1);
        Vfx.ring(level, to, 1.5F, 10, Vfx.WHITE);
        sound(p, to, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.3F);
        return true;
    }

    private static boolean manaShield(ServerPlayer p, SkillDefinition s) {
        TimedFlags.set(p, "mage_mana_shield", s.power(), s.duration());
        ServerLevel level = p.serverLevel();
        repeat(p, 10, (int) Math.round(s.duration() * 2), i ->
                level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 6, 0.5, 0.7, 0.5, 0.3));
        Vfx.ring(level, p.position(), 1.8F, 14, Vfx.AZURE);
        sound(p, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.6F);
        return true;
    }

    private static boolean meditation(ServerPlayer p, SkillDefinition s) {
        PlayerResources r = ResourceService.get(p);
        float max = ResourceService.maxMana(p);
        r.mana = Math.min(max, r.mana + max * (float) s.power());
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 2, p.getZ(), 60, 0.8, 0.8, 0.8, 0.8);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 15, 0.5, 0.8, 0.5, 0.02);
        Vfx.ring(level, p.position(), 2.0F, 14, Vfx.AZURE);
        sound(p, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.8F);
        p.displayClientMessage(Component.literal("メディテーション：マナが回復した").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    private MageSpells() {}
}
