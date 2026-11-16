package com.chigusa.jsncraft.fishing;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 魚図鑑の魚の一覧（data/<名前空間>/jsn_fish/）と、釣れる魚の抽選。 */
public class FishSpeciesManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final FishSpeciesManager INSTANCE = new FishSpeciesManager();

    private Map<ResourceLocation, FishSpecies> species = Map.of();

    private FishSpeciesManager() {
        super(GSON, "jsn_fish");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        List<Map.Entry<ResourceLocation, FishSpecies>> list = new ArrayList<>();
        files.forEach((id, json) -> FishSpecies.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> JsnCraft.LOGGER.error("魚 {} の読み込みに失敗しました: {}", id, err))
                .ifPresent(s -> list.add(Map.entry(id, s))));
        list.sort(Comparator.comparingInt((Map.Entry<ResourceLocation, FishSpecies> e) -> e.getValue().no())
                .thenComparing(e -> e.getKey().toString()));
        Map<ResourceLocation, FishSpecies> out = new LinkedHashMap<>();
        for (var e : list) out.put(e.getKey(), e.getValue());
        species = out;
        JsnCraft.LOGGER.info("[jsn-Craft] 魚を {} 種類読み込みました", species.size());
    }

    public Map<ResourceLocation, FishSpecies> all() {
        return species;
    }

    @Nullable
    public FishSpecies get(ResourceLocation id) {
        return species.get(id);
    }

    /** このアイテムの魚（バニラのタラなど、図鑑に記録するため） */
    @Nullable
    public ResourceLocation byItem(ItemStack stack) {
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (var e : species.entrySet()) if (e.getValue().item().equals(item)) return e.getKey();
        return null;
    }

    /**
     * このランクの魚を釣れるか。コモンとアンコモンは誰でも。
     * レアは釣り師Lv10、エピックは釣り師Lv25、レジェンドは大海の網主から（網主なら全部）。
     */
    public static boolean unlocked(ServerPlayer player, FishRank rank) {
        if (rank.ordinal() <= FishRank.UNCOMMON.ordinal()) return true;
        var data = JobService.data(player);
        ResourceLocation life = data.getJob(JobType.LIFE);
        if (life == null) return false;
        if (life.equals(JsnCraft.id("net_master"))) return true;
        if (!life.equals(JsnCraft.id("angler"))) return false;
        int lv = data.level(life);
        return rank == FishRank.RARE ? lv >= 10 : rank == FishRank.EPIC && lv >= 25;
    }

    /** ここで釣れる魚を1匹選ぶ（釣れる魚がなければ null）。幸運が高いとレア以上が出やすい */
    @Nullable
    public ResourceLocation roll(ServerPlayer player, ServerLevel level, BlockPos pos, String fluid) {
        double luck = Math.max(0, player.getLuck());
        List<ResourceLocation> ids = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0;
        for (var e : species.entrySet()) {
            FishSpecies s = e.getValue();
            if (!unlocked(player, s.rankEnum()) || !s.matches(level, pos, fluid)) continue;
            if (!BuiltInRegistries.ITEM.containsKey(s.item())) continue;
            double w = s.baseWeight();
            if (s.rankEnum().ordinal() >= FishRank.RARE.ordinal()) w *= 1 + luck * 0.1;
            if (w <= 0) continue;
            ids.add(e.getKey());
            weights.add(w);
            total += w;
        }
        if (ids.isEmpty()) return null;
        double r = player.getRandom().nextDouble() * total;
        for (int i = 0; i < ids.size(); i++) {
            r -= weights.get(i);
            if (r <= 0) return ids.get(i);
        }
        return ids.get(ids.size() - 1);
    }

    public static FishDexData dex(ServerPlayer player) {
        return player.getData(ModAttachments.FISH_DEX);
    }
}
