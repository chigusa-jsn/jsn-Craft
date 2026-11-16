package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** サーバー → 本人：鉱石の場所（BlockPos.asLong）と色（0xRRGGBB）を、ticks の間だけ光らせる。 */
public record OreRevealPayload(List<Long> positions, List<Integer> colors, int ticks) implements CustomPacketPayload {
    public static final Type<OreRevealPayload> TYPE = new Type<>(JsnCraft.id("ore_reveal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OreRevealPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), OreRevealPayload::positions,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), OreRevealPayload::colors,
            ByteBufCodecs.VAR_INT, OreRevealPayload::ticks,
            OreRevealPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
