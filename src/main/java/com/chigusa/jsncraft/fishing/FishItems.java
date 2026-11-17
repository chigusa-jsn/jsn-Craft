package com.chigusa.jsncraft.fishing;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/** jsn-Craft の魚アイテム（jsn_craft:fish/<名前>）。生で食べられる。名前の色がランクの色になる。 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FishItems {

    /** ランクごとの魚の一覧（ここに足すと登録される。絵は tools/gen_fish_art.py） */
    private static final String[][] FISH = {
            {"common", "crucian_carp", "carp", "sardine", "goby", "bluegill", "lava_goby", "hollow_goby"},
            {"uncommon", "yamame", "iwana", "ayu", "horse_mackerel", "mackerel", "red_sea_bream", "flounder", "catfish",
                    "eel", "piranha", "squid", "goldfish", "flame_shrimp", "ash_eel", "warped_catfish", "chorus_fish", "purpur_pleco", "ender_medaka"},
            {"rare", "moonlight_crucian", "rainbow_trout", "anglerfish", "tuna", "swordfish", "arapaima", "jewel_crab", "crimson_piranha", "basalt_catfish", "ghast_fish", "stardust_sardine", "crystal_shrimp"},
            {"epic", "thunder_eel", "glacier_salmon", "coelacanth", "golden_carp", "arowana", "ghost_fish", "inferno_shachihoko", "soul_eater", "debris_turtle", "shulker_fish", "void_jelly", "elytra_ray"},
            {"legend", "moon_lord", "storm_king", "ice_queen", "inferno_dragonfish", "soulflame_catfish", "ender_dragonfish"},
    };

    public static final List<DeferredItem<Item>> ALL = new ArrayList<>();

    static {
        for (String[] group : FISH) {
            FishRank rank = FishRank.byName(group[0]);
            for (int i = 1; i < group.length; i++) {
                String name = group[i];
                ALL.add(ModEntities.ITEMS.register("fish/" + name, () -> new FishItem(rank)));
            }
        }
    }

    /** JsnCraft から呼んで、登録を確実に済ませる */
    public static void init() {}

    public static class FishItem extends Item {
        private final FishRank rank;

        public FishItem(FishRank rank) {
            super(new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(2 + rank.ordinal()).saturationModifier(0.1F + rank.ordinal() * 0.1F).build())
                    .rarity(rank == FishRank.LEGEND ? Rarity.EPIC : Rarity.COMMON));
            this.rank = rank;
        }

        public FishRank rank() {
            return rank;
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable(getDescriptionId(stack)).withStyle(rank.color);
        }
    }

    private FishItems() {}
}
