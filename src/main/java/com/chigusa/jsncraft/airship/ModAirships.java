package com.chigusa.jsncraft.airship;

import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/** 飛空艇の登録。 */
public final class ModAirships {
    public static final DeferredHolder<EntityType<?>, EntityType<Airship>> AIRSHIP = ModEntities.ENTITY_TYPES.register("airship",
            () -> EntityType.Builder.<Airship>of(Airship::new, MobCategory.MISC).sized(1.5F, 0.6F).clientTrackingRange(10).updateInterval(1).build("airship"));

    public static final DeferredItem<AirshipItem> SMALL = ModEntities.ITEMS.register("airship_small",
            () -> new AirshipItem(1, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<AirshipItem> MEDIUM = ModEntities.ITEMS.register("airship_medium",
            () -> new AirshipItem(2, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<AirshipItem> LARGE = ModEntities.ITEMS.register("airship_large",
            () -> new AirshipItem(3, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final List<DeferredItem<AirshipItem>> ITEMS = List.of(SMALL, MEDIUM, LARGE);

    public static final DeferredHolder<MenuType<?>, MenuType<AirshipMenu>> MENU = ModCooking.MENUS.register("airship",
            () -> IMenuTypeExtension.create(AirshipMenu::new));

    public static Item item(int tier) {
        return ITEMS.get(Math.max(1, Math.min(3, tier)) - 1).get();
    }

    public static void init() {}

    private ModAirships() {}
}
