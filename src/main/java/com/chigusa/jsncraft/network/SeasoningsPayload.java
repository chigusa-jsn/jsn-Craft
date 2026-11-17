package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.CookingManager;
import com.chigusa.jsncraft.cooking.Seasoning;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/** サーバー → クライアント：隠し味の一覧（調理台の画面とツールチップに出すため）。番号はサーバーと同じ順。 */
public record SeasoningsPayload(List<Seasoning> seasonings) implements CustomPacketPayload {

    public static final Type<SeasoningsPayload> TYPE = new Type<>(JsnCraft.id("seasonings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SeasoningsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(Seasoning.CODEC).apply(ByteBufCodecs.list()), SeasoningsPayload::seasonings,
            SeasoningsPayload::new);

    public static SeasoningsPayload client = new SeasoningsPayload(List.of());

    public static SeasoningsPayload create() {
        List<Seasoning> list = new ArrayList<>();
        for (var e : CookingManager.SEASONINGS.entries()) list.add(e.getValue());
        return new SeasoningsPayload(list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
