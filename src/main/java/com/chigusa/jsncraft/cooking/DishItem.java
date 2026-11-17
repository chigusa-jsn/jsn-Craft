package com.chigusa.jsncraft.cooking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 調理台で作る料理。品質（★）とバフは、作ったときにアイテムに書き込まれる。
 * 食べるとバフがかかる。
 */
public class DishItem extends Item {

    public DishItem() {
        this(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build()));
    }

    protected DishItem(Item.Properties props) {
        super(props);
    }

    public static int alcohol(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("jsn_alcohol");
    }

    public static int quality(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("jsn_quality");
    }

    public static List<FoodBuff> buffs(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<FoodBuff> list = new ArrayList<>();
        for (Tag t : tag.getList("jsn_buffs", Tag.TAG_COMPOUND)) list.add(FoodBuff.load((CompoundTag) t));
        return list;
    }

    /** 品質とバフを書き込む */
    public static void write(ItemStack stack, int quality, List<FoodBuff> buffs) {
        write(stack, quality, buffs, 0);
    }

    public static void write(ItemStack stack, int quality, List<FoodBuff> buffs, int alcohol) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("jsn_quality", quality);
        if (alcohol > 0) tag.putInt("jsn_alcohol", alcohol);
        var list = new net.minecraft.nbt.ListTag();
        for (FoodBuff b : buffs) list.add(b.save());
        tag.put("jsn_buffs", list);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String stars(int quality) {
        int q = Math.max(1, quality);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= Math.max(3, q); i++) sb.append(i <= q ? "★" : "☆");
        return sb.toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int q = quality(stack);
        if (q > 0) {
            ChatFormatting color = q >= 4 ? ChatFormatting.LIGHT_PURPLE : q == 3 ? ChatFormatting.GOLD : q == 2 ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
            tooltip.add(Component.literal(stars(q) + (q >= 4 ? " 至高" : "")).withStyle(color));
        }
        for (FoodBuff b : buffs(stack)) tooltip.add(Component.literal(" ").append(b.describe()));
        int alc = alcohol(stack);
        if (alc > 0) tooltip.add(Component.literal("お酒の強さ " + "●".repeat(Math.min(5, alc)) + "○".repeat(Math.max(0, 5 - alc))).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (q == 0) tooltip.add(Component.literal("調理台で炎の調理師が作る料理").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** しゃがみ右クリックで、ブロックの上に料理を置く */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
        var player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown() || ctx.getClickedFace() != net.minecraft.core.Direction.UP) return super.useOn(ctx);
        Level level = ctx.getLevel();
        net.minecraft.core.BlockPos pos = ctx.getClickedPos().above();
        if (!level.getBlockState(pos).canBeReplaced()) return net.minecraft.world.InteractionResult.FAIL;
        if (!level.isClientSide) {
            var state = ModCooking.PLACED_DISH.get().defaultBlockState()
                    .setValue(PlacedDishBlock.FACING, player.getDirection().getOpposite());
            level.setBlock(pos, state, 3);
            if (level.getBlockEntity(pos) instanceof PlacedDishBlockEntity be) be.setDish(ctx.getItemInHand().copyWithCount(1));
            if (!player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOOD_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.4F);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) CookingService.onEat(player, stack);
        return super.finishUsingItem(stack, level, entity);
    }
}
