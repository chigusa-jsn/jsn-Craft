package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** ドッジロールした（向き：0 前 / 1 後ろ / 2 左 / 3 右） */
public record DodgePayload(int dir) implements CustomPacketPayload {
    public static final Type<DodgePayload> TYPE = new Type<>(JsnCraft.id("dodge"));
    public static final StreamCodec<ByteBuf, DodgePayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, DodgePayload::dir, DodgePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
