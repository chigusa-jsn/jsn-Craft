package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 天馬が羽ばたいた（まわりに音と羽を見せる） */
public record PegasusFlapPayload(int horseId) implements CustomPacketPayload {
    public static final Type<PegasusFlapPayload> TYPE = new Type<>(JsnCraft.id("pegasus_flap"));
    public static final StreamCodec<ByteBuf, PegasusFlapPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, PegasusFlapPayload::horseId, PegasusFlapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
