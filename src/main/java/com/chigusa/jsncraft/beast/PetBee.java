package com.chigusa.jsncraft.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;

/** 相棒のミツバチ：毒針で戦い（刺しても死なない）、ときどき主人をハチミツで回復する。保存されない。 */
public class PetBee extends Bee {
    public PetBee(EntityType<? extends Bee> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(g -> true);
        this.targetSelector.removeAllGoals(g -> true);
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
        this.goalSelector.addGoal(2, new PetCommon.FollowGoal(this, 1.3));
        this.targetSelector.addGoal(1, new PetCommon.PickTargetGoal(this));
    }

    /** 刺しても死なない毒針 */
    @Override
    public boolean doHurtTarget(Entity target) {
        float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean ok = target.hurt(damageSources().sting(this), dmg);
        if (ok && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), this);
        if (ok) playSound(net.minecraft.sounds.SoundEvents.BEE_STING, 1.0F, 1.0F);
        return ok;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        PetCommon.tick(this);
        // 10秒ごとに、近くの主人をハチミツで少し回復
        if (level() instanceof ServerLevel level && tickCount % 200 == 0) {
            ServerPlayer o = PetCommon.owner(this);
            if (o != null && o.distanceTo(this) < 8 && o.getHealth() < o.getMaxHealth()) {
                o.heal(1.0F + PetCommon.beastLevel(o) / 25.0F);
                level.sendParticles(ParticleTypes.DRIPPING_HONEY, o.getX(), o.getY() + 1.8, o.getZ(), 6, 0.3, 0.2, 0.3, 0);
                level.sendParticles(ParticleTypes.HEART, o.getX(), o.getY() + 2.0, o.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
        }
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }
}
