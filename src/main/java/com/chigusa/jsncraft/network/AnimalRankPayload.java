package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** サーバー → 見ている人：この動物の★（血統）は rank、突然変異は mutant（0 なし） */
public record AnimalRankPayload(int entityId, int rank, int mutant) implements CustomPacketPayload {

    public static final Type<AnimalRankPayload> TYPE = new Type<>(JsnCraft.id("animal_rank"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnimalRankPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AnimalRankPayload::entityId,
            ByteBufCodecs.VAR_INT, AnimalRankPayload::rank,
            ByteBufCodecs.VAR_INT, AnimalRankPayload::mutant,
            AnimalRankPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
