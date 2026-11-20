package com.chigusa.jsncraft.chinchiro;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

public final class ModChinchiro {
    public static final DeferredBlock<ChinchiroTableBlock> TABLE = ModCooking.BLOCKS.register("chinchiro_versus", () -> new ChinchiroTableBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> TABLE_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(TABLE);
    public static final Supplier<BlockEntityType<ChinchiroTableBlockEntity>> TABLE_BE = ModCooking.BLOCK_ENTITIES.register(
            "chinchiro_versus", () -> BlockEntityType.Builder.of(ChinchiroTableBlockEntity::new, TABLE.get()).build(null));

    public static void init() {}

    private ModChinchiro() {}
}
