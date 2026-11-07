package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 「このプレイヤーの肩書・職業・レベルはこれだよ」とクライアントに教える通信。
 * 頭の上の表示は、自分だけでなく周りの全員のクライアントで描くので、
 * サーバーが周りのプレイヤーにもこれを送る必要がある（ここが前回つまずきやすいポイント）。
 * level が 0 のときは「その枠の職業なし」。
 */
public record JobSyncPayload(
        int entityId,
        String title,
        Component combatName,
        int combatLevel,
        Component lifeName,
        int lifeLevel
) implements CustomPacketPayload {

    public static final Type<JobSyncPayload> TYPE = new Type<>(JsnCraft.id("job_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JobSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, JobSyncPayload::entityId,
            ByteBufCodecs.STRING_UTF8, JobSyncPayload::title,
            ComponentSerialization.STREAM_CODEC, JobSyncPayload::combatName,
            ByteBufCodecs.VAR_INT, JobSyncPayload::combatLevel,
            ComponentSerialization.STREAM_CODEC, JobSyncPayload::lifeName,
            ByteBufCodecs.VAR_INT, JobSyncPayload::lifeLevel,
            JobSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
