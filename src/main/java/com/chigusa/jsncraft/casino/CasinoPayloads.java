package com.chigusa.jsncraft.casino;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** カジノの通信 */
public final class CasinoPayloads {

    /** サーバー → 本人：台の画面を開く（game は CasinoGame の番号） */
    public record Open(int game, BlockPos pos, long chips, long wallet, boolean lightman) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(JsnCraft.id("casino_open"));
        public static final StreamCodec<ByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::game, BlockPos.STREAM_CODEC, Open::pos, ByteBufCodecs.VAR_LONG, Open::chips,
                ByteBufCodecs.VAR_LONG, Open::wallet, ByteBufCodecs.BOOL, Open::lightman, Open::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** サーバー → 本人：持っているチップとお金 */
    public record Balance(long chips, long wallet) implements CustomPacketPayload {
        public static final Type<Balance> TYPE = new Type<>(JsnCraft.id("casino_balance"));
        public static final StreamCodec<ByteBuf, Balance> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Balance::chips, ByteBufCodecs.VAR_LONG, Balance::wallet, Balance::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 本人 → サーバー：両替（buy=true でお金→チップ） */
    public record Exchange(BlockPos pos, boolean buy, int amount) implements CustomPacketPayload {
        public static final Type<Exchange> TYPE = new Type<>(JsnCraft.id("casino_exchange"));
        public static final StreamCodec<ByteBuf, Exchange> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Exchange::pos, ByteBufCodecs.BOOL, Exchange::buy, ByteBufCodecs.VAR_INT, Exchange::amount, Exchange::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 本人 → サーバー：スロットを回す */
    public record Spin(BlockPos pos, int bet) implements CustomPacketPayload {
        public static final Type<Spin> TYPE = new Type<>(JsnCraft.id("casino_spin"));
        public static final StreamCodec<ByteBuf, Spin> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Spin::pos, ByteBufCodecs.VAR_INT, Spin::bet, Spin::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** サーバー → 本人：スロットの結果（ok=false ならチップが足りないなど） */
    public record SpinResult(boolean ok, int a, int b, int c, long payout, long chips) implements CustomPacketPayload {
        public static final Type<SpinResult> TYPE = new Type<>(JsnCraft.id("casino_spin_result"));
        public static final StreamCodec<ByteBuf, SpinResult> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, SpinResult::ok, ByteBufCodecs.VAR_INT, SpinResult::a, ByteBufCodecs.VAR_INT, SpinResult::b,
                ByteBufCodecs.VAR_INT, SpinResult::c, ByteBufCodecs.VAR_LONG, SpinResult::payout, ByteBufCodecs.VAR_LONG, SpinResult::chips,
                SpinResult::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private CasinoPayloads() {}
}
