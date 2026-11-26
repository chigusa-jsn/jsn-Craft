package com.chigusa.jsncraft.farming;

import com.chigusa.jsncraft.job.JobLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** 豊穣の骨粉：作物を一気に実らせる。農家が使うと、その作物のランクが1つ上がる（★5まで）。 */
public class FertileBoneMealItem extends Item {
    public FertileBoneMealItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        BlockState st = ctx.getLevel().getBlockState(pos);
        if (!(st.getBlock() instanceof CropBlock crop)) return InteractionResult.PASS;
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        level.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), 3);
        int r = CropRanks.at(level, pos);
        if (JobLevels.farmer(p) >= 0 && r < 5) {
            CropRanks.set(level, pos, r + 1);
            p.displayClientMessage(Component.literal("作物ランク " + CropRanks.label(r + 1)).withStyle(ChatFormatting.GREEN), true);
        } else if (JobLevels.farmer(p) < 0) {
            p.displayClientMessage(Component.literal("実った！（ランクを上げられるのは農家だけ）").withStyle(ChatFormatting.GRAY), true);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0);
        level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.3F);
        if (!p.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("作物を一気に実らせる").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("農家が使うと、作物ランクが1つ上がる（★5まで）").withStyle(ChatFormatting.GREEN));
    }
}
