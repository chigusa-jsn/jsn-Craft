package com.chigusa.jsncraft.smithing.arrow;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** 三叉槍の穂先：持って三叉槍の上で右クリックで付け替え。 */
public class TridentTipItem extends Item {
    public enum Tip {
        FIRE("fire", "炎の穂先", "当てた敵を燃やす", ChatFormatting.RED),
        FROST("frost", "氷の穂先", "当てた敵を凍らせて遅くする", ChatFormatting.AQUA),
        STORM("storm", "雷の穂先", "投げて当てると雷が落ちる（晴れでも）", ChatFormatting.YELLOW),
        ABYSS("abyss", "深淵の穂先", "衰弱させて、自分の体力を少し吸い取る（名工）", ChatFormatting.DARK_PURPLE);

        public final String id, name, desc;
        public final ChatFormatting color;

        Tip(String id, String name, String desc, ChatFormatting color) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.color = color;
        }

        public static Tip byId(String id) {
            for (Tip t : values()) if (t.id.equals(id)) return t;
            return null;
        }
    }

    private final Tip tip;

    public TridentTipItem(Tip tip, Properties props) {
        super(props);
        this.tip = tip;
    }

    public static Tip tipOf(ItemStack trident) {
        return Tip.byId(trident.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("jsn_tip"));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack target = slot.getItem();
        if (!target.is(Items.TRIDENT)) return false;
        if (!player.level().isClientSide) {
            CustomData.update(DataComponents.CUSTOM_DATA, target, t -> t.putString("jsn_tip", tip.id));
            self.shrink(1);
            slot.setChanged();
            player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
            player.displayClientMessage(Component.literal("三叉槍に" + tip.name + "を付けた").withStyle(tip.color), true);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal(tip.desc).withStyle(ChatFormatting.GRAY));
        list.add(Component.literal("持って、三叉槍の上で右クリックで付け替え").withStyle(ChatFormatting.DARK_AQUA));
    }
}
