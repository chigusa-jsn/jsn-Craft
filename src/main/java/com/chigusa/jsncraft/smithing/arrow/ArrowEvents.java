package com.chigusa.jsncraft.smithing.arrow;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 三叉槍の穂先の効果と、光の矢の明かりを消す処理。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class ArrowEvents {

    private record Light(ResourceKey<Level> dim, BlockPos pos, long until) {}

    private static final List<Light> LIGHTS = new ArrayList<>();

    static void lightPlaced(ServerLevel level, BlockPos pos, int ticks) {
        LIGHTS.add(new Light(level.dimension(), pos, level.getGameTime() + ticks));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (LIGHTS.isEmpty()) return;
        var server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        for (Iterator<Light> it = LIGHTS.iterator(); it.hasNext(); ) {
            Light l = it.next();
            ServerLevel level = server.getLevel(l.dim());
            if (level == null) { it.remove(); continue; }
            if (level.getGameTime() < l.until()) continue;
            if (level.isLoaded(l.pos()) && level.getBlockState(l.pos()).is(Blocks.LIGHT)) level.removeBlock(l.pos(), false);
            it.remove();
        }
    }

    private static void applyTip(ServerLevel level, TridentTipItem.Tip tip, LivingEntity target, ServerPlayer owner, boolean thrown) {
        switch (tip) {
            case FIRE -> target.igniteForSeconds(thrown ? 6 : 4);
            case FROST -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, thrown ? 100 : 60, 1), owner);
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 100, target.getTicksFrozen() + 120));
                level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
            }
            case STORM -> {
                if (!thrown && level.random.nextFloat() >= 0.2F) return;
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt == null) return;
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                if (owner != null) bolt.setCause(owner);
                level.addFreshEntity(bolt);
            }
        }
    }

    /** 投げた三叉槍が当たったとき */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownTrident trident) || !(trident.level() instanceof ServerLevel level)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)) return;
        ItemStack weapon = trident.getWeaponItem();
        if (weapon == null) return;
        TridentTipItem.Tip tip = TridentTipItem.tipOf(weapon);
        if (tip == null) return;
        applyTip(level, tip, target, trident.getOwner() instanceof ServerPlayer p ? p : null, true);
    }

    /** 手に持った三叉槍で殴ったとき */
    @SubscribeEvent
    public static void onMelee(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        ItemStack weapon = p.getMainHandItem();
        if (!weapon.is(Items.TRIDENT)) return;
        TridentTipItem.Tip tip = TridentTipItem.tipOf(weapon);
        if (tip != null) applyTip(level, tip, event.getEntity(), p, false);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack st = event.getItemStack();
            if (!st.is(Items.TRIDENT)) return;
            TridentTipItem.Tip tip = TridentTipItem.tipOf(st);
            if (tip != null) event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                    Component.literal("◇ " + tip.name + "：" + tip.desc).withStyle(tip.color));
        }
    }

    private ArrowEvents() {}
}
