package com.chigusa.jsncraft.combat;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.skill.HitFlags;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.weapon.DaggerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 暗殺者の効果：二刀流、背後討ち、隠密からの一撃、毒刃、出血、死の刻印のため。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class DaggerEvents {

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        // 死の刻印：刻まれた敵が受けたダメージをためる（だれの攻撃でも）
        if (victim.getPersistentData().getLong(DaggerSkills.MARK_UNTIL) > victim.level().getGameTime()) {
            victim.getPersistentData().putFloat(DaggerSkills.MARK_STORE, victim.getPersistentData().getFloat(DaggerSkills.MARK_STORE) + event.getAmount());
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p) return;
        if (!(p.getMainHandItem().getItem() instanceof DaggerItem)) return;
        float amount = event.getAmount();
        boolean skill = HitFlags.SKILL.get();
        // 二刀流：左手の短剣の力も乗る
        if (!skill && p.getOffhandItem().getItem() instanceof DaggerItem off) {
            SkillDefinition twin = SkillService.passive(p, "dagger_twin_master");
            amount += off.spec().damage() * (twin != null ? (float) twin.power() : 0.4F);
        }
        // 背後討ち
        SkillDefinition back = SkillService.passive(p, "dagger_backstab");
        if (back != null && DaggerSkills.behind(p, victim)) {
            amount *= 1 + (float) back.power();
            p.serverLevel().sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1, victim.getZ(), 10, 0.3, 0.4, 0.3, 0.3);
        }
        // 隠密からの一撃
        double stealth = TimedFlags.get(p, "dagger_stealth");
        if (stealth > 0 && !skill) {
            amount *= (float) stealth;
            TimedFlags.set(p, "dagger_stealth", 0, 0);
            p.removeEffect(MobEffects.INVISIBILITY);
            p.displayClientMessage(Component.literal("奇襲！").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        }
        event.setAmount(amount);
        // 毒刃
        double poison = TimedFlags.get(p, "dagger_poison");
        if (poison > 0) victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, (int) poison - 1));
        // 出血
        SkillDefinition bleed = SkillService.passive(p, "dagger_bleed");
        if (bleed != null && !skill && p.getRandom().nextDouble() < bleed.chance()) {
            float tick = (float) (AxeSkills.atk(p) * bleed.power());
            for (int i = 1; i <= 3; i++) {
                SkillEffects.later(p, i * 20, () -> {
                    if (!victim.isAlive()) return;
                    SkillEffects.hit(p, victim, tick);
                    p.serverLevel().sendParticles(DaggerSkills.BLOOD, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 8, 0.2, 0.3, 0.2, 0);
                });
            }
        }
    }

    private DaggerEvents() {}
}
