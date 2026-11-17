package com.chigusa.jsncraft.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 酒樽の中身。枠 0〜3 が材料、4 が出来上がり。
 * 仕込んだ時刻を覚えておくので、遠くに行って読み込まれていない間も熟成は進む。
 */
public class BrewingBarrelBlockEntity extends BlockEntity {
    public static final int INGREDIENTS = 4, OUTPUT = 4, SIZE = 5;

    private final SimpleContainer items = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            BrewingBarrelBlockEntity.this.setChanged();
        }
    };
    @Nullable private ResourceLocation recipe;
    private int baseQuality, maxQuality, time;
    private long start;

    public BrewingBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModCooking.BREWING_BARREL_BE.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public List<ItemStack> ingredients() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < INGREDIENTS; i++) list.add(items.getItem(i));
        return list;
    }

    @Nullable
    public ResourceLocation recipe() {
        return recipe;
    }

    public boolean isBrewing() {
        return recipe != null;
    }

    public long elapsed() {
        return level == null ? 0 : level.getGameTime() - start;
    }

    public int time() {
        return time;
    }

    public boolean ready() {
        return isBrewing() && elapsed() >= time;
    }

    /** 今出したときの★（熟成が終わってから time ごとに1つ上がる） */
    public int currentQuality() {
        if (!ready()) return 0;
        long extra = (elapsed() - time) / Math.max(1, time);
        return (int) Math.min(maxQuality, baseQuality + extra);
    }

    public boolean atMax() {
        return currentQuality() >= maxQuality;
    }

    /** 0〜1000：熟成が終わるまで、または次の★までの進み具合 */
    public int permille() {
        if (!isBrewing()) return 0;
        long e = elapsed();
        if (e < time) return (int) (e * 1000 / Math.max(1, time));
        if (atMax()) return 1000;
        return (int) (((e - time) % Math.max(1, time)) * 1000 / Math.max(1, time));
    }

    public void start(ResourceLocation recipe, int baseQuality, int maxQuality, int time) {
        this.recipe = recipe;
        this.baseQuality = baseQuality;
        this.maxQuality = maxQuality;
        this.time = Math.max(20, time);
        this.start = level == null ? 0 : level.getGameTime();
        setChanged();
    }

    public void clear() {
        recipe = null;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items.getItems(), registries);
        if (recipe != null) {
            tag.putString("recipe", recipe.toString());
            tag.putInt("base", baseQuality);
            tag.putInt("max", maxQuality);
            tag.putInt("time", time);
            tag.putLong("start", start);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clearContent();
        ContainerHelper.loadAllItems(tag, items.getItems(), registries);
        recipe = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
        baseQuality = tag.getInt("base");
        maxQuality = tag.getInt("max");
        time = tag.getInt("time");
        start = tag.getLong("start");
    }
}
