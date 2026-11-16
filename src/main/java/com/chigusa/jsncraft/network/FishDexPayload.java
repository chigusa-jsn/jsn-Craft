package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.fishing.FishDexData;
import com.chigusa.jsncraft.fishing.FishSpecies;
import com.chigusa.jsncraft.fishing.FishSpeciesManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** サーバー → 本人：魚図鑑（全部の魚と、自分の記録）。 */
public record FishDexPayload(List<Entry> entries) implements CustomPacketPayload {

    /** 図鑑の1ページ。count が 0 なら、まだ釣っていない */
    public record Entry(int no, ItemStack icon, int rank, String dimension, String hint, int count, float best) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeVarInt(e.no());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.icon());
                    buf.writeVarInt(e.rank());
                    buf.writeUtf(e.dimension());
                    buf.writeUtf(e.hint());
                    buf.writeVarInt(e.count());
                    buf.writeFloat(e.best());
                },
                buf -> new Entry(buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt(),
                        buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readFloat()));
    }

    public static final Type<FishDexPayload> TYPE = new Type<>(JsnCraft.id("fish_dex"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FishDexPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), FishDexPayload::entries,
            FishDexPayload::new);

    /** クライアント側の控え */
    public static FishDexPayload client = new FishDexPayload(List.of());

    public static FishDexPayload of(ServerPlayer player) {
        FishDexData dex = FishSpeciesManager.dex(player);
        List<Entry> list = new ArrayList<>();
        for (var e : FishSpeciesManager.INSTANCE.all().entrySet()) {
            FishSpecies s = e.getValue();
            if (!BuiltInRegistries.ITEM.containsKey(s.item())) continue;
            FishDexData.Rec rec = dex.get(e.getKey().toString());
            list.add(new Entry(s.no(), new ItemStack(BuiltInRegistries.ITEM.get(s.item())), s.rankEnum().ordinal(),
                    s.dimension().getPath(), s.hint(), rec == null ? 0 : rec.count(), rec == null ? 0 : rec.best()));
        }
        return new FishDexPayload(list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
