package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 射手の効果：遠矢（遠いほど強い）、鷹の目（会心）、風読み、矢筒、貫通、爆裂矢、重い矢のノックバック。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class ArcherEvents {

    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer p)) return;
        // 風読み：矢がまっすぐ速く飛び、少し追いかける
        double wind = TimedFlags.get(p, "archer_wind");
        if (wind > 0) {
            arrow.setNoGravity(true);
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1 + wind));
            CompoundTag t = arrow.getPersistentData().getCompound(BowSkills.TAG);
            t.putInt("nograv", 60);
            arrow.getPersistentData().put(BowSkills.TAG, t);
            CompoundTag runes = arrow.getPersistentData().getCompound("jsn_runes");
            if (runes.getInt("homing") < 1) runes.putInt("homing", 1);
            arrow.getPersistentData().put("jsn_runes", runes);
        }
        // 貫通：矢が敵を貫く
        int extra = 0;
        SkillDefinition pierce = SkillService.passive(p, "archer_pierce");
        if (pierce != null) extra += Math.max(1, pierce.amplifier());
        SkillDefinition pierceUp = SkillService.passive(p, "archer_pierce_up");
        if (pierceUp != null) extra += Math.max(1, pierceUp.amplifier());
        if (extra > 0) BowSkills.setPierce(arrow, arrow.getPierceLevel() + extra);
        // 矢筒：ときどき矢が減らない（ふつうに撃った矢だけ）
        SkillDefinition quiver = SkillService.passive(p, "archer_quiver");
        if (quiver != null && arrow.pickup == AbstractArrow.Pickup.ALLOWED && !p.isCreative() && p.getRandom().nextDouble() < quiver.chance()) {
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            p.getInventory().add(arrow.getPickupItemStackOrigin().copyWithCount(1));
        }
    }

    /** 重力なしの矢は、しばらくしたら落ちる（空の彼方へ飛び続けないように）。狙撃の矢は光の筋を残す */
    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel level)) return;
        if (!arrow.getPersistentData().contains(BowSkills.TAG)) return;
        CompoundTag t = arrow.getPersistentData().getCompound(BowSkills.TAG);
        int ng = t.getInt("nograv");
        if (ng > 0 && arrow.tickCount >= ng && arrow.isNoGravity()) arrow.setNoGravity(false);
        if (t.getBoolean("trail") && arrow.tickCount < 60 && !arrow.onGround()) {
            level.sendParticles(ParticleTypes.END_ROD, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof ServerPlayer p)) return;
        LivingEntity victim = event.getEntity();
        float amount = event.getAmount();
        // 遠矢：8ブロックより遠いほど強い
        SkillDefinition far = SkillService.passive(p, "archer_far_shot_up");
        if (far == null) far = SkillService.passive(p, "archer_far_shot");
        if (far != null) {
            double dist = p.distanceTo(victim);
            double bonus = Math.min(far.chance() > 0 ? far.chance() : 0.4, Math.max(0, dist - 8) * far.power());
            amount *= (float) (1 + bonus);
        }
        // 鷹の目：ときどき会心
        SkillDefinition hawk = SkillService.passive(p, "archer_hawk_eye");
        if (hawk != null && p.getRandom().nextDouble() < hawk.chance()) {
            amount *= (float) (1 + hawk.power());
            p.serverLevel().sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1, victim.getZ(), 20, 0.4, 0.5, 0.4, 0.4);
            p.serverLevel().playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        event.setAmount(amount);
        // 重い矢：吹き飛ばす
        CompoundTag t = arrow.getPersistentData().getCompound(BowSkills.TAG);
        float knock = t.getFloat("knock");
        if (knock > 0) {
            Vec3 v = arrow.getDeltaMovement().multiply(1, 0, 1);
            if (v.lengthSqr() > 1e-4) {
                v = v.normalize().scale(knock);
                victim.push(v.x, 0.3, v.z);
                victim.hurtMarked = true;
            }
        }
    }

    /** 爆裂矢：当たった場所で爆発（地形は壊れない） */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof ServerPlayer p)) return;
        CompoundTag t = arrow.getPersistentData().getCompound(BowSkills.TAG);
        float r = t.getFloat("blast");
        if (r <= 0) return;
        t.putFloat("blast", 0);   // 1回だけ
        Vec3 at = event.getRayTraceResult().getLocation();
        float dmg = t.getFloat("blast_dmg") / (float) (1 + p.getAttributeValue(com.chigusa.jsncraft.stat.ModAttributes.SKILL_DAMAGE));
        for (LivingEntity e : AxeSkills.around(p, at, r)) {
            SkillEffects.hit(p, e, dmg * 0.8F);
            Vec3 push = e.position().subtract(at).normalize().scale(0.8);
            e.push(push.x, 0.4, push.z);
            e.hurtMarked = true;
        }
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.3, 0.3, 0.3, 0);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 30, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.3F);
    }

    private ArcherEvents() {}
}
