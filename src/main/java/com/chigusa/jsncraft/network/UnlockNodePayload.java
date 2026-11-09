package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** クライアント → サーバー：「このマスを解放したい」。jobType は "life" か "combat"。 */
public record UnlockNodePayload(String jobType, String nodeId) implements CustomPacketPayload {

    public static final Type<UnlockNodePayload> TYPE = new Type<>(JsnCraft.id("unlock_node"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockNodePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UnlockNodePayload::jobType,
            ByteBufCodecs.STRING_UTF8, UnlockNodePayload::nodeId,
            UnlockNodePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
