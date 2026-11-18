package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.magic.MageSpells;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.weapon.SeaTridentItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 槍兵の効果：潮の加護（水・雨の中で強い）、槍兵が投げたトライデント（強く・すぐ手元に戻る）、
 * 海神の三叉槍（投げると濡らす・雨なら雷）。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class SpearEvents {

    static boolean isLancer(ServerPlayer p) {
        ResourceLocation c = JobService.data(p).getJob(JobType.COMBAT);
        return c != null && (c.getPath().equals("lancer") || c.getPath().equals("sea_lancer"));
    }

    static boolean wetPlace(ServerPlayer p) {
        return p.isInWaterRainOrBubble();
    }

    /** 潮の加護：水の中・雨の中では速くなる（1秒ごとに見る） */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        SkillDefinition tide = SkillService.passive(p, "spear_tide");
        if (tide == null || !wetPlace(p)) return;
        SkillEffects.attributeBuff(p, Attributes.MOVEMENT_SPEED, "spear_tide", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 1.5);
        if (p.isInWater()) SkillEffects.attributeBuff(p, Attributes.WATER_MOVEMENT_EFFICIENCY, "spear_tide_water", 0.5, AttributeModifier.Operation.ADD_VALUE, 1.5);
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        var src = event.getSource();
        // 潮の加護：水・雨の中の攻撃が強い
        if (src.getEntity() instanceof ServerPlayer p && wetPlace(p)) {
            SkillDefinition tide = SkillService.passive(p, "spear_tide");
            if (tide != null) event.setAmount(event.getAmount() * (float) (1 + tide.power()));
        }
        // 投げたトライデント
        if (src.getDirectEntity() instanceof ThrownTrident t && t.getOwner() instanceof ServerPlayer p) {
            ItemStack stack = t.getPickupItemStackOrigin();
            float mult = 1;
            if (stack.getItem() instanceof SeaTridentItem sea) mult *= (float) sea.spec().throwMult();
            if (isLancer(p)) mult *= 1.3F;
            event.setAmount(event.getAmount() * mult);
        }
    }

    /** 投げたトライデントが当たったとき：三叉槍の効果、槍兵ならすぐ手元に戻る */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownTrident t) || !(t.getOwner() instanceof ServerPlayer p)) return;
        if (t.getPersistentData().getBoolean("jsn_done")) return;
        t.getPersistentData().putBoolean("jsn_done", true);
        ServerLevel level = p.serverLevel();
        ItemStack stack = t.getPickupItemStackOrigin();
        Vec3 at = event.getRayTraceResult().getLocation();
        if (stack.getItem() instanceof SeaTridentItem sea) {
            level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 40, 0.6, 0.4, 0.6, 0.3);
            level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 15, 0.4, 0.4, 0.4, 0.05);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 1.2F);
            for (LivingEntity e : AxeSkills.around(p, at, 2.5)) MageSpells.wet(e, 200);
            if (level.isRainingAt(net.minecraft.core.BlockPos.containing(at).above())) {
                var bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(at);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                for (LivingEntity e : AxeSkills.around(p, at, 3.0)) {
                    MageSpells.magicHit(p, e, 6.0F * (float) sea.spec().throwMult(), MageSpells.Elem.THUNDER);
                }
            }
        }
        if (!isLancer(p)) return;
        // 槍兵：当たったら手元に戻る
        SkillEffects.later(p, 3, () -> {
            if (t.isRemoved() || !p.isAlive()) return;
            if (t.pickup == AbstractArrow.Pickup.ALLOWED && !p.isCreative()) {
                ItemStack back = t.getPickupItemStackOrigin().copy();
                if (p.getMainHandItem().isEmpty()) p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, back);
                else if (!p.getInventory().add(back)) p.drop(back, false);
            }
            level.sendParticles(ParticleTypes.SPLASH, t.getX(), t.getY(), t.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
            t.discard();
            level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 1.2F);
        });
    }

    private SpearEvents() {}
}
