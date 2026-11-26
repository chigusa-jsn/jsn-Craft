package com.chigusa.jsncraft.tools;

import com.chigusa.jsncraft.jewelry.ModJewelry;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/**
 * ネザライトの先の道具：月銀（5段目）・星銀（6段目）のツルハシ・斧・スコップ・クワ、
 * そして段階のある釣り竿（鉄・ダイヤ・ネザライト・月銀・星銀）。
 * 星銀の道具は特別な力を持つ（しゃがんで3×3で掘る・3×3で耕す）。
 */
public final class ModTools {
    public static final Tier MOON = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2800, 10.5F, 4.5F, 18,
            () -> Ingredient.of(ModJewelry.MOON_SILVER.get()));
    public static final Tier STAR = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 13.0F, 5.5F, 22,
            () -> Ingredient.of(ModJewelry.STAR_SILVER.get()));

    private static Item.Properties props(Tier t) {
        return new Item.Properties().fireResistant().rarity(t == STAR ? Rarity.EPIC : Rarity.RARE);
    }

    public static final DeferredItem<PickaxeItem> MOON_PICKAXE = ModEntities.ITEMS.register("moon_silver_pickaxe",
            () -> new PickaxeItem(MOON, props(MOON).attributes(PickaxeItem.createAttributes(MOON, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> MOON_AXE = ModEntities.ITEMS.register("moon_silver_axe",
            () -> new AxeItem(MOON, props(MOON).attributes(AxeItem.createAttributes(MOON, 5.0F, -3.0F))));
    public static final DeferredItem<ShovelItem> MOON_SHOVEL = ModEntities.ITEMS.register("moon_silver_shovel",
            () -> new ShovelItem(MOON, props(MOON).attributes(ShovelItem.createAttributes(MOON, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> MOON_HOE = ModEntities.ITEMS.register("moon_silver_hoe",
            () -> new HoeItem(MOON, props(MOON).attributes(HoeItem.createAttributes(MOON, -4.5F, 0.0F))));

    public static final DeferredItem<PickaxeItem> STAR_PICKAXE = ModEntities.ITEMS.register("star_silver_pickaxe",
            () -> new PickaxeItem(STAR, props(STAR).attributes(PickaxeItem.createAttributes(STAR, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> STAR_AXE = ModEntities.ITEMS.register("star_silver_axe",
            () -> new AxeItem(STAR, props(STAR).attributes(AxeItem.createAttributes(STAR, 5.0F, -3.0F))));
    public static final DeferredItem<ShovelItem> STAR_SHOVEL = ModEntities.ITEMS.register("star_silver_shovel",
            () -> new ShovelItem(STAR, props(STAR).attributes(ShovelItem.createAttributes(STAR, 1.5F, -3.0F))));
    public static final DeferredItem<StarHoeItem> STAR_HOE = ModEntities.ITEMS.register("star_silver_hoe",
            () -> new StarHoeItem(STAR, props(STAR).attributes(HoeItem.createAttributes(STAR, -5.5F, 0.0F))));

    public static final List<DeferredItem<? extends Item>> TOOLS = List.of(MOON_PICKAXE, MOON_AXE, MOON_SHOVEL, MOON_HOE,
            STAR_PICKAXE, STAR_AXE, STAR_SHOVEL, STAR_HOE);

    // ---- 釣り竿 ----
    public static final DeferredItem<JsnRodItem> IRON_ROD = rod("iron_fishing_rod", 1, 160, Rarity.COMMON, false);
    public static final DeferredItem<JsnRodItem> DIAMOND_ROD = rod("diamond_fishing_rod", 2, 320, Rarity.UNCOMMON, false);
    public static final DeferredItem<JsnRodItem> NETHERITE_ROD = rod("netherite_fishing_rod", 3, 520, Rarity.RARE, true);
    public static final DeferredItem<JsnRodItem> MOON_ROD = rod("moon_silver_fishing_rod", 4, 800, Rarity.RARE, true);
    public static final DeferredItem<JsnRodItem> STAR_ROD = rod("star_silver_fishing_rod", 5, 1200, Rarity.EPIC, true);
    public static final List<DeferredItem<JsnRodItem>> RODS = List.of(IRON_ROD, DIAMOND_ROD, NETHERITE_ROD, MOON_ROD, STAR_ROD);

    // ---- 釣り竿カスタム台 ----
    public static final net.neoforged.neoforge.registries.DeferredBlock<RodTableBlock> ROD_TABLE = com.chigusa.jsncraft.cooking.ModCooking.BLOCKS.register("rod_table",
            () -> new RodTableBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(2.0F).sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ROD_TABLE_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(ROD_TABLE);
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<RodTableBlockEntity>> ROD_TABLE_BE =
            com.chigusa.jsncraft.cooking.ModCooking.BLOCK_ENTITIES.register("rod_table",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(RodTableBlockEntity::new, ROD_TABLE.get()).build(null));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<RodTableMenu>> ROD_TABLE_MENU =
            com.chigusa.jsncraft.cooking.ModCooking.MENUS.register("rod_table", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(RodTableMenu::new));

    private static DeferredItem<JsnRodItem> rod(String id, int tier, int dur, Rarity rarity, boolean fire) {
        return ModEntities.ITEMS.register(id, () -> {
            Item.Properties p = new Item.Properties().durability(dur).rarity(rarity);
            if (fire) p = p.fireResistant();
            return new JsnRodItem(tier, p);
        });
    }

    public static void init() {
        RodParts.init();
    }

    private ModTools() {}
}
