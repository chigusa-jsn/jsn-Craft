package com.chigusa.jsncraft.jewelry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 宝珠ばさみ：持って防具の上で右クリックすると、はまっている宝珠を外す（細工師だけ）。 */
public class OrbTweezersItem extends Item {
    public OrbTweezersItem(Properties p) {
        super(p);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack tool, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack armor = slot.getItem();
        OrbItem orb = Orbs.orbOf(armor);
        if (orb == null) return false;
        if (player instanceof ServerPlayer sp) {
            if (Orbs.jewelLevel(sp) < 0) {
                sp.displayClientMessage(Component.literal("宝珠を外せるのは細工師だけ").withStyle(ChatFormatting.RED), true);
                return true;
            }
            Orbs.setOrb(armor, null);
            slot.setChanged();
            ItemStack back = new ItemStack(orb);
            if (!sp.getInventory().add(back)) sp.drop(back, false);
            tool.hurtAndBreak(1, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("持って防具の上で右クリックで、宝珠を外す（細工師だけ）").withStyle(ChatFormatting.GRAY));
    }
}
