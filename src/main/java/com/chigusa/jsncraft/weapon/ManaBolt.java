package com.chigusa.jsncraft.weapon;

import com.chigusa.jsncraft.skill.SkillEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** マナ弾：杖の先から飛ぶ光の弾（エンティティを使わず、サーバーで毎tick進める）。 */
public final class ManaBolt {

    private static final double SPEED = 1.6;
    private static final int LIFE = 22;   // 約35ブロック

    public static void fire(ServerPlayer player, StaffItem staff) {
        StaffItem.Tier t = staff.tier();
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition().add(look.scale(0.6)).add(0, -0.15, 0);
        int c = t.color();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), 1.3F);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.6F + t.tier() * 0.05F);
        step(player, start, look.scale(SPEED), t.bolt(), dust, LIFE);
    }

    private static void step(ServerPlayer player, Vec3 pos, Vec3 vel, float damage, DustParticleOptions dust, int life) {
        if (!player.isAlive() || player.isRemoved()) return;
        ServerLevel level = player.serverLevel();
        Vec3 next = pos.add(vel);
        BlockHitResult bh = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = bh.getType() == HitResult.Type.MISS ? next : bh.getLocation();
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(level, player, pos, end, new AABB(pos, end).inflate(0.6),
                e -> e instanceof LivingEntity le && SkillEffects.isEnemy(player, le), 0.3F);

        // 軌跡
        for (int i = 0; i < 4; i++) {
            Vec3 p = pos.add(end.subtract(pos).scale(i / 4.0));
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }
        level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 1, 0.03, 0.03, 0.03, 0.005);

        if (eh != null && eh.getEntity() instanceof LivingEntity target) {
            SkillEffects.hit(player, target, damage);
            Vec3 push = vel.normalize().scale(0.25);
            target.push(push.x, 0.08, push.z);
            target.hurtMarked = true;
            burst(level, eh.getLocation(), dust);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.7F, 1.4F);
            return;
        }
        if (bh.getType() != HitResult.Type.MISS) {
            burst(level, bh.getLocation(), dust);
            return;
        }
        if (life <= 0) {
            burst(level, next, dust);
            return;
        }
        SkillEffects.later(player, 1, () -> step(player, next, vel, damage, dust, life - 1));
    }

    private static void burst(ServerLevel level, Vec3 p, DustParticleOptions dust) {
        level.sendParticles(dust, p.x, p.y, p.z, 10, 0.2, 0.2, 0.2, 0);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, p.x, p.y, p.z, 6, 0.15, 0.15, 0.15, 0.1);
    }

    private ManaBolt() {}
}
