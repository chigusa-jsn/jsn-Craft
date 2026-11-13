package com.chigusa.jsncraft.skill;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.DamageNumberPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** プレイヤーが与えたダメージを、数字で見せる。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class CombatFeedback {

    /** ダメージの処理が始まるとき、前の会心の目印を消しておく */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        HitFlags.CRIT.set(false);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        boolean crit = HitFlags.CRIT.get();
        HitFlags.CRIT.set(false);
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity victim = event.getEntity();
        if (victim == player || event.getNewDamage() <= 0) return;
        int style = HitFlags.SKILL.get() ? 2 : crit ? 1 : 0;
        PacketDistributor.sendToPlayer(player, new DamageNumberPayload(
                victim.getX(), victim.getY() + victim.getBbHeight() + 0.3, victim.getZ(), event.getNewDamage(), style));
    }

    private CombatFeedback() {}
}
