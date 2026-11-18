package com.chigusa.jsncraft.beast;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.level.Level;

/** 相棒の鷹：空を飛んでついてきて、敵に急降下して攻撃する。保存されない。 */
public class PetHawk extends Parrot {
    public PetHawk(EntityType<? extends Parrot> type, Level level) {
        super(type, level);
        setVariant(Variant.GRAY);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(g -> true);
        this.targetSelector.removeAllGoals(g -> true);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.8, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.2, 5.0F, 1.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.targetSelector.addGoal(1, new PetCommon.PickTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        PetCommon.tick(this);
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
