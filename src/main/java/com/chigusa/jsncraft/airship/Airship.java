package com.chigusa.jsncraft.airship;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 飛空艇。小型（2人・荷物9）→ 中型（4人・荷物18）→ 大型（8人・荷物27）。
 * 燃料（かまどの燃料と同じ物）を入れて飛ぶ。燃料が切れるとゆっくり降りる。誰も操縦していなければ、その場に浮いて止まる。
 * 右クリックで乗る（最初に乗った人が操縦）、しゃがんで右クリックで荷物と燃料の画面（ここから仕舞える）。
 * 染料で気球の色、木材で船体の色を変えられる。
 */
public class Airship extends Entity {
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(Airship.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(Airship.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BALLOON = SynchedEntityData.defineId(Airship.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HULL = SynchedEntityData.defineId(Airship.class, EntityDataSerializers.INT);

    public static final int MAX_FUEL = 20 * 60 * 20;   // 20分ぶん
    public static final int DEFAULT_HULL = 0xB8945F;    // オーク

    /** 席（ブロック単位、y は上、z は前） */
    private static final Vec3[][] SEATS = {
            {new Vec3(0, 0.15, 0.3), new Vec3(0, 0.15, -0.4)},
            {new Vec3(0.3, 0.2, 0.7), new Vec3(-0.3, 0.2, 0.7), new Vec3(0.3, 0.2, -0.2), new Vec3(-0.3, 0.2, -0.2)},
            {new Vec3(0.4, 0.25, 1.1), new Vec3(-0.4, 0.25, 1.1), new Vec3(0.4, 0.25, 0.4), new Vec3(-0.4, 0.25, 0.4),
                    new Vec3(0.4, 0.25, -0.3), new Vec3(-0.4, 0.25, -0.3), new Vec3(0.4, 0.25, -1.0), new Vec3(-0.4, 0.25, -1.0)}};
    private static final float[][] SIZE = {{1.5F, 0.6F}, {2.2F, 0.8F}, {2.9F, 1.0F}};
    private static final double[] SPEED = {0.42, 0.52, 0.62};

    /** 燃料の枠（0）と荷物（1〜27） */
    private final SimpleContainer items = new SimpleContainer(28);
    @Nullable private UUID owner;
    private float yawDelta;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot;
    /** プロペラの回り具合（見た目だけ） */
    public float propAngle, propAnglePrev;

    public Airship(EntityType<? extends Airship> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    // ================= データ =================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(TIER, 1);
        b.define(FUEL, 0);
        b.define(BALLOON, 0xFFF0F0F0);
        b.define(HULL, DEFAULT_HULL);
    }

    public int tier() { return Mth.clamp(entityData.get(TIER), 1, 3); }
    public int fuel() { return entityData.get(FUEL); }
    public int balloonColor() { return entityData.get(BALLOON); }
    public int hullColor() { return entityData.get(HULL); }
    public int seats() { return SEATS[tier() - 1].length; }
    public int cargoSlots() { return 9 * tier(); }
    public SimpleContainer items() { return items; }
    @Nullable public UUID owner() { return owner; }

    public boolean isOwner(Player p) {
        return owner == null || owner.equals(p.getUUID()) || p.hasPermissions(2);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (TIER.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float[] s = SIZE[tier() - 1];
        return EntityDimensions.scalable(s[0], s[1]);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(3 + tier() * 2, 4 + tier() * 2, 3 + tier() * 2);
    }

    // ================= アイテムとの行き来 =================

    /** アイテム（仕舞った飛空艇）から中身を読む */
    public void loadFrom(ItemStack stack, @Nullable Player placer) {
        entityData.set(TIER, stack.getItem() instanceof AirshipItem ai ? ai.tier() : 1);
        CompoundTag t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("jsn_airship");
        if (t.contains("balloon")) entityData.set(BALLOON, t.getInt("balloon"));
        if (t.contains("hull")) entityData.set(HULL, t.getInt("hull"));
        entityData.set(FUEL, t.getInt("fuel"));
        items.clearContent();
        ContainerHelper.loadAllItems(t, items.getItems(), registryAccess());
        if (t.hasUUID("owner")) owner = t.getUUID("owner");
        else if (placer != null) owner = placer.getUUID();
        if (stack.has(DataComponents.CUSTOM_NAME)) setCustomName(stack.getHoverName());
        refreshDimensions();
    }

    /** 仕舞う：飛空艇のアイテムにする */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(ModAirships.item(tier()));
        CompoundTag t = new CompoundTag();
        t.putInt("balloon", balloonColor());
        t.putInt("hull", hullColor());
        t.putInt("fuel", fuel());
        if (owner != null) t.putUUID("owner", owner);
        ContainerHelper.saveAllItems(t, items.getItems(), registryAccess());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> root.put("jsn_airship", t));
        if (hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, getCustomName());
        return stack;
    }

    public boolean packUp(ServerPlayer p) {
        if (!isOwner(p)) {
            p.displayClientMessage(Component.literal("仕舞えるのは持ち主だけです").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ejectPassengers();
        ItemStack stack = toItem();
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.NEUTRAL, 1.0F, 0.8F);
        ((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 30, 1.0, 0.8, 1.0, 0.05);
        discard();
        return true;
    }

    // ================= 乗る・さわる =================

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide && player instanceof ServerPlayer sp) {
                sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new AirshipMenu(id, inv, this), getDisplayName()), buf -> {
                    buf.writeVarInt(getId());
                    buf.writeVarInt(tier());
                });
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.getItem() instanceof DyeItem dye) {
            if (!level().isClientSide) {
                if (!isOwner(player)) return InteractionResult.FAIL;
                entityData.set(BALLOON, 0xFF000000 | dye.getDyeColor().getTextureDiffuseColor());
                if (!player.getAbilities().instabuild) held.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.is(ItemTags.PLANKS)) {
            if (!level().isClientSide) {
                if (!isOwner(player)) return InteractionResult.FAIL;
                entityData.set(HULL, woodColor(held));
                if (!player.getAbilities().instabuild) held.shrink(1);
                level().playSound(null, blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (getPassengers().size() >= seats()) return InteractionResult.PASS;
        if (!level().isClientSide) {
            if (player.startRiding(this) && getPassengers().size() == 1)
                player.displayClientMessage(Component.literal("操縦：W/S 前後・A/D 旋回・スペース 上昇・ダッシュキー 下降・しゃがみで降りる").withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private static int woodColor(ItemStack planks) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(planks.getItem()).getPath();
        if (id.startsWith("spruce")) return 0x7A5A34;
        if (id.startsWith("birch")) return 0xD7C185;
        if (id.startsWith("jungle")) return 0xB88764;
        if (id.startsWith("acacia")) return 0xBA6337;
        if (id.startsWith("dark_oak")) return 0x4F3218;
        if (id.startsWith("mangrove")) return 0x7F4234;
        if (id.startsWith("cherry")) return 0xE2B3AC;
        if (id.startsWith("bamboo")) return 0xD4C060;
        if (id.startsWith("crimson")) return 0x7E3A56;
        if (id.startsWith("warped")) return 0x398382;
        return DEFAULT_HULL;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < seats();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) return;
        int i = Math.max(0, getPassengers().indexOf(passenger));
        Vec3[] seats = SEATS[tier() - 1];
        Vec3 s = seats[Math.min(i, seats.length - 1)].yRot(-getYRot() * Mth.DEG_TO_RAD);
        Vec3 attach = passenger.getVehicleAttachmentPoint(this);
        callback.accept(passenger, getX() + s.x, getY() + s.y - attach.y + 0.35, getZ() + s.z);
        passenger.setYRot(passenger.getYRot() + yawDelta);
        passenger.setYHeadRot(passenger.getYHeadRot() + yawDelta);
    }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    public boolean isPickable() { return !isRemoved(); }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }   // 壊れない（仕舞うのは画面から）

    @Override
    public boolean causeFallDamage(float dist, float mul, DamageSource source) { return false; }

    // ================= 動き =================

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x; lerpY = y; lerpZ = z; lerpYRot = yRot;
        lerpSteps = 10;
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, getXRot());
            lerpSteps--;
        }
    }

    @Override
    public void tick() {
        super.tick();
        float before = getYRot();
        tickLerp();
        if (isControlledByLocalInstance()) {
            control();
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        yawDelta = Mth.wrapDegrees(getYRot() - before);
        propAnglePrev = propAngle;
        boolean powered = getControllingPassenger() != null && fuel() > 0;
        propAngle += powered ? 55F : 6F;
        if (!level().isClientSide) serverTick();
        else if (powered && random.nextInt(4) == 0) {
            Vec3 back = new Vec3(0, 0.5, -1.2 * tier()).yRot(-getYRot() * Mth.DEG_TO_RAD);
            level().addParticle(ParticleTypes.SMOKE, getX() + back.x, getY() + back.y, getZ() + back.z, 0, 0.02, 0);
        }
    }

    /** 操縦（操縦する人のパソコン、または誰も乗っていないときはサーバー） */
    private void control() {
        Vec3 v = getDeltaMovement();
        boolean hasFuel = fuel() > 0;
        if (!(getControllingPassenger() instanceof Player pilot)) {
            // 誰も操縦していない：止まって浮く（燃料がなければゆっくり降りる）
            double vy = !hasFuel && !onGround() ? -0.06 : 0;
            setDeltaMovement(v.x * 0.8, onGround() ? 0 : Mth.lerp(0.2, v.y, vy), v.z * 0.8);
            return;
        }
        int t = tier() - 1;
        float turn = pilot.xxa, fwd = pilot.zza;
        setYRot(getYRot() - turn * (3.2F - t * 0.5F));
        double max = SPEED[t] * (hasFuel ? 1 : 0.25);
        double speed = fwd > 0 ? fwd * max : fwd * max * 0.4;
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Vec3 dir = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).scale(speed);
        double vy;
        if (hasFuel) {
            boolean up = level().isClientSide && AirshipInputBridge.up();
            boolean down = level().isClientSide && AirshipInputBridge.down();
            vy = up ? 0.2 : down ? -0.2 : 0;
        } else vy = onGround() ? 0 : -0.07;
        setDeltaMovement(Mth.lerp(0.08, v.x, dir.x), Mth.lerp(0.25, v.y, vy), Mth.lerp(0.08, v.z, dir.z));
    }

    private int hudTick;
    private Vec3 lastPos = Vec3.ZERO;

    private void serverTick() {
        // 燃料の枠から燃料を補給
        ItemStack f = items.getItem(0);
        int fuel = fuel();
        if (!f.isEmpty()) {
            int burn = f.getBurnTime(RecipeType.SMELTING);
            if (burn > 0 && fuel + burn <= MAX_FUEL) {
                ItemStack rem = f.getCraftingRemainingItem();
                f.shrink(1);
                if (f.isEmpty() && !rem.isEmpty()) items.setItem(0, rem);
                fuel += burn;
                entityData.set(FUEL, fuel);
            }
        }
        // 操縦して動いていれば燃料を使う
        boolean moving = position().distanceToSqr(lastPos) > 0.0004;
        lastPos = position();
        if (getControllingPassenger() instanceof ServerPlayer pilot) {
            if (moving && fuel > 0) entityData.set(FUEL, --fuel);
            if (++hudTick % 10 == 0) {
                int bars = (int) Math.ceil(10.0 * fuel / MAX_FUEL);
                int sec = fuel / 20;
                Component c = fuel > 0
                        ? Component.literal("⛽ 燃料 ").withStyle(ChatFormatting.GOLD).append(Component.literal("■".repeat(bars)).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("■".repeat(10 - bars)).withStyle(ChatFormatting.DARK_GRAY)).append(Component.literal(String.format(" %d:%02d", sec / 60, sec % 60)).withStyle(ChatFormatting.WHITE))
                        : Component.literal("⛽ 燃料切れ…ゆっくり降りていく（しゃがんで右クリックで燃料を入れよう）").withStyle(ChatFormatting.RED);
                pilot.displayClientMessage(c, true);
            }
            if (moving && tickCount % 16 == 0 && fuel > 0)
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.15F, 0.6F + tier() * 0.1F);
        }
    }

    // ================= 保存 =================

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("tier", tier());
        tag.putInt("fuel", fuel());
        tag.putInt("balloon", balloonColor());
        tag.putInt("hull", hullColor());
        if (owner != null) tag.putUUID("owner", owner);
        ContainerHelper.saveAllItems(tag, items.getItems(), registryAccess());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(TIER, Math.max(1, tag.getInt("tier")));
        entityData.set(FUEL, tag.getInt("fuel"));
        if (tag.contains("balloon")) entityData.set(BALLOON, tag.getInt("balloon"));
        if (tag.contains("hull")) entityData.set(HULL, tag.getInt("hull"));
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.getItems(), registryAccess());
        refreshDimensions();
    }

    public static List<Vec3> seatsOf(int tier) {
        return List.of(SEATS[Math.max(1, Math.min(3, tier)) - 1]);
    }
}
