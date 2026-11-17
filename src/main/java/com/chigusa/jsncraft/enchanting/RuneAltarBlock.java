package com.chigusa.jsncraft.enchanting;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** 魔紋の祭壇。右クリックで魔紋を刻む・上限突破・付け替えの画面が開く（使えるのは付与術師だけ）。 */
public class RuneAltarBlock extends BaseEntityBlock {
    public static final MapCodec<RuneAltarBlock> CODEC = simpleCodec(RuneAltarBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RuneAltarBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RuneAltarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof RuneAltarBlockEntity be) {
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new RuneAltarMenu(id, inv, be), Component.literal("魔紋の祭壇")),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RuneAltarBlockEntity be) {
            Containers.dropContents(level, pos, be.items());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 1.1, cz = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.ENCHANT, cx + (r.nextDouble() - 0.5) * 1.6, cy + r.nextDouble() * 0.8,
                    cz + (r.nextDouble() - 0.5) * 1.6, (r.nextDouble() - 0.5) * 0.3, -0.4, (r.nextDouble() - 0.5) * 0.3);
        }
        if (r.nextInt(6) == 0) level.addParticle(ParticleTypes.WITCH, cx + (r.nextDouble() - 0.5) * 0.6, cy - 0.1, cz + (r.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
    }
}
