package com.chigusa.jsncraft.smithing;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 分解機の計算：作業台のレシピを逆にたどって、材料の一部を返す。 */
public final class RecyclerService {

    private static RecipeManager cachedFor;
    private static final Map<Item, Map<Item, Double>> CACHE = new HashMap<>();

    /** 鍛冶の腕による戻る割合：鍛冶師 40%〜60%、名工 60%〜75% */
    public static double rate(int smithLevel) {
        if (smithLevel < 0) return 0;
        if (smithLevel <= 50) return 0.4 + smithLevel * 0.004;
        return Math.min(0.75, 0.6 + (smithLevel - 50) * 0.003);
    }

    /** 分解できない物（中身の入った入れ物・レシピの無い物など）はそのまま通す */
    public static boolean passThrough(ServerLevel level, ItemStack stack) {
        ItemContainerContents cont = stack.get(DataComponents.CONTAINER);
        if (cont != null && !cont.equals(ItemContainerContents.EMPTY)) return true;
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null && !bundle.isEmpty()) return true;
        if (stack.getItem() instanceof BlockItem b && b.getBlock() instanceof ShulkerBoxBlock) return true;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.getNamespace().equals("jsn_storage")) return true;
        return parts(level, stack.getItem()).isEmpty();
    }

    /** 1個あたりの材料（個数は小数のこともある） */
    public static Map<Item, Double> parts(ServerLevel level, Item item) {
        RecipeManager rm = level.getRecipeManager();
        if (rm != cachedFor) {
            CACHE.clear();
            cachedFor = rm;
        }
        Map<Item, Double> got = CACHE.get(item);
        if (got != null) return got;
        CACHE.put(item, Map.of());   // 輪になったレシピで止まらないように
        Map<Item, Double> out = find(level, rm, item);
        if (out.isEmpty()) out = findSmithing(level, rm, item);
        if (out.isEmpty()) {
            // ネザライト装備：ダイヤ装備の材料 ＋ ネザライトの欠片
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getPath().startsWith("netherite_")) {
                Item dia = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "diamond_" + id.getPath().substring(10)));
                if (dia != Items.AIR) {
                    Map<Item, Double> d = find(level, rm, dia);
                    if (!d.isEmpty()) {
                        out = new LinkedHashMap<>(d);
                        out.merge(Items.NETHERITE_SCRAP, 4.0, Double::sum);
                    }
                }
            }
        }
        CACHE.put(item, out);
        return out;
    }

    private static Map<Item, Double> find(ServerLevel level, RecipeManager rm, Item item) {
        for (var holder : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
            var recipe = holder.value();
            ItemStack res;
            try {
                res = recipe.getResultItem(level.registryAccess());
            } catch (Exception e) {
                continue;
            }
            if (res.isEmpty() || !res.is(item)) continue;
            Map<Item, Double> out = new LinkedHashMap<>();
            boolean loop = false;
            for (Ingredient ing : recipe.getIngredients()) {
                if (ing.isEmpty()) continue;
                ItemStack[] options = ing.getItems();
                if (options.length == 0) continue;
                Item part = options[0].getItem();
                if (part == item) {
                    loop = true;
                    break;
                }
                out.merge(part, 1.0 / res.getCount(), Double::sum);
            }
            if (loop || out.isEmpty()) continue;
            return out;
        }
        return Map.of();
    }

    /** 鍛冶台（スミシング）で作る物：元の装備の材料 ＋ 足した素材 ＋ 型（半分） */
    private static Map<Item, Double> findSmithing(ServerLevel level, RecipeManager rm, Item item) {
        for (var holder : rm.getAllRecipesFor(RecipeType.SMITHING)) {
            if (!(holder.value() instanceof net.minecraft.world.item.crafting.SmithingTransformRecipe recipe)) continue;
            ItemStack res;
            try {
                res = recipe.getResultItem(level.registryAccess());
            } catch (Exception e) {
                continue;
            }
            if (res.isEmpty() || !res.is(item)) continue;
            Item base = first(field(recipe, "base")), addition = first(field(recipe, "addition")), template = first(field(recipe, "template"));
            if (base == null || base == item) continue;
            Map<Item, Double> out = new LinkedHashMap<>();
            Map<Item, Double> baseParts = parts(level, base);
            if (baseParts.isEmpty()) out.merge(base, 1.0, Double::sum);
            else baseParts.forEach((k, v) -> out.merge(k, v, Double::sum));
            if (addition != null) out.merge(addition, 1.0, Double::sum);
            if (template != null) out.merge(template, 0.5, Double::sum);
            return out;
        }
        return Map.of();
    }

    private static Ingredient field(Object recipe, String name) {
        try {
            var f = net.minecraft.world.item.crafting.SmithingTransformRecipe.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(recipe) instanceof Ingredient i ? i : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Item first(Ingredient ing) {
        if (ing == null || ing.isEmpty()) return null;
        ItemStack[] items = ing.getItems();
        return items.length == 0 ? null : items[0].getItem();
    }

    /** 1個を分解した結果 */
    public static List<ItemStack> salvage(ServerLevel level, ItemStack one, int smithLevel, RandomSource r) {
        List<ItemStack> out = new ArrayList<>();
        double rate = rate(smithLevel);
        double dur = 1.0;
        if (one.getMaxDamage() > 0) dur = Durability.broken(one) ? 0.3 : 0.3 + 0.7 * Durability.ratio(one);
        for (var e : parts(level, one.getItem()).entrySet()) {
            int n = roll(e.getValue() * rate * dur, r);
            if (n > 0) add(out, new ItemStack(e.getKey(), n));
        }
        int plus = SmithData.plus(one);
        for (int k = 1; k <= plus; k++) {
            if (r.nextDouble() < rate * 0.6) add(out, new ItemStack(ModSmithing.STONES.get(SmithingService.tierFor(k)).get()));
        }
        return out;
    }

    private static int roll(double expected, RandomSource r) {
        int n = (int) Math.floor(expected);
        if (r.nextDouble() < expected - n) n++;
        return n;
    }

    private static void add(List<ItemStack> out, ItemStack st) {
        for (ItemStack o : out) {
            if (ItemStack.isSameItemSameComponents(o, st) && o.getCount() < o.getMaxStackSize()) {
                int m = Math.min(st.getCount(), o.getMaxStackSize() - o.getCount());
                o.grow(m);
                st.shrink(m);
                if (st.isEmpty()) return;
            }
        }
        while (!st.isEmpty()) out.add(st.split(st.getMaxStackSize()));
    }

    private RecyclerService() {}
}
