package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.magic.HolySpells;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/** 盾騎士の守り：鉄壁の構え、挑発中の守り、金剛不壊、かばう、聖盾の裁きのため、反撃の盾、不動。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class GuardEvents {

    private static final ThreadLocal<Boolean> COVERING = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        float amount = event.getAmount();
        double wall = TimedFlags.get(p, "guard_iron_wall");
        if (wall > 0) amount *= (float) (1 - wall);
        double taunt = TimedFlags.get(p, "guard_taunt");
        if (taunt > 0) amount *= (float) (1 - taunt);
        double adamant = TimedFlags.get(p, "guard_adamant");
        if (adamant > 0) amount *= (float) (1 - adamant);

        // かばう：近くでかばってくれている盾騎士が、ダメージの一部を代わりに受ける
        if (!COVERING.get()) {
            for (ServerPlayer knight : HolySpells.allies(p, p.position(), 8)) {
                if (knight == p) continue;
                double share = TimedFlags.get(knight, "guard_cover");
                if (share <= 0) continue;
                float moved = amount * (float) share;
                amount -= moved;
                COVERING.set(true);
                try {
                    knight.hurt(knight.damageSources().generic(), moved);
                } finally {
                    COVERING.set(false);
                }
                p.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.5, 0.3, 0.1);
                break;
            }
        }
        event.setAmount(amount);

        // 聖盾の裁き：受けたダメージをためる
        if (TimedFlags.get(p, "guard_store") > 0) {
            p.getPersistentData().putFloat(GuardSkills.STORE, p.getPersistentData().getFloat(GuardSkills.STORE) + amount);
        }
    }

    /** 反撃の盾：盾で防いだら、相手にダメージを返す */
    @SubscribeEvent
    public static void onBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !event.getBlocked()) return;
        SkillDefinition counter = SkillService.passive(p, "guard_counter");
        if (counter == null || !(event.getDamageSource().getEntity() instanceof LivingEntity attacker) || attacker == p) return;
        float dmg = event.getBlockedDamage() * (float) counter.power() + AxeSkills.atk(p) * 0.5F;
        if (dmg > 0) SkillEffects.hit(p, attacker, dmg);
    }

    /** 不動：ノックバックしない */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && SkillService.passive(p, "guard_steadfast") != null) event.setCanceled(true);
    }

    private GuardEvents() {}
}
