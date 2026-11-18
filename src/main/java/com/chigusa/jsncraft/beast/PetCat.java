package com.chigusa.jsncraft.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 風の猫：すばやく飛びかかって引っかく。かまいたち（前へ飛ぶ風の刃）と、3回に1回つむじ風。 */
public class PetCat extends GeoPet {

    public PetCat(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected String model() {
        return "wind_cat";
    }

    @Override
    protected List<String> attackAnims() {
        return List.of("kamaitachi", "tornado", "gust", "charge", "air_jump");
    }

    @Override
    protected String meleeAnim() {
        return "charge";
    }

    @Override
    protected int specialCooldown() {
        return 50;
    }

    @Override
    protected double specialRange() {
        return 9;
    }

    @Override
    protected String special(ServerPlayer owner, LivingEntity t) {
        ServerLevel level = serverLevel();
        if (specialCount % 3 == 2) {
            // つむじ風：まわりの敵を巻き上げる
            for (int i = 0; i < 40; i++) {
                double a = i * 0.5, r = 0.5 + i * 0.07;
                level.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(a) * r, getY() + i * 0.06, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.5), e -> PetCommon.hostileTo(owner, e))) {
                hit(owner, e, 1.0);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                e.hurtMarked = true;
            }
            level.playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.NEUTRAL, 1.0F, 1.2F);
            return "tornado";
        }
        // かまいたち：敵に向かってまっすぐ風の刃
        Vec3 from = position().add(0, 0.6, 0);
        Vec3 dir = t.getBoundingBox().getCenter().subtract(from).normalize();
        for (int i = 1; i <= 16; i++) {
            Vec3 p = from.add(dir.scale(i * 0.5));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            if (i % 2 == 0) level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0);
        }
        Vec3 end = from.add(dir.scale(8));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1.0), e -> PetCommon.hostileTo(owner, e))) hit(owner, e, 1.3);
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.0F, 1.4F);
        return "kamaitachi";
    }
}
