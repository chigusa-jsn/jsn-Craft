package com.chigusa.jsncraft.gacha;

import com.chigusa.jsncraft.casino.TallCasinoBlock;
import com.chigusa.jsncraft.casino.CasinoGame;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/** ガチャガチャ：本体・フィギュア・肩書チケット */
public final class ModGacha {
    /** 4台：装備・素材・フィギュア・肩書（縦2マス） */
    public static final java.util.List<DeferredBlock<TallCasinoBlock>> MACHINES = java.util.Arrays.stream(GachaKind.values()).map(k ->
            ModCooking.BLOCKS.register("gacha_" + k.id, () -> new TallCasinoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK)
                    .strength(2.5F).sound(SoundType.METAL).lightLevel(s -> 7).noOcclusion(), k.game))).toList();
    public static final java.util.List<DeferredItem<BlockItem>> MACHINE_ITEMS = MACHINES.stream().map(ModEntities.ITEMS::registerSimpleBlockItem).toList();

    public static final DeferredBlock<FigureBlock> FIGURE = ModCooking.BLOCKS.register("figure", () -> new FigureBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.6F).sound(SoundType.STONE).noOcclusion()));
    public static final DeferredItem<FigureItem> FIGURE_ITEM = ModEntities.ITEMS.register("figure", () -> new FigureItem(FIGURE.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final Supplier<BlockEntityType<FigureBlockEntity>> FIGURE_BE = ModCooking.BLOCK_ENTITIES.register(
            "figure", () -> BlockEntityType.Builder.of(FigureBlockEntity::new, FIGURE.get()).build(null));

    public static final DeferredItem<TitleTicketItem> TITLE_TICKET = ModEntities.ITEMS.register("title_ticket", () -> new TitleTicketItem(new Item.Properties().rarity(Rarity.RARE)));

    public static void init() {}

    private ModGacha() {}
}
