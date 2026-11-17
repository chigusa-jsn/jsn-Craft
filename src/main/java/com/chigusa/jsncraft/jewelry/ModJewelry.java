package com.chigusa.jsncraft.jewelry;

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** 細工台・原石・宝石・月銀と星銀・装飾品の登録（ブロックの登録先は調理台と共通）。 */
public final class ModJewelry {

    public static final DeferredBlock<JewelTableBlock> TABLE = ModCooking.BLOCKS.register("jeweler_table",
            () -> new JewelTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F)
                    .sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<BlockItem> TABLE_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(TABLE);
    public static final Supplier<BlockEntityType<JewelTableBlockEntity>> TABLE_BE = ModCooking.BLOCK_ENTITIES.register(
            "jeweler_table", () -> BlockEntityType.Builder.of(JewelTableBlockEntity::new, TABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<JewelMenu>> TABLE_MENU = ModCooking.MENUS.register("jeweler_table",
            () -> IMenuTypeExtension.create(JewelMenu::new));

    public static final DeferredItem<Item> MOON_SILVER = ModEntities.ITEMS.registerSimpleItem("moon_silver_ingot", new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> STAR_SILVER = ModEntities.ITEMS.registerSimpleItem("star_silver_ingot", new Item.Properties().rarity(Rarity.EPIC));

    /** 原石と宝石（Jewels.GEMS と同じ順番） */
    public static final List<DeferredItem<Item>> RAW_GEMS = new ArrayList<>();
    public static final List<DeferredItem<Item>> GEMS = new ArrayList<>();
    /** 装飾品："台座_種類" → アイテム */
    public static final Map<String, DeferredItem<Item>> JEWELRY = new LinkedHashMap<>();
    /** 特別な装飾品（Jewels.UNIQUES と同じ順番） */
    public static final List<DeferredItem<Item>> UNIQUES = new ArrayList<>();

    static {
        for (Jewels.Gem g : Jewels.GEMS) {
            Rarity r = g.rarity() == 2 ? Rarity.RARE : g.rarity() == 1 ? Rarity.UNCOMMON : Rarity.COMMON;
            RAW_GEMS.add(ModEntities.ITEMS.registerSimpleItem("raw_" + g.id(), new Item.Properties()));
            GEMS.add(ModEntities.ITEMS.registerSimpleItem(g.id(), new Item.Properties().rarity(r)));
        }
        for (Jewels.Tier t : Jewels.TIERS) {
            for (Jewels.Kind k : Jewels.KINDS) {
                JEWELRY.put(t.id() + "_" + k.id(), ModEntities.ITEMS.register(t.id() + "_" + k.id(), () -> new JewelryItem(k, t)));
            }
        }
        for (Jewels.Unique u : Jewels.UNIQUES) UNIQUES.add(ModEntities.ITEMS.register(u.id(), () -> new JewelryItem(u)));
    }

    public static Item jewelry(Jewels.Tier t, Jewels.Kind k) {
        return JEWELRY.get(t.id() + "_" + k.id()).get();
    }

    /** 宝石のアイテムから種類を探す（原石なら raw = true のときだけ） */
    public static Jewels.Gem gemOf(Item item, boolean raw) {
        List<DeferredItem<Item>> list = raw ? RAW_GEMS : GEMS;
        for (int i = 0; i < list.size(); i++) if (list.get(i).get() == item) return Jewels.GEMS.get(i);
        return null;
    }

    public static void init() {}

    private ModJewelry() {}
}
