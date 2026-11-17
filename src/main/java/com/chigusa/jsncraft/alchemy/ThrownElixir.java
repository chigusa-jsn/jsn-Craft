package com.chigusa.jsncraft.alchemy;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 投げた薬。当たったところの周り3ブロックのプレイヤーに効く。 */
public class ThrownElixir extends ThrowableItemProjectile {
    public ThrownElixir(EntityType<? extends ThrownElixir> type, Level level) {
        super(type, level);
    }

    public ThrownElixir(Level level, LivingEntity owner) {
        super(ModAlchemy.THROWN_ELIXIR.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SPLASH_POTION;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel level)) return;
        ItemStack stack = getItem();
        if (stack.getItem() instanceof ElixirItem item) {
            ElixirData d = ElixirData.of(stack);
            Vec3 at = result.getLocation();
            for (ServerPlayer p : ElixirEffects.around(level, at, 3.0)) ElixirEffects.apply(p, item.type(), d, 0.8);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack), at.x, at.y + 0.2, at.z, 20, 0.3, 0.3, 0.3, 0.1);
            level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.2, at.z, 30, 1.0, 0.3, 1.0, 0.1);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        discard();
    }
}
