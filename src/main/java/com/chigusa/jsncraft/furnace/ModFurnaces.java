package com.chigusa.jsncraft.furnace;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** 大きい炉・コークス炉・コークスの登録。 */
public final class ModFurnaces {
    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                .lightLevel(s -> s.getValue(MachineBlock.LIT) ? 13 : 0);
    }

    public static final DeferredBlock<MachineBlock> BIG_FURNACE = ModCooking.BLOCKS.register("big_furnace", () -> new MachineBlock(MachineKind.BIG_FURNACE, stone()));
    public static final DeferredBlock<MachineBlock> COKE_OVEN = ModCooking.BLOCKS.register("coke_oven", () -> new MachineBlock(MachineKind.COKE_OVEN, stone()));
    public static final DeferredItem<BlockItem> BIG_FURNACE_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(BIG_FURNACE);
    public static final DeferredItem<BlockItem> COKE_OVEN_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(COKE_OVEN);
    public static final Supplier<BlockEntityType<MachineBlockEntity>> MACHINE_BE = ModCooking.BLOCK_ENTITIES.register("job_machine",
            () -> BlockEntityType.Builder.of(MachineBlockEntity::new, BIG_FURNACE.get(), COKE_OVEN.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MENU = ModCooking.MENUS.register("job_machine",
            () -> IMenuTypeExtension.create(MachineMenu::new));

    /** コークス：石炭の2倍よく燃える／コークスブロック：その10倍 */
    public static final DeferredItem<Item> COKE = ModEntities.ITEMS.register("coke", () -> new FuelItem(3200, new Item.Properties()));
    public static final DeferredBlock<Block> COKE_BLOCK = ModCooking.BLOCKS.register("coke_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5F, 6F).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> COKE_BLOCK_ITEM = ModEntities.ITEMS.register("coke_block", () -> new FuelBlockItem(COKE_BLOCK.get(), 32000, new Item.Properties()));

    public static class FuelItem extends Item {
        private final int burn;

        public FuelItem(int burn, Properties p) {
            super(p);
            this.burn = burn;
        }

        @Override
        public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> type) {
            return burn;
        }
    }

    public static class FuelBlockItem extends BlockItem {
        private final int burn;

        public FuelBlockItem(Block b, int burn, Properties p) {
            super(b, p);
            this.burn = burn;
        }

        @Override
        public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> type) {
            return burn;
        }
    }

    public static void init() {}

    private ModFurnaces() {}
}
