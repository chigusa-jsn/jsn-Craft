package com.chigusa.jsncraft.smithing;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.function.Supplier;

/** 鍛冶台と強化石の登録（ブロックなどの登録先は調理台と共通）。 */
public final class ModSmithing {

    public static final DeferredBlock<ForgeBlock> FORGE = ModCooking.BLOCKS.register("forge",
            () -> new ForgeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F)
                    .sound(SoundType.ANVIL).requiresCorrectToolForDrops().lightLevel(s -> 7).noOcclusion()));
    public static final DeferredItem<BlockItem> FORGE_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(FORGE);
    public static final Supplier<BlockEntityType<ForgeBlockEntity>> FORGE_BE = ModCooking.BLOCK_ENTITIES.register(
            "forge", () -> BlockEntityType.Builder.of(ForgeBlockEntity::new, FORGE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ForgeMenu>> FORGE_MENU = ModCooking.MENUS.register("forge",
            () -> IMenuTypeExtension.create(ForgeMenu::new));

    /** 強化石：0 鉄（+1〜3）/ 1 金（+4〜6）/ 2 ダイヤ（+7〜9）/ 3 ネザライト（+10〜12）/ 4 星鉄（+13〜15） */
    public static final List<DeferredItem<Item>> STONES = List.of(
            ModEntities.ITEMS.registerSimpleItem("enhance_stone_iron", new Item.Properties()),
            ModEntities.ITEMS.registerSimpleItem("enhance_stone_gold", new Item.Properties()),
            ModEntities.ITEMS.registerSimpleItem("enhance_stone_diamond", new Item.Properties().rarity(Rarity.UNCOMMON)),
            ModEntities.ITEMS.registerSimpleItem("enhance_stone_netherite", new Item.Properties().rarity(Rarity.RARE)),
            ModEntities.ITEMS.registerSimpleItem("enhance_stone_star", new Item.Properties().rarity(Rarity.EPIC)));

    // ---- 分解機 ----
    public static final DeferredBlock<RecyclerBlock> RECYCLER = ModCooking.BLOCKS.register("recycler",
            () -> new RecyclerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> RECYCLER_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(RECYCLER);
    public static final Supplier<BlockEntityType<RecyclerBlockEntity>> RECYCLER_BE = ModCooking.BLOCK_ENTITIES.register(
            "recycler", () -> BlockEntityType.Builder.of(RecyclerBlockEntity::new, RECYCLER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<RecyclerMenu>> RECYCLER_MENU = ModCooking.MENUS.register("recycler",
            () -> IMenuTypeExtension.create(RecyclerMenu::new));

    // ---- 砥石・修理キット・当て板（鍛冶台で鍛冶師が作る） ----
    public static final DeferredItem<RepairToolItem> WHETSTONE = ModEntities.ITEMS.register("whetstone",
            () -> new RepairToolItem(RepairToolItem.Kind.WHETSTONE, 0.25, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<RepairToolItem> WHETSTONE_ADV = ModEntities.ITEMS.register("whetstone_adv",
            () -> new RepairToolItem(RepairToolItem.Kind.WHETSTONE, 0.5, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<RepairToolItem> REPAIR_KIT = ModEntities.ITEMS.register("repair_kit",
            () -> new RepairToolItem(RepairToolItem.Kind.KIT, 0.25, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<RepairToolItem> REPAIR_KIT_ADV = ModEntities.ITEMS.register("repair_kit_adv",
            () -> new RepairToolItem(RepairToolItem.Kind.KIT, 0.5, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<RepairToolItem> ARMOR_PLATE = ModEntities.ITEMS.register("armor_plate",
            () -> new RepairToolItem(RepairToolItem.Kind.PLATE, 0, new Item.Properties().stacksTo(16)));

    public static final DeferredItem<RepairToolItem> WHETSTONE_ROUGH = ModEntities.ITEMS.register("whetstone_rough",
            () -> new RepairToolItem(RepairToolItem.Kind.WHETSTONE, 0.1, new Item.Properties().stacksTo(32)));
    public static final DeferredItem<RepairToolItem> REPAIR_KIT_SIMPLE = ModEntities.ITEMS.register("repair_kit_simple",
            () -> new RepairToolItem(RepairToolItem.Kind.KIT, 0.1, new Item.Properties().stacksTo(32)));
    public static final DeferredItem<RepairToolItem> IRON_PLATE = ModEntities.ITEMS.register("iron_plate",
            () -> new RepairToolItem(RepairToolItem.Kind.PLATE, 0, 2, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<RepairToolItem> HONE_WHITE = ModEntities.ITEMS.register("finishing_whetstone",
            () -> new RepairToolItem(RepairToolItem.Kind.HONE, 0, 1, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<RepairToolItem> HONE_PURPLE = ModEntities.ITEMS.register("divine_whetstone",
            () -> new RepairToolItem(RepairToolItem.Kind.HONE, 0, 2, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final DeferredItem<RepairToolItem> REPAIR_KIT_DIVINE = ModEntities.ITEMS.register("repair_kit_divine",
            () -> new RepairToolItem(RepairToolItem.Kind.KIT, 0.5, 2, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    /** 強化石の段階（強化石でなければ -1） */
    public static int stoneTier(ItemStack stack) {
        for (int i = 0; i < STONES.size(); i++) if (stack.is(STONES.get(i).get())) return i;
        return -1;
    }

    public static void init() {
        com.chigusa.jsncraft.smithing.arrow.ModArrows.init();
        com.chigusa.jsncraft.airship.ModAirships.init();
        com.chigusa.jsncraft.furnace.ModFurnaces.init();
        com.chigusa.jsncraft.forestry.ModForestry.init();
        com.chigusa.jsncraft.farming.ModFarming.init();
        com.chigusa.jsncraft.tools.ModTools.init();
        com.chigusa.jsncraft.capture.CaptureItems.init();
        com.chigusa.jsncraft.jewelry.Orbs.init();
        com.chigusa.jsncraft.enchanting.SelectEnchant.init();
    }

    private ModSmithing() {}
}
