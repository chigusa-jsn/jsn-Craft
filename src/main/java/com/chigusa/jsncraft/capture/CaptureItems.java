package com.chigusa.jsncraft.capture;

import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** 捕獲の杖と、動物の入った捕獲玉。 */
public final class CaptureItems {
    public static final DeferredItem<CaptureWandItem> WAND = ModEntities.ITEMS.register("capture_wand",
            () -> new CaptureWandItem(new Item.Properties().durability(128).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<CapturedOrbItem> ORB = ModEntities.ITEMS.register("captured_animal",
            () -> new CapturedOrbItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static void init() {}

    private CaptureItems() {}
}
