package com.chigusa.jsncraft.smithing.arrow;

import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 鍛冶師が作る矢・投げ物・三叉槍の穂先の登録。 */
public final class ModArrows {

    public static final DeferredHolder<EntityType<?>, EntityType<SpecialArrow>> SPECIAL_ARROW = ModEntities.ENTITY_TYPES.register(
            "special_arrow", () -> EntityType.Builder.<SpecialArrow>of(SpecialArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build("special_arrow"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownBlade>> THROWN_BLADE = ModEntities.ENTITY_TYPES.register(
            "thrown_blade", () -> EntityType.Builder.<ThrownBlade>of(ThrownBlade::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("thrown_blade"));

    public static final Map<ArrowKind, DeferredItem<SpecialArrowItem>> ARROWS = new EnumMap<>(ArrowKind.class);

    static {
        for (ArrowKind k : ArrowKind.values()) {
            ARROWS.put(k, ModEntities.ITEMS.register(k.id, () -> new SpecialArrowItem(k,
                    new Item.Properties().rarity(k == ArrowKind.STAR || k == ArrowKind.DRAGON ? Rarity.EPIC : k.ordinal() >= ArrowKind.HARDENED.ordinal() ? Rarity.RARE : k == ArrowKind.HOMING || k == ArrowKind.RETRIEVE ? Rarity.UNCOMMON : Rarity.COMMON))));
        }
    }

    public static final DeferredItem<ThrowingItem> THROWING_KNIFE = ModEntities.ITEMS.register("throwing_knife",
            () -> new ThrowingItem(true, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<ThrowingItem> SHURIKEN = ModEntities.ITEMS.register("shuriken",
            () -> new ThrowingItem(false, new Item.Properties().stacksTo(32)));

    public static final List<DeferredItem<TridentTipItem>> TIPS = List.of(
            ModEntities.ITEMS.register("trident_tip_fire", () -> new TridentTipItem(TridentTipItem.Tip.FIRE, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))),
            ModEntities.ITEMS.register("trident_tip_frost", () -> new TridentTipItem(TridentTipItem.Tip.FROST, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON))),
            ModEntities.ITEMS.register("trident_tip_storm", () -> new TridentTipItem(TridentTipItem.Tip.STORM, new Item.Properties().stacksTo(16).rarity(Rarity.RARE))),
            ModEntities.ITEMS.register("trident_tip_abyss", () -> new TridentTipItem(TridentTipItem.Tip.ABYSS, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))));

    public static Item arrow(ArrowKind k) {
        return ARROWS.get(k).get();
    }

    public static void init() {}

    private ModArrows() {}
}
