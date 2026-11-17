package com.chigusa.jsncraft.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 調理台の中身。枠 0〜3 が材料、4 が隠し味、5 が出来上がり。
 * 「調理する」を押すと材料を使い、決まった料理（pending）を time かけて仕上げる。
 */
public class CookingStationBlockEntity extends BlockEntity {
    public static final int INGREDIENTS = 4, SEASONING = 4, OUTPUT = 5, SIZE = 6;

    private final SimpleContainer items = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            CookingStationBlockEntity.this.setChanged();
        }
    };
    private ItemStack pending = ItemStack.EMPTY;
    private int progress, total;

    public CookingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModCooking.COOKING_STATION_BE.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public List<ItemStack> ingredients() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < INGREDIENTS; i++) list.add(items.getItem(i));
        return list;
    }

    public boolean isCooking() {
        return !pending.isEmpty();
    }

    public int progress() {
        return progress;
    }

    public int total() {
        return total;
    }

    /** 出来上がりの枠に入るか */
    public boolean outputFree() {
        ItemStack out = items.getItem(OUTPUT);
        return out.isEmpty();
    }

    public void start(ItemStack result, int time) {
        pending = result;
        total = Math.max(1, time);
        progress = 0;
        setLit(true);
        setChanged();
    }

    public void dropPending() {
        if (!pending.isEmpty() && level != null) Block.popResource(level, worldPosition, pending);
        pending = ItemStack.EMPTY;
    }

    private void setLit(boolean lit) {
        if (level != null && getBlockState().getValue(CookingStationBlock.LIT) != lit) {
            level.setBlock(worldPosition, getBlockState().setValue(CookingStationBlock.LIT, lit), 3);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CookingStationBlockEntity be) {
        if (be.pending.isEmpty()) return;
        if (be.progress < be.total) {
            be.progress++;
            return;
        }
        // 出来上がり：出来上がりの枠が空くまで待つ
        if (!be.outputFree()) return;
        be.items.setItem(OUTPUT, be.pending);
        be.pending = ItemStack.EMPTY;
        be.progress = 0;
        be.total = 0;
        be.setLit(false);
        level.playSound(null, pos, SoundEvents.PLAYER_BURP, SoundSource.BLOCKS, 0.6F, 1.4F);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 1.6F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    8, 0.3, 0.2, 0.3, 0);
        }
        be.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items.getItems(), registries);
        if (!pending.isEmpty()) tag.put("pending", pending.save(registries));
        tag.putInt("progress", progress);
        tag.putInt("total", total);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.getItems(), registries);
        pending = tag.contains("pending") ? ItemStack.parseOptional(registries, tag.getCompound("pending")) : ItemStack.EMPTY;
        progress = tag.getInt("progress");
        total = tag.getInt("total");
    }
}
