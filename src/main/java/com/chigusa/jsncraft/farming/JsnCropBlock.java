package com.chigusa.jsncraft.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/**
 * jsn-Craft の作物。perennial（果樹：ブドウ・レモン・梅）は、実ったら右クリックで実だけ取れて、木は残る。
 */
public class JsnCropBlock extends CropBlock {
    /** 果樹の実を取ったあとの成長段階 */
    public static final int REGROW_AGE = 4;

    private final Supplier<? extends ItemLike> seed;
    private final Supplier<? extends ItemLike> fruit;
    private final boolean perennial;

    public JsnCropBlock(Properties props, Supplier<? extends ItemLike> seed, Supplier<? extends ItemLike> fruit, boolean perennial) {
        super(props);
        this.seed = seed;
        this.fruit = fruit;
        this.perennial = perennial;
    }

    public boolean perennial() {
        return perennial;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!perennial || !isMaxAge(state)) return super.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide) {
            Block.popResource(level, pos, new ItemStack(fruit.get(), 2 + level.random.nextInt(2)));
            level.setBlock(pos, getStateForAge(REGROW_AGE), 2);
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
