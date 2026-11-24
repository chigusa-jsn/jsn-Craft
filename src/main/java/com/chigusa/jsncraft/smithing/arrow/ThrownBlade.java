package com.chigusa.jsncraft.smithing.arrow;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** 投げナイフ・手裏剣。 */
public class ThrownBlade extends ThrowableItemProjectile {
    public ThrownBlade(EntityType<? extends ThrownBlade> type, Level level) {
        super(type, level);
    }

    public ThrownBlade(Level level, LivingEntity owner) {
        super(ModArrows.THROWN_BLADE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModArrows.THROWING_KNIFE.get();
    }

    private boolean knife() {
        return getItem().is(ModArrows.THROWING_KNIFE.get());
    }

    @Override
    protected double getDefaultGravity() {
        return knife() ? 0.03 : 0.012;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel sl)) return;
        ItemStack st = getItem();
        float dmg = (float) ((knife() ? 6.0 : 3.5) * Quality.mul(Quality.of(st)));
        if (result.getEntity().hurt(damageSources().thrown(this, getOwner()), dmg)) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.6F, knife() ? 1.3F : 1.8F);
        }
        if (knife() && random.nextFloat() < 0.5F) spawnAtLocation(st.copyWithCount(1));
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.05);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 0.5F, 1.6F);
        if (knife() || random.nextFloat() < 0.5F) spawnAtLocation(getItem().copyWithCount(1));
        discard();
    }
}
