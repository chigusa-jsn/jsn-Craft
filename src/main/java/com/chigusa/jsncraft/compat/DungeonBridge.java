package com.chigusa.jsncraft.compat;

import com.chigusa.jsncraft.quest.QuestDefinition;
import com.chigusa.jsncraft.quest.QuestManager;
import com.chigusa.jsncraft.quest.QuestService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Party Dungeon とのつなぎ（どちらのMODも、相手がいなくても動く）。
 * ・Party Dungeon → {@link #onClear}：ダンジョンをクリアした（依頼の「ダンジョンをクリアする」を進める）
 * ・Party Dungeon → {@link #unlockHint}：まだのダンジョンを、どの依頼で解放できるか
 * ・jsn-Craft → Party Dungeon の PDApi：ダンジョンの一覧・解放
 */
public final class DungeonBridge {
    public static final String[] DIFFS = {"easy", "normal", "hard", "very_hard", "chaos"};
    public static final String[] DIFF_LABELS = {"イージー", "ノーマル", "ハード", "ベリーハード", "カオス"};
    private static final String API = "com.chigusa.partydungeon.api.PDApi";

    public static int diffRank(String id) {
        for (int i = 0; i < DIFFS.length; i++) if (DIFFS[i].equals(id)) return i;
        return 0;
    }

    // ---------------- Party Dungeon から呼ばれる ----------------

    /** ダンジョンをクリアした（パーティの1人ずつ） */
    public static void onClear(ServerPlayer p, String dungeonId, String dungeonName, String difficulty) {
        QuestService.onDungeonClear(p, dungeonId, dungeonName, difficulty);
    }

    /** そのダンジョンを解放できる依頼の名前（なければ空） */
    public static String unlockHint(String dungeonId, String dungeonName) {
        for (Map.Entry<net.minecraft.resources.ResourceLocation, QuestDefinition> e : QuestManager.INSTANCE.all().entrySet()) {
            for (String d : e.getValue().rewards().unlockDungeons())
                if (d.equals(dungeonId) || d.equals(dungeonName)) return e.getValue().title().getString();
        }
        return "";
    }

    // ---------------- Party Dungeon を呼ぶ ----------------

    private static Method m(String name, Class<?>... args) {
        if (!ModList.get().isLoaded("partydungeon")) return null;
        try {
            return Class.forName(API).getMethod(name, args);
        } catch (Throwable t) {
            return null;
        }
    }

    /** ダンジョンの一覧 [id, 名前] */
    @SuppressWarnings("unchecked")
    public static List<String[]> dungeons(MinecraftServer server) {
        Method m = m("dungeons", MinecraftServer.class);
        if (m == null) return List.of();
        try {
            return (List<String[]>) m.invoke(null, server);
        } catch (Throwable t) {
            return List.of();
        }
    }

    /** ダンジョンを解放する（名前かID）。解放したダンジョンの名前、できなければ null */
    public static String unlock(ServerPlayer p, String dungeon) {
        Method m = m("unlock", ServerPlayer.class, String.class);
        if (m == null) return null;
        try {
            return (String) m.invoke(null, p, dungeon);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 表示用の名前（"id@hard" → "はじまりの洞窟（ハード以上）"） */
    public static String label(String target) {
        String[] parts = target.split("@", 2);
        String name = parts[0];
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null) for (String[] d : dungeons(s)) if (d[0].equals(parts[0]) || d[1].equals(parts[0])) name = d[1];
        String diff = parts.length > 1 && diffRank(parts[1]) > 0 ? "（" + DIFF_LABELS[diffRank(parts[1])] + "以上）" : "";
        return name + diff;
    }

    public static List<String> diffs() {
        return new ArrayList<>(List.of(DIFFS));
    }

    private DungeonBridge() {}
}
