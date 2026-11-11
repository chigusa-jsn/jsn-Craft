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
        String start,
        String end,
        List<Obj> objectives,
        String xpJob,
        int xpAmount,
        String rewardTitle,
        List<ItemStack> items
) {
    public static final int MAX_OBJECTIVES = 3;
    public static final int MAX_ITEMS = 4;

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
            Codec.STRING.fieldOf("start").forGetter(QuestEditorData::start),
            Codec.STRING.fieldOf("end").forGetter(QuestEditorData::end),
            Obj.CODEC.listOf().fieldOf("objectives").forGetter(QuestEditorData::objectives),
            Codec.STRING.fieldOf("xp_job").forGetter(QuestEditorData::xpJob),
            Codec.INT.fieldOf("xp_amount").forGetter(QuestEditorData::xpAmount),
            Codec.STRING.fieldOf("reward_title").forGetter(QuestEditorData::rewardTitle),
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(QuestEditorData::items)
    ).apply(i, QuestEditorData::new));

    public static QuestEditorData blank() {
        List<Obj> objs = new ArrayList<>();
        for (int i = 0; i < MAX_OBJECTIVES; i++) objs.add(new Obj("break_block", "", 1));
        return new QuestEditorData("", "", "", "normal", "once", "", 0, "", "", objs, "life", 0, "", List.of());
    }
}
