package com.chigusa.jsncraft.casino.pachinko;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** パチンコの通信 */
public final class PachinkoPayloads {

    /** 本人 → サーバー：玉を1つ打つ（strength は 0〜1000） */
    public record Shoot(BlockPos pos, int strength) implements CustomPacketPayload {
        public static final Type<Shoot> TYPE = new Type<>(JsnCraft.id("pachinko_shoot"));
        public static final StreamCodec<ByteBuf, Shoot> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Shoot::pos, ByteBufCodecs.VAR_INT, Shoot::strength, Shoot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 本人 → サーバー：玉を借りる（buy=true：チップ25枚で100玉）／玉をチップに戻す */
    public record Balls(BlockPos pos, boolean buy) implements CustomPacketPayload {
        public static final Type<Balls> TYPE = new Type<>(JsnCraft.id("pachinko_balls"));
        public static final StreamCodec<ByteBuf, Balls> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Balls::pos, ByteBufCodecs.BOOL, Balls::buy, Balls::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** サーバー → 本人：打った玉（画面は同じ計算で玉を動かす） */
    public record Shot(long seed, int strength, boolean attacker, boolean denchu, int result) implements CustomPacketPayload {
        public static final Type<Shot> TYPE = new Type<>(JsnCraft.id("pachinko_shot"));
        public static final StreamCodec<ByteBuf, Shot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Shot::seed, ByteBufCodecs.VAR_INT, Shot::strength, ByteBufCodecs.BOOL, Shot::attacker,
                ByteBufCodecs.BOOL, Shot::denchu, ByteBufCodecs.VAR_INT, Shot::result, Shot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * サーバー → 本人：台の様子。
     * spin：回っている図柄の残りティック（0 は止まっている） digits：止まる図柄（3けた、例 777）
     * mode：0=通常 1=確変 2=時短   round：大当たり中のラウンド（0 は大当たりではない）
     */
    public record State(int balls, long chips, int holds, int spin, int spinTotal, int digits, boolean reach, int mode, int jitan,
                        int round, int roundCount, boolean attacker, int jackpots, int lastWin) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(JsnCraft.id("pachinko_state"));
        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.of((buf, s) -> {
            for (int v : new int[]{s.balls, s.holds, s.spin, s.spinTotal, s.digits, s.mode, s.jitan, s.round, s.roundCount, s.jackpots, s.lastWin})
                ByteBufCodecs.VAR_INT.encode(buf, v);
            ByteBufCodecs.VAR_LONG.encode(buf, s.chips);
            buf.writeBoolean(s.reach);
            buf.writeBoolean(s.attacker);
        }, buf -> {
            int[] v = new int[11];
            for (int i = 0; i < 11; i++) v[i] = ByteBufCodecs.VAR_INT.decode(buf);
            long chips = ByteBufCodecs.VAR_LONG.decode(buf);
            boolean reach = buf.readBoolean(), att = buf.readBoolean();
            return new State(v[0], chips, v[1], v[2], v[3], v[4], reach, v[5], v[6], v[7], v[8], att, v[9], v[10]);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private PachinkoPayloads() {}
}
