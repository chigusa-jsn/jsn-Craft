package com.chigusa.jsncraft.enchanting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 選べる付与台：枠0 に付ける物、枠1 にラピスラズリ。 */
public class SelectTableBlockEntity extends BlockEntity {
    private final SimpleContainer items = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            SelectTableBlockEntity.this.setChanged();
        }
    };

    public SelectTableBlockEntity(BlockPos pos, BlockState state) {
        super(SelectEnchant.BE.get(), pos, state);
    }

    public SimpleContainer items() { return items; }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        ContainerHelper.saveAllItems(tag, items.getItems(), reg);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.getItems(), reg);
    }
}
