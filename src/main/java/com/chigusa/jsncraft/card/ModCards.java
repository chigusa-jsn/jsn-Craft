package com.chigusa.jsncraft.card;

import com.chigusa.jsncraft.casino.CasinoBlock;
import com.chigusa.jsncraft.casino.CasinoGame;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Consumer;

/** カード：カードマシン・カード・装備用バインダー・アルバム */
public final class ModCards {
    public static final DeferredBlock<CasinoBlock> CARD_MACHINE = ModCooking.BLOCKS.register("card_machine", () -> new CasinoBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.5F).sound(SoundType.METAL).lightLevel(s -> 6).noOcclusion(), CasinoGame.CARD));
    public static final DeferredItem<BlockItem> CARD_MACHINE_ITEM = ModEntities.ITEMS.registerSimpleBlockItem(CARD_MACHINE);
    public static final DeferredItem<CardItem> CARD = ModEntities.ITEMS.register("card", () -> new CardItem(new Item.Properties()));
    public static final DeferredItem<CardBinderItem> BINDER = ModEntities.ITEMS.register("card_binder", () -> new CardBinderItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<CardAlbumItem> ALBUM = ModEntities.ITEMS.register("card_album", () -> new CardAlbumItem(new Item.Properties().stacksTo(1)));

    /** 画面を開く（クライアントでセットする） */
    public static Consumer<InteractionHand> openAlbum, openBinder;

    public static void init() {}

    private ModCards() {}
}
