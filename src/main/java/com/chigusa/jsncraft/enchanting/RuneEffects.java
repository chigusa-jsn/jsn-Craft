package com.chigusa.jsncraft.enchanting;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.mining.MiningService;
import com.chigusa.jsncraft.skill.HitFlags;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** 魔紋の効果。能力値になるものは装備の能力値に足し、それ以外はできごとに合わせて動く。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class RuneEffects {

    /** 魔紋が起こしたダメージの間だけ立てる目印（効果が連鎖しないように） */
    private static final ThreadLocal<Boolean> RUNE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> false);
    private static final String ARROW_KEY = "jsn_runes";
    private static final String SOUL_KEY = "jsn_soulbound";

    // ================= 能力値になる魔紋 =================

    @SubscribeEvent
    public static void onModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        Map<String, Integer> runes = RuneData.runes(stack);
        if (runes.isEmpty()) return;
        EquipmentSlotGroup group = stack.getItem() instanceof ArmorItem a ? EquipmentSlotGroup.bySlot(a.getEquipmentSlot()) : EquipmentSlotGroup.MAINHAND;
        String g = group.getSerializedName();
        for (var e : runes.entrySet()) {
            int l = e.getValue();
            String id = e.getKey();
            switch (id) {
                case "swift" -> add(event, Attributes.MOVEMENT_SPEED, id, g, 0.04 * l, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, group);
                case "leap" -> {
                    add(event, Attributes.JUMP_STRENGTH, id, g, 0.04 * l, AttributeModifier.Operation.ADD_VALUE, group);
                    add(event, Attributes.SAFE_FALL_DISTANCE, id + "_fall", g, l, AttributeModifier.Operation.ADD_VALUE, group);
                }
                case "feather" -> add(event, Attributes.FALL_DAMAGE_MULTIPLIER, id, g, -0.2 * l, AttributeModifier.Operation.ADD_VALUE, group);
                case "tenacity" -> {
                    add(event, Attributes.KNOCKBACK_RESISTANCE, id, g, 0.1 * l, AttributeModifier.Operation.ADD_VALUE, group);
                    add(event, ModAttributes.DAMAGE_REDUCTION, id + "_dr", g, 0.02 * l, AttributeModifier.Operation.ADD_VALUE, group);
                }
                case "regen" -> add(event, ModAttributes.HP_REGEN, id, g, 0.1 * l, AttributeModifier.Operation.ADD_VALUE, group);
                case "mana" -> add(event, ModAttributes.MAX_MANA, id, g, 10 * l, AttributeModifier.Operation.ADD_VALUE, group);
                case "meditation" -> add(event, ModAttributes.MANA_REGEN, id, g, 0.4 * l, AttributeModifier.Operation.ADD_VALUE, group);
                case "arcane" -> add(event, ModAttributes.MAGIC_POWER, id, g, 0.06 * l, AttributeModifier.Operation.ADD_VALUE, group);
                case "resonance" -> add(event, ModAttributes.COOLDOWN_REDUCTION, id, g, 0.03 * l, AttributeModifier.Operation.ADD_VALUE, group);
                case "chant" -> add(event, ModAttributes.CAST_SPEED, id, g, 0.08 * l, AttributeModifier.Operation.ADD_VALUE, group);
                case "mercy" -> add(event, ModAttributes.HEALING_POWER, id, g, 0.08 * l, AttributeModifier.Operation.ADD_VALUE, group);
                case "luck" -> {
                    add(event, Attributes.LUCK, id, g, l, AttributeModifier.Operation.ADD_VALUE, group);
                    add(event, ModAttributes.MINING_FORTUNE, id + "_mine", g, 0.05 * l, AttributeModifier.Operation.ADD_VALUE, group);
                }
                default -> {
                }
            }
        }
    }

    private static void add(ItemAttributeModifierEvent event, Holder<Attribute> attr, String id, String group, double amount,
                            AttributeModifier.Operation op, EquipmentSlotGroup slots) {
        event.addModifier(attr, new AttributeModifier(JsnCraft.id("rune/" + id + "_" + group), amount, op), slots);
    }

    // ================= 近接武器 =================

    private static ServerPlayer meleeAttacker(DamageSource src) {
        if (RUNE.get() || HitFlags.SKILL.get()) return null;
        if (src.getEntity() instanceof ServerPlayer p && src.getDirectEntity() == p) return p;
        return null;
    }

    /** ダメージの量を変える魔紋（処刑・業火） */
    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        ServerPlayer p = meleeAttacker(event.getSource());
        if (p == null) return;
        ItemStack weapon = p.getMainHandItem();
        LivingEntity target = event.getEntity();
        int exe = RuneData.level(weapon, "execute");
        if (exe > 0 && target.getHealth() <= target.getMaxHealth() * 0.3F) {
            event.setAmount(event.getAmount() * (1 + 0.2F * exe));
            p.serverLevel().sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
        }
        int fire = RuneData.level(weapon, "inferno");
        if (fire > 0) {
            if (target.isOnFire()) event.setAmount(event.getAmount() * (1 + 0.1F * fire));
            target.igniteForSeconds(3);
        }
    }

    /** 当てた後に起きる魔紋（吸血・氷結・雷撃・連撃・斬撃波） */
    @SubscribeEvent
    public static void onDealt(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        float dealt = event.getNewDamage();
        // 反撃（防具）
        if (target instanceof ServerPlayer victim && !RUNE.get() && event.getSource().getDirectEntity() instanceof LivingEntity attacker
                && attacker == event.getSource().getEntity() && attacker != victim && dealt > 0) {
            int counter = 0;
            for (ItemStack armor : victim.getArmorSlots()) counter += RuneData.level(armor, "counter");
            if (counter > 0) {
                float back = dealt * 0.15F * counter;
                runeHurt(attacker, victim.damageSources().thorns(victim), back);
                victim.serverLevel().sendParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.6, attacker.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
            }
        }
        ServerPlayer p = meleeAttacker(event.getSource());
        if (p == null || dealt <= 0 || target == p) return;
        ItemStack weapon = p.getMainHandItem();
        Map<String, Integer> runes = RuneData.runes(weapon);
        if (runes.isEmpty()) return;
        ServerLevel level = p.serverLevel();
        var r = p.getRandom();
        Vec3 center = target.getBoundingBox().getCenter();

        int vamp = runes.getOrDefault("vampire", 0);
        if (vamp > 0) p.heal(dealt * 0.04F * vamp);

        int frost = runes.getOrDefault("frost", 0);
        if (frost > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (2 + frost) * 20, frost >= 3 ? 1 : 0), p);
            level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 8, 0.3, 0.4, 0.3, 0.02);
        }

        int thunder = runes.getOrDefault("thunder", 0);
        if (thunder > 0 && r.nextDouble() < 0.08 * thunder) {
            strike(level, target);
            runeHurt(target, p.damageSources().indirectMagic(p, p), 4 * thunder);
        }

        int wave = runes.getOrDefault("wave", 0);
        if (wave > 0) {
            boolean any = false;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5))) {
                if (e == target || !SkillEffects.isEnemy(p, e)) continue;
                runeHurt(e, p.damageSources().playerAttack(p), dealt * 0.2F * wave);
                any = true;
            }
            if (any) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 3, 0.8, 0.2, 0.8, 0);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.3F);
            }
        }

        int combo = runes.getOrDefault("combo", 0);
        if (combo > 0 && r.nextDouble() < 0.07 * combo) {
            float extra = dealt * 0.5F;
            SkillEffects.later(p, 5, () -> {
                if (!target.isAlive() || target.level() != p.level()) return;
                runeHurt(target, p.damageSources().playerAttack(p), extra);
                Vec3 c = target.getBoundingBox().getCenter();
                level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 10, 0.3, 0.3, 0.3, 0.3);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 1.4F);
            });
        }
    }

    /** 魔紋のダメージ（ほかの魔紋が連鎖しないように目印を立てる） */
    private static void runeHurt(LivingEntity target, DamageSource src, float amount) {
        if (amount <= 0) return;
        RUNE.set(true);
        try {
            target.invulnerableTime = 0;
            target.hurt(src, amount);
        } finally {
            RUNE.set(false);
        }
    }

    /** 見た目だけの雷 */
    private static void strike(ServerLevel level, Entity at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(at.getX(), at.getY(), at.getZ());
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    /** 魂狩り：経験値アップ */
    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer p)) return;
        int l = RuneData.level(p.getMainHandItem(), "reaper");
        if (l > 0) event.setDroppedExperience((int) Math.round(event.getDroppedExperience() * (1 + 0.5 * l)));
    }

    /** 魂狩り：倒すとHP回復 */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getEntity() == p) return;
        int l = RuneData.level(p.getMainHandItem(), "reaper");
        if (l <= 0) return;
        p.heal(l);
        Vec3 c = event.getEntity().getBoundingBox().getCenter();
        p.serverLevel().sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 6, 0.3, 0.3, 0.3, 0.03);
    }

    // ================= 弓・クロスボウ =================

    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer owner)) return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;
        Map<String, Integer> runes = RuneData.runes(weapon);
        if (runes.isEmpty()) return;
        CompoundTag tag = new CompoundTag();
        runes.forEach(tag::putInt);
        arrow.getPersistentData().put(ARROW_KEY, tag);
        int split = runes.getOrDefault("split", 0);
        if (split > 0) {
            SkillEffects.later(owner, 1, () -> {
                if (arrow.isRemoved()) return;
                Vec3 v = arrow.getDeltaMovement();
                for (int i = 1; i <= split; i++) {
                    float angle = (float) Math.toRadians(8 * ((i + 1) / 2) * (i % 2 == 0 ? 1 : -1));
                    AbstractArrow child = arrow instanceof SpectralArrow
                            ? new SpectralArrow(owner.level(), owner, new ItemStack(Items.SPECTRAL_ARROW), null)
                            : new Arrow(owner.level(), owner, new ItemStack(Items.ARROW), null);
                    child.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
                    child.setDeltaMovement(v.yRot(angle));
                    child.setBaseDamage(arrow.getBaseDamage());
                    child.setCritArrow(arrow.isCritArrow());
                    child.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    CompoundTag ct = tag.copy();
                    ct.remove("split");
                    ct.remove("recovery");
                    child.getPersistentData().put(ARROW_KEY, ct);
                    owner.level().addFreshEntity(child);
                }
            });
        }
    }

    private static int arrowRune(Entity arrow, String id) {
        CompoundTag t = arrow.getPersistentData().getCompound(ARROW_KEY);
        return t.getInt(id);
    }

    /** 追尾：飛んでいる矢が近くの敵へ曲がる */
    @SubscribeEvent
    public static void onArrowTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || arrow.level().isClientSide()) return;
        if (!arrow.getPersistentData().contains(ARROW_KEY)) return;
        int homing = arrowRune(arrow, "homing");
        if (homing <= 0 || arrow.tickCount < 3 || arrow.tickCount > 100 || arrow.tickCount % 2 != 0) return;
        Vec3 v = arrow.getDeltaMovement();
        double speed = v.length();
        if (speed < 0.4) return; // 刺さった・落ちかけ
        double range = 6 + 3 * homing;
        Vec3 dir = v.normalize();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : arrow.level().getEntitiesOfClass(LivingEntity.class, arrow.getBoundingBox().inflate(range))) {
            if (!(e instanceof Enemy) || !e.isAlive() || e == arrow.getOwner()) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(arrow.position());
            double d = to.length();
            if (d > range || to.normalize().dot(dir) < 0.3) continue;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        if (best == null) return;
        Vec3 to = best.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        double k = 0.15 + 0.1 * homing;
        Vec3 nv = dir.scale(1 - k).add(to.scale(k)).normalize().scale(speed);
        arrow.setDeltaMovement(nv);
        arrow.hurtMarked = true;
        if (arrow.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.PORTAL, arrow.getX(), arrow.getY(), arrow.getZ(), 2, 0.05, 0.05, 0.05, 0);
    }

    /** 当たったとき：爆裂矢・雷矢・回収 */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel level)) return;
        if (!arrow.getPersistentData().contains(ARROW_KEY)) return;
        CompoundTag t = arrow.getPersistentData().getCompound(ARROW_KEY);
        if (t.getBoolean("done")) return;
        HitResult hit = event.getRayTraceResult();
        if (hit.getType() == HitResult.Type.MISS) return;
        t.putBoolean("done", true);
        ServerPlayer owner = arrow.getOwner() instanceof ServerPlayer sp ? sp : null;
        var r = level.getRandom();
        Vec3 at = hit.getLocation();

        int blast = t.getInt("blast");
        if (blast > 0) {
            level.explode(arrow, at.x, at.y, at.z, 0.6F + 0.4F * blast, Level.ExplosionInteraction.NONE);
        }
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity target && owner != null) {
            int th = t.getInt("thunder_arrow");
            if (th > 0 && r.nextDouble() < 0.2 * th) {
                strike(level, target);
                runeHurt(target, owner.damageSources().indirectMagic(arrow, owner), 4 * th);
            }
            int rec = t.getInt("recovery");
            if (rec > 0 && arrow.pickup == AbstractArrow.Pickup.ALLOWED && !owner.isCreative() && r.nextDouble() < 0.3 * rec) {
                ItemStack back = new ItemStack(arrow instanceof SpectralArrow ? Items.SPECTRAL_ARROW : Items.ARROW);
                if (!owner.getInventory().add(back)) owner.drop(back, false);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED; // 二重にもらえないように
            }
        }
    }

    // ================= 道具 =================

    /** 範囲掘り・鉱脈 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || BREAKING.get() || !(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        ItemStack tool = player.getMainHandItem();
        Map<String, Integer> runes = RuneData.runes(tool);
        if (runes.isEmpty()) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockState state = event.getState();
        BlockPos origin = event.getPos();
        int vein = runes.getOrDefault("vein", 0);
        int area = runes.getOrDefault("area", 0);
        BREAKING.set(true);
        try {
            if (player.isShiftKeyDown()) {
                if (vein > 0 && state.is(Tags.Blocks.ORES) && tool.isCorrectToolForDrops(state)) {
                    List<BlockPos> found = MiningService.findVein(level, origin, state.getBlock(), 4 << (vein - 1));
                    int mined = breakAll(player, tool, found);
                    if (mined > 0) level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
                }
                return;
            }
            if (area > 0 && tool.isCorrectToolForDrops(state)) {
                Direction face = MiningService.facing(player, origin);
                float hardness = state.getDestroySpeed(level, origin);
                int rad = area >= 3 ? 2 : 1;
                List<BlockPos> targets = new java.util.ArrayList<>();
                for (int a = -rad; a <= rad; a++) for (int b = -rad; b <= rad; b++) {
                    if (a == 0 && b == 0) continue;
                    if (area == 1 && a != 0 && b != 0) continue; // 十字
                    BlockPos p = switch (face.getAxis()) {
                        case X -> origin.offset(0, a, b);
                        case Y -> origin.offset(a, 0, b);
                        case Z -> origin.offset(a, b, 0);
                    };
                    BlockState n = level.getBlockState(p);
                    if (n.isAir() || !tool.isCorrectToolForDrops(n) || level.getBlockEntity(p) != null) continue;
                    float h = n.getDestroySpeed(level, p);
                    if (h < 0 || h > hardness + 3) continue;
                    targets.add(p);
                }
                breakAll(player, tool, targets);
            }
        } finally {
            BREAKING.set(false);
        }
    }

    private static int breakAll(ServerPlayer player, ItemStack tool, List<BlockPos> list) {
        boolean silk = com.chigusa.jsncraft.event.GameEvents.hasSilkTouch(player);
        int mined = 0;
        for (BlockPos p : list) {
            if (MiningService.toolAlmostBroken(tool, mined + 1)) break;
            if (MiningService.breakBlock(player, p, silk)) mined++;
        }
        if (mined > 0) tool.hurtAndBreak(mined, player, EquipmentSlot.MAINHAND);
        return mined;
    }

    /** 自動精錬・経験値増幅・磁力（と、付与術師のラピスの恵み） */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        ServerLevel level = event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getPos();

        SkillDefinition lapis = state.is(Tags.Blocks.ORES_LAPIS) ? SkillService.passive(player, "lapis_luck") : null;
        if (lapis != null && player.getRandom().nextDouble() < lapis.chance()) {
            event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(Items.LAPIS_LAZULI, 2 + player.getRandom().nextInt(3))));
        }

        ItemStack tool = event.getTool();
        Map<String, Integer> runes = RuneData.runes(tool);
        if (runes.isEmpty()) return;
        int xp = event.getDroppedExperience();

        if (runes.containsKey("smelt")) {
            float bonus = 0;
            boolean any = false;
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
                if (recipe.isEmpty()) continue;
                ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                ItemStack out = result.copyWithCount(Math.min(result.getMaxStackSize(), stack.getCount() * result.getCount()));
                bonus += recipe.get().value().getExperience() * stack.getCount();
                drop.setItem(out);
                any = true;
            }
            xp += (int) bonus + (player.getRandom().nextFloat() < bonus - (int) bonus ? 1 : 0);
            if (any) level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.01);
        }
        int wisdom = runes.getOrDefault("wisdom", 0);
        if (wisdom > 0 && xp > 0) xp = (int) Math.round(xp * (1 + 0.5 * wisdom));

        if (runes.containsKey("magnet")) {
            Iterator<ItemEntity> it = event.getDrops().iterator();
            while (it.hasNext()) {
                ItemEntity drop = it.next();
                ItemStack stack = drop.getItem().copy();
                player.getInventory().add(stack);
                if (stack.isEmpty()) it.remove();
                else drop.setItem(stack);
            }
            if (xp > 0) {
                player.giveExperiencePoints(xp);
                xp = 0;
            }
        }
        event.setDroppedExperience(xp);
    }

    // ================= 持っているだけで効く魔紋 =================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isAlive()) return;
        int t = p.tickCount;
        // 磁力 II：手に持っていると周りの物を引き寄せる
        if (t % 4 == 0 && RuneData.level(p.getMainHandItem(), "magnet") >= 2) {
            for (ItemEntity item : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(6))) {
                if (item.hasPickUpDelay()) continue;
                Vec3 to = p.position().add(0, 0.5, 0).subtract(item.position());
                if (to.lengthSqr() < 0.5) continue;
                item.setDeltaMovement(to.normalize().scale(0.45));
                item.hurtMarked = true;
            }
        }
        if (t % 20 == 0) {
            boolean night = false;
            for (ItemStack armor : p.getArmorSlots()) if (RuneData.level(armor, "night_eye") > 0) night = true;
            if (night) {
                MobEffectInstance cur = p.getEffect(MobEffects.NIGHT_VISION);
                if (cur == null || (cur.getDuration() < 240 && cur.getDuration() >= 0))
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
            }
        }
        // 自己修復：2秒ごと
        if (t % 40 == 0) {
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isDamaged()) continue;
                int l = RuneData.level(s, "self_repair");
                if (l > 0) s.setDamageValue(Math.max(0, s.getDamageValue() - l));
            }
        }
    }

    // ================= 魂結び =================

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ListTag keep = new ListTag();
        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack s = it.next().getItem();
            if (RuneData.level(s, "soulbound") > 0) {
                keep.add(s.save(p.registryAccess()));
                it.remove();
            }
        }
        if (!keep.isEmpty()) p.getPersistentData().put(SOUL_KEY, keep);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer now)) return;
        CompoundTag old = event.getOriginal().getPersistentData();
        if (!old.contains(SOUL_KEY)) return;
        for (Tag t : old.getList(SOUL_KEY, Tag.TAG_COMPOUND)) {
            ItemStack.parse(now.registryAccess(), t).ifPresent(s -> {
                if (!now.getInventory().add(s)) now.drop(s, false);
            });
        }
        old.remove(SOUL_KEY);
        now.getPersistentData().remove(SOUL_KEY);
    }

    private RuneEffects() {}
}
