package com.chigusa.jsncraft.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 職業1つ分の設定。data/jsn_craft/jsn_jobs/*.json から読み込まれる。
 * 職業を増やすときは、Javaを書かずにJSONファイルを1つ足すだけでOK。
 */
public record JobDefinition(
        Component name,
        JobType type,
        JobTier tier,
        int maxLevel,
        List<Requirement> requires,
        List<XpRule> xp
) {
    public static final Codec<JobDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(JobDefinition::name),
            JobType.CODEC.fieldOf("type").forGetter(JobDefinition::type),
            JobTier.CODEC.optionalFieldOf("tier", JobTier.BASIC).forGetter(JobDefinition::tier),
            Codec.intRange(1, 1000).optionalFieldOf("max_level", 50).forGetter(JobDefinition::maxLevel),
            Requirement.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(JobDefinition::requires),
            XpRule.CODEC.listOf().optionalFieldOf("xp", List.of()).forGetter(JobDefinition::xp)
    ).apply(i, JobDefinition::new));

    /** 転職条件：「この職業がこのレベル以上」。合体職なら2つ書けばいい。 */
    public record Requirement(ResourceLocation job, int level) {
        public static final Codec<Requirement> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("job").forGetter(Requirement::job),
                Codec.INT.fieldOf("level").forGetter(Requirement::level)
        ).apply(i, Requirement::new));
    }

    /**
     * 経験値のもらえる行動。
     * action: "break_block"（ブロックを壊す） / "kill_entity"（モブを倒す）
     * target: "minecraft:stone" のようなID、"#minecraft:logs" のようなタグ、"*" で何でも
     */
    public record XpRule(String action, String target, int xp) {
        public static final Codec<XpRule> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("action").forGetter(XpRule::action),
                Codec.STRING.fieldOf("target").forGetter(XpRule::target),
                Codec.INT.fieldOf("xp").forGetter(XpRule::xp)
        ).apply(i, XpRule::new));

        public boolean matchesBlock(BlockState state) {
            if (target.equals("*")) return true;
            if (target.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
                return tag != null && state.is(TagKey.create(Registries.BLOCK, tag));
            }
            ResourceLocation id = ResourceLocation.tryParse(target);
            return id != null && id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        }

        public boolean matchesEntity(Entity entity) {
            if (target.equals("*")) return true;
            if (target.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
                return tag != null && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, tag));
            }
            ResourceLocation id = ResourceLocation.tryParse(target);
            return id != null && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        }
    }
}
