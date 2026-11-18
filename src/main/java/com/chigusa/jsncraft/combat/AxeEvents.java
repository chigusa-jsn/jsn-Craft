package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.skill.HitFlags;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 斧使いの効果：断鋼（受けるダメージ+20%）、激昂（自分の受けるダメージも増える）、薙ぎ払い、血の渇き。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class AxeEvents {

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        // 断鋼：守りを砕かれた敵は、だれの攻撃でも +20%
        if (AxeSkills.sundered(victim)) event.setAmount(event.getAmount() * 1.2F);
        // 激昂中は、自分の受けるダメージも増える
        if (victim instanceof ServerPlayer vp) {
            double rage = TimedFlags.get(vp, "axe_rage");
            if (rage > 0) event.setAmount(event.getAmount() * (float) (1 + rage));
        }
        // 薙ぎ払い：斧の近接攻撃が、周りの敵にも当たる
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getSource().getDirectEntity() == p
                && !HitFlags.SKILL.get() && p.getMainHandItem().getItem() instanceof AxeItem) {
            SkillDefinition cleave = SkillService.passive(p, "axe_cleave_passive");
            if (cleave != null) {
                float splash = event.getAmount() * (float) cleave.power();
                for (LivingEntity e : AxeSkills.around(p, victim.position(), Math.max(2.0, cleave.radius()))) {
                    if (e == victim) continue;
                    SkillEffects.hit(p, e, splash);
                }
                p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + 1, victim.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    /** 血の渇き：倒すとHPが回復 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getEntity() == p) return;
        SkillDefinition blood = SkillService.passive(p, "axe_bloodthirst");
        if (blood == null || !(p.getMainHandItem().getItem() instanceof AxeItem)) return;
        p.heal(p.getMaxHealth() * (float) blood.power());
        p.serverLevel().sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
    }

    private AxeEvents() {}
}
