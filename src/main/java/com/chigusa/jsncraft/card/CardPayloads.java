package com.chigusa.jsncraft.card;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class CardPayloads {
    public record Buy(BlockPos pos, int pack) implements CustomPacketPayload {
        public static final Type<Buy> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "card_buy"));
        public static final StreamCodec<ByteBuf, Buy> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Buy::pos, ByteBufCodecs.VAR_INT, Buy::pack, Buy::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Pack(List<String> cards, long wallet) implements CustomPacketPayload {
        public static final Type<Pack> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "card_pack"));
        public static final StreamCodec<ByteBuf, Pack> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), Pack::cards, ByteBufCodecs.VAR_LONG, Pack::wallet, Pack::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** kind：0=アルバムに全部しまう 1=アルバムから1枚出す 2=バインダーに入れる 3=バインダーから出す */
    public record Action(int hand, int kind, String card) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "card_action"));
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::hand, ByteBufCodecs.VAR_INT, Action::kind, ByteBufCodecs.stringUtf8(64), Action::card, Action::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private CardPayloads() {}
}
