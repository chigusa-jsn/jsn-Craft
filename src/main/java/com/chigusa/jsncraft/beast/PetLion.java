package com.chigusa.jsncraft.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import java.util.List;

/** ミニ雷の獅子：力強く殴り、敵に雷を落とす。3回に1回、咆哮で仲間を奮い立たせる。 */
public class PetLion extends GeoPet {

    public PetLion(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected String model() {
        return "thunder_lion";
    }

    @Override
    protected List<String> attackAnims() {
        return List.of("rush", "ground_bolt", "howl", "lightning_call", "ring", "storm");
    }

    @Override
    protected String meleeAnim() {
        return "rush";
    }

    @Override
    protected int specialCooldown() {
        return 60;
    }

    @Override
    protected double specialRange() {
        return 12;
    }

    @Override
    protected String special(ServerPlayer owner, LivingEntity t) {
        ServerLevel level = serverLevel();
        if (specialCount % 3 == 2) {
            // 咆哮：仲間の相棒と主人を奮い立たせる
            for (Mob m : PetCommon.petsOf(owner, "")) {
                if (m.distanceTo(this) > 12) continue;
                m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.getX(), m.getY() + 0.8, m.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
            }
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
            level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, 1.0F, 1.4F);
            return "howl";
        }
        // 雷：敵のところに雷を落とす（見た目だけの雷＋まわりにダメージ）
        LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(t.getX(), t.getY(), t.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, t.getX(), t.getY() + 0.5, t.getZ(), 40, 1.2, 0.5, 1.2, 0.2);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(3), e -> PetCommon.hostileTo(owner, e))) {
            hit(owner, e, 1.6);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), this);
        }
        return "ground_bolt";
    }
}
