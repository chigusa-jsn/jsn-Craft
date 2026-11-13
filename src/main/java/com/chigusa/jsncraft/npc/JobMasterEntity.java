package com.chigusa.jsncraft.npc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 転職の神官。右クリックで転職の画面が開く。
 * 動き（倒れない・動かない・プレイヤーの方を向く）は受付嬢と同じ。
 */
public class JobMasterEntity extends ReceptionistEntity {

    public JobMasterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setCustomName(Component.literal("転職の神官"));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            JobChangeService.talkedTo(sp, this);
            JobChangeService.open(sp);
            playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.6F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
