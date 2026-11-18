package com.chigusa.jsncraft.beast;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.skill.HitFlags;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.weapon.WhipItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.UUID;

/**
 * 獣使いの相棒のしくみ：同士討ちなし、鞭で叩いた敵を狙う、パッシブ（絆・鞭の印・群れの狩り）、突撃・野生解放のボーナス。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class BeastEvents {

    private static final ThreadLocal<Boolean> SPLASH = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        Entity src = event.getSource().getEntity();

        // ---- 相棒が受けるダメージ ----
        if (PetCommon.isPet(victim)) {
            ServerPlayer owner = PetCommon.owner(victim);
            if (src instanceof Player || (src != null && PetCommon.isPet(src))) { // 主人・ほかのプレイヤー・相棒どうしの攻撃は当たらない
                event.setCanceled(true);
                return;
            }
            if (owner != null) {
                SkillDefinition bond = SkillService.passive(owner, "beast_bond");
                if (bond != null) event.setAmount((float) (event.getAmount() * (1 - bond.power())));
            }
            return;
        }

        // ---- 相棒が与えるダメージ ----
        if (src instanceof Mob pet && PetCommon.isPet(pet)) {
            if (victim instanceof Player) {
                event.setCanceled(true);
                return;
            }
            ServerPlayer owner = PetCommon.owner(pet);
            if (owner == null) return;
            double mult = 1;
            // 鞭の印：主人が叩いた敵へのダメージアップ
            SkillDefinition mark = SkillService.passive(owner, "beast_whip_mark");
            if (mark != null && owner.getPersistentData().hasUUID(PetCommon.FOCUS)
                    && victim.getUUID().equals(owner.getPersistentData().getUUID(PetCommon.FOCUS))
                    && owner.getPersistentData().getLong(PetCommon.FOCUS_UNTIL) > victim.level().getGameTime()) mult += mark.power();
            // 群れの狩り：同じ敵のまわりにいる仲間の数だけダメージアップ
            SkillDefinition pack = SkillService.passive(owner, "beast_pack");
            if (pack != null) {
                int near = 0;
                UUID oid = owner.getUUID();
                near = victim.level().getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(6), m -> m != pet && oid.equals(PetCommon.ownerId(m))).size();
                mult += pack.power() * Math.min(near, 6);
            }
            // 突撃：最初の一撃が強い
            if (pet.getPersistentData().contains(BeastSkills.CHARGE)) {
                mult += pet.getPersistentData().getDouble(BeastSkills.CHARGE);
                pet.getPersistentData().remove(BeastSkills.CHARGE);
                ((ServerLevel) victim.level()).sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 16, 0.3, 0.3, 0.3, 0.3);
            }
            // 野生解放
            boolean frenzy = pet.getPersistentData().getLong(BeastSkills.FRENZY_UNTIL) > victim.level().getGameTime();
            if (frenzy) mult += pet.getPersistentData().getDouble(BeastSkills.FRENZY_POW);
            float amount = (float) (event.getAmount() * mult);
            event.setAmount(amount);
            if (frenzy && !SPLASH.get()) splash(owner, pet, victim, amount * 0.5F);
            return;
        }

        // ---- 主人が鞭で叩いた敵を、相棒みんなで狙う ----
        if (src instanceof ServerPlayer p && event.getSource().getDirectEntity() == p && !HitFlags.SKILL.get()
                && p.getMainHandItem().getItem() instanceof WhipItem && PetCommon.beastLevel(p) > 0
                && SkillEffects.isEnemy(p, victim)) {
            PetCommon.focus(p, victim, 200);
            ((ServerLevel) victim.level()).sendParticles(BeastSkills.AMBER, victim.getX(), victim.getY() + victim.getBbHeight() + 0.3, victim.getZ(), 6, 0.2, 0.1, 0.2, 0);
        }
    }

    /** 野生解放中は、まわりの敵にも半分のダメージ */
    private static void splash(ServerPlayer owner, Mob pet, LivingEntity victim, float dmg) {
        SPLASH.set(true);
        try {
            ServerLevel level = (ServerLevel) victim.level();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(2.5),
                    e -> e != victim && SkillEffects.isEnemy(owner, e))) {
                e.hurt(pet.damageSources().mobAttack(pet), dmg);
            }
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 1, 0, 0, 0, 0);
        } finally {
            SPLASH.set(false);
        }
    }

    private BeastEvents() {}
}
