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
 * スキルボードのマス1つ。
 *
 * x, y   … 盤面上の位置（1マス単位）
 * start  … 最初から解放できるマスなら true
 * links  … つながっているマスのID。つながったマスのどれかを解放済みなら、このマスも解放できる
 *          （つながりは両向き。ツリー型なら「親」だけを書けばいい）
 * cost   … 必要なスキルポイント
 * effects… 解放すると上がる能力
 */
public record BoardNode(
        String id,
        int x,
        int y,
        Component name,
        Optional<ResourceLocation> icon,
        boolean start,
        int cost,
        List<String> links,
        List<Effect> effects
) {
    public static final Codec<BoardNode> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(BoardNode::id),
            Codec.INT.fieldOf("x").forGetter(BoardNode::x),
            Codec.INT.fieldOf("y").forGetter(BoardNode::y),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(BoardNode::name),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(BoardNode::icon),
            Codec.BOOL.optionalFieldOf("start", false).forGetter(BoardNode::start),
            Codec.intRange(0, 100).optionalFieldOf("cost", 1).forGetter(BoardNode::cost),
            Codec.STRING.listOf().optionalFieldOf("links", List.of()).forGetter(BoardNode::links),
            Effect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(BoardNode::effects)
    ).apply(i, BoardNode::new));

    /**
     * 能力アップ1つ分。attribute はマイクラの能力値ID（例: minecraft:generic.attack_damage）。
     * operation: add_value（そのまま足す） / add_multiplied_base（元の値の○%足す） / add_multiplied_total（最終値の○%足す）
     */
    public record Effect(ResourceLocation attribute, double amount, AttributeModifier.Operation operation) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("attribute").forGetter(Effect::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(Effect::amount),
                AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                        .forGetter(Effect::operation)
        ).apply(i, Effect::new));
    }
}
