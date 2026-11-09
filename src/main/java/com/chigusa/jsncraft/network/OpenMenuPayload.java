package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** クライアント → サーバー：「職業メニューを開きたい」（Kキー）。 */
public record OpenMenuPayload() implements CustomPacketPayload {

    public static final OpenMenuPayload INSTANCE = new OpenMenuPayload();
    public static final Type<OpenMenuPayload> TYPE = new Type<>(JsnCraft.id("open_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
