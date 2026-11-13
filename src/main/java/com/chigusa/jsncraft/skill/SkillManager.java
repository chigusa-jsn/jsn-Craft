package com.chigusa.jsncraft.skill;

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

/** data/<名前空間>/jsn_skills/ のJSONを読み込む係（サブフォルダもOK：jsn_skills/sword/sweep.json → jsn_craft:sword/sweep）。 */
public class SkillManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final SkillManager INSTANCE = new SkillManager();

    private Map<ResourceLocation, SkillDefinition> skills = Map.of();

    private SkillManager() {
        super(GSON, "jsn_skills");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, SkillDefinition> loaded = new LinkedHashMap<>();
        files.forEach((id, json) -> SkillDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> JsnCraft.LOGGER.error("スキル {} の読み込みに失敗しました: {}", id, err))
                .ifPresent(def -> loaded.put(id, def)));
        skills = Collections.unmodifiableMap(loaded);
        JsnCraft.LOGGER.info("[jsn-Craft] {} 個のスキルを読み込みました", skills.size());
    }

    @Nullable
    public SkillDefinition get(@Nullable ResourceLocation id) {
        return id == null ? null : skills.get(id);
    }
}
