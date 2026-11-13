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

import java.util.List;

/** 転職の神官の通信まとめ。 */
public final class JobChangePayloads {

    /**
     * 職業1つ（転職画面の表示用）。
     * requirements は「樹海の伐り手 Lv.30（今 Lv.12）」のような条件の行（満たしていれば緑）。
     */
    public record JobEntry(String id, Component name, String type, String tier, int level, int maxLevel,
                           boolean current, boolean canChange, Component reason, List<Component> requirements) {
        public static final Codec<JobEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(JobEntry::id),
                ComponentSerialization.CODEC.fieldOf("name").forGetter(JobEntry::name),
                Codec.STRING.fieldOf("type").forGetter(JobEntry::type),
                Codec.STRING.fieldOf("tier").forGetter(JobEntry::tier),
                Codec.INT.fieldOf("level").forGetter(JobEntry::level),
                Codec.INT.fieldOf("max_level").forGetter(JobEntry::maxLevel),
                Codec.BOOL.fieldOf("current").forGetter(JobEntry::current),
                Codec.BOOL.fieldOf("can_change").forGetter(JobEntry::canChange),
                ComponentSerialization.CODEC.fieldOf("reason").forGetter(JobEntry::reason),
                ComponentSerialization.CODEC.listOf().fieldOf("requirements").forGetter(JobEntry::requirements)
        ).apply(i, JobEntry::new));
    }

    /** サーバー → 本人：転職画面を開く（開いていれば中身を新しくする） */
    public record Open(List<JobEntry> jobs) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(JsnCraft.id("job_change_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(JobEntry.CODEC.listOf()).map(Open::new, Open::jobs);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 本人 → サーバー：この職業に転職したい */
    public record Change(String jobId) implements CustomPacketPayload {
        public static final Type<Change> TYPE = new Type<>(JsnCraft.id("job_change"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Change> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Change::jobId, Change::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private JobChangePayloads() {}
}
