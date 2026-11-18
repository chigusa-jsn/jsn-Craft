package com.chigusa.jsncraft.beast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

/**
 * 百獣の王の相棒（ミニ白狐・風の猫・雷の獅子・影刃竜）の共通部分。
 * 見た目と動きは jsn-boss のボスと同じモデル（GeckoLib）を小さくしたもの。
 * ふつうの攻撃に加えて、決まった間隔で「技」を出す（special）。
 */
public abstract class GeoPet extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected int specialCd = 40;
    protected int specialCount;

    protected GeoPet(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** モデルの名前（animation.<これ>.idle など） */
    protected abstract String model();

    /** 技に使うアニメーションの名前 */
    protected abstract List<String> attackAnims();

    /** 離れて戦う距離（0なら近づいて殴る） */
    protected double keepRange() {
        return 0;
    }

    protected abstract int specialCooldown();

    protected abstract double specialRange();

    /** 技を出す。出したアニメーションの名前（出さなかったら null） */
    protected abstract String special(ServerPlayer owner, LivingEntity target);

    /** 殴ったときのアニメーション */
    protected String meleeAnim() {
        return null;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (keepRange() > 0) this.goalSelector.addGoal(1, new KeepRangeGoal(this, 1.2, keepRange()));
        else this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            public boolean canUse() {
                return !mob.isVehicle() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new PetCommon.FollowGoal(this, 1.25) {
            @Override
            public boolean canUse() {
                return !GeoPet.this.isVehicle() && super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new PetCommon.PickTargetGoal(this));
    }

    public boolean frenzy() {
        return getPersistentData().getLong(BeastSkills.FRENZY_UNTIL) > level().getGameTime();
    }

    /** 次の技をすぐ出せるように（百獣の号令） */
    public void readySpecial() {
        specialCd = 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        PetCommon.tick(this);
        if (level().isClientSide || !isAlive()) return;
        if (specialCd > 0) specialCd--;
        if (specialCd > 0) return;
        ServerPlayer owner = PetCommon.owner(this);
        LivingEntity t = specialTarget(owner);
        if (owner == null || t == null || !t.isAlive() || t.distanceTo(this) > specialRange() || !hasLineOfSight(t)) return;
        String anim = special(owner, t);
        if (anim != null) {
            triggerAnim("main", anim);
            specialCount++;
        }
        specialCd = (int) (specialCooldown() * (frenzy() ? 0.5 : 1.0));
    }

    protected LivingEntity specialTarget(ServerPlayer owner) {
        return getTarget();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && meleeAnim() != null) triggerAnim("main", meleeAnim());
        return ok;
    }

    /** 相棒の攻撃（主人の敵だけ・連続で当たる） */
    protected void hit(ServerPlayer owner, LivingEntity e, double mult) {
        if (!PetCommon.hostileTo(owner, e)) return;
        e.invulnerableTime = 0;
        e.hurt(damageSources().mobAttack(this), (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * mult));
    }

    protected ServerLevel serverLevel() {
        return (ServerLevel) level();
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
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

    // ================= GeckoLib =================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        String a = "animation." + model() + ".";
        RawAnimation idle = RawAnimation.begin().thenLoop(a + "idle"), walk = RawAnimation.begin().thenLoop(a + "walk"),
                run = RawAnimation.begin().thenLoop(a + "run"), death = RawAnimation.begin().thenPlayAndHold(a + "death");
        AnimationController<GeoPet> main = new AnimationController<>(this, "main", 3, state -> {
            if (this.isDeadOrDying()) return state.setAndContinue(death);
            if (!state.isMoving()) return state.setAndContinue(idle);
            return state.setAndContinue(this.getDeltaMovement().horizontalDistanceSqr() > 0.02 ? run : walk);
        });
        for (String n : attackAnims()) main.triggerableAnim(n, RawAnimation.begin().thenPlay(a + n));
        controllers.add(main);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** 離れたところから戦う（近すぎず遠すぎず） */
    public static class KeepRangeGoal extends Goal {
        private final Mob mob;
        private final double speed, range;

        public KeepRangeGoal(Mob mob, double speed, double range) {
            this.mob = mob;
            this.speed = speed;
            this.range = range;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return !mob.isVehicle() && t != null && t.isAlive();
        }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30, 30);
            double d = mob.distanceTo(t);
            if (d > range || !mob.hasLineOfSight(t)) {
                if (mob.tickCount % 5 == 0) mob.getNavigation().moveTo(t, speed);
            } else {
                mob.getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }
    }
}
