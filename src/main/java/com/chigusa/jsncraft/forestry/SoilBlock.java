package com.chigusa.jsncraft.forestry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.util.TriState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 作物がよく育つ土。耕さずにそのまま種を植えられる。
 * 段階 1（腐葉土）：ときどき上の作物を1段育てる / 段階 2（豊穣の土）：もっと育てる。巨大作物の土台にもなる。
 */
public class SoilBlock extends Block {
    public final int grade;

    public SoilBlock(int grade, Properties props) {
        super(props);
        this.grade = grade;
    }

    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPos, Direction facing, BlockState plant) {
        if (facing == Direction.UP && (plant.getBlock() instanceof CropBlock || plant.getBlock() instanceof StemBlock
                || plant.getBlock() instanceof net.minecraft.world.level.block.BushBlock)) return TriState.TRUE;
        return TriState.DEFAULT;
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        BlockPos up = pos.above();
        BlockState plant = level.getBlockState(up);
        if (grade >= 2 && r.nextFloat() < 0.04F && com.chigusa.jsncraft.farming.GiantCropBlock.tryForm(level, up)) return;
        if (!(plant.getBlock() instanceof BonemealableBlock b)) return;
        float chance = grade >= 2 ? 0.5F : 0.25F;
        if (r.nextFloat() < chance && b.isValidBonemealTarget(level, up, plant)) {
            plant.randomTick(level, up, r);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, up.getX() + 0.5, up.getY() + 0.4, up.getZ() + 0.5, 2, 0.25, 0.2, 0.25, 0);
        }
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx,
                                java.util.List<net.minecraft.network.chat.Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tip.add(net.minecraft.network.chat.Component.literal(grade >= 2 ? "作物がとてもよく育つ。たまに作物が巨大になる" : "作物がよく育つ")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tip.add(net.minecraft.network.chat.Component.literal("耕さずに、そのまま種を植えられる").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
