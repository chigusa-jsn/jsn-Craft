package com.chigusa.jsncraft.enchanting;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
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

import java.util.function.Supplier;

/** 魔紋の祭壇と魔紋の書の登録（登録先は調理台と共通）。 */
public final class ModEnchanting {

    public static final DeferredBlock<RuneAltarBlock> ALTAR = ModCooking.BLOCKS.register("rune_altar",
            () -> new RuneAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0F, 1200F)
                    .sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 9).noOcclusion()));
    public static final DeferredItem<BlockItem> ALTAR_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(ALTAR);
    public static final Supplier<BlockEntityType<RuneAltarBlockEntity>> ALTAR_BE = ModCooking.BLOCK_ENTITIES.register(
            "rune_altar", () -> BlockEntityType.Builder.of(RuneAltarBlockEntity::new, ALTAR.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<RuneAltarMenu>> ALTAR_MENU = ModCooking.MENUS.register("rune_altar",
            () -> IMenuTypeExtension.create(RuneAltarMenu::new));

    public static final DeferredItem<Item> RUNE_BOOK = ModEntities.ITEMS.register("rune_book",
            () -> new RuneBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static void init() {}

    private ModEnchanting() {}
}
