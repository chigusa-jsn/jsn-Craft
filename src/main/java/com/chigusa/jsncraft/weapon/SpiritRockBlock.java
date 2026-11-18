package com.chigusa.jsncraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 精霊の岩壁（アースウォールで出る岩）。決まった時間がたつと消える（予約tickなので、
 * セーブして入り直しても消える）。何も落とさない。
 */
public class SpiritRockBlock extends Block {

    public SpiritRockBlock(Properties props) {
        super(props);
    }

    /** 置いて、ticks 後に消える予約をする */
    public static boolean place(ServerLevel level, BlockPos pos, int ticks) {
        BlockState here = level.getBlockState(pos);
        if (!here.isAir() && !(here.canBeReplaced() && here.getFluidState().isEmpty())) return false;
        Block b = ModWeapons.SPIRIT_ROCK.get();
        level.setBlock(pos, b.defaultBlockState(), 3);
        level.scheduleTick(pos, b, ticks);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.01);
    }
}
