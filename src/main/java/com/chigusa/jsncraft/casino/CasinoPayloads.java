package com.chigusa.jsncraft.casino;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

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

    /** ルーレットのかけ1つ */
    public record Bet(int type, int value, int amount) {}

    private static final StreamCodec<ByteBuf, List<Integer>> INTS = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64));

    /** 本人 → サーバー：ルーレットを回す */
    public record RouletteSpin(BlockPos pos, List<Bet> bets) implements CustomPacketPayload {
        public static final Type<RouletteSpin> TYPE = new Type<>(JsnCraft.id("roulette_spin"));
        public static final StreamCodec<ByteBuf, RouletteSpin> STREAM_CODEC = StreamCodec.of((buf, s) -> {
            BlockPos.STREAM_CODEC.encode(buf, s.pos);
            ByteBufCodecs.VAR_INT.encode(buf, s.bets.size());
            for (Bet b : s.bets) {
                ByteBufCodecs.VAR_INT.encode(buf, b.type());
                ByteBufCodecs.VAR_INT.encode(buf, b.value());
                ByteBufCodecs.VAR_INT.encode(buf, b.amount());
            }
        }, buf -> {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            int n = Math.min(64, ByteBufCodecs.VAR_INT.decode(buf));
            List<Bet> bets = new ArrayList<>();
            for (int i = 0; i < n; i++) bets.add(new Bet(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));
            return new RouletteSpin(pos, bets);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** サーバー → 本人：ルーレットの結果 */
    public record RouletteResult(boolean ok, int number, long payout, long chips) implements CustomPacketPayload {
        public static final Type<RouletteResult> TYPE = new Type<>(JsnCraft.id("roulette_result"));
        public static final StreamCodec<ByteBuf, RouletteResult> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, RouletteResult::ok, ByteBufCodecs.VAR_INT, RouletteResult::number,
                ByteBufCodecs.VAR_LONG, RouletteResult::payout, ByteBufCodecs.VAR_LONG, RouletteResult::chips, RouletteResult::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 本人 → サーバー：ブラックジャックの操作（0=配る 1=ヒット 2=スタンド 3=ダブル 4=スプリット） */
    public record BjAction(BlockPos pos, int action, int bet) implements CustomPacketPayload {
        public static final Type<BjAction> TYPE = new Type<>(JsnCraft.id("bj_action"));
        public static final StreamCodec<ByteBuf, BjAction> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, BjAction::pos, ByteBufCodecs.VAR_INT, BjAction::action, ByteBufCodecs.VAR_INT, BjAction::bet, BjAction::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** サーバー → 本人：ブラックジャックの卓（伏せたカードは -1） */
    public record BjState(boolean playing, List<Integer> dealer, List<List<Integer>> hands, List<Integer> bets, List<Integer> results,
                          int current, long payout, long chips) implements CustomPacketPayload {
        public static final Type<BjState> TYPE = new Type<>(JsnCraft.id("bj_state"));
        public static final StreamCodec<ByteBuf, BjState> STREAM_CODEC = StreamCodec.of((buf, s) -> {
            buf.writeBoolean(s.playing);
            INTS.encode(buf, s.dealer);
            ByteBufCodecs.VAR_INT.encode(buf, s.hands.size());
            for (List<Integer> h : s.hands) INTS.encode(buf, h);
            INTS.encode(buf, s.bets);
            INTS.encode(buf, s.results);
            ByteBufCodecs.VAR_INT.encode(buf, s.current);
            ByteBufCodecs.VAR_LONG.encode(buf, s.payout);
            ByteBufCodecs.VAR_LONG.encode(buf, s.chips);
        }, buf -> {
            boolean playing = buf.readBoolean();
            List<Integer> dealer = INTS.decode(buf);
            int n = Math.min(4, ByteBufCodecs.VAR_INT.decode(buf));
            List<List<Integer>> hands = new ArrayList<>();
            for (int i = 0; i < n; i++) hands.add(INTS.decode(buf));
            return new BjState(playing, dealer, hands, INTS.decode(buf), INTS.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf));
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private CasinoPayloads() {}
}
