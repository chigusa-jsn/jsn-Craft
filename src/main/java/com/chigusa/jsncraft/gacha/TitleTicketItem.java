package com.chigusa.jsncraft.gacha;

import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.quest.QuestService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/** 肩書チケット：使うとその肩書が手に入って、いまの肩書になる */
public class TitleTicketItem extends Item {
    public TitleTicketItem(Properties p) {
        super(p);
    }

    public static ItemStack of(String title) {
        ItemStack s = new ItemStack(ModGacha.TITLE_TICKET.get());
        CompoundTag t = new CompoundTag();
        t.putString("title", title);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    public static String title(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? "" : d.copyTag().getString("title");
    }

    @Override
    public Component getName(ItemStack stack) {
        String t = title(stack);
        return t.isEmpty() ? super.getName(stack) : Component.literal("肩書チケット《" + t + "》");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7右クリックで肩書《" + title(stack) + "》を手に入れる"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        String t = title(s);
        if (t.isEmpty()) return InteractionResultHolder.pass(s);
        if (player instanceof ServerPlayer sp) {
            boolean had = !QuestService.data(sp).titles.add(t);
            JobService.data(sp).setTitle(t);
            JobService.sync(sp);
            sp.sendSystemMessage(Component.literal(had ? "肩書を《" + t + "》にしたよ" : "肩書《" + t + "》を手に入れた！").withStyle(net.minecraft.ChatFormatting.GOLD));
            level.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);
            if (!had) s.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
    }
}
