package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** サーバー → 本人：ためのゲージを出す（技の名前と、ためる時間 tick）。 */
public record CastBarPayload(Component name, int ticks) implements CustomPacketPayload {

    public static final Type<CastBarPayload> TYPE = new Type<>(JsnCraft.id("cast_bar"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastBarPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, CastBarPayload::name,
            ByteBufCodecs.VAR_INT, CastBarPayload::ticks,
            CastBarPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
