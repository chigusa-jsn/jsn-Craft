package com.chigusa.jsncraft.warp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** ワープの石碑：右クリックで行き先を選ぶ。OP（クリエイティブ）しか置いたり壊したりできない。 */
public class WarpStoneBlock extends Block {
    public WarpStoneBlock(Properties p) {
        super(p);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) Warps.openList(sp);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return player.hasPermissions(2) && player.isCreative() ? 1.0F : 0.0F;
    }
}
