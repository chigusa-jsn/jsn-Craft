package com.chigusa.jsncraft.compat;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.quest.CustomQuestStore;
import com.chigusa.jsncraft.quest.QuestDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Party Dungeon の「まとめて書き出し／読み込み」に、依頼エディターで作った依頼を入れる。
 * Party Dungeon からリフレクションで呼ばれる（引数・戻り値はマイクラと Java の型だけ）。
 *
 * どこでも同じ形にするため、依頼の中の
 * ・ダンジョンID（攻略の目標・ダンジョン解放）→ "pd:<ダンジョンの印>"
 * ・「先に達成」の依頼ID → "q:<依頼の印>"
 * に置き換えて書き出し、読み込むときに、そのワールドのIDへ戻す。
 */
public final class QuestBundle {
    private static final String PD = "pd:";
    private static final String Q = "q:";

    private static RegistryOps<Tag> ops(MinecraftServer s) {
        return s.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    }

    @Nullable
    private static CompoundTag encode(MinecraftServer s, QuestDefinition def) {
        return QuestDefinition.CODEC.encodeStart(ops(s), def).result()
                .filter(t -> t instanceof CompoundTag).map(t -> (CompoundTag) t).orElse(null);
    }

    @Nullable
    private static QuestDefinition decode(MinecraftServer s, CompoundTag t) {
        return QuestDefinition.CODEC.parse(ops(s), t)
                .resultOrPartial(err -> JsnCraft.LOGGER.error("依頼を読み込めませんでした: {}", err)).orElse(null);
    }

    /** ダンジョンの指定（"d3" や "d3@hard"）を変換する。変換できなければそのまま */
    private static String mapRef(String ref, Map<String, String> map, boolean out) {
        int at = ref.indexOf('@');
        String base = at < 0 ? ref : ref.substring(0, at);
        String suffix = at < 0 ? "" : ref.substring(at);
        if (out) {
            String uid = map.get(base);
            return uid == null ? ref : PD + uid + suffix;
        }
        if (!base.startsWith(PD)) return ref;
        String local = map.get(base.substring(PD.length()));
        return local == null ? ref : local + suffix;
    }

    /** 依頼1つ分の NBT の中のダンジョン指定を変換する */
    private static void mapDungeons(CompoundTag t, Map<String, String> map, boolean out) {
        ListTag obj = t.getList("objectives", Tag.TAG_COMPOUND);
        for (int i = 0; i < obj.size(); i++) {
            CompoundTag o = obj.getCompound(i);
            if ("clear_dungeon".equals(o.getString("type"))) o.putString("target", mapRef(o.getString("target"), map, out));
        }
        if (t.contains("rewards", Tag.TAG_COMPOUND)) {
            CompoundTag r = t.getCompound("rewards");
            if (r.contains("unlock_dungeons", Tag.TAG_LIST)) {
                ListTag l = r.getList("unlock_dungeons", Tag.TAG_STRING);
                ListTag n = new ListTag();
                for (int i = 0; i < l.size(); i++) n.add(StringTag.valueOf(mapRef(l.getString(i), map, out)));
                r.put("unlock_dungeons", n);
            }
        }
    }

    /** こちらの依頼を、どこでも同じ形にする */
    @Nullable
    private static CompoundTag portable(MinecraftServer s, CustomQuestStore store, QuestDefinition def, Map<String, String> dungeonIdToUid) {
        CompoundTag t = encode(s, def);
        if (t == null) return null;
        t = t.copy();
        mapDungeons(t, dungeonIdToUid, true);
        if (t.contains("requires")) {
            ResourceLocation req = ResourceLocation.tryParse(t.getString("requires"));
            if (req != null && store.find(req) != null) t.putString("requires", Q + store.uidOf(req));
        }
        return t;
    }

    // ------------------------------------------------------------------ 書き出し

    public static CompoundTag export(MinecraftServer s, Map<String, String> dungeonIdToUid) {
        CompoundTag part = new CompoundTag();
        ListTag list = new ListTag();
        CustomQuestStore store = CustomQuestStore.get();
        if (store != null) {
            for (Map.Entry<ResourceLocation, QuestDefinition> e : store.all().entrySet()) {
                CompoundTag t = portable(s, store, e.getValue(), dungeonIdToUid);
                if (t == null) continue;
                String hash = Canon.hash(t);
                store.setSync(e.getKey(), hash);
                CompoundTag q = new CompoundTag();
                q.putString("uid", store.uidOf(e.getKey()));
                q.putString("name", e.getValue().title().getString());
                q.putBoolean("disabled", store.isDisabled(e.getKey()));
                q.putString("hash", hash);
                q.put("def", t);
                list.add(q);
            }
        }
        part.put("quests", list);
        return part;
    }

    // ------------------------------------------------------------------ 確認

    public static ListTag preview(MinecraftServer s, CompoundTag part, Map<String, String> dungeonIdToUid) {
        ListTag rows = new ListTag();
        CustomQuestStore store = CustomQuestStore.get();
        ListTag list = part.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag q = list.getCompound(i);
            String status = "new";
            ResourceLocation local = store == null ? null : store.idByUid(q.getString("uid"));
            if (local != null) {
                CompoundTag now = portable(s, store, store.find(local), dungeonIdToUid);
                String h = now == null ? "" : Canon.hash(now);
                if (h.equals(q.getString("hash"))) status = "same";
                else if (h.equals(store.syncOf(local))) status = "update";
                else status = "changed";
            }
            CompoundTag r = new CompoundTag();
            r.putString("uid", q.getString("uid"));
            r.putString("name", q.getString("name"));
            r.putString("status", status);
            rows.add(r);
        }
        return rows;
    }

    // ------------------------------------------------------------------ 読み込み

    /**
     * @param dungeonUidToRef ダンジョンの印 → こちらのダンジョンID（こちらにないダンジョンは名前）
     */
    public static String apply(MinecraftServer s, CompoundTag part, List<String> uids, Map<String, String> dungeonUidToRef) {
        CustomQuestStore store = CustomQuestStore.get();
        if (store == null) return "依頼を読み込めませんでした。";
        Map<String, CompoundTag> picked = new LinkedHashMap<>();
        ListTag list = part.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag q = list.getCompound(i);
            if (uids.contains(q.getString("uid"))) picked.put(q.getString("uid"), q);
        }
        int added = 0, updated = 0, failed = 0;
        // 1回目：ダンジョンだけ直した仮の中身で入れて、IDを決める（「先に達成」が新しい依頼を指していても大丈夫なように）
        Map<String, ResourceLocation> ids = new HashMap<>();
        for (Map.Entry<String, CompoundTag> e : picked.entrySet()) {
            CompoundTag t = e.getValue().getCompound("def").copy();
            mapDungeons(t, dungeonUidToRef, false);
            t.remove("requires");
            QuestDefinition def = decode(s, t);
            if (def == null) {
                failed++;
                continue;
            }
            ResourceLocation id = store.idByUid(e.getKey());
            if (id == null) {
                id = store.add(def);
                store.setUid(id, e.getKey());
                store.setDisabled(id, e.getValue().getBoolean("disabled"));
                added++;
            } else {
                updated++;
            }
            ids.put(e.getKey(), id);
        }
        // 2回目：「先に達成」もこちらのIDに直して、中身を入れ替える
        Map<String, String> back = new HashMap<>();
        dungeonUidToRef.forEach((uid, ref) -> back.put(ref, uid));
        for (Map.Entry<String, ResourceLocation> e : ids.entrySet()) {
            CompoundTag t = picked.get(e.getKey()).getCompound("def").copy();
            mapDungeons(t, dungeonUidToRef, false);
            if (t.contains("requires")) {
                String req = t.getString("requires");
                if (req.startsWith(Q)) {
                    ResourceLocation local = store.idByUid(req.substring(Q.length()));
                    if (local != null) t.putString("requires", local.toString());
                    else t.remove("requires");
                }
            }
            QuestDefinition def = decode(s, t);
            if (def == null) continue;
            store.replace(e.getValue(), def);
            CompoundTag now = portable(s, store, def, back);
            store.setSync(e.getValue(), now == null ? "" : Canon.hash(now));
        }
        return "依頼：追加 " + added + "・更新 " + updated + (failed > 0 ? "・読めなかった " + failed : "");
    }

    private QuestBundle() {}
}
