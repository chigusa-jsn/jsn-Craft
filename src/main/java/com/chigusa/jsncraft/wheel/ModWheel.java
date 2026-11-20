package com.chigusa.jsncraft.wheel;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

public final class ModWheel {
    public static final DeferredBlock<BigWheelBlock> WHEEL = ModCooking.BLOCKS.register("big_wheel", () -> new BigWheelBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.5F).sound(SoundType.WOOD).lightLevel(s -> 10).noOcclusion()));
    public static final DeferredItem<BlockItem> WHEEL_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(WHEEL);
    public static final Supplier<BlockEntityType<BigWheelBlockEntity>> WHEEL_BE = ModCooking.BLOCK_ENTITIES.register(
            "big_wheel", () -> BlockEntityType.Builder.of(BigWheelBlockEntity::new, WHEEL.get()).build(null));
    public static final DeferredBlock<WheelTerminalBlock> TERMINAL = ModCooking.BLOCKS.register("wheel_terminal", () -> new WheelTerminalBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2F).sound(SoundType.METAL).lightLevel(s -> 5).noOcclusion()));
    public static final DeferredItem<BlockItem> TERMINAL_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(TERMINAL);

    public static void init() {}

    private ModWheel() {}
}
