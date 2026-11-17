package com.chigusa.jsncraft.ranch;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/** エサ箱と畜産物の登録（ブロックの登録先は調理台と共通）。 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModRanch {

    public static final DeferredBlock<FeederBlock> FEEDER = ModCooking.BLOCKS.register("feeder",
            () -> new FeederBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> FEEDER_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(FEEDER);
    public static final Supplier<BlockEntityType<FeederBlockEntity>> FEEDER_BE = ModCooking.BLOCK_ENTITIES.register(
            "feeder", () -> BlockEntityType.Builder.of(FeederBlockEntity::new, FEEDER.get()).build(null));

    private static FoodProperties raw(int n, float s) {
        return new FoodProperties.Builder().nutrition(n).saturationModifier(s).build();
    }

    public static final DeferredItem<Item> FRESH_MILK = ModEntities.ITEMS.register("fresh_milk",
            () -> new FreshMilkItem(new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)));
    public static final DeferredItem<Item> PREMIUM_BEEF = ModEntities.ITEMS.registerSimpleItem("premium_beef", new Item.Properties().food(raw(4, 0.4F)).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> PREMIUM_PORK = ModEntities.ITEMS.registerSimpleItem("premium_pork", new Item.Properties().food(raw(4, 0.4F)).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> PREMIUM_MUTTON = ModEntities.ITEMS.registerSimpleItem("premium_mutton", new Item.Properties().food(raw(3, 0.4F)).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> PREMIUM_CHICKEN = ModEntities.ITEMS.registerSimpleItem("premium_chicken", new Item.Properties().food(raw(3, 0.4F)).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> GOLDEN_EGG = ModEntities.ITEMS.registerSimpleItem("golden_egg", new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

    public static void init() {}

    @SubscribeEvent
    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(FEEDER_ITEM);
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(FRESH_MILK);
            event.accept(PREMIUM_BEEF);
            event.accept(PREMIUM_PORK);
            event.accept(PREMIUM_MUTTON);
            event.accept(PREMIUM_CHICKEN);
            event.accept(GOLDEN_EGG);
        }
    }

    private ModRanch() {}
}
