package com.chigusa.jsncraft.race;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModRace {
    public static final DeferredBlock<RaceTerminalBlock> TERMINAL = ModCooking.BLOCKS.register("race_terminal", () -> new RaceTerminalBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2F).sound(SoundType.METAL).lightLevel(s -> 6).noOcclusion()));
    public static final DeferredItem<BlockItem> TERMINAL_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(TERMINAL);

    public static void init() {}

    private ModRace() {}
}
