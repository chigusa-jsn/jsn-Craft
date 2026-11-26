package com.chigusa.jsncraft.airship;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** 仕舞った飛空艇。地面に向けて使うと、そこに出てくる（中の荷物・燃料・色はそのまま）。 */
public class AirshipItem extends Item {
    private final int tier;

    public AirshipItem(int tier, Properties props) {
        super(props);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        Airship ship = ModAirships.AIRSHIP.get().create(sl);
        if (ship == null) return InteractionResult.FAIL;
        ItemStack stack = ctx.getItemInHand();
        ship.loadFrom(stack, player);
        ship.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player != null ? player.getYRot() : 0, 0);
        if (!sl.noCollision(ship, ship.getBoundingBox())) {
            if (player != null) player.displayClientMessage(Component.literal("ここは狭くて出せない（広い所で使おう）").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        sl.addFreshEntity(ship);
        sl.playSound(null, pos, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.NEUTRAL, 1.0F, 0.8F);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 30, 1.0, 0.8, 1.0, 0.05);
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        int seats = Airship.seatsOf(tier).size();
        tip.add(Component.literal(seats + "人乗り・荷物 " + 9 * tier + " 枠").withStyle(ChatFormatting.GRAY));
        CompoundTag t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound("jsn_airship");
        int fuel = t.getInt("fuel");
        int sec = fuel / 20;
        tip.add(Component.literal(String.format("燃料 %d:%02d", sec / 60, sec % 60)).withStyle(fuel > 0 ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        if (t.contains("Items")) tip.add(Component.literal("荷物が入っている").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("地面に向けて使うと出てくる").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("乗る：右クリック／荷物・燃料・仕舞う：しゃがんで右クリック").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("染料で気球、木材で船体の色が変わる").withStyle(ChatFormatting.DARK_GRAY));
        if (tier < 3) tip.add(Component.literal("鍛冶師が鍛冶台で大きくできる").withStyle(ChatFormatting.DARK_GRAY));
    }
}
