package com.chigusa.jsncraft.chinchiro;

import com.chigusa.jsncraft.JsnCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ChinchiroPayloads {
    /** action：0=様子 1=座る 2=立つ 3=始める 4=かける(value) 5=ふる 6=CPUを入れる 7=CPUを帰す */
    public record Act(BlockPos pos, int action, int value) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "chinchiro_vs_act"));
        public static final StreamCodec<ByteBuf, Act> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Act::pos,
                ByteBufCodecs.VAR_INT, Act::action, ByteBufCodecs.VAR_INT, Act::value, Act::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 席ごと：names / flags(1=CPU 2=自分 4=空き 8=役が決まった) / bets / dice(3つずつ) / hands / nets */
    public record State(BlockPos pos, int phase, int timer, int parent, int turn, int busy, int rollId, List<String> names, List<Integer> flags,
                        List<Long> bets, List<Integer> dice, List<String> hands, List<Long> nets, List<String> log, long chips) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsnCraft.MODID, "chinchiro_vs_state"));
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.of((b, s) -> {
            b.writeBlockPos(s.pos());
            for (int v : new int[]{s.phase(), s.timer(), s.parent(), s.turn() + 1, s.busy(), s.rollId()}) b.writeVarInt(v);
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(b, s.names());
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(b, s.flags());
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).encode(b, s.bets());
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(b, s.dice());
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(b, s.hands());
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).encode(b, s.nets());
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(b, s.log());
            b.writeVarLong(s.chips());
        }, b -> new State(b.readBlockPos(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt() - 1, b.readVarInt(), b.readVarInt(),
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(b), ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(b),
                ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).decode(b), ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(b),
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(b), ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).decode(b),
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(b), b.readVarLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private ChinchiroPayloads() {}
}
