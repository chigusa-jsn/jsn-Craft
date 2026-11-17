package com.chigusa.jsncraft.smithing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 鍛冶台の中身：枠0 が鍛える物、枠1 が素材（強化石・触媒・修理素材）。 */
public class ForgeBlockEntity extends BlockEntity {
    private final SimpleContainer items = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            ForgeBlockEntity.this.setChanged();
        }
    };

    public ForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModSmithing.FORGE_BE.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public ItemStack item() {
        return items.getItem(0);
    }

    public ItemStack material() {
        return items.getItem(1);
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
