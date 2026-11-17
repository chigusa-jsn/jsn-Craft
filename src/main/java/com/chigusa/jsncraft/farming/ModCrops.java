package com.chigusa.jsncraft.farming;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.npc.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 新しい作物と食材アイテム。
 *  作物 … 種の形で3種類：produce（取れた物がそのまま種：米・玉ねぎ・大豆・大麦）
 *                       seeds（別に種がある：トマト・キャベツ・唐辛子・ホップ）
 *                       sapling（苗を植える果樹：ブドウ・レモン・梅・茶・コーヒー）
 *  加工食材 … 調理台で作る（ごはん・小麦粉・塩・味噌・醤油など）
 */
public final class ModCrops {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JsnCraft.MODID);

    /** 名前, 種の形, 満腹度, 満腹度の持ち（0 なら食べられない） */
    private record Crop(String name, String kind, int nutrition, float saturation) {}

    private static final Crop[] CROPS = {
            new Crop("rice", "produce", 1, 0.1F),
            new Crop("onion", "produce", 2, 0.3F),
            new Crop("soybean", "produce", 1, 0.2F),
            new Crop("barley", "produce", 0, 0),
            new Crop("tomato", "seeds", 3, 0.4F),
            new Crop("cabbage", "seeds", 2, 0.3F),
            new Crop("chili", "seeds", 1, 0.1F),
            new Crop("hops", "seeds", 0, 0),
            new Crop("grape", "sapling", 2, 0.3F),
            new Crop("lemon", "sapling", 1, 0.2F),
            new Crop("ume", "sapling", 1, 0.2F),
            new Crop("tea", "sapling", 0, 0),
            new Crop("coffee", "sapling", 0, 0),
            // 薬草（秘薬の調合師の素材）
            new Crop("herb", "seeds", 0, 0),
            new Crop("vigor_herb", "seeds", 0, 0),
            new Crop("moon_herb", "seeds", 0, 0),
            new Crop("antidote_herb", "seeds", 0, 0),
            new Crop("fire_herb", "seeds", 0, 0),
            new Crop("mandrake", "produce", 0, 0),
    };

    /** 加工食材：名前, 満腹度, 持ち（0 なら食べられない） */
    private static final Object[][] INGREDIENTS = {
            {"cooked_rice", 4, 0.6F}, {"flour", 0, 0F}, {"dough", 0, 0F}, {"salt", 0, 0F}, {"koji", 0, 0F},
            {"miso", 0, 0F}, {"soy_sauce", 0, 0F}, {"butter", 1, 0.2F}, {"cheese", 3, 0.6F}, {"dashi", 0, 0F},
            {"malt", 0, 0F}, {"tomato_sauce", 0, 0F}, {"matcha_powder", 0, 0F},
    };

    public static final Map<String, DeferredBlock<JsnCropBlock>> CROP_BLOCKS = new LinkedHashMap<>();
    /** 取れる物（作物の名前で引く） */
    public static final Map<String, DeferredItem<Item>> PRODUCE = new LinkedHashMap<>();
    /** 植える物（種・苗。produce の作物は取れる物と同じ） */
    public static final Map<String, DeferredItem<Item>> SEEDS = new LinkedHashMap<>();
    public static final List<DeferredItem<Item>> INGREDIENT_ITEMS = new ArrayList<>();

    private static Item.Properties food(int n, float s) {
        Item.Properties p = new Item.Properties();
        if (n > 0) p.food(new FoodProperties.Builder().nutrition(n).saturationModifier(s).build());
        return p;
    }

    static {
        for (Crop c : CROPS) {
            boolean perennial = c.kind().equals("sapling");
            DeferredBlock<JsnCropBlock> block = BLOCKS.register(c.name() + "_crop", () -> new JsnCropBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), () -> SEEDS.get(c.name()).get(), () -> PRODUCE.get(c.name()).get(), perennial));
            CROP_BLOCKS.put(c.name(), block);
            if (c.kind().equals("produce")) {
                DeferredItem<Item> item = ModEntities.ITEMS.register(c.name(), () -> new ItemNameBlockItem(block.get(), food(c.nutrition(), c.saturation())));
                PRODUCE.put(c.name(), item);
                SEEDS.put(c.name(), item);
            } else {
                PRODUCE.put(c.name(), ModEntities.ITEMS.register(c.name(), () -> new Item(food(c.nutrition(), c.saturation()))));
                String seedName = c.name() + (perennial ? "_sapling" : "_seeds");
                SEEDS.put(c.name(), ModEntities.ITEMS.register(seedName, () -> new ItemNameBlockItem(block.get(), new Item.Properties())));
            }
        }
        for (Object[] i : INGREDIENTS) {
            String name = (String) i[0];
            int n = (int) i[1];
            float s = (float) i[2];
            INGREDIENT_ITEMS.add(ModEntities.ITEMS.register(name, () -> new Item(food(n, s))));
        }
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    // ---- 種の手に入れ方：草を壊すとたまに種、葉っぱからたまに苗 ----

    private static final String[] GRASS_SEEDS = {"rice", "onion", "soybean", "barley", "tomato", "cabbage", "chili", "hops",
            "herb", "herb", "vigor_herb", "antidote_herb"};

    @EventBusSubscriber(modid = JsnCraft.MODID)
    public static final class Drops {
        @SubscribeEvent
        public static void onDrops(BlockDropsEvent event) {
            BlockState st = event.getState();
            var level = event.getLevel();
            var r = level.random;
            BlockPos pos = event.getPos();
            ItemStack tool = event.getTool();
            String give = null;
            if ((st.is(Blocks.FERN) || st.is(Blocks.LARGE_FERN)) && !tool.is(Items.SHEARS) && r.nextInt(8) == 0) {
                give = "moon_herb";                                   // 月光草はシダから
            } else if (st.is(Blocks.DEAD_BUSH) && r.nextInt(4) == 0) {
                give = "fire_herb";                                   // 火炎草は枯れ木から
            } else if ((st.is(Blocks.SHORT_GRASS) || st.is(Blocks.TALL_GRASS)) && !tool.is(Items.SHEARS) && r.nextInt(150) == 0) {
                give = "mandrake";                                    // マンドラゴラはとてもまれ
            } else if ((st.is(Blocks.SHORT_GRASS) || st.is(Blocks.TALL_GRASS) || st.is(Blocks.FERN)) && !tool.is(Items.SHEARS)) {
                if (r.nextInt(10) == 0) give = GRASS_SEEDS[r.nextInt(GRASS_SEEDS.length)];
            } else if (st.is(BlockTags.LEAVES) && !tool.is(Items.SHEARS) && r.nextInt(40) == 0) {
                if (st.is(Blocks.CHERRY_LEAVES)) give = "ume";
                else if (st.is(Blocks.JUNGLE_LEAVES)) give = "lemon";
                else if (st.is(Blocks.OAK_LEAVES) || st.is(Blocks.DARK_OAK_LEAVES)) give = "grape";
                else if (st.is(Blocks.BIRCH_LEAVES)) give = "tea";
                else if (st.is(Blocks.ACACIA_LEAVES)) give = "coffee";
            }
            if (give == null) return;
            event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(SEEDS.get(give).get())));
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Tabs {
        @SubscribeEvent
        public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
                for (String n : SEEDS.keySet()) if (SEEDS.get(n) != PRODUCE.get(n)) event.accept(SEEDS.get(n));
            }
            if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
                for (var p : PRODUCE.values()) event.accept(p);
                for (var i : INGREDIENT_ITEMS) event.accept(i);
            }
        }
    }

    private ModCrops() {}
}
