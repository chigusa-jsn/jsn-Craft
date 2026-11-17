package com.chigusa.jsncraft.cooking;

import com.chigusa.jsncraft.JsnCraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 調理台のレシピ（jsn_cooking）と隠し味（jsn_seasonings）を読み込む。 */
public class CookingManager<T> extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public static final CookingManager<CookingRecipe> RECIPES = new CookingManager<>("jsn_cooking", CookingRecipe.CODEC);
    public static final CookingManager<Seasoning> SEASONINGS = new CookingManager<>("jsn_seasonings", Seasoning.CODEC);
    /** 酒樽で熟成させるお酒（中身の形は調理台のレシピと同じ。time は熟成にかかる tick） */
    public static final CookingManager<CookingRecipe> BREWING = new CookingManager<>("jsn_brewing", CookingRecipe.CODEC);

    private final Codec<T> codec;
    private final String folder;
    private List<Map.Entry<ResourceLocation, T>> entries = List.of();

    private CookingManager(String folder, Codec<T> codec) {
        super(GSON, folder);
        this.folder = folder;
        this.codec = codec;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        List<Map.Entry<ResourceLocation, T>> list = new ArrayList<>();
        files.forEach((id, json) -> codec.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> JsnCraft.LOGGER.error("{} {} の読み込みに失敗しました: {}", folder, id, err))
                .ifPresent(v -> list.add(Map.entry(id, v))));
        list.sort(Comparator.comparing((Map.Entry<ResourceLocation, T> e) ->
                e.getValue() instanceof CookingRecipe r ? r.level() : 0).thenComparing(e -> e.getKey().toString()));
        entries = List.copyOf(list);
        JsnCraft.LOGGER.info("[jsn-Craft] {} を {} 個読み込みました", folder, entries.size());
    }

    public List<Map.Entry<ResourceLocation, T>> entries() {
        return entries;
    }

    // ---- レシピ用 ----

    /** 材料に合うレシピの番号（なければ -1）。材料が多いレシピ、次に「#タグ」でなく名指しの材料が多いレシピを優先 */
    public static int findRecipe(List<ItemStack> slots) {
        return find(RECIPES, slots);
    }

    public static int find(CookingManager<CookingRecipe> manager, List<ItemStack> slots) {
        int best = -1, bestScore = -1;
        List<Map.Entry<ResourceLocation, CookingRecipe>> list = manager.entries();
        for (int i = 0; i < list.size(); i++) {
            CookingRecipe r = list.get(i).getValue();
            int specific = 0;
            for (String s : r.ingredients()) if (!s.startsWith("#")) specific++;
            int score = r.ingredients().size() * 10 + specific;
            if (score > bestScore && r.match(slots) != null) {
                best = i;
                bestScore = score;
            }
        }
        return best;
    }

    @Nullable
    public static CookingRecipe recipe(int index) {
        return get(RECIPES, index);
    }

    @Nullable
    public static CookingRecipe get(CookingManager<CookingRecipe> manager, int index) {
        var list = manager.entries();
        return index >= 0 && index < list.size() ? list.get(index).getValue() : null;
    }

    @Nullable
    public static Seasoning seasoning(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (var e : SEASONINGS.entries()) if (CookingRecipe.matchesOne(e.getValue().item(), stack)) return e.getValue();
        return null;
    }
}
