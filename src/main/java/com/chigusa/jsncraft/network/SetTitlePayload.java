package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 本人 → サーバー：持っている肩書の中から付け替える（空文字で外す）。 */
public record SetTitlePayload(String title) implements CustomPacketPayload {

    public static final Type<SetTitlePayload> TYPE = new Type<>(JsnCraft.id("set_title"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetTitlePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetTitlePayload::title, SetTitlePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
