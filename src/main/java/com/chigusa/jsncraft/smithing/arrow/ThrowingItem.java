package com.chigusa.jsncraft.smithing.arrow;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 投げナイフ（重くて強い・半分くらい拾える）／手裏剣（軽くて連投できる）。 */
public class ThrowingItem extends Item {
    private final boolean knife;

    public ThrowingItem(boolean knife, Properties props) {
        super(props);
        this.knife = knife;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), knife ? SoundEvents.TRIDENT_THROW.value() : SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.6F, knife ? 1.4F : 1.8F);
        if (!level.isClientSide) {
            ThrownBlade blade = new ThrownBlade(level, player);
            blade.setItem(stack.copyWithCount(1));
            blade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, knife ? 1.8F : 2.2F, 0.6F);
            level.addFreshEntity(blade);
        }
        player.getCooldowns().addCooldown(this, knife ? 12 : 6);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(knife ? "右クリックで投げる。ダメージ 6（半分くらいは拾える）" : "右クリックで投げる。ダメージ 3.5・すばやく連投できる")
                .withStyle(ChatFormatting.GRAY));
        tip.add(Quality.line(stack));
    }
}
