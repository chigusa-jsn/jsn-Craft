package com.chigusa.jsncraft.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * 隠し味。data/<名前空間>/jsn_seasonings/*.json から読む。
 * { "item": "minecraft:sugar", "name": "甘み", "quality": 0.0, "buffs": [ … ] }
 *   item    … アイテムID か "#タグ"
 *   quality … 品質が上がりやすくなる量（0.3 なら +30%）
 */
public record Seasoning(String item, String name, double quality, List<FoodBuff> buffs) {
    public static final Codec<Seasoning> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("item").forGetter(Seasoning::item),
            Codec.STRING.optionalFieldOf("name", "").forGetter(Seasoning::name),
            Codec.DOUBLE.optionalFieldOf("quality", 0.0).forGetter(Seasoning::quality),
            FoodBuff.CODEC.listOf().optionalFieldOf("buffs", List.of()).forGetter(Seasoning::buffs)
    ).apply(i, Seasoning::new));
}
