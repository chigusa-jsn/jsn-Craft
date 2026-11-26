package com.chigusa.jsncraft.warp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** モニュメントの真ん中：上に浮かぶクリスタルを描くためだけのもの */
public class WarpMonumentBlockEntity extends BlockEntity {
    public WarpMonumentBlockEntity(BlockPos pos, BlockState state) {
        super(Monuments.BE.get(), pos, state);
    }
}
