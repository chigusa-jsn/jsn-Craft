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

/** 宝珠。持って防具の上で右クリックすると、はめる（細工師だけ。前の宝珠は持ち物に戻る）。 */
public class OrbItem extends Item {
    public final Jewels.Gem gem;
    public final int grade;

    public OrbItem(Jewels.Gem gem, int grade, Properties p) {
        super(p);
        this.gem = gem;
        this.grade = grade;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(gem.name() + "の" + Orbs.GRADE_NAME[grade - 1]);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack orb, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack armor = slot.getItem();
        if (!Orbs.socketable(armor)) return false;
        if (player instanceof ServerPlayer sp) {
            int lv = Orbs.jewelLevel(sp);
            if (lv < 0) {
                sp.displayClientMessage(Component.literal("宝珠をはめられるのは細工師だけ").withStyle(ChatFormatting.RED), true);
                return true;
            }
            if (grade > Orbs.maxGrade(lv)) {
                sp.displayClientMessage(Component.literal(Orbs.GRADE_NAME[grade - 1] + "をはめるには、細工の腕がまだ足りない").withStyle(ChatFormatting.RED), true);
                return true;
            }
            OrbItem old = Orbs.orbOf(armor);
            if (old != null) {
                ItemStack back = new ItemStack(old);
                if (!sp.getInventory().add(back)) sp.drop(back, false);
            }
            Orbs.setOrb(armor, this);
            orb.shrink(1);
            slot.setChanged();
            sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, 1.4F);
            sp.displayClientMessage(Component.literal("宝珠をはめた！").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            com.chigusa.jsncraft.job.JobService.onAction(sp, rule -> rule.action().equals("jewel"), grade);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        StringBuilder sb = new StringBuilder();
        for (Jewels.Stat s : gem.stats()) sb.append(sb.length() > 0 ? "・" : "").append(Jewels.format(s, Orbs.MULT[grade - 1]));
        tip.add(Component.literal("防具にはめると：" + sb).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("持って防具の上で右クリック（細工師だけ）").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("はめられる腕：" + (grade == 1 ? "細工師" : grade == 2 ? "細工師Lv20" : "細工師Lv40・宝飾師")).withStyle(ChatFormatting.DARK_GRAY));
    }
}
