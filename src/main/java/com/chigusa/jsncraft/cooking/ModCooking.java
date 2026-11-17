package com.chigusa.jsncraft.cooking;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 調理台ブロック・料理アイテムの登録。 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCooking {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JsnCraft.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JsnCraft.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, JsnCraft.MODID);

    public static final DeferredBlock<CookingStationBlock> COOKING_STATION = BLOCKS.register("cooking_station",
            () -> new CookingStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F)
                    .sound(SoundType.STONE).lightLevel(s -> s.getValue(CookingStationBlock.LIT) ? 13 : 0).noOcclusion()));
    public static final DeferredItem<BlockItem> COOKING_STATION_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(COOKING_STATION);

    public static final Supplier<BlockEntityType<CookingStationBlockEntity>> COOKING_STATION_BE = BLOCK_ENTITIES.register(
            "cooking_station", () -> BlockEntityType.Builder.of(CookingStationBlockEntity::new, COOKING_STATION.get()).build(null));

    /** 置いた料理（アイテムはなし。料理を持ってしゃがみ右クリックで置く） */
    public static final DeferredBlock<PlacedDishBlock> PLACED_DISH = BLOCKS.register("placed_dish",
            () -> new PlacedDishBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.2F)
                    .sound(SoundType.WOOD).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final Supplier<BlockEntityType<PlacedDishBlockEntity>> PLACED_DISH_BE = BLOCK_ENTITIES.register(
            "placed_dish", () -> BlockEntityType.Builder.of(PlacedDishBlockEntity::new, PLACED_DISH.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<CookingMenu>> COOKING_MENU = MENUS.register("cooking_station",
            () -> IMenuTypeExtension.create(CookingMenu::new));

    /** 料理の一覧（ここに足すと登録される。絵は tools/gen_dish_art.py） */
    public static final String[] DISHES = {
            "onigiri", "edamame", "vegetable_soup", "grilled_fish_set", "miso_soup",
            "salad", "honey_toast", "meat_stew", "pancake", "lemonade",
            "mushroom_omelette", "yaki_onigiri", "fish_and_chips", "berry_tart", "cabbage_roll",
            "sashimi_platter", "curry_rice", "sushi", "omurice", "ramen",
            "miner_lunch", "warrior_steak", "pizza", "tempura", "grape_jelly",
            "gyudon", "hamburger", "ume_ochazuke", "gratin", "mapo_tofu",
            "lemon_chicken", "paella", "cheese_fondue", "golden_feast", "toro_don",
            "swordfish_steak", "anglerfish_hotpot", "eel_kabayaki", "golden_carp_nitsuke", "glacier_sashimi",
            "coelacanth_stew", "crimson_curry", "soul_eater_skewer", "void_jelly_dessert", "moon_lord_sashimi",
            "storm_king_steak", "ice_queen_carpaccio", "inferno_dragon_roast", "soulflame_nabe", "ender_dragon_feast"};

    public static final List<DeferredItem<Item>> DISH_ITEMS = new ArrayList<>();

    static {
        for (String d : DISHES) DISH_ITEMS.add(ModEntities.ITEMS.register("dish/" + d, DishItem::new));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
    }

    @SubscribeEvent
    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(COOKING_STATION_ITEM);
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) for (var d : DISH_ITEMS) event.accept(d);
    }

    private ModCooking() {}
}
