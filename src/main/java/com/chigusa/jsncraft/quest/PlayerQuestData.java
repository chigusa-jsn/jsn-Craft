package com.chigusa.jsncraft.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * プレイヤーの依頼データ。
 * active   … 受注中の依頼と、目標ごとの進み具合
 * reported … 報告済みで、受付嬢から報酬をまだ受け取っていない依頼
 * completed… 達成した依頼と、最後に達成した日（日替わり依頼の判定に使う）
 * titles   … 依頼でもらった肩書
 */
public class PlayerQuestData {
    public static final Codec<PlayerQuestData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT.listOf())
                    .optionalFieldOf("active", Map.of()).forGetter(d -> d.active),
            ResourceLocation.CODEC.listOf()
                    .optionalFieldOf("reported", List.of()).forGetter(d -> new ArrayList<>(d.reported)),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                    .optionalFieldOf("completed", Map.of()).forGetter(d -> d.completed),
            Codec.STRING.listOf().optionalFieldOf("titles", List.of()).forGetter(d -> new ArrayList<>(d.titles))
    ).apply(i, PlayerQuestData::new));

    public final Map<ResourceLocation, List<Integer>> active = new LinkedHashMap<>();
    public final Set<ResourceLocation> reported = new LinkedHashSet<>();
    public final Map<ResourceLocation, Long> completed = new LinkedHashMap<>();
    public final Set<String> titles = new LinkedHashSet<>();

    public PlayerQuestData() {}

    private PlayerQuestData(Map<ResourceLocation, List<Integer>> active, List<ResourceLocation> reported,
                            Map<ResourceLocation, Long> completed, List<String> titles) {
        active.forEach((k, v) -> this.active.put(k, new ArrayList<>(v)));
        this.reported.addAll(reported);
        this.completed.putAll(completed);
        this.titles.addAll(titles);
    }
}
