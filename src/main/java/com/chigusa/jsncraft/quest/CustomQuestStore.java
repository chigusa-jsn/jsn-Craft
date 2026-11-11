package com.chigusa.jsncraft.quest;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * ゲームの中（依頼エディター）で作った依頼。ワールドのセーブデータに保存される。
 * IDは jsn_craft:custom_1, custom_2 … の順につく。
 */
public class CustomQuestStore extends SavedData {

    private static final String FILE = "jsn_craft_custom_quests";

    @Nullable private static CustomQuestStore current;

    private final Map<ResourceLocation, QuestDefinition> quests = new LinkedHashMap<>();
    private final Set<ResourceLocation> disabled = new LinkedHashSet<>();
    private int nextId = 1;

    public static void onServerStarted(MinecraftServer server) {
        current = server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CustomQuestStore::new, CustomQuestStore::load, null), FILE);
    }

    public static void onServerStopped() {
        current = null;
    }

    @Nullable
    public static CustomQuestStore get() {
        return current;
    }

    // ---- 読み書き ----

    public Map<ResourceLocation, QuestDefinition> all() {
        return Collections.unmodifiableMap(quests);
    }

    public boolean isDisabled(ResourceLocation id) {
        return disabled.contains(id);
    }

    @Nullable
    public QuestDefinition find(ResourceLocation id) {
        return quests.get(id);
    }

    /** 新しく追加して、ついたIDを返す。 */
    public ResourceLocation add(QuestDefinition def) {
        ResourceLocation id;
        do {
            id = JsnCraft.id("custom_" + nextId++);
        } while (quests.containsKey(id));
        quests.put(id, def);
        setDirty();
        return id;
    }

    public void replace(ResourceLocation id, QuestDefinition def) {
        quests.put(id, def);
        setDirty();
    }

    public boolean remove(ResourceLocation id) {
        disabled.remove(id);
        boolean removed = quests.remove(id) != null;
        if (removed) setDirty();
        return removed;
    }

    /** 一時停止 ⇔ 再開。停止中なら true を返す。 */
    public boolean toggle(ResourceLocation id) {
        boolean nowDisabled = disabled.contains(id) ? !disabled.remove(id) : disabled.add(id);
        setDirty();
        return nowDisabled;
    }

    // ---- セーブ ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        quests.forEach((id, def) -> QuestDefinition.CODEC.encodeStart(ops, def)
                .resultOrPartial(err -> JsnCraft.LOGGER.error("依頼 {} を保存できませんでした: {}", id, err))
                .ifPresent(t -> {
                    CompoundTag e = new CompoundTag();
                    e.putString("id", id.toString());
                    e.put("def", t);
                    e.putBoolean("disabled", disabled.contains(id));
                    list.add(e);
                }));
        tag.put("quests", list);
        tag.putInt("next_id", nextId);
        return tag;
    }

    private static CustomQuestStore load(CompoundTag tag, HolderLookup.Provider registries) {
        CustomQuestStore store = new CustomQuestStore();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = tag.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(e.getString("id"));
            if (id == null) continue;
            QuestDefinition.CODEC.parse(ops, e.get("def"))
                    .resultOrPartial(err -> JsnCraft.LOGGER.error("依頼 {} を読み込めませんでした: {}", id, err))
                    .ifPresent(def -> {
                        store.quests.put(id, def);
                        if (e.getBoolean("disabled")) store.disabled.add(id);
                    });
        }
        store.nextId = Math.max(1, tag.getInt("next_id"));
        return store;
    }
}
