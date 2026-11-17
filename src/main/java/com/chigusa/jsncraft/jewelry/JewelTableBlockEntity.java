package com.chigusa.jsncraft.jewelry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 細工台の中身：枠0 金属・原石・装飾品・素材A / 枠1〜3 宝石（特別では枠1が素材B）。 */
public class JewelTableBlockEntity extends BlockEntity {
    private final SimpleContainer items = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();
            JewelTableBlockEntity.this.setChanged();
        }
    };

    public JewelTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModJewelry.TABLE_BE.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public ItemStack get(int slot) {
        return items.getItem(slot);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items.getItems(), registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.getItems(), registries);
    }
}
