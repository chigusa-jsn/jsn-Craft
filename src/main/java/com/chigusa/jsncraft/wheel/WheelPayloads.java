package com.chigusa.jsncraft.wheel;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class WheelPayloads {
    /** かけ台の画面 → サーバー。slot=-1 なら様子を聞くだけ、slot=-2 なら自分のかけを取り消す */
    public record Bet(BlockPos terminal, int slot, int amount) implements CustomPacketPayload {
        public static final Type<Bet> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "wheel_bet"));
        public static final StreamCodec<ByteBuf, Bet> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Bet::terminal,
                ByteBufCodecs.VAR_INT, Bet::slot, ByteBufCodecs.INT, Bet::amount, Bet::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** サーバー → 画面 */
    public record State(BlockPos terminal, boolean linked, int phase, int ticksLeft, List<Long> mine, List<Long> totals, List<Integer> history,
                        long chips, int players, int result, long lastWin) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "wheel_state"));
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.of((b, s) -> {
            b.writeBlockPos(s.terminal());
            b.writeBoolean(s.linked());
            b.writeVarInt(s.phase());
            b.writeVarInt(s.ticksLeft());
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).encode(b, s.mine());
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).encode(b, s.totals());
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(b, s.history());
            b.writeVarLong(s.chips());
            b.writeVarInt(s.players());
            b.writeInt(s.result());
            b.writeVarLong(s.lastWin());
        }, b -> new State(b.readBlockPos(), b.readBoolean(), b.readVarInt(), b.readVarInt(), ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).decode(b),
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).decode(b), ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(b),
                b.readVarLong(), b.readVarInt(), b.readInt(), b.readVarLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private WheelPayloads() {}
}
