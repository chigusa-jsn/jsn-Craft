package com.chigusa.jsncraft.gacha;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class GachaPayloads {
    /** 画面 → サーバー：count 回まわす（0 なら様子だけ聞く） */
    public record Pull(BlockPos pos, int count) implements CustomPacketPayload {
        public static final Type<Pull> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "gacha_pull"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Pull> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Pull::pos, ByteBufCodecs.VAR_INT, Pull::count, Pull::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** サーバー → 画面：出たもの（空なら様子だけ）、天井までの残り、お金、値段、見本 */
    public record Result(int kind, List<ItemStack> items, List<Integer> rarity, int pityLeft, long wallet, int price, List<ItemStack> preview, List<Integer> previewRarity,
                         List<Float> rates) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "gacha_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.of((buf, r) -> {
            buf.writeVarInt(r.kind());
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, r.items());
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, r.rarity());
            buf.writeVarInt(r.pityLeft());
            buf.writeVarLong(r.wallet());
            buf.writeVarInt(r.price());
            ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, r.preview());
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, r.previewRarity());
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()).encode(buf, r.rates());
        }, buf -> new Result(buf.readVarInt(), ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf), ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf),
                buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf),
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf), ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()).decode(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private GachaPayloads() {}
}
