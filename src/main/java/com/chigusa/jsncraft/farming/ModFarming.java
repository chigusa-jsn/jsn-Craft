package com.chigusa.jsncraft.farming;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** 農家の品：豊穣の骨粉・巨大作物・交配台・黄金の食材。 */
public final class ModFarming {
    public static final DeferredItem<FertileBoneMealItem> FERTILE_BONE_MEAL = ModEntities.ITEMS.register("fertile_bone_meal",
            () -> new FertileBoneMealItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredBlock<GiantCropBlock> GIANT_CROP = ModCooking.BLOCKS.register("giant_crop",
            () -> new GiantCropBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(1.0F).sound(SoundType.CROP).noOcclusion()));

    public static final DeferredBlock<CrossbreedBlock> CROSSBREED = ModCooking.BLOCKS.register("crossbreed_table",
            () -> new CrossbreedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> CROSSBREED_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(CROSSBREED);
    public static final Supplier<BlockEntityType<CrossbreedBlockEntity>> CROSSBREED_BE = ModCooking.BLOCK_ENTITIES.register("crossbreed_table",
            () -> BlockEntityType.Builder.of(CrossbreedBlockEntity::new, CROSSBREED.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CrossbreedMenu>> CROSSBREED_MENU = ModCooking.MENUS.register("crossbreed_table",
            () -> IMenuTypeExtension.create(CrossbreedMenu::new));

    private static DeferredItem<Item> golden(String id, int food, float sat) {
        return ModEntities.ITEMS.register(id, () -> new Item(new Item.Properties().rarity(Rarity.RARE)
                .food(new FoodProperties.Builder().nutrition(food).saturationModifier(sat).build())));
    }

    public static final DeferredItem<Item> GOLDEN_RICE = golden("golden_rice", 3, 0.8F);
    public static final DeferredItem<Item> GOLDEN_WHEAT = ModEntities.ITEMS.register("golden_wheat", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> GOLDEN_POTATO = golden("golden_potato", 4, 0.9F);
    public static final DeferredItem<Item> GOLDEN_TOMATO = golden("golden_tomato", 5, 0.9F);
    public static final List<DeferredItem<Item>> GOLDEN = List.of(GOLDEN_RICE, GOLDEN_WHEAT, GOLDEN_POTATO, GOLDEN_TOMATO);

    public static boolean isGolden(ItemStack st) {
        for (var g : GOLDEN) if (st.is(g.get())) return true;
        return false;
    }

    /** 黄金の作物（ランク6）を収穫したときの黄金の食材（なれない作物は null） */
    @Nullable
    public static Item goldenFor(BlockState st) {
        Block b = st.getBlock();
        if (b == Blocks.WHEAT) return GOLDEN_WHEAT.get();
        if (b == Blocks.POTATOES) return GOLDEN_POTATO.get();
        if (b == ModCrops.CROP_BLOCKS.get("rice").get()) return GOLDEN_RICE.get();
        if (b == ModCrops.CROP_BLOCKS.get("tomato").get()) return GOLDEN_TOMATO.get();
        return null;
    }

    /** 黄金の食材のもとの作物 */
    public static Item goldenBase(Item golden) {
        if (golden == GOLDEN_WHEAT.get()) return Items.WHEAT;
        if (golden == GOLDEN_POTATO.get()) return Items.POTATO;
        if (golden == GOLDEN_RICE.get()) return ModCrops.PRODUCE.get("rice").get();
        return ModCrops.PRODUCE.get("tomato").get();
    }

    /** ★5 どうしの交配で「黄金」に進化できる種か */
    public static boolean evolvable(ItemStack seed) {
        return seed.is(Items.WHEAT_SEEDS) || seed.is(Items.POTATO) || seed.is(ModCrops.PRODUCE.get("rice").get()) || seed.is(ModCrops.SEEDS.get("tomato").get());
    }

    /** 交配できる種（植えると作物になる物） */
    public static boolean isSeed(ItemStack st) {
        return st.getItem() instanceof net.minecraft.world.item.BlockItem bi
                && (bi.getBlock() instanceof net.minecraft.world.level.block.CropBlock || bi.getBlock() instanceof net.minecraft.world.level.block.StemBlock);
    }

    public static void init() {}

    private ModFarming() {}
}
