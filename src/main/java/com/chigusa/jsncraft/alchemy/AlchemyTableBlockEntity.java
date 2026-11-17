package com.chigusa.jsncraft.alchemy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 調合台の中身：枠0 ビン / 枠1 主素材（錬金では素材A）/ 枠2〜4 追加素材（錬金では枠2が素材B）。 */
public class AlchemyTableBlockEntity extends BlockEntity {
    private final SimpleContainer items = new SimpleContainer(5) {
        @Override
        public void setChanged() {
            super.setChanged();
            AlchemyTableBlockEntity.this.setChanged();
        }
    };

    public AlchemyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModAlchemy.TABLE_BE.get(), pos, state);
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
