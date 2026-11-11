package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.quest.QuestEditorData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** 依頼エディターの通信まとめ。 */
public final class QuestEditorPayloads {

    /** 職業の選択肢（エディターの「職業条件」用） */
    public record JobOption(String id, Component name) {
        public static final Codec<JobOption> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(JobOption::id),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(JobOption::name)
        ).apply(i, JobOption::new));
    }

    /** サーバー → OP：エディターを開く */
    public record Open(QuestEditorData data, List<JobOption> jobs) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(JsnCraft.id("quest_editor_open"));
        public static final Codec<Open> CODEC = RecordCodecBuilder.create(i -> i.group(
                QuestEditorData.CODEC.fieldOf("data").forGetter(Open::data),
                JobOption.CODEC.listOf().fieldOf("jobs").forGetter(Open::jobs)
        ).apply(i, Open::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** OP → サーバー：この内容で保存して */
    public record Save(QuestEditorData data) implements CustomPacketPayload {
        public static final Type<Save> TYPE = new Type<>(JsnCraft.id("quest_editor_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Save> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(QuestEditorData.CODEC).map(Save::new, Save::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** サーバー → OP：保存できたか。できなければ理由 */
    public record Result(boolean ok, Component message) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(JsnCraft.id("quest_editor_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Result::ok,
                ComponentSerialization.STREAM_CODEC, Result::message,
                Result::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private QuestEditorPayloads() {}
}
