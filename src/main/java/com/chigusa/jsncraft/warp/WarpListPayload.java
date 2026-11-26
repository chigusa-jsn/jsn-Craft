package com.chigusa.jsncraft.warp;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** サーバー → 本人：ワープ地点の一覧を開く */
public record WarpListPayload(List<Entry> entries, String here) implements CustomPacketPayload {
    public record Entry(String name, String dim, int x, int y, int z, int cost) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.name());
                    buf.writeUtf(e.dim());
                    buf.writeVarInt(e.x());
                    buf.writeVarInt(e.y());
                    buf.writeVarInt(e.z());
                    buf.writeVarInt(e.cost());
                },
                buf -> new Entry(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
    }

    public static final Type<WarpListPayload> TYPE = new Type<>(JsnCraft.id("warp_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpListPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), WarpListPayload::entries,
            ByteBufCodecs.STRING_UTF8, WarpListPayload::here,
            WarpListPayload::new);

    public static WarpListPayload of(ServerPlayer p) {
        List<Entry> out = new ArrayList<>();
        for (WarpData.Point pt : WarpData.get(p.server).points)
            out.add(new Entry(pt.name, pt.dim, (int) Math.floor(pt.x), (int) Math.floor(pt.y), (int) Math.floor(pt.z), pt.cost));
        return new WarpListPayload(out, p.level().dimension().location().toString());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
