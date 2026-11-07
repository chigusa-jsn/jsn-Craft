package com.chigusa.jsncraft.job;

import com.chigusa.jsncraft.JsnCraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** data/<名前空間>/jsn_jobs/ にあるJSONを読み込んで、職業の一覧を持っておく係。 */
public class JobManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final JobManager INSTANCE = new JobManager();

    private Map<ResourceLocation, JobDefinition> jobs = Map.of();

    private JobManager() {
        super(GSON, "jsn_jobs");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JobDefinition> loaded = new LinkedHashMap<>();
        files.forEach((id, json) -> JobDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> JsnCraft.LOGGER.error("職業 {} の読み込みに失敗しました: {}", id, err))
                .ifPresent(def -> loaded.put(id, def)));
        jobs = Collections.unmodifiableMap(loaded);
        JsnCraft.LOGGER.info("[jsn-Craft] {} 個の職業を読み込みました: {}", jobs.size(), jobs.keySet());
    }

    @Nullable
    public JobDefinition get(@Nullable ResourceLocation id) {
        return id == null ? null : jobs.get(id);
    }

    public Map<ResourceLocation, JobDefinition> all() {
        return jobs;
    }
}
