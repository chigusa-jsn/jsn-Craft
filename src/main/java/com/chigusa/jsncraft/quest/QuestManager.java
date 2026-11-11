package com.chigusa.jsncraft.quest;

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

/** data/<名前空間>/jsn_quests/ のJSONを読み込む係。/reload で読み直せる。 */
public class QuestManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final QuestManager INSTANCE = new QuestManager();

    private Map<ResourceLocation, QuestDefinition> quests = Map.of();

    private QuestManager() {
        super(GSON, "jsn_quests");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, QuestDefinition> loaded = new LinkedHashMap<>();
        files.forEach((id, json) -> QuestDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> JsnCraft.LOGGER.error("依頼 {} の読み込みに失敗しました: {}", id, err))
                .ifPresent(def -> loaded.put(id, def)));
        quests = Collections.unmodifiableMap(loaded);
        JsnCraft.LOGGER.info("[jsn-Craft] {} 個の依頼を読み込みました", quests.size());
    }

    /** 依頼を探す。ファイルの依頼 → ゲーム内で作った依頼（一時停止中も含む）の順。 */
    @Nullable
    public QuestDefinition get(@Nullable ResourceLocation id) {
        if (id == null) return null;
        QuestDefinition def = quests.get(id);
        if (def != null) return def;
        CustomQuestStore custom = CustomQuestStore.get();
        return custom == null ? null : custom.find(id);
    }

    /** 今募集している依頼の一覧（ファイルの依頼＋ゲーム内で作った依頼のうち、一時停止していないもの）。 */
    public Map<ResourceLocation, QuestDefinition> all() {
        CustomQuestStore custom = CustomQuestStore.get();
        if (custom == null || custom.all().isEmpty()) return quests;
        Map<ResourceLocation, QuestDefinition> merged = new LinkedHashMap<>(quests);
        custom.all().forEach((id, def) -> {
            if (!custom.isDisabled(id)) merged.put(id, def);
        });
        return merged;
    }
}
