package com.chigusa.jsncraft.casino.pachi;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** パチスロの通信 */
public final class PachiPayloads {

    /** 本人 → サーバー：レバーを叩く（bet 枚） */
    public record Lever(BlockPos pos, int bet, boolean auto) implements CustomPacketPayload {
        public static final Type<Lever> TYPE = new Type<>(JsnCraft.id("pachi_lever"));
        public static final StreamCodec<ByteBuf, Lever> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Lever::pos, ByteBufCodecs.VAR_INT, Lever::bet, ByteBufCodecs.BOOL, Lever::auto, Lever::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 本人 → サーバー：リールを止める（押したときに中段にあったコマ） */
    public record Stop(BlockPos pos, int reel, int pressed) implements CustomPacketPayload {
        public static final Type<Stop> TYPE = new Type<>(JsnCraft.id("pachi_stop"));
        public static final StreamCodec<ByteBuf, Stop> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Stop::pos, ByteBufCodecs.VAR_INT, Stop::reel, ByteBufCodecs.VAR_INT, Stop::pressed, Stop::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * サーバー → 本人：台の様子。
     * event：0=開いた 1=レバーOK 2=リールが止まった 3=チップ不足 4=ボーナス開始 5=ボーナス終了
     * stops：止まったコマ（-1 はまだ回っている） wins：そろったラインのビット
     */
    public record State(int machine, int event, long chips, int bet, int s0, int s1, int s2, boolean inGame, int lamp,
                        int bonus, int bonusOut, int bonusTarget, int games, int big, int reg, boolean replay,
                        int payout, int wins) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(JsnCraft.id("pachi_state"));
        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.of((buf, s) -> {
            ByteBufCodecs.VAR_INT.encode(buf, s.machine);
            ByteBufCodecs.VAR_INT.encode(buf, s.event);
            ByteBufCodecs.VAR_LONG.encode(buf, s.chips);
            for (int v : new int[]{s.bet, s.s0 + 1, s.s1 + 1, s.s2 + 1}) ByteBufCodecs.VAR_INT.encode(buf, v);
            buf.writeBoolean(s.inGame);
            for (int v : new int[]{s.lamp, s.bonus, s.bonusOut, s.bonusTarget, s.games, s.big, s.reg}) ByteBufCodecs.VAR_INT.encode(buf, v);
            buf.writeBoolean(s.replay);
            ByteBufCodecs.VAR_INT.encode(buf, s.payout);
            ByteBufCodecs.VAR_INT.encode(buf, s.wins);
        }, buf -> {
            int m = ByteBufCodecs.VAR_INT.decode(buf), e = ByteBufCodecs.VAR_INT.decode(buf);
            long c = ByteBufCodecs.VAR_LONG.decode(buf);
            int bet = ByteBufCodecs.VAR_INT.decode(buf), a = ByteBufCodecs.VAR_INT.decode(buf) - 1, b = ByteBufCodecs.VAR_INT.decode(buf) - 1,
                    d = ByteBufCodecs.VAR_INT.decode(buf) - 1;
            boolean in = buf.readBoolean();
            int lamp = ByteBufCodecs.VAR_INT.decode(buf), bonus = ByteBufCodecs.VAR_INT.decode(buf), bo = ByteBufCodecs.VAR_INT.decode(buf),
                    bt = ByteBufCodecs.VAR_INT.decode(buf), g = ByteBufCodecs.VAR_INT.decode(buf), big = ByteBufCodecs.VAR_INT.decode(buf),
                    reg = ByteBufCodecs.VAR_INT.decode(buf);
            boolean rp = buf.readBoolean();
            int pay = ByteBufCodecs.VAR_INT.decode(buf), w = ByteBufCodecs.VAR_INT.decode(buf);
            return new State(m, e, c, bet, a, b, d, in, lamp, bonus, bo, bt, g, big, reg, rp, pay, w);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public int stop(int reel) {
            return reel == 0 ? s0 : reel == 1 ? s1 : s2;
        }
    }

    private PachiPayloads() {}
}
