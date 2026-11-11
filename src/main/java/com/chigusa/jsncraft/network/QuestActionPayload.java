package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** クライアント → サーバー：依頼を「受ける」「報告する」「あきらめる」。 */
public record QuestActionPayload(String questId, int action) implements CustomPacketPayload {

    public static final int ACCEPT = 0, TURN_IN = 1, ABANDON = 2;

    public static final Type<QuestActionPayload> TYPE = new Type<>(JsnCraft.id("quest_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestActionPayload::questId,
            ByteBufCodecs.VAR_INT, QuestActionPayload::action,
            QuestActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
