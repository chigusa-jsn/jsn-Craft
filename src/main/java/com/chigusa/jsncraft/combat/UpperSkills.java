package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.jewelry.ModJewelry;
import com.chigusa.jsncraft.party.PartyManager;
import com.chigusa.jsncraft.skill.HitFlags;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 上位職に足したスキル（各職業2つ）。
 * アクティブは effect が upper_ で始まるもの。パッシブはこのクラスのイベントで効く。
 * 生活職の「能力アップ」パッシブは effect "stat"、target に「能力ID=量|能力ID*倍率」。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class UpperSkills {

    // ================= アクティブ =================

    public static boolean cast(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        double dur = Math.max(1, s.duration());
        switch (s.effect()) {
            case "upper_still_water" -> { // 明鏡止水
                SkillEffects.attributeBuff(p, ModAttributes.CRIT_CHANCE, "upper_still_crit", s.power(), AttributeModifier.Operation.ADD_VALUE, dur);
                SkillEffects.attributeBuff(p, Attributes.ATTACK_SPEED, "upper_still_speed", 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, dur);
                TimedFlags.set(p, "upper_parry", s.chance(), dur);
                Vfx.magicCircle(level, p.position(), 2.4F, 30, Vfx.AZURE);
                Vfx.ring(level, p.position().add(0, 1, 0), 2.0F, 16, Vfx.WHITE);
                level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            case "upper_blood_feast" -> { // 血の狂宴
                SkillEffects.attributeBuff(p, Attributes.ATTACK_SPEED, "upper_feast_speed", 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, dur);
                TimedFlags.set(p, "upper_feast", s.power(), dur);
                Vfx.ring(level, p.position(), 3.0F, 16, Vfx.RED);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 14, 0.5, 0.5, 0.5, 0.1);
                level.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            case "upper_star_arrows" -> { // 星降りの矢
                TimedFlags.set(p, "upper_star_arrows", s.power(), dur);
                Vfx.magicCircle(level, p.position(), 2.2F, 30, Vfx.GOLD);
                level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.5, p.getZ(), 20, 0.5, 0.6, 0.5, 0.05);
                level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.6F);
            }
            case "upper_war_god" -> { // 闘神降臨
                TimedFlags.set(p, "upper_war_god", s.power(), dur);
                SkillEffects.attributeBuff(p, Attributes.KNOCKBACK_RESISTANCE, "upper_war_god_kb", 1.0, AttributeModifier.Operation.ADD_VALUE, dur);
                Vfx.ring(level, p.position(), 3.0F, 16, Vfx.GOLD);
                Vfx.ring(level, p.position().add(0, 1, 0), 2.0F, 16, Vfx.RED);
                level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
                level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
            }
            case "upper_fortress" -> { // 不落の城塞
                SkillEffects.attributeBuff(p, ModAttributes.DAMAGE_REDUCTION, "upper_fortress", s.power(), AttributeModifier.Operation.ADD_VALUE, dur);
                for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(s.radius()), m -> m instanceof Enemy && m.isAlive())) m.setTarget(p);
                Vfx.magicCircle(level, p.position(), (float) s.radius(), 30, Vfx.GOLD);
                Vfx.ring(level, p.position().add(0, 1, 0), 2.2F, 20, Vfx.WHITE);
                level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 0.8F);
            }
            case "upper_shadow_clone" -> { // 影分身
                TimedFlags.set(p, "upper_clone", s.power(), dur);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.02);
                level.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case "upper_thunder_tide" -> { // 雷潮の槍
                TimedFlags.set(p, "upper_thunder", s.power(), dur);
                Vfx.ring(level, p.position(), 2.6F, 16, Vfx.AZURE);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.2);
                level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    // ================= 戦闘のパッシブとアクティブの続き =================

    private static final Map<UUID, Integer> SWORD_COUNT = new HashMap<>();
    private record Combo(int stacks, long last) {}
    private static final Map<UUID, Combo> COMBO = new HashMap<>();

    private static boolean melee(LivingIncomingDamageEvent e, ServerPlayer p) {
        return e.getSource().getDirectEntity() == p && !HitFlags.SKILL.get();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();

        // ---- 受ける側 ----
        if (victim instanceof ServerPlayer vp) {
            double parry = TimedFlags.get(vp, "upper_parry");
            if (parry > 0 && event.getSource().getEntity() instanceof LivingEntity && vp.getRandom().nextDouble() < parry) {
                event.setCanceled(true);
                Vfx.slash(vp.serverLevel(), vp.position().add(0, 1.1, 0), 1.2F, Vfx.WHITE, 45);
                vp.serverLevel().playSound(null, vp.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.6F);
                return;
            }
            SkillDefinition ind = SkillService.passive(vp, "upper_indomitable");
            if (ind != null && vp.getHealth() <= vp.getMaxHealth() * ind.chance()) event.setAmount(event.getAmount() * (float) (1 - ind.bonus()));
        }

        // ---- 攻める側 ----
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || victim == p) return;
        ItemStack main = p.getMainHandItem();
        float amount = event.getAmount();

        // 不屈の闘志：HPが少ないと強くなる
        SkillDefinition ind = SkillService.passive(p, "upper_indomitable");
        if (ind != null && p.getHealth() <= p.getMaxHealth() * ind.chance()) amount *= (float) (1 + ind.power());

        // 鷹の目：遠い敵ほど矢が強い
        if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
            SkillDefinition hawk = SkillService.passive(p, "upper_hawk_eye");
            if (hawk != null) amount *= (float) (1 + hawk.power() * Math.min(1, p.distanceTo(victim) / Math.max(1, hawk.radius())));
            double star = TimedFlags.get(p, "upper_star_arrows");
            if (star > 0) {
                float splash = amount * (float) star;
                for (LivingEntity e : AxeSkills.around(p, victim.position(), 2.5)) if (e != victim) SkillEffects.hit(p, e, splash);
                ServerLevel level = p.serverLevel();
                level.sendParticles(ParticleTypes.FIREWORK, victim.getX(), victim.getY() + 1, victim.getZ(), 16, 0.3, 0.3, 0.3, 0.15);
                Vfx.ring(level, victim.position(), 2.5F, 10, Vfx.GOLD);
                level.playSound(null, victim.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.6F, 1.3F);
            }
        }

        if (melee(event, p)) {
            // 影渡り：倒したあとの一撃は2倍
            if (TimedFlags.get(p, "upper_shadow_next") > 0) {
                amount *= 2;
                TimedFlags.set(p, "upper_shadow_next", 0, 0);
                p.serverLevel().sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1, victim.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
            }
            // 連撃の極意：素手で殴り続けると強くなる
            if (main.isEmpty()) {
                SkillDefinition combo = SkillService.passive(p, "upper_combo");
                if (combo != null) {
                    long now = p.level().getGameTime();
                    Combo c = COMBO.get(p.getUUID());
                    int st = c == null || now - c.last() > 40 ? 0 : c.stacks();
                    amount *= (float) (1 + combo.power() * st);
                    COMBO.put(p.getUUID(), new Combo(Math.min(Math.max(1, combo.amplifier()), st + 1), now));
                }
                double god = TimedFlags.get(p, "upper_war_god");
                if (god > 0) {
                    float wave = amount * (float) god;
                    for (LivingEntity e : AxeSkills.around(p, victim.position(), 2.5)) if (e != victim) SkillEffects.hit(p, e, wave);
                    Vfx.ring(p.serverLevel(), victim.position(), 2.5F, 8, Vfx.GOLD);
                    p.serverLevel().sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 0.8, victim.getZ(), 1, 0, 0, 0, 0);
                }
            }
            // 蒼天の太刀筋：剣で4回目ごとに蒼い斬撃
            if (main.getItem() instanceof SwordItem) {
                SkillDefinition edge = SkillService.passive(p, "upper_azure_edge");
                if (edge != null) {
                    int n = SWORD_COUNT.merge(p.getUUID(), 1, Integer::sum);
                    if (n >= Math.max(2, edge.amplifier())) {
                        SWORD_COUNT.put(p.getUUID(), 0);
                        azureWave(p, edge);
                    }
                }
            }
            // 血の狂宴：斧で与えたダメージで回復
            double feast = TimedFlags.get(p, "upper_feast");
            if (feast > 0 && main.getItem() instanceof AxeItem) {
                p.heal(amount * (float) feast);
                p.serverLevel().sendParticles(ParticleTypes.DAMAGE_INDICATOR, victim.getX(), victim.getY() + 1, victim.getZ(), 4, 0.2, 0.3, 0.2, 0.05);
            }
            // 影分身：短剣の攻撃が影でもう一度
            double clone = TimedFlags.get(p, "upper_clone");
            if (clone > 0 && main.getItem() instanceof com.chigusa.jsncraft.weapon.DaggerItem) {
                float again = amount * (float) clone;
                SkillEffects.later(p, 4, () -> {
                    if (!victim.isAlive()) return;
                    SkillEffects.hit(p, victim, again);
                    p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, victim.getX(), victim.getY() + 1, victim.getZ(), 8, 0.3, 0.4, 0.3, 0.01);
                    p.serverLevel().playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.6F);
                });
            }
            // 雷潮の槍：槍の攻撃で、ときどき雷
            double thunder = TimedFlags.get(p, "upper_thunder");
            if (thunder > 0 && (main.getItem() instanceof com.chigusa.jsncraft.weapon.JobSpearItem || main.getItem() instanceof TridentItem)
                    && p.getRandom().nextDouble() < 0.3) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(p.serverLevel());
                if (bolt != null) {
                    bolt.moveTo(victim.getX(), victim.getY(), victim.getZ());
                    bolt.setVisualOnly(true);
                    p.serverLevel().addFreshEntity(bolt);
                }
                float dmg = AxeSkills.atk(p) * (float) thunder;
                SkillEffects.later(p, 1, () -> {
                    if (victim.isAlive()) SkillEffects.hit(p, victim, dmg);
                });
            }
        }
        event.setAmount(amount);
    }

    /** 蒼い斬撃を前へ飛ばす（一直線の敵に当たる） */
    private static void azureWave(ServerPlayer p, SkillDefinition s) {
        ServerLevel level = p.serverLevel();
        Vec3 look = AxeSkills.flatLook(p);
        Vec3 start = p.position().add(0, 1.0, 0);
        float dmg = AxeSkills.atk(p) * (float) s.power();
        Set<LivingEntity> hit = new HashSet<>();
        double len = Math.max(3, s.radius());
        for (double d = 1; d <= len; d += 1) {
            Vec3 at = start.add(look.scale(d));
            for (LivingEntity e : AxeSkills.around(p, at, 1.2)) if (hit.add(e)) SkillEffects.hit(p, e, dmg);
            if ((int) d % 2 == 1) Vfx.slash(level, at, 1.3F, Vfx.AZURE, 90);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.3F);
    }

    /** 鉄壁の誓い：盾で防ぐと、自分と仲間を回復して攻撃力アップ */
    @SubscribeEvent
    public static void onBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !event.getBlocked()) return;
        SkillDefinition oath = SkillService.passive(p, "upper_iron_oath");
        if (oath == null) return;
        PartyManager.Party party = PartyManager.partyOf(p);
        for (ServerPlayer o : p.serverLevel().players()) {
            boolean ally = o == p || (party != null && party.members.contains(o.getUUID()));
            if (!ally || o.distanceTo(p) > oath.radius()) continue;
            o.heal((float) oath.power());
        }
        SkillEffects.attributeBuff(p, Attributes.ATTACK_DAMAGE, "upper_iron_oath", oath.bonus(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 3);
        Vfx.ring(p.serverLevel(), p.position(), 1.8F, 10, Vfx.GOLD);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getEntity() == p) return;
        LivingEntity dead = event.getEntity();
        // 影渡り
        SkillDefinition step = SkillService.passive(p, "upper_shadow_step");
        if (step != null && p.getMainHandItem().getItem() instanceof com.chigusa.jsncraft.weapon.DaggerItem) {
            double sec = Math.max(1, step.duration());
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int) (sec * 20), 0, false, false));
            SkillEffects.attributeBuff(p, Attributes.MOVEMENT_SPEED, "upper_shadow_step", step.power(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, sec);
            TimedFlags.set(p, "upper_shadow_next", 1, 5);
            p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 14, 0.4, 0.6, 0.4, 0.02);
        }
        // 宝探しの鼻
        SkillDefinition nose = SkillService.passive(p, "upper_treasure_nose");
        if (nose != null && dead instanceof Enemy && p.getRandom().nextDouble() < nose.chance()) {
            ItemStack em = new ItemStack(Items.EMERALD, 1 + p.getRandom().nextInt(3));
            Block.popResource(p.level(), dead.blockPosition(), em);
            p.displayClientMessage(Component.literal("宝探しの鼻：エメラルドを見つけた！").withStyle(ChatFormatting.GREEN), true);
        }
    }

    // ================= 生活のパッシブ =================

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p) || p.isCreative() || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockState st = event.getState();
        BlockPos pos = event.getPos();
        var r = p.getRandom();
        if (st.is(Tags.Blocks.ORES)) {
            SkillDefinition gem = SkillService.passive(p, "upper_gem_vein");
            if (gem != null && r.nextDouble() < gem.chance() && !ModJewelry.RAW_GEMS.isEmpty()) {
                ItemStack g = new ItemStack(ModJewelry.RAW_GEMS.get(r.nextInt(ModJewelry.RAW_GEMS.size())).get());
                Block.popResource(level, pos, g);
                level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.1);
                level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.8F, 1.4F);
            }
        }
        if (st.is(BlockTags.LOGS)) {
            SkillDefinition bounty = SkillService.passive(p, "upper_forest_bounty");
            if (bounty != null) {
                double x = r.nextDouble();
                ItemStack got = x < bounty.bonus() ? new ItemStack(Items.GOLDEN_APPLE)
                        : x < bounty.chance() ? new ItemStack(r.nextBoolean() ? Items.APPLE : Items.STICK, 1 + r.nextInt(2)) : ItemStack.EMPTY;
                if (!got.isEmpty()) Block.popResource(level, pos, got);
            }
        }
    }

    // ================= 毎秒：能力アップのパッシブ、潮の加護、実りの足跡 =================

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        applyStats(p);

        SkillDefinition sea = SkillService.passive(p, "upper_sea_grace");
        if (sea != null && (p.isInWaterOrRain())) {
            SkillEffects.attributeBuff(p, Attributes.ATTACK_DAMAGE, "upper_sea_atk", sea.power(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 2);
            SkillEffects.attributeBuff(p, Attributes.MOVEMENT_SPEED, "upper_sea_spd", sea.bonus(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 2);
            p.serverLevel().sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.5, p.getZ(), 4, 0.3, 0.3, 0.3, 0.05);
        }

        SkillDefinition steps = SkillService.passive(p, "upper_fertile_steps");
        if (steps != null) {
            ServerLevel level = p.serverLevel();
            int rad = (int) Math.max(1, steps.radius());
            int tries = Math.max(1, steps.amplifier());
            for (int i = 0; i < tries; i++) {
                BlockPos pos = p.blockPosition().offset(p.getRandom().nextInt(rad * 2 + 1) - rad, p.getRandom().nextInt(3) - 1, p.getRandom().nextInt(rad * 2 + 1) - rad);
                BlockState st = level.getBlockState(pos);
                if (st.getBlock() instanceof CropBlock crop && !crop.isMaxAge(st)) {
                    st.randomTick(level, pos, level.random);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0);
                }
            }
        }
    }

    private static final String STAT_PREFIX = "upper_stat/";
    private static final Map<UUID, Set<ResourceLocation>> APPLIED = new HashMap<>();

    /** 「能力アップ」パッシブ：覚えているものの能力を付けて、覚えていないものは外す */
    private static void applyStats(ServerPlayer p) {
        Set<ResourceLocation> want = new HashSet<>();
        for (Map.Entry<ResourceLocation, SkillDefinition> e : SkillService.passives(p, "stat")) {
            int i = 0;
            for (String part : e.getValue().target().split("\\|")) {
                boolean mult = part.contains("*");
                String[] kv = part.split(mult ? "\\*" : "=");
                if (kv.length != 2) continue;
                ResourceLocation attrId = ResourceLocation.tryParse(kv[0].trim());
                if (attrId == null) continue;
                var holder = BuiltInRegistries.ATTRIBUTE.getHolder(attrId);
                if (holder.isEmpty()) continue;
                AttributeInstance inst = p.getAttribute(holder.get());
                if (inst == null) continue;
                ResourceLocation mod = JsnCraft.id(STAT_PREFIX + e.getKey().getPath() + "_" + i++);
                want.add(mod);
                double amount;
                try { amount = Double.parseDouble(kv[1].trim()); } catch (NumberFormatException ex) { continue; }
                if (!inst.hasModifier(mod)) inst.addTransientModifier(new AttributeModifier(mod, amount,
                        mult ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE));
            }
        }
        Set<ResourceLocation> had = APPLIED.getOrDefault(p.getUUID(), Set.of());
        for (ResourceLocation old : had) {
            if (want.contains(old)) continue;
            for (Holder<Attribute> h : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
                AttributeInstance inst = p.getAttribute(h);
                if (inst != null) inst.removeModifier(old);
            }
        }
        APPLIED.put(p.getUUID(), want);
    }

    public static boolean hasShield(ServerPlayer p) {
        return p.getMainHandItem().getItem() instanceof ShieldItem || p.getOffhandItem().getItem() instanceof ShieldItem;
    }

    private UpperSkills() {}
}
