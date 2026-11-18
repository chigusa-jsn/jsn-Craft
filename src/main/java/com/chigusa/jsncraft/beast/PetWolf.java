package com.chigusa.jsncraft.beast;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

/** 相棒のオオカミ：主人に飼われたオオカミの動き＋主人の指示した敵を狙う。保存されない。 */
public class PetWolf extends Wolf {
    public PetWolf(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(1, new PetCommon.PickTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        PetCommon.tick(this);
    }

    @Override
    public boolean canMate(Animal other) {
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
