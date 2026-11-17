package com.chigusa.jsncraft.alchemy;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import java.util.List;
import java.util.function.Supplier;

/** 調合台・薬・香の素・投げた薬の登録（ブロックの登録先は調理台と共通）。 */
public final class ModAlchemy {

    public static final DeferredBlock<AlchemyTableBlock> TABLE = ModCooking.BLOCKS.register("alchemy_table",
            () -> new AlchemyTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F)
                    .sound(SoundType.WOOD).lightLevel(s -> 6).noOcclusion()));
    public static final DeferredItem<BlockItem> TABLE_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(TABLE);
    public static final Supplier<BlockEntityType<AlchemyTableBlockEntity>> TABLE_BE = ModCooking.BLOCK_ENTITIES.register(
            "alchemy_table", () -> BlockEntityType.Builder.of(AlchemyTableBlockEntity::new, TABLE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<AlchemyMenu>> TABLE_MENU = ModCooking.MENUS.register("alchemy_table",
            () -> IMenuTypeExtension.create(AlchemyMenu::new));

    /** 香の素：薬に入れると香になる */
    public static final DeferredItem<Item> DUST_BASE = ModEntities.ITEMS.registerSimpleItem("dust_base", new Item.Properties());

    /** 薬（種類ごと。Elixirs.TYPES と同じ順番） */
    public static final List<DeferredItem<Item>> ELIXIRS = new ArrayList<>();

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownElixir>> THROWN_ELIXIR = ModEntities.ENTITY_TYPES.register(
            "thrown_elixir", () -> EntityType.Builder.<ThrownElixir>of(ThrownElixir::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("thrown_elixir"));

    static {
        for (Elixirs.Type t : Elixirs.TYPES) {
            int i = t.index();
            ELIXIRS.add(ModEntities.ITEMS.register("elixir_" + t.id(), () -> new ElixirItem(i)));
        }
    }

    public static ItemStack make(Elixirs.Type t, ElixirData d, int count) {
        ItemStack s = new ItemStack(ELIXIRS.get(t.index()).get(), count);
        d.write(s);
        return s;
    }

    public static void init() {}

    private ModAlchemy() {}
}
