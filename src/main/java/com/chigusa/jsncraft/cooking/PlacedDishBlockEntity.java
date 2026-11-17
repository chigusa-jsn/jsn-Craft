package com.chigusa.jsncraft.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 置いた料理の中身（★やバフもそのまま残る）。 */
public class PlacedDishBlockEntity extends BlockEntity {
    private ItemStack dish = ItemStack.EMPTY;

    public PlacedDishBlockEntity(BlockPos pos, BlockState state) {
        super(ModCooking.PLACED_DISH_BE.get(), pos, state);
    }

    public ItemStack dish() {
        return dish;
    }

    public void setDish(ItemStack stack) {
        dish = stack;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!dish.isEmpty()) tag.put("dish", dish.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        dish = tag.contains("dish") ? ItemStack.parseOptional(registries, tag.getCompound("dish")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
