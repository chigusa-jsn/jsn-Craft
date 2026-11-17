package com.chigusa.jsncraft.enchanting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 魔紋の祭壇の中身：枠0 刻む物 / 枠1 ラピスラズリ / 枠2 触媒（本もここ）。 */
public class RuneAltarBlockEntity extends BlockEntity {
    private final SimpleContainer items = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            RuneAltarBlockEntity.this.setChanged();
        }
    };

    public RuneAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModEnchanting.ALTAR_BE.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public ItemStack item() {
        return items.getItem(0);
    }

    public ItemStack lapis() {
        return items.getItem(1);
    }

    public ItemStack catalyst() {
        return items.getItem(2);
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
