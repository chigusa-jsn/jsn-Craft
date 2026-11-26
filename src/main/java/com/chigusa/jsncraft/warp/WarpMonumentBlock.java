package com.chigusa.jsncraft.warp;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * ワープのモニュメント（3×3、高さ6）の1マス。OPしか壊せない。どこを壊しても全体が消える。
 * 右クリックで行き先を選ぶ画面。
 */
public class WarpMonumentBlock extends BaseEntityBlock {
    public static final MapCodec<WarpMonumentBlock> CODEC = simpleCodec(WarpMonumentBlock::new);
    public static final EnumProperty<MonumentPart> PART = EnumProperty.create("part", MonumentPart.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SLAB = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape CENTER = Shapes.or(SLAB, Block.box(3, 8, 3, 13, 16, 13));
    private static final VoxelShape PILLAR = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape TOP = Block.box(2, 0, 2, 14, 6, 14);

    public WarpMonumentBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(PART, MonumentPart.CENTER).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PART, FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == MonumentPart.CENTER ? new WarpMonumentBlockEntity(pos, state) : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(PART)) {
            case CENTER -> CENTER;
            case PILLAR -> PILLAR;
            case TOP -> TOP;
            default -> SLAB;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) Warps.openList(sp);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 0.0F;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) Monuments.onPartRemoved(level, pos, state);
        super.onRemove(state, level, pos, newState, moved);
    }

    /** 真ん中のまわりに光の粒 */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (state.getValue(PART) != MonumentPart.CENTER) return;
        double a = r.nextDouble() * Math.PI * 2;
        level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + Math.cos(a) * 1.9, pos.getY() + 0.6, pos.getZ() + 0.5 + Math.sin(a) * 1.9, 0, 0.02, 0);
        if (r.nextInt(3) == 0)
            level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.6, pos.getY() + 5.2, pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
    }
}
