package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** スキルの通信まとめ。 */
public final class SkillPayloads {

    /** スキル1つ（メニューとスキルバーの表示用） */
    public record SkillView(String id, Component name, Component description, ResourceLocation icon, boolean active,
                            int level, Component jobName, boolean learned, float stamina, float mana, float cooldown) {
        public static final Codec<SkillView> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(SkillView::id),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(SkillView::name),
                ComponentSerialization.CODEC.fieldOf("description").forGetter(SkillView::description),
                ResourceLocation.CODEC.fieldOf("icon").forGetter(SkillView::icon),
                Codec.BOOL.fieldOf("active").forGetter(SkillView::active),
                Codec.INT.fieldOf("level").forGetter(SkillView::level),
                ComponentSerialization.CODEC.fieldOf("job_name").forGetter(SkillView::jobName),
                Codec.BOOL.fieldOf("learned").forGetter(SkillView::learned),
                Codec.FLOAT.fieldOf("stamina").forGetter(SkillView::stamina),
                Codec.FLOAT.fieldOf("mana").forGetter(SkillView::mana),
                Codec.FLOAT.fieldOf("cooldown").forGetter(SkillView::cooldown)
        ).apply(i, SkillView::new));
    }

    /**
     * サーバー → 本人：スキルの一覧、スキルバーの5枠、各枠の残り待ち時間（秒）。
     * slots の空文字は空き枠。
     */
    public record State(List<SkillView> skills, List<String> slots, List<Float> cooldownLeft) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(JsnCraft.id("skill_state"));
        public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
                SkillView.CODEC.listOf().fieldOf("skills").forGetter(State::skills),
                Codec.STRING.listOf().fieldOf("slots").forGetter(State::slots),
                Codec.FLOAT.listOf().fieldOf("cooldown_left").forGetter(State::cooldownLeft)
        ).apply(i, State::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 本人 → サーバー：スキルバーの○番を使う */
    public record Cast(int slot) implements CustomPacketPayload {
        public static final Type<Cast> TYPE = new Type<>(JsnCraft.id("skill_cast"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Cast> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Cast::slot, Cast::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 本人 → サーバー：スキルバーの○番にこのスキルをセット（空文字で外す） */
    public record Assign(int slot, String skill) implements CustomPacketPayload {
        public static final Type<Assign> TYPE = new Type<>(JsnCraft.id("skill_assign"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Assign> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Assign::slot,
                ByteBufCodecs.STRING_UTF8, Assign::skill,
                Assign::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private SkillPayloads() {}
}
