package com.chigusa.jsncraft;

import com.chigusa.jsncraft.alchemy.ElixirData;
import com.chigusa.jsncraft.alchemy.Elixirs;
import com.chigusa.jsncraft.alchemy.ModAlchemy;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.enchanting.ModEnchanting;
import com.chigusa.jsncraft.farming.ModCrops;
import com.chigusa.jsncraft.fishing.FishItems;
import com.chigusa.jsncraft.jewelry.ModJewelry;
import com.chigusa.jsncraft.npc.ModEntities;
import com.chigusa.jsncraft.ranch.ModRanch;
import com.chigusa.jsncraft.ranch.Mutant;
import com.chigusa.jsncraft.ranch.MutantEggItem;
import com.chigusa.jsncraft.smithing.ModSmithing;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** クリエイティブのタブ（職業ごとに小分け）と、突然変異のスポーンエッグ。 */
public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JsnCraft.MODID);

    private static final Set<String> HERBS = Set.of("herb", "vigor_herb", "moon_herb", "antidote_herb", "fire_herb", "mandrake");

    // ---- 突然変異のスポーンエッグ（クリエイティブ用） ----
    public static final DeferredItem<Item> EGG_GOLDEN_CHICKEN = egg("golden_chicken", Mutant.GOLDEN_CHICKEN, () -> EntityType.CHICKEN, 0xF0C020, 0xFFF0A0);
    public static final DeferredItem<Item> EGG_RAINBOW_SHEEP = egg("rainbow_sheep", Mutant.RAINBOW_SHEEP, () -> EntityType.SHEEP, 0xE04060, 0x40C0F0);
    public static final DeferredItem<Item> EGG_MARBLED_COW = egg("marbled_cow", Mutant.MARBLED_COW, () -> EntityType.COW, 0xF8F0E8, 0xE07890);
    public static final DeferredItem<Item> EGG_TRUFFLE_PIG = egg("truffle_pig", Mutant.TRUFFLE_PIG, () -> EntityType.PIG, 0x4A2E1C, 0x9A7050);
    public static final DeferredItem<Item> EGG_PEGASUS = egg("pegasus", Mutant.PEGASUS, () -> EntityType.HORSE, 0xF8FCFF, 0x70C8F8);
    public static final DeferredItem<Item> EGG_QUEEN_BEE = egg("queen_bee", Mutant.QUEEN_BEE, () -> EntityType.BEE, 0x7A3AC8, 0xF0C030);
    public static final List<DeferredItem<Item>> MUTANT_EGGS = List.of(EGG_GOLDEN_CHICKEN, EGG_RAINBOW_SHEEP, EGG_MARBLED_COW, EGG_TRUFFLE_PIG, EGG_PEGASUS, EGG_QUEEN_BEE);

    private static DeferredItem<Item> egg(String name, Mutant m, Supplier<? extends EntityType<? extends net.minecraft.world.entity.animal.Animal>> type, int base, int spots) {
        return ModEntities.ITEMS.register("mutant_egg_" + name, () -> new MutantEggItem(m, type, base, spots));
    }

    private static DeferredHolder<CreativeModeTab, CreativeModeTab> tab(String id, String title, Supplier<ItemStack> icon, Consumer<CreativeModeTab.Output> items) {
        return TABS.register(id, () -> CreativeModeTab.builder()
                .title(Component.literal(title))
                .icon(icon)
                .displayItems((params, out) -> items.accept(out))
                .build());
    }

    private static void all(CreativeModeTab.Output out, Iterable<? extends DeferredItem<?>> items) {
        for (var i : items) out.accept(i.get());
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COMMON = tab("common", "jsn-Craft：共通",
            () -> new ItemStack(ModEntities.JOB_MASTER_EGG.get()), out -> {
                out.accept(ModEntities.RECEPTIONIST_EGG.get());
                out.accept(ModEntities.JOB_MASTER_EGG.get());
                out.accept(ModEntities.OBLIVION_ORB.get());
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FISHING = tab("fishing", "jsn-Craft：釣り",
            () -> new ItemStack(FishItems.ALL.get(0).get()), out -> all(out, FishItems.ALL));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FARMING = tab("farming", "jsn-Craft：農業",
            () -> new ItemStack(ModCrops.PRODUCE.get("tomato").get()), out -> {
                for (var e : ModCrops.SEEDS.entrySet()) if (!HERBS.contains(e.getKey()) && e.getValue() != ModCrops.PRODUCE.get(e.getKey())) out.accept(e.getValue().get());
                for (var e : ModCrops.PRODUCE.entrySet()) if (!HERBS.contains(e.getKey())) out.accept(e.getValue().get());
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COOKING = tab("cooking", "jsn-Craft：料理",
            () -> new ItemStack(ModCooking.DISH_ITEMS.get(0).get()), out -> {
                out.accept(ModCooking.COOKING_STATION_ITEM.get());
                out.accept(ModCooking.BREWING_BARREL_ITEM.get());
                all(out, ModCrops.INGREDIENT_ITEMS);
                all(out, ModCooking.DISH_ITEMS);
                all(out, ModCooking.DRINK_ITEMS);
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SMITHING = tab("smithing", "jsn-Craft：鍛冶",
            () -> new ItemStack(ModSmithing.FORGE_ITEM.get()), out -> {
                out.accept(ModSmithing.FORGE_ITEM.get());
                out.accept(ModSmithing.RECYCLER_ITEM.get());
                all(out, ModSmithing.STONES);
                out.accept(ModSmithing.WHETSTONE.get());
                out.accept(ModSmithing.WHETSTONE_ADV.get());
                out.accept(ModSmithing.REPAIR_KIT.get());
                out.accept(ModSmithing.REPAIR_KIT_ADV.get());
                out.accept(ModSmithing.ARMOR_PLATE.get());
                out.accept(ModSmithing.WHETSTONE_ROUGH.get());
                out.accept(ModSmithing.REPAIR_KIT_SIMPLE.get());
                out.accept(ModSmithing.IRON_PLATE.get());
                out.accept(ModSmithing.HONE_WHITE.get());
                out.accept(ModSmithing.HONE_PURPLE.get());
                out.accept(ModSmithing.REPAIR_KIT_DIVINE.get());
                all(out, com.chigusa.jsncraft.smithing.arrow.ModArrows.ARROWS.values());
                out.accept(com.chigusa.jsncraft.smithing.arrow.ModArrows.THROWING_KNIFE.get());
                out.accept(com.chigusa.jsncraft.smithing.arrow.ModArrows.SHURIKEN.get());
                all(out, com.chigusa.jsncraft.smithing.arrow.ModArrows.TIPS);
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENCHANTING = tab("enchanting", "jsn-Craft：付与",
            () -> new ItemStack(ModEnchanting.ALTAR_ITEM.get()), out -> {
                out.accept(ModEnchanting.ALTAR_ITEM.get());
                out.accept(ModEnchanting.RUNE_BOOK.get());
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALCHEMY = tab("alchemy", "jsn-Craft：調合",
            () -> new ItemStack(ModAlchemy.TABLE_ITEM.get()), out -> {
                out.accept(ModAlchemy.TABLE_ITEM.get());
                for (String h : HERBS) {
                    if (ModCrops.SEEDS.get(h) != ModCrops.PRODUCE.get(h)) out.accept(ModCrops.SEEDS.get(h).get());
                    out.accept(ModCrops.PRODUCE.get(h).get());
                }
                out.accept(ModAlchemy.DUST_BASE.get());
                for (Elixirs.Type t : Elixirs.TYPES) out.accept(ModAlchemy.make(t, ElixirData.DEFAULT, 1));
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> JEWELRY = tab("jewelry", "jsn-Craft：細工",
            () -> new ItemStack(ModJewelry.GEMS.get(4).get()), out -> {
                out.accept(ModJewelry.TABLE_ITEM.get());
                all(out, ModJewelry.RAW_GEMS);
                all(out, ModJewelry.GEMS);
                out.accept(ModJewelry.MOON_SILVER.get());
                out.accept(ModJewelry.STAR_SILVER.get());
                all(out, ModJewelry.JEWELRY.values());
                all(out, ModJewelry.UNIQUES);
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEAPONS = tab("weapons", "jsn-Craft：職業武器",
            () -> new ItemStack(com.chigusa.jsncraft.weapon.ModWeapons.STAFFS.get(5).get()), out -> {
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.TEMPLATES);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.STAFFS);
                out.accept(com.chigusa.jsncraft.weapon.ModWeapons.SPIRIT_STONE.get());
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.GRIMOIRES);
                out.accept(com.chigusa.jsncraft.weapon.ModWeapons.SILVER_INGOT.get());
                out.accept(com.chigusa.jsncraft.weapon.ModWeapons.HOLY_CRYSTAL.get());
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.HOLY_STAFFS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.KATANAS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.JOB_SWORDS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.TOWER_SHIELDS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.JOB_AXES);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.JOB_BOWS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.SPEARS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.SEA_TRIDENTS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.GAUNTLETS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.DAGGERS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.WHIPS);
                all(out, com.chigusa.jsncraft.weapon.ModWeapons.CARDS);
                all(out, com.chigusa.jsncraft.armor.ModArmor.ITEMS);
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CASINO = tab("casino", "jsn-Craft：カジノ",
            () -> new ItemStack(com.chigusa.jsncraft.casino.ModCasino.SLOT_MACHINE_ITEM.get()), out -> {
                out.accept(com.chigusa.jsncraft.casino.ModCasino.EXCHANGER_ITEM.get());
                out.accept(com.chigusa.jsncraft.casino.ModCasino.SLOT_MACHINE_ITEM.get());
                out.accept(com.chigusa.jsncraft.casino.ModCasino.PACHI_CROWN_ITEM.get());
                out.accept(com.chigusa.jsncraft.casino.ModCasino.PACHI_TROPICAL_ITEM.get());
                out.accept(com.chigusa.jsncraft.casino.ModCasino.PACHINKO_ITEM.get());
                for (var m : com.chigusa.jsncraft.gacha.ModGacha.MACHINE_ITEMS) out.accept(m.get());
                out.accept(com.chigusa.jsncraft.wheel.ModWheel.WHEEL_ITEM.get());
                out.accept(com.chigusa.jsncraft.wheel.ModWheel.TERMINAL_ITEM.get());
                out.accept(com.chigusa.jsncraft.race.ModRace.TERMINAL_ITEM.get());
                out.accept(com.chigusa.jsncraft.chinchiro.ModChinchiro.TABLE_ITEM.get());
                for (String f : new String[]{"minecraft:creeper", "minecraft:zombie", "minecraft:enderman", "minecraft:warden", "minecraft:ender_dragon"})
                    out.accept(com.chigusa.jsncraft.gacha.FigureItem.of(f));
                out.accept(com.chigusa.jsncraft.gacha.TitleTicketItem.of("ガチャの申し子"));
                out.accept(com.chigusa.jsncraft.card.ModCards.CARD_MACHINE_ITEM.get());
                out.accept(com.chigusa.jsncraft.card.ModCards.BINDER.get());
                out.accept(com.chigusa.jsncraft.card.ModCards.ALBUM.get());
                for (var c : com.chigusa.jsncraft.card.CardList.ALL) out.accept(com.chigusa.jsncraft.card.CardItem.of(c.id()));
                out.accept(com.chigusa.jsncraft.casino.ModCasino.ROULETTE_ITEM.get());
                out.accept(com.chigusa.jsncraft.casino.ModCasino.BLACKJACK_ITEM.get());
                out.accept(com.chigusa.jsncraft.casino.ModCasino.POKER_ITEM.get());
                out.accept(com.chigusa.jsncraft.casino.ModCasino.CHINCHIRO_ITEM.get());
                all(out, com.chigusa.jsncraft.casino.ModCasino.CHIPS);
            });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RANCH = tab("ranch", "jsn-Craft：牧場",
            () -> new ItemStack(ModRanch.GOLDEN_EGG.get()), out -> {
                out.accept(ModRanch.FEEDER_ITEM.get());
                out.accept(ModRanch.FRESH_MILK.get());
                out.accept(ModRanch.PREMIUM_BEEF.get());
                out.accept(ModRanch.PREMIUM_PORK.get());
                out.accept(ModRanch.PREMIUM_MUTTON.get());
                out.accept(ModRanch.PREMIUM_CHICKEN.get());
                out.accept(ModRanch.GOLDEN_EGG.get());
                out.accept(ModRanch.TRUFFLE_HINT.get());
                all(out, MUTANT_EGGS);
            });

    public static void init() {}

    private ModTabs() {}
}
