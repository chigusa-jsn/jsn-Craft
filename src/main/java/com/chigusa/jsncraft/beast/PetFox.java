package com.chigusa.jsncraft.beast;

import com.chigusa.jsncraft.skill.SkillEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/** ミニ光の白狐：離れたところから狐火（光の弾）を撃つ。3回に1回、光の柱を立てて主人を癒す。 */
public class PetFox extends GeoPet {
    static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.86F, 0.45F), 1.3F);

    public PetFox(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected String model() {
        return "white_fox";
    }

    @Override
    protected List<String> attackAnims() {
        return List.of("foxfire", "pillar", "flash", "laser");
    }

    @Override
    protected double keepRange() {
        return 8;
    }

    @Override
    protected int specialCooldown() {
        return 40;
    }

    @Override
    protected double specialRange() {
        return 14;
    }

    @Override
    protected String special(ServerPlayer owner, LivingEntity t) {
        ServerLevel level = serverLevel();
        if (specialCount % 3 == 2) {
            // 光の柱：敵のまわりにダメージ、主人を回復
            Vec3 c = t.position();
            for (int i = 0; i < 20; i++) level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + i * 0.3, c.z, 2, 0.3, 0.05, 0.3, 0.01);
            level.sendParticles(GOLD, c.x, c.y + 0.2, c.z, 30, 1.2, 0.1, 1.2, 0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(2.5), e -> PetCommon.hostileTo(owner, e))) hit(owner, e, 1.6);
            if (owner.distanceTo(this) < 16) {
                owner.heal(2.0F);
                level.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 2, owner.getZ(), 2, 0.3, 0.1, 0.3, 0);
            }
            level.playSound(null, t.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8F, 1.6F);
            return "pillar";
        }
        // 狐火：光の弾を3つ
        for (int k = 0; k < 3; k++) {
            int kk = k;
            SkillEffects.later(owner, k * 4, () -> {
                if (!isAlive() || !t.isAlive()) return;
                Vec3 from = position().add(0, getBbHeight() * 0.8, 0), to = t.getBoundingBox().getCenter();
                int n = (int) Math.max(4, from.distanceTo(to) * 3);
                double side = (kk - 1) * 0.8;
                for (int i = 0; i <= n; i++) {
                    double q = i / (double) n;
                    Vec3 p = from.lerp(to, q).add(0, Math.sin(q * Math.PI) * side, 0);
                    level.sendParticles(GOLD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    if (i % 3 == 0) level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
                hit(owner, t, 0.8);
                level.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.5F);
            });
        }
        return "foxfire";
    }
}
