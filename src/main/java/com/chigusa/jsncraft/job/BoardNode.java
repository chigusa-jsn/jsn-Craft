package com.chigusa.jsncraft.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.Optional;

/**
 * スキルボードのマス（パネル）1つ。
 *
 * x, y      … 盤面上の位置（1マス単位、小数OK。六角形なら1段ごとに0.5ずらす）
 * start     … 最初から解放できるマスなら true
 * links     … つながっているマスのID。つながったマスのどれかを解放済みなら、このマスも解放できる（両向き）
 * cost      … 必要なスキルポイント
 * effects   … 解放したときの効果（下の Effect）
 * hidden    … true なら「？パネル」。隣を開けるまで中身が見えない
 * min_level … この職業レベル以上でないと開けられない（鍵パネル）
 * big       … true なら大きく表示する（★パネル）
 */
public record BoardNode(
        String id,
        double x,
        double y,
        Component name,
        Optional<ResourceLocation> icon,
        boolean start,
        int cost,
        List<String> links,
        List<Effect> effects,
        boolean hidden,
        int minLevel,
        boolean big
) {
    public static final Codec<BoardNode> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(BoardNode::id),
            Codec.DOUBLE.fieldOf("x").forGetter(BoardNode::x),
            Codec.DOUBLE.fieldOf("y").forGetter(BoardNode::y),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(BoardNode::name),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(BoardNode::icon),
            Codec.BOOL.optionalFieldOf("start", false).forGetter(BoardNode::start),
            Codec.intRange(0, 100).optionalFieldOf("cost", 1).forGetter(BoardNode::cost),
            Codec.STRING.listOf().optionalFieldOf("links", List.of()).forGetter(BoardNode::links),
            Effect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(BoardNode::effects),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(BoardNode::hidden),
            Codec.INT.optionalFieldOf("min_level", 0).forGetter(BoardNode::minLevel),
            Codec.BOOL.optionalFieldOf("big", false).forGetter(BoardNode::big)
    ).apply(i, BoardNode::new));

    /**
     * パネルの効果1つ。書き方は3通り：
     *
     *  能力値アップ   { "attribute": "minecraft:generic.attack_damage", "amount": 0.5 }
     *                 operation: add_value（そのまま足す） / add_multiplied_base / add_multiplied_total（○%足す）
     *  技の強化       { "skill": "jsn_craft:sword/sweep", "field": "power_pct", "amount": 0.15 }
     *                 field: power_pct（威力○%） / radius（範囲） / cooldown（待ち時間・秒） / stamina（消費ST）
     *                        amplifier（回数） / duration（時間・秒） / cast_time（ため・秒） / chance（確率）
     *  パッシブを覚える { "grant": "jsn_craft:sword/board/last_stand" }
     */
    public record Effect(Optional<ResourceLocation> attribute, Optional<ResourceLocation> skill, String field,
                         Optional<ResourceLocation> grant, double amount, AttributeModifier.Operation operation) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("attribute").forGetter(Effect::attribute),
                ResourceLocation.CODEC.optionalFieldOf("skill").forGetter(Effect::skill),
                Codec.STRING.optionalFieldOf("field", "").forGetter(Effect::field),
                ResourceLocation.CODEC.optionalFieldOf("grant").forGetter(Effect::grant),
                Codec.DOUBLE.optionalFieldOf("amount", 0.0).forGetter(Effect::amount),
                AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                        .forGetter(Effect::operation)
        ).apply(i, Effect::new));
    }
}
