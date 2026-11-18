package com.chigusa.jsncraft.beast;

import com.chigusa.jsncraft.magic.MageSpells;
import com.chigusa.jsncraft.skill.SkillEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * ミニ影刃竜：空を飛ぶ相棒。主人は右クリックで背中に乗れる（スニークで降りる）。
 * 乗っているとき：向いている方向へ飛ぶ（前・後ろ・横）。上を向けば上へ、下を向けば下へ。
 * 乗っていても、主人がねらった敵（または主人が攻撃した敵）へ影の剣を撃ち出して戦う。
 */
public class PetDragon extends GeoPet {
    static final DustParticleOptions SHADOW = new DustParticleOptions(new Vector3f(0.45F, 0.2F, 0.7F), 1.4F);

    public PetDragon(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected String model() {
        return "blade_dragon";
    }

    @Override
    protected List<String> attackAnims() {
        return List.of("sword_volley", "tail_spin", "wing_slash", "pounce", "roar");
    }

    @Override
    protected double keepRange() {
        return 9;
    }

    @Override
    protected int specialCooldown() {
        return 30;
    }

    @Override
    protected double specialRange() {
        return isVehicle() ? 24 : 16;
    }

    // ================= 乗る =================

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || isVehicle()) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player.getUUID().equals(PetCommon.ownerId(this))) {
            player.startRiding(this);
            getNavigation().stop();
            setTarget(null);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        setYRot(player.getYRot());
        yRotO = getYRot();
        setXRot(player.getXRot() * 0.5F);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    public void travel(Vec3 input) {
        if (isVehicle() && getControllingPassenger() instanceof Player p) {
            double speed = getAttributeValue(Attributes.FLYING_SPEED);
            Vec3 look = p.getLookAngle();
            Vec3 left = new Vec3(look.z, 0, -look.x);
            if (left.lengthSqr() > 1e-4) left = left.normalize();
            float fwd = p.zza, side = p.xxa;
            Vec3 want = look.scale(fwd * speed * (fwd < 0 ? 0.4 : 1.0)).add(left.scale(side * speed * 0.6));
            setDeltaMovement(getDeltaMovement().scale(0.75).add(want.scale(0.3)));
            move(MoverType.SELF, getDeltaMovement());
            calculateEntityAnimation(true);
            return;
        }
        super.travel(input);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide && passenger instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
    }

    @Override
    public void tick() {
        super.tick();
        if (!isNoGravity()) setNoGravity(true);
    }

    /** 乗っているときは、主人がねらった敵か、主人が攻撃した敵を撃つ */
    @Override
    protected LivingEntity specialTarget(ServerPlayer owner) {
        if (owner != null && getFirstPassenger() == owner) {
            LivingEntity look = MageSpells.lookTarget(owner, 24);
            if (look != null && PetCommon.hostileTo(owner, look)) return look;
            LivingEntity hurt = owner.getLastHurtMob();
            if (hurt != null && hurt.isAlive() && PetCommon.hostileTo(owner, hurt) && owner.tickCount - owner.getLastHurtMobTimestamp() < 100) return hurt;
            return null;
        }
        return getTarget();
    }

    @Override
    protected String special(ServerPlayer owner, LivingEntity t) {
        ServerLevel level = serverLevel();
        if (specialCount % 4 == 3 && t.distanceTo(this) < 5) {
            // しっぽ回し：まわりをなぎ払う
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                level.sendParticles(SHADOW, getX() + Math.cos(a) * 2.5, getY() + 0.6, getZ() + Math.sin(a) * 2.5, 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.6, getZ(), 4, 1.2, 0.2, 1.2, 0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.5), e -> PetCommon.hostileTo(owner, e))) hit(owner, e, 1.3);
            level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.2F, 0.7F);
            return "tail_spin";
        }
        // 影の剣：3本を続けて撃つ
        for (int k = 0; k < 3; k++) {
            int kk = k;
            SkillEffects.later(owner, k * 3, () -> {
                if (!isAlive() || !t.isAlive()) return;
                Vec3 from = position().add(0, getBbHeight() * 0.9, 0).add(new Vec3(kk - 1, 0, 0).yRot(-getYRot() * 0.017453292F));
                Vec3 to = t.getBoundingBox().getCenter();
                int n = (int) Math.max(4, from.distanceTo(to) * 3);
                for (int i = 0; i <= n; i++) {
                    Vec3 p = from.lerp(to, i / (double) n);
                    level.sendParticles(SHADOW, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    if (i % 4 == 0) level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(1.2), e -> PetCommon.hostileTo(owner, e)))
                    hit(owner, e, e == t ? 0.9 : 0.45);
                level.playSound(null, t.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.NEUTRAL, 0.8F, 1.3F);
            });
        }
        return "sword_volley";
    }
}
