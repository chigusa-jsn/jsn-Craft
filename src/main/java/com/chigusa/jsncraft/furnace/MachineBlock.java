package com.chigusa.jsncraft.furnace;

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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** 職業の炉（大きい炉・コークス炉）。誰でも物を入れられるが、動かせるのはその職業の人だけ。 */
public class MachineBlock extends BaseEntityBlock {
    public static final MapCodec<MachineBlock> CODEC = simpleCodec(p -> new MachineBlock(MachineKind.BIG_FURNACE, p));
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public final MachineKind kind;

    public MachineBlock(MachineKind kind, Properties props) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
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
        return new MachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModFurnaces.MACHINE_BE.get(), MachineBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new MachineMenu(id, inv, be), Component.literal(kind.name)),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            Containers.dropContents(level, pos, be.items());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.3 + r.nextDouble() * 0.4, z = pos.getZ() + 0.3 + r.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.LARGE_SMOKE, x, pos.getY() + 1.02, z, 0, 0.05, 0);
        Direction f = state.getValue(FACING);
        double fx = pos.getX() + 0.5 + f.getStepX() * 0.52, fz = pos.getZ() + 0.5 + f.getStepZ() * 0.52;
        level.addParticle(ParticleTypes.FLAME, fx + (r.nextDouble() - 0.5) * 0.4 * Math.abs(f.getStepZ()), pos.getY() + 0.3, fz + (r.nextDouble() - 0.5) * 0.4 * Math.abs(f.getStepX()), 0, 0, 0);
        if (r.nextInt(8) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, net.minecraft.sounds.SoundEvents.BLASTFURNACE_FIRE_CRACKLE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F, false);
    }
}
