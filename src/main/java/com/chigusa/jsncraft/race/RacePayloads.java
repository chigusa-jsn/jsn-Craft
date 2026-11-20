package com.chigusa.jsncraft.race;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class RacePayloads {
    public record Bet(BlockPos terminal, int horse, int type, int amount) implements CustomPacketPayload {
        public static final Type<Bet> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "race_bet"));
        public static final StreamCodec<ByteBuf, Bet> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Bet::terminal,
                ByteBufCodecs.INT, Bet::horse, ByteBufCodecs.VAR_INT, Bet::type, ByteBufCodecs.INT, Bet::amount, Bet::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record State(BlockPos terminal, int raceNo, int phase, int ticksLeft, int elapsed, long horseSeed, long raceSeed,
                        List<Float> oddsWin, List<Float> oddsPlace, List<Long> mine, List<Integer> history, long chips, long lastWin, int players)
            implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "race_state"));
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.of((b, s) -> {
            b.writeBlockPos(s.terminal());
            b.writeVarInt(s.raceNo());
            b.writeVarInt(s.phase());
            b.writeVarInt(s.ticksLeft());
            b.writeVarInt(s.elapsed());
            b.writeLong(s.horseSeed());
            b.writeLong(s.raceSeed());
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()).encode(b, s.oddsWin());
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()).encode(b, s.oddsPlace());
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).encode(b, s.mine());
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(b, s.history());
            b.writeVarLong(s.chips());
            b.writeVarLong(s.lastWin());
            b.writeVarInt(s.players());
        }, b -> new State(b.readBlockPos(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readLong(), b.readLong(),
                ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()).decode(b), ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()).decode(b),
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).decode(b), ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(b),
                b.readVarLong(), b.readVarLong(), b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private RacePayloads() {}
}
