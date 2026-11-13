package com.chigusa.jsncraft.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/**
 * スキル1つ分の設定。data/jsn_craft/jsn_skills/*.json から読み込まれる。
 *
 * type:   active（キーで使う） / passive（覚えたら常に効く）
 * effect: 効果の種類（SkillEffects に書いてあるもの）
 *   アクティブ：sweep（前方なぎ払い） / dash（踏み込み） / slash_line（斬撃を飛ばす）
 *               whirlwind（回転斬り） / parry（受け流し） / blink_slash（駆け抜けて斬る）
 *               buff（一時的に能力アップ） / potion（ポーション効果） / heal_area（周りを回復）
 *   パッシブ：  dodge（攻撃をかわす） / full_hp_bonus（HP満タンで攻撃アップ） / tree_feller（木を丸ごと倒す）
 *               replant（苗木を植え直す） / extra_drop（ドロップが増える）
 * stamina / mana: 使うときに減る量    cooldown: 待ち時間（秒）
 * power:   強さ（攻撃力の何倍か、能力の上がる量など）  bonus: おまけの強さ（踏み込み後の一撃など）
 * radius:  範囲・距離（ブロック）   duration: 続く時間（秒）   chance: 確率（0.1 = 10%）
 * target:  対象（能力値ID、ポーションID、タグなど）   amplifier: ポーションの強さ（0 = I）／連続攻撃の回数
 * cast_time: ため（詠唱）の時間（秒）。0 ならすぐ出る。ため中は足元に魔法陣が出て、動きが遅くなる
 */
public record SkillDefinition(
        Component name,
        Component description,
        ResourceLocation icon,
        String type,
        String effect,
        float stamina,
        float mana,
        float cooldown,
        double power,
        double bonus,
        double radius,
        double duration,
        double chance,
        String target,
        int amplifier,
        float castTime
) {
    public static final Codec<SkillDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(SkillDefinition::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(SkillDefinition::description),
            ResourceLocation.CODEC.optionalFieldOf("icon", ResourceLocation.withDefaultNamespace("book")).forGetter(SkillDefinition::icon),
            Codec.STRING.optionalFieldOf("type", "active").forGetter(SkillDefinition::type),
            Codec.STRING.fieldOf("effect").forGetter(SkillDefinition::effect),
            Codec.FLOAT.optionalFieldOf("stamina", 0F).forGetter(SkillDefinition::stamina),
            Codec.FLOAT.optionalFieldOf("mana", 0F).forGetter(SkillDefinition::mana),
            Codec.FLOAT.optionalFieldOf("cooldown", 0F).forGetter(SkillDefinition::cooldown),
            Codec.DOUBLE.optionalFieldOf("power", 1.0).forGetter(SkillDefinition::power),
            Codec.DOUBLE.optionalFieldOf("bonus", 0.0).forGetter(SkillDefinition::bonus),
            Codec.DOUBLE.optionalFieldOf("radius", 3.0).forGetter(SkillDefinition::radius),
            Codec.DOUBLE.optionalFieldOf("duration", 0.0).forGetter(SkillDefinition::duration),
            Codec.DOUBLE.optionalFieldOf("chance", 0.0).forGetter(SkillDefinition::chance),
            Codec.STRING.optionalFieldOf("target", "").forGetter(SkillDefinition::target),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(SkillDefinition::amplifier),
            Codec.FLOAT.optionalFieldOf("cast_time", 0F).forGetter(SkillDefinition::castTime)
    ).apply(i, SkillDefinition::new));

    public boolean isActive() {
        return type.equals("active");
    }
}
