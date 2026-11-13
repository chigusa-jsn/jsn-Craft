package com.chigusa.jsncraft.stat;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 新しい能力値を、実際のゲームの動きにつなげる場所。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class StatEvents {

    /** マナ・スタミナの回復 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceService.tick(player);
        }
    }

    /** 前のバージョン（回復速度5や2）で遊んでいた人を、新しい初期値1にそろえる */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var inst = player.getAttribute(ModAttributes.STAMINA_REGEN);
            if (inst != null && (inst.getBaseValue() == 5.0 || inst.getBaseValue() == 2.0)) inst.setBaseValue(1.0);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceService.refill(player);
        }
    }

    /** 採掘の幸運：鉱石を掘ったとき、確率でドロップが増える */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        if (!event.getState().is(Tags.Blocks.ORES)) return;

        double fortune = player.getAttributeValue(ModAttributes.MINING_FORTUNE);
        if (fortune <= 0) return;

        int extra = (int) fortune;
        if (player.getRandom().nextDouble() < fortune - extra) extra++;
        if (extra <= 0) return;

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            // シルクタッチで鉱石ブロックそのものが落ちたときは増やさない
            if (stack.is(event.getState().getBlock().asItem())) continue;
            stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() * (1 + extra)));
            drop.setItem(stack);
        }
    }

    /** 会心：プレイヤーの近接攻撃が、確率で大ダメージになる */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getSource().getDirectEntity() != player) return; // 矢や魔法は対象外

        double chance = player.getAttributeValue(ModAttributes.CRIT_CHANCE);
        if (chance <= 0 || player.getRandom().nextDouble() >= chance) return;

        double bonus = player.getAttributeValue(ModAttributes.CRIT_DAMAGE);
        event.setAmount((float) (event.getAmount() * (1 + bonus)));
        player.crit(event.getEntity());
        player.level().playSound(null, event.getEntity().blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                player.getSoundSource(), 1.0F, 1.0F);
    }

    private StatEvents() {}
}
