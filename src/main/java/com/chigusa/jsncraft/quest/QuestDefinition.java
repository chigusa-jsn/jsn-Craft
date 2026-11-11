package com.chigusa.jsncraft.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * 依頼1つ分の設定。data/jsn_craft/jsn_quests/*.json から読み込まれる。
 *
 * category: normal（通常依頼） / job（職業依頼） / special（運営の特別依頼）
 * repeat:   once（1回だけ） / daily（1日1回） / always（何度でも）
 * start/end: "2026-12-24T00:00" の形。サーバーのPCの時刻で判定。書かなければいつでも。
 */
public record QuestDefinition(
        Component title,
        Component description,
        String category,
        Optional<ResourceLocation> requiredJob,
        int minLevel,
        String repeat,
        Optional<String> start,
        Optional<String> end,
        List<Objective> objectives,
        Rewards rewards
) {
    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("title").forGetter(QuestDefinition::title),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(QuestDefinition::description),
            Codec.STRING.optionalFieldOf("category", "normal").forGetter(QuestDefinition::category),
            ResourceLocation.CODEC.optionalFieldOf("required_job").forGetter(QuestDefinition::requiredJob),
            Codec.INT.optionalFieldOf("min_level", 0).forGetter(QuestDefinition::minLevel),
            Codec.STRING.optionalFieldOf("repeat", "once").forGetter(QuestDefinition::repeat),
            Codec.STRING.optionalFieldOf("start").forGetter(QuestDefinition::start),
            Codec.STRING.optionalFieldOf("end").forGetter(QuestDefinition::end),
            Objective.CODEC.listOf().fieldOf("objectives").forGetter(QuestDefinition::objectives),
            Rewards.CODEC.optionalFieldOf("rewards", Rewards.NONE).forGetter(QuestDefinition::rewards)
    ).apply(i, QuestDefinition::new));

    /**
     * 目標1つ。
     * type: break_block（ブロックを壊す） / kill_entity（倒す） / deliver_item（納品） / reach_level（職業レベル）
     * target: ID（minecraft:oak_log）、タグ（#minecraft:logs）、"*"（何でも）。reach_level なら職業ID
     * count: 個数。reach_level ならレベル
     * label: 画面に出す説明（省略すると自動で作る）
     */
    public record Objective(String type, String target, int count, Optional<Component> label) {
        public static final Codec<Objective> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(Objective::type),
                Codec.STRING.fieldOf("target").forGetter(Objective::target),
                Codec.intRange(1, 1_000_000).optionalFieldOf("count", 1).forGetter(Objective::count),
                ComponentSerialization.CODEC.optionalFieldOf("label").forGetter(Objective::label)
        ).apply(i, Objective::new));

        /** 行動するたびに数える目標か（納品とレベルは報告のときに確認する） */
        public boolean isCounted() {
            return type.equals("break_block") || type.equals("kill_entity");
        }
    }

    /** ごほうび。job_xp の job は "life" / "combat"（今の職業）か、職業ID。 */
    public record Rewards(List<JobXp> jobXp, List<ItemReward> items, Optional<String> title) {
        public static final Rewards NONE = new Rewards(List.of(), List.of(), Optional.empty());
        public static final Codec<Rewards> CODEC = RecordCodecBuilder.create(i -> i.group(
                JobXp.CODEC.listOf().optionalFieldOf("job_xp", List.of()).forGetter(Rewards::jobXp),
                ItemReward.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(Rewards::items),
                Codec.STRING.optionalFieldOf("title").forGetter(Rewards::title)
        ).apply(i, Rewards::new));
    }

    public record JobXp(String job, int amount) {
        public static final Codec<JobXp> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("job").forGetter(JobXp::job),
                Codec.INT.fieldOf("amount").forGetter(JobXp::amount)
        ).apply(i, JobXp::new));
    }

    public record ItemReward(ResourceLocation id, int count) {
        public static final Codec<ItemReward> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(ItemReward::id),
                Codec.intRange(1, 64 * 36).optionalFieldOf("count", 1).forGetter(ItemReward::count)
        ).apply(i, ItemReward::new));
    }

    // ---- 期間 ----

    public Optional<LocalDateTime> startTime() {
        return start.flatMap(QuestDefinition::parse);
    }

    public Optional<LocalDateTime> endTime() {
        return end.flatMap(QuestDefinition::parse);
    }

    public boolean isOpen(LocalDateTime now) {
        if (startTime().map(now::isBefore).orElse(false)) return false;
        return !endTime().map(now::isAfter).orElse(false);
    }

    private static Optional<LocalDateTime> parse(String s) {
        try {
            return Optional.of(LocalDateTime.parse(s));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
