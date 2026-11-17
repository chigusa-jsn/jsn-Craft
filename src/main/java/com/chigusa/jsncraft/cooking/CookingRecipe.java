package com.chigusa.jsncraft.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 調理台のレシピ。data/<名前空間>/jsn_cooking/*.json から読む。
 * {
 *   "result": "jsn_craft:dish/grilled_fish_set",
 *   "ingredients": ["#c:foods/raw_fish", "minecraft:baked_potato", "minecraft:dried_kelp"],  … 1〜4個（順番は自由）
 *   "level": 1,           … 炎の調理師の必要レベル（宴の料理長は 50 + レベル として数える）
 *   "time": 100,          … 調理時間（tick）
 *   "xp": 6,              … 職業経験値
 *   "nutrition": 10, "saturation": 0.8,
 *   "buffs": [ … FoodBuff … ],
 *   "rank": "normal",     … 料理図鑑のランク：normal（ふつう）/ feast（ごちそう）/ fine（逸品）/ legend（伝説）
 *   "no": 1,              … 料理図鑑の番号（書かなければ図鑑に出ない ＝ 加工食材など）
 *   "alcohol": 3          … お酒の強さ（飲むと酔いがたまる。0 ならお酒ではない）
 * }
 */
public record CookingRecipe(ResourceLocation result, int count, List<String> ingredients, int level, int time, int xp,
                            int nutrition, float saturation, List<FoodBuff> buffs, String rank, int no, int alcohol) {

    public static final Codec<CookingRecipe> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("result").forGetter(CookingRecipe::result),
            Codec.INT.optionalFieldOf("count", 1).forGetter(CookingRecipe::count),
            Codec.STRING.listOf().fieldOf("ingredients").forGetter(CookingRecipe::ingredients),
            Codec.INT.optionalFieldOf("level", 1).forGetter(CookingRecipe::level),
            Codec.INT.optionalFieldOf("time", 100).forGetter(CookingRecipe::time),
            Codec.INT.optionalFieldOf("xp", 5).forGetter(CookingRecipe::xp),
            Codec.INT.optionalFieldOf("nutrition", 6).forGetter(CookingRecipe::nutrition),
            Codec.FLOAT.optionalFieldOf("saturation", 0.6F).forGetter(CookingRecipe::saturation),
            FoodBuff.CODEC.listOf().optionalFieldOf("buffs", List.of()).forGetter(CookingRecipe::buffs),
            Codec.STRING.optionalFieldOf("rank", "normal").forGetter(CookingRecipe::rank),
            Codec.INT.optionalFieldOf("no", 999).forGetter(CookingRecipe::no),
            Codec.INT.optionalFieldOf("alcohol", 0).forGetter(CookingRecipe::alcohol)
    ).apply(i, CookingRecipe::new));

    public DishRank rankEnum() {
        return DishRank.byName(rank);
    }

    public static boolean matchesOne(String spec, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (spec.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(spec.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        ResourceLocation id = ResourceLocation.tryParse(spec);
        return id != null && id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /**
     * 材料の枠（空きを含む）がこのレシピにぴったり合うか。合えば「どの枠を使うか」を返す（合わなければ null）。
     */
    public int[] match(List<ItemStack> slots) {
        List<Integer> filled = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) if (!slots.get(i).isEmpty()) filled.add(i);
        if (filled.size() != ingredients.size()) return null;
        int[] use = new int[ingredients.size()];
        boolean[] taken = new boolean[slots.size()];
        return assign(0, slots, taken, use) ? use : null;
    }

    private boolean assign(int k, List<ItemStack> slots, boolean[] taken, int[] use) {
        if (k == ingredients.size()) return true;
        for (int i = 0; i < slots.size(); i++) {
            if (taken[i] || !matchesOne(ingredients.get(k), slots.get(i))) continue;
            taken[i] = true;
            use[k] = i;
            if (assign(k + 1, slots, taken, use)) return true;
            taken[i] = false;
        }
        return false;
    }
}
