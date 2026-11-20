package com.chigusa.jsncraft.compat;

import com.chigusa.jsncraft.casino.Chips;
import com.chigusa.jsncraft.data.PlayerJobData;
import com.chigusa.jsncraft.job.JobDefinition;
import com.chigusa.jsncraft.job.JobManager;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillManager;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.stat.ResourceService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * jsn-Phone（スマホ）とのつなぎ。どちらのMODも、相手がいなくても動くようにリフレクションで呼び合う。
 * ・スマホの「jsn-Craft」アプリ → {@link #info(ServerPlayer)} を呼んで、職業・スキル・カジノの記録をもらう
 * ・jsn-Craft → {@link #news} でスマホの「お知らせ」に載せる
 */
public final class PhoneBridge {
    private static Method newsMethod;
    private static boolean tried;

    /** スマホのお知らせに載せる（jsn-Phone がなければ何もしない） */
    public static void news(ServerPlayer p, String text, String kind) {
        if (!tried) {
            tried = true;
            if (ModList.get().isLoaded("jsn_phone")) {
                try {
                    newsMethod = Class.forName("com.chigusa.jsnphone.PhoneApi").getMethod("news", ServerPlayer.class, String.class, String.class);
                } catch (Throwable ignored) {
                }
            }
        }
        if (newsMethod == null) return;
        try {
            newsMethod.invoke(null, p, text, kind);
        } catch (Throwable ignored) {
        }
    }

    // ---------------- カジノの記録（プレイヤーのデータに残す） ----------------
    private static CompoundTag casino(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains("jsn_casino_stats")) root.put("jsn_casino_stats", new CompoundTag());
        return root.getCompound("jsn_casino_stats");
    }

    public static void casinoPlayed(ServerPlayer p, long bet, long won) {
        CompoundTag t = casino(p);
        t.putLong("plays", t.getLong("plays") + 1);
        t.putLong("bet", t.getLong("bet") + bet);
        t.putLong("won", t.getLong("won") + won);
        if (won > t.getLong("best")) t.putLong("best", won);
        if (won >= bet * 10 && won >= 50) t.putLong("jackpots", t.getLong("jackpots") + 1);
    }

    // ---------------- スマホのアプリに渡す情報（JSONの文字） ----------------
    public static String info(ServerPlayer p) {
        JsonObject o = new JsonObject();
        PlayerJobData data = JobService.data(p);
        o.addProperty("title", data.getTitle());
        for (JobType type : new JobType[]{JobType.COMBAT, JobType.LIFE}) {
            ResourceLocation id = data.getJob(type);
            JobDefinition def = id == null ? null : JobManager.INSTANCE.get(id);
            JsonObject j = new JsonObject();
            if (def != null) {
                var prog = data.progress(id);
                j.addProperty("name", def.name().getString());
                j.addProperty("level", prog.level);
                j.addProperty("max", def.maxLevel());
                j.addProperty("xp", prog.xp);
                j.addProperty("next", prog.level >= def.maxLevel() ? 0 : JobService.xpForNextLevel(def, prog.level));
            }
            o.add(type == JobType.COMBAT ? "combat" : "life", j);
        }
        // これまで就いた職業
        JsonArray jobs = new JsonArray();
        for (Map.Entry<ResourceLocation, JobDefinition> e : JobManager.INSTANCE.all().entrySet()) {
            int lv = data.level(e.getKey());
            if (lv <= 0) continue;
            JsonObject j = new JsonObject();
            j.addProperty("name", e.getValue().name().getString());
            j.addProperty("type", e.getValue().type() == JobType.COMBAT ? "combat" : "life");
            j.addProperty("level", lv);
            j.addProperty("max", e.getValue().maxLevel());
            jobs.add(j);
        }
        o.add("jobs", jobs);
        // セットしているスキル
        JsonArray skills = new JsonArray();
        for (String s : SkillService.data(p).slots) {
            if (s == null || s.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.tryParse(s);
            SkillDefinition def = id == null ? null : SkillManager.INSTANCE.get(id);
            if (def != null) skills.add(def.name().getString());
        }
        o.add("skills", skills);
        // ステータス
        JsonObject st = new JsonObject();
        st.addProperty("hp", Math.round(p.getHealth()));
        st.addProperty("maxHp", Math.round(p.getMaxHealth()));
        var r = ResourceService.get(p);
        st.addProperty("mana", Math.round(Math.max(0, r.mana)));
        st.addProperty("maxMana", Math.round(ResourceService.maxMana(p)));
        st.addProperty("stamina", Math.round(Math.max(0, r.stamina)));
        st.addProperty("maxStamina", Math.round(ResourceService.maxStamina(p)));
        st.addProperty("luck", Math.round(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK) * 10) / 10.0);
        o.add("stats", st);
        // カジノ
        CompoundTag t = casino(p);
        JsonObject c = new JsonObject();
        c.addProperty("chips", Chips.count(p));
        c.addProperty("plays", t.getLong("plays"));
        c.addProperty("bet", t.getLong("bet"));
        c.addProperty("won", t.getLong("won"));
        c.addProperty("best", t.getLong("best"));
        c.addProperty("jackpots", t.getLong("jackpots"));
        o.add("casino", c);
        return o.toString();
    }

    private PhoneBridge() {}
}
