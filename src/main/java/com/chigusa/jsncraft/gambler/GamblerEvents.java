package com.chigusa.jsncraft.gambler;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * ギャンブラーのパッシブと一時的な効果（ダメージの増減）。
 *  ツキ（gamble_lucky）：たまに「ラッキー！」でダメージ1.5倍
 *  ラッキーセブン（gamble_lucky_seven）：7回目の攻撃が大ダメージ
 *  大勝負（gamble_high_roll）：体力が少ないほど強くなる
 *  コイントスの表・オールイン・ダブルアップ・胴元の掟
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class GamblerEvents {

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        long now = target.level().getGameTime();
        // 胴元の掟：掟の中の敵が、掟の主を攻撃するときは弱くなる
        if (target instanceof ServerPlayer owner && event.getSource().getEntity() instanceof LivingEntity src && !(src instanceof ServerPlayer)) {
            CompoundTag d = owner.getPersistentData();
            if (d.getLong(GamblerSkills.HOUSE_UNTIL) > now && src.distanceTo(owner) <= d.getDouble(GamblerSkills.HOUSE_POW + "_r")) {
                event.setAmount(event.getAmount() * (float) (1 - d.getDouble(GamblerSkills.HOUSE_POW) * 0.5));
            }
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || target == p) return;
        CompoundTag d = p.getPersistentData();
        float mult = 1;
        if (d.getLong(GamblerSkills.COIN_UNTIL) > now) mult += (float) d.getDouble(GamblerSkills.COIN_UNTIL + "_pow");
        if (d.getLong(GamblerSkills.ALLIN_UNTIL) > now) mult += (float) d.getDouble(GamblerSkills.ALLIN_POW);
        if (d.getLong(GamblerSkills.HOUSE_UNTIL) > now && target.distanceTo(p) <= d.getDouble(GamblerSkills.HOUSE_POW + "_r")) {
            mult += (float) d.getDouble(GamblerSkills.HOUSE_POW);
        }
        // ダブルアップ：次の一撃が2倍か半分
        if (d.getLong(GamblerSkills.DOUBLE_UNTIL) > now) {
            d.remove(GamblerSkills.DOUBLE_UNTIL);
            boolean win = GamblerSkills.luckyRoll(p, d.getDouble(GamblerSkills.DOUBLE_UNTIL + "_chance"));
            mult *= win ? 2 : 0.5F;
            GamblerSkills.say(p, win ? "§6§lダブルアップ成功！ ×2" : "§7ダブルアップ失敗… ×0.5");
            p.level().playSound(null, p.blockPosition(), win ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
        }
        // 大勝負：体力が少ないほど強い
        SkillDefinition high = SkillService.passive(p, "gamble_high_roll");
        if (high != null) {
            float lost = 1 - p.getHealth() / p.getMaxHealth();
            mult += (float) (high.power() * lost);
        }
        // ふつうの攻撃だけ数える（スキルの連続ヒットは数えない）
        boolean melee = event.getSource().getDirectEntity() == p && !com.chigusa.jsncraft.skill.HitFlags.SKILL.get();
        SkillDefinition seven = SkillService.passive(p, "gamble_lucky_seven");
        if (seven != null && melee) {
            int n = d.getInt("jsn_gamble_seven") + 1;
            if (n >= 7) {
                n = 0;
                mult += (float) seven.power();
                GamblerSkills.say(p, "§c§l7 7 7 ！ ラッキーセブン！");
                p.serverLevel().sendParticles(GamblerSkills.GOLD, target.getX(), target.getY(0.8), target.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
                p.level().playSound(null, target.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 2.0F);
            }
            d.putInt("jsn_gamble_seven", n);
        }
        SkillDefinition lucky = SkillService.passive(p, "gamble_lucky");
        if (lucky != null && GamblerSkills.luckyRoll(p, lucky.chance())) {
            mult += (float) lucky.power();
            p.serverLevel().sendParticles(GamblerSkills.GOLD, target.getX(), target.getY(1.0), target.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
            if (melee) GamblerSkills.say(p, "§eラッキー！");
        }
        if (mult != 1) event.setAmount(event.getAmount() * mult);
    }

    private GamblerEvents() {}
}
