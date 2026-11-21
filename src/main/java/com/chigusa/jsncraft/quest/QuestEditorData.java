package com.chigusa.jsncraft.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 依頼エディターの入力内容。サーバーとクライアントの間でやりとりする。
 * id が空なら新しい依頼、"jsn_craft:custom_3" のように入っていればその依頼の編集。
 */
public record QuestEditorData(
        String id,
        String title,
        String description,
        String category,
        String repeat,
        String requiredJob,
        int minLevel,
        Period period,
        List<Obj> objectives,
        String xpJob,
        int xpAmount,
        String rewardTitle,
        List<ItemStack> items,
        List<ItemStack> choices,
        int choose,
        Extra extra
) {
    /** 報酬を隠すか・先に達成が必要な依頼・解放するダンジョン */
    public record Extra(boolean hideRewards, String requires, String unlockDungeon) {
        public static final Codec<Extra> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.fieldOf("hide_rewards").forGetter(Extra::hideRewards),
                Codec.STRING.optionalFieldOf("requires", "").forGetter(Extra::requires),
                Codec.STRING.optionalFieldOf("unlock_dungeon", "").forGetter(Extra::unlockDungeon)
        ).apply(i, Extra::new));
    }

    public boolean hideRewards() {
        return extra.hideRewards();
    }

    /** 受けられる期間（"2026-12-24 18:00" の形、空なら指定なし） */
    public record Period(String start, String end) {
        public static final Codec<Period> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("start").forGetter(Period::start),
                Codec.STRING.fieldOf("end").forGetter(Period::end)
        ).apply(i, Period::new));
    }

    public String start() {
        return period.start();
    }

    public String end() {
        return period.end();
    }

    public static final int MAX_OBJECTIVES = 3;
    public static final int MAX_ITEMS = 4;
    public static final int MAX_CHOICES = 5;

    public record Obj(String type, String target, int count) {
        public static final Codec<Obj> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(Obj::type),
                Codec.STRING.fieldOf("target").forGetter(Obj::target),
                Codec.INT.fieldOf("count").forGetter(Obj::count)
        ).apply(i, Obj::new));
    }

    public static final Codec<QuestEditorData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(QuestEditorData::id),
            Codec.STRING.fieldOf("title").forGetter(QuestEditorData::title),
            Codec.STRING.fieldOf("description").forGetter(QuestEditorData::description),
            Codec.STRING.fieldOf("category").forGetter(QuestEditorData::category),
            Codec.STRING.fieldOf("repeat").forGetter(QuestEditorData::repeat),
            Codec.STRING.fieldOf("required_job").forGetter(QuestEditorData::requiredJob),
            Codec.INT.fieldOf("min_level").forGetter(QuestEditorData::minLevel),
            Period.CODEC.fieldOf("period").forGetter(QuestEditorData::period),
            Obj.CODEC.listOf().fieldOf("objectives").forGetter(QuestEditorData::objectives),
            Codec.STRING.fieldOf("xp_job").forGetter(QuestEditorData::xpJob),
            Codec.INT.fieldOf("xp_amount").forGetter(QuestEditorData::xpAmount),
            Codec.STRING.fieldOf("reward_title").forGetter(QuestEditorData::rewardTitle),
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(QuestEditorData::items),
            ItemStack.CODEC.listOf().fieldOf("choices").forGetter(QuestEditorData::choices),
            Codec.INT.fieldOf("choose").forGetter(QuestEditorData::choose),
            Extra.CODEC.fieldOf("extra").forGetter(QuestEditorData::extra)
    ).apply(i, QuestEditorData::new));

    public static QuestEditorData blank() {
        List<Obj> objs = new ArrayList<>();
        for (int i = 0; i < MAX_OBJECTIVES; i++) objs.add(new Obj("break_block", "", 1));
        return new QuestEditorData("", "", "", "normal", "once", "", 0, new Period("", ""), objs, "life", 0, "", List.of(), List.of(), 1, new Extra(false, "", ""));
    }
}
