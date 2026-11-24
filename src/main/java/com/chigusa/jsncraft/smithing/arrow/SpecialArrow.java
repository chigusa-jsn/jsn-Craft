package com.chigusa.jsncraft.smithing.arrow;

import com.chigusa.jsncraft.smithing.SmithData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** 特別な矢（1種類のエンティティで、種類は KIND で見分ける）。 */
public class SpecialArrow extends AbstractArrow {
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(SpecialArrow.class, EntityDataSerializers.INT);

    private int quality = 1;
    private int bowPlus = 0;
    private boolean effectDone;

    public SpecialArrow(EntityType<? extends SpecialArrow> type, Level level) {
        super(type, level);
    }

    public SpecialArrow(Level level, LivingEntity owner, ItemStack pickup, @Nullable ItemStack weapon, ArrowKind kind) {
        super(ModArrows.SPECIAL_ARROW.get(), owner, level, pickup, weapon);
        setup(pickup, weapon, kind);
    }

    public SpecialArrow(Level level, double x, double y, double z, ItemStack pickup, ArrowKind kind) {
        super(ModArrows.SPECIAL_ARROW.get(), x, y, z, level, pickup, null);
        setup(pickup, null, kind);
    }

    private void setup(ItemStack pickup, @Nullable ItemStack weapon, ArrowKind kind) {
        entityData.set(KIND, kind.ordinal());
        quality = Quality.of(pickup);
        bowPlus = weapon != null ? SmithData.plus(weapon) : 0;
        double dmg = getBaseDamage() * kind.damage * Quality.mul(quality);
        if (kind == ArrowKind.BOLT && weapon != null && weapon.getItem() instanceof BowItem) dmg *= 0.4;
        setBaseDamage(dmg);
        if (kind == ArrowKind.FIRE) igniteForSeconds(100);
        if (kind == ArrowKind.PIERCE) setPierce((byte) (3 + bowPlus / 10));
        if (kind == ArrowKind.STAR) setPierce((byte) 2);
        if (kind == ArrowKind.INFERNO) igniteForSeconds(100);
        if (kind == ArrowKind.LIGHT || kind == ArrowKind.HOMING || kind == ArrowKind.STAR) setGlowingTag(true);
    }

    private void setPierce(byte level) {
        try {
            var m = AbstractArrow.class.getDeclaredMethod("setPierceLevel", byte.class);
            m.setAccessible(true);
            m.invoke(this, level);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public ArrowKind kind() {
        return ArrowKind.byId(entityData.get(KIND));
    }

    /** 強化した弓で撃つと効果が強くなる */
    private double power() {
        return 1.0 + bowPlus * 0.05;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModArrows.arrow(kind()));
    }

    @Override
    public void tick() {
        super.tick();
        ArrowKind k = kind();
        if (level().isClientSide) {
            if (!inGround && tickCount > 1) {
                int c = k.color;
                level().addParticle(new DustParticleOptions(new Vector3f((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F), 0.8F),
                        getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }
        if (k == ArrowKind.HOMING && !inGround && tickCount > 3) home();
    }

    private void home() {
        Vec3 v = getDeltaMovement();
        double speed = v.length();
        if (speed < 0.2) return;
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        Entity owner = getOwner();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10), e -> e.isAlive() && e != owner && e instanceof Enemy)) {
            Vec3 to = e.getEyePosition().subtract(position());
            if (to.normalize().dot(v.normalize()) < 0.3) continue;   // 前にいる敵だけ
            double d = to.lengthSqr();
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        if (best == null) return;
        Vec3 dir = best.getBoundingBox().getCenter().subtract(position()).normalize().scale(speed);
        double turn = 0.12 + bowPlus * 0.004;
        setDeltaMovement(v.scale(1 - turn).add(dir.scale(turn)).normalize().scale(speed));
        hasImpulse = true;
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        if (!(level() instanceof ServerLevel sl)) return;
        double p = power();
        Entity owner = getOwner();
        switch (kind()) {
            case ICE -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (60 * p), 1), owner);
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 140, target.getTicksFrozen() + (int) (140 * p)));
                sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
            }
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, (int) (80 * p), 0), owner);
            case WEAKNESS -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int) (100 * p), 0), owner);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (60 * p), 0), owner);
            }
            case THUNDER -> {
                if (random.nextDouble() < 0.25 + bowPlus * 0.015 || sl.isThundering()) strike(sl, target.position());
            }
            case RETRIEVE -> pull(sl, target.position());
            case INFERNO -> {
                target.igniteForSeconds((float) (10 * p));
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5), e -> e != owner && e instanceof Enemy))
                    e.igniteForSeconds((float) (5 * p));
                sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1, target.getZ(), 30, 1.0, 0.5, 1.0, 0.05);
            }
            case GLACIER -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (100 * p), 2), owner);
                target.setTicksFrozen(target.getTicksRequiredToFreeze() + 200);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5), e -> e != owner && e instanceof Enemy))
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (60 * p), 1), owner);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 50, 1.2, 0.6, 1.2, 0.03);
            }
            case STORM -> strike(sl, target.position());
            default -> {
            }
        }
    }

    private static boolean bigOne(Entity e) {
        return e instanceof LivingEntity le && (le.getMaxHealth() >= 100 || le.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)
                || le.getBbWidth() >= 1.8F);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (kind() == ArrowKind.DRAGON && bigOne(result.getEntity())) {
            double base = getBaseDamage();
            setBaseDamage(base * 2.5);
            super.onHitEntity(result);
            setBaseDamage(base);
            if (level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY(), getZ(), 20, 0.4, 0.4, 0.4, 0.02);
            return;
        }
        super.onHitEntity(result);
        if (kind() == ArrowKind.BLAST && level() instanceof ServerLevel sl && !effectDone) {
            effectDone = true;
            blast(sl, result.getLocation());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel sl) || effectDone) return;
        effectDone = true;
        Direction face = result.getDirection();
        BlockPos at = result.getBlockPos().relative(face);
        switch (kind()) {
            case BLAST -> blast(sl, result.getLocation());
            case ROPE -> rope(sl, result.getBlockPos(), face);
            case LIGHT -> {
                if (sl.getBlockState(at).isAir()) {
                    sl.setBlock(at, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14), 3);
                    ArrowEvents.lightPlaced(sl, at, (int) (1200 * power()));
                }
            }
            case RETRIEVE -> pull(sl, result.getLocation());
            case STORM -> strike(sl, result.getLocation());
            default -> {
            }
        }
    }

    private void blast(ServerLevel sl, Vec3 at) {
        float r = (float) (1.6 + bowPlus * 0.03) * (float) Quality.mul(quality);
        sl.explode(this, at.x, at.y, at.z, r, Level.ExplosionInteraction.NONE);
        discard();
    }

    private void strike(ServerLevel sl, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt == null) return;
        bolt.moveTo(at.x, at.y, at.z);
        if (getOwner() instanceof ServerPlayer sp) bolt.setCause(sp);
        sl.addFreshEntity(bolt);
    }

    private void pull(ServerLevel sl, Vec3 at) {
        if (!(getOwner() instanceof ServerPlayer p) || p.level() != sl) return;
        double r = 5 + bowPlus * 0.1;
        var box = new net.minecraft.world.phys.AABB(at, at).inflate(r);
        int n = 0;
        for (ItemEntity it : sl.getEntitiesOfClass(ItemEntity.class, box)) {
            it.teleportTo(p.getX(), p.getY() + 0.5, p.getZ());
            it.setNoPickUpDelay();
            n++;
        }
        for (ExperienceOrb orb : sl.getEntitiesOfClass(ExperienceOrb.class, box)) orb.teleportTo(p.getX(), p.getY() + 0.5, p.getZ());
        if (n > 0) sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.6F);
        sl.sendParticles(ParticleTypes.PORTAL, at.x, at.y, at.z, 30, 0.6, 0.6, 0.6, 0.3);
    }

    /** 刺さった壁から、下へはしごを伸ばす */
    private void rope(ServerLevel sl, BlockPos wall, Direction face) {
        if (face.getAxis().isVertical()) return;
        int max = 12 + bowPlus / 2;
        BlockPos pos = wall.relative(face);
        int placed = 0;
        for (int i = 0; i < max; i++) {
            BlockState here = sl.getBlockState(pos);
            if (!here.isAir() && !here.is(Blocks.WATER)) break;
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, face);
            if (!ladder.canSurvive(sl, pos)) break;
            sl.setBlock(pos, ladder.setValue(LadderBlock.WATERLOGGED, here.is(Blocks.WATER)), 3);
            placed++;
            pos = pos.below();
        }
        if (placed > 0) {
            sl.playSound(null, wall, SoundEvents.LADDER_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("jsn_kind", entityData.get(KIND));
        tag.putInt("jsn_q", quality);
        tag.putInt("jsn_bow_plus", bowPlus);
        tag.putBoolean("jsn_done", effectDone);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getInt("jsn_kind"));
        quality = Math.max(1, tag.getInt("jsn_q"));
        bowPlus = tag.getInt("jsn_bow_plus");
        effectDone = tag.getBoolean("jsn_done");
    }
}
