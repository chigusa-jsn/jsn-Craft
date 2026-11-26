package com.chigusa.jsncraft.warp;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 本人 → サーバー：この番号のワープ地点へ行く */
public record WarpGoPayload(int index) implements CustomPacketPayload {
    public static final Type<WarpGoPayload> TYPE = new Type<>(JsnCraft.id("warp_go"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpGoPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WarpGoPayload::index, WarpGoPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
