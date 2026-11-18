package com.chigusa.jsncraft.magic;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.PlayerResources;
import com.chigusa.jsncraft.stat.ResourceService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 魔導士の守りの魔法：マナシールド、フレイムアーマー。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class MagicEvents {

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;

        // フレイムアーマー：火のダメージは受けない、殴ってきた相手を燃やす
        double armor = TimedFlags.get(p, "mage_fire_armor");
        if (armor > 0) {
            if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                event.setCanceled(true);
                return;
            }
            if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != p && attacker.distanceTo(p) < 4) {
                MageSpells.magicHit(p, attacker, (float) armor, MageSpells.Elem.FIRE);
                p.serverLevel().sendParticles(ParticleTypes.FLAME, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
            }
        }

        // マナシールド：受けるダメージの一部をマナで肩代わり（マナ2で1ダメージ）
        double shield = TimedFlags.get(p, "mage_mana_shield");
        if (shield > 0 && event.getAmount() > 0) {
            PlayerResources r = ResourceService.get(p);
            float absorb = Math.min(event.getAmount() * (float) shield, r.mana / 2F);
            if (absorb > 0) {
                r.mana -= absorb * 2;
                event.setAmount(event.getAmount() - absorb);
                p.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 10, 0.4, 0.6, 0.4, 0.2);
                p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
        }
    }

    private MagicEvents() {}
}
