package com.chigusa.jsncraft.beast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** 相棒のミニゴーレム：体力が多く、近くの敵を自分に引きつけて殴り飛ばす。保存されない。 */
public class PetGolem extends IronGolem {
    public PetGolem(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        setPlayerCreated(true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(g -> true);
        this.targetSelector.removeAllGoals(g -> true);
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new PetCommon.FollowGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new PetCommon.PickTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        PetCommon.tick(this);
        // 守り役：2秒ごとに、主人を狙っている近くの敵を自分に向けさせる
        if (!level().isClientSide && tickCount % 40 == 0) {
            ServerPlayer o = PetCommon.owner(this);
            if (o == null) return;
            for (Mob m : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(8), m -> m.getTarget() == o)) m.setTarget(this);
        }
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
