package com.chigusa.jsncraft.farming;

import com.chigusa.jsncraft.job.JobService;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * 巨大作物（3×3 の畑がまとめて1つの大きな作物になる）。壊すと、作物 27 個ぶん（＋種）が取れる。
 * 豊穣の土の上で、実った同じ作物が 3×3 に並んでいると、ときどき合体してできる。
 */
public class GiantCropBlock extends Block {
    public static final MapCodec<GiantCropBlock> CODEC = simpleCodec(GiantCropBlock::new);
    public static final EnumProperty<GiantKind> KIND = EnumProperty.create("kind", GiantKind.class);
    private static final VoxelShape SHAPE = Shapes.box(-1, 0, -1, 2, 1.4, 2);

    public GiantCropBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(KIND, GiantKind.WHEAT));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(KIND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        GiantKind k = state.getValue(KIND);
        List<ItemStack> out = new ArrayList<>();
        int n = 27;
        while (n > 0) {
            int c = Math.min(64, n);
            out.add(new ItemStack(k.produce(), c));
            n -= c;
        }
        if (k.seed() != null) out.add(new ItemStack(k.seed(), 4));
        return out;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player instanceof ServerPlayer sp && !sp.isCreative()) {
            JobService.onAction(sp, rule -> rule.action().equals("harvest") && rule.matchesBlock(state));
            level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** 豊穣の土の上の作物（pos）を中心に、3×3 が実った同じ作物なら巨大作物にする */
    public static boolean tryForm(ServerLevel level, BlockPos center) {
        BlockState st = level.getBlockState(center);
        GiantKind kind = GiantKind.of(st.getBlock());
        if (kind == null) return false;
        int rank = 1;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos p = center.offset(dx, 0, dz);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() != st.getBlock() || !(s.getBlock() instanceof net.minecraft.world.level.block.CropBlock c) || !c.isMaxAge(s)) return false;
            if (!(level.getBlockState(p.below()).getBlock() instanceof com.chigusa.jsncraft.forestry.SoilBlock soil) || soil.grade < 2) return false;
            rank = Math.max(rank, CropRanks.at(level, p));
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos p = center.offset(dx, 0, dz);
            CropRanks.set(level, p, 1);
            if (dx != 0 || dz != 0) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(center, ModFarming.GIANT_CROP.get().defaultBlockState().setValue(KIND, kind), 3);
        CropRanks.set(level, center, Math.min(5, rank + 1));
        level.playSound(null, center, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 0.8F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 40, 1.2, 0.6, 1.2, 0);
        return true;
    }
}
