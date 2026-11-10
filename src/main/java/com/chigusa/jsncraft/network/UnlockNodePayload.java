package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** クライアント → サーバー：「この職業のボードの、このマスを解放したい」。 */
public record UnlockNodePayload(String jobId, String nodeId) implements CustomPacketPayload {

    public static final Type<UnlockNodePayload> TYPE = new Type<>(JsnCraft.id("unlock_node"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockNodePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UnlockNodePayload::jobId,
            ByteBufCodecs.STRING_UTF8, UnlockNodePayload::nodeId,
            UnlockNodePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
