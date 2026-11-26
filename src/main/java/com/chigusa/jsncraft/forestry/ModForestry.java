package com.chigusa.jsncraft.forestry;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/** 伐り手の葉っぱの品（落ち葉・腐葉土）と、農家の土（豊穣の土）。 */
public final class ModForestry {
    /** 落ち葉：伐り手が葉っぱを壊すと出る。骨粉や腐葉土の材料 */
    public static final DeferredItem<Item> FALLEN_LEAVES = ModEntities.ITEMS.register("fallen_leaves", () -> new Item(new Item.Properties()));

    /** 腐葉土：作物がよく育つ土（耕さずにそのまま植えられる） */
    public static final DeferredBlock<SoilBlock> LEAF_MOLD = ModCooking.BLOCKS.register("leaf_mold",
            () -> new SoilBlock(1, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6F).sound(SoundType.ROOTED_DIRT).randomTicks()));
    public static final DeferredItem<BlockItem> LEAF_MOLD_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(LEAF_MOLD);

    /** 豊穣の土：もっとよく育ち、たまに作物が巨大になる（農家の巨大作物） */
    public static final DeferredBlock<SoilBlock> RICH_SOIL = ModCooking.BLOCKS.register("rich_soil",
            () -> new SoilBlock(2, BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(0.7F).sound(SoundType.ROOTED_DIRT).randomTicks()));
    public static final DeferredItem<BlockItem> RICH_SOIL_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(RICH_SOIL);

    public static void init() {}

    private ModForestry() {}
}
