package com.chigusa.jsncraft.casino;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/** カジノ：チップ、両替機、ゲーム台 */
public final class ModCasino {

    public static final List<DeferredItem<ChipItem>> CHIPS = List.of(
            chip(1, Rarity.COMMON), chip(10, Rarity.COMMON), chip(100, Rarity.UNCOMMON), chip(1000, Rarity.RARE));

    private static DeferredItem<ChipItem> chip(int value, Rarity rarity) {
        return ModEntities.ITEMS.register("casino_chip_" + value, () -> new ChipItem(new Item.Properties().rarity(rarity), value));
    }

    public static final DeferredBlock<CasinoBlock> EXCHANGER = block("chip_exchanger", CasinoGame.EXCHANGE);
    public static final DeferredBlock<CasinoBlock> SLOT_MACHINE = block("slot_machine", CasinoGame.SLOT);
    public static final DeferredItem<BlockItem> EXCHANGER_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(EXCHANGER);
    public static final DeferredItem<BlockItem> SLOT_MACHINE_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(SLOT_MACHINE);

    private static DeferredBlock<CasinoBlock> block(String id, CasinoGame game) {
        return ModCooking.BLOCKS.register(id, () -> new CasinoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                .strength(2.5F).sound(SoundType.METAL).lightLevel(s -> 6).noOcclusion(), game));
    }

    public static void init() {}

    private ModCasino() {}
}
