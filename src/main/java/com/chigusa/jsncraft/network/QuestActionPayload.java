package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** クライアント → サーバー：依頼を「受ける」「報告する」「あきらめる」「報酬を受け取る」。picks は選んだ選択報酬の番号。 */
public record QuestActionPayload(String questId, int action, List<Integer> picks) implements CustomPacketPayload {

    public QuestActionPayload(String questId, int action) {
        this(questId, action, List.of());
    }

    public static final int ACCEPT = 0, TURN_IN = 1, ABANDON = 2, CLAIM = 3;

    public static final Type<QuestActionPayload> TYPE = new Type<>(JsnCraft.id("quest_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestActionPayload::questId,
            ByteBufCodecs.VAR_INT, QuestActionPayload::action,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)), QuestActionPayload::picks,
            QuestActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
