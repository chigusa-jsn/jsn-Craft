package com.chigusa.jsncraft.wheel;

import com.chigusa.jsncraft.casino.Chips;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** かけ台と、近くのビッグホイールをつなぐ */
public final class WheelService {
    public static final int RANGE = 12;
    public static final int MAX_BET = 1000;

    public static BigWheelBlockEntity find(ServerLevel level, BlockPos terminal) {
        BigWheelBlockEntity best = null;
        double bestD = RANGE * RANGE + 1;
        ChunkPos c = new ChunkPos(terminal);
        for (int cx = c.x - 1; cx <= c.x + 1; cx++)
            for (int cz = c.z - 1; cz <= c.z + 1; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk ch = level.getChunk(cx, cz);
                for (BlockEntity be : ch.getBlockEntities().values()) {
                    if (!(be instanceof BigWheelBlockEntity w)) continue;
                    double d = be.getBlockPos().distSqr(terminal);
                    if (d < bestD) {
                        bestD = d;
                        best = w;
                    }
                }
            }
        return best;
    }

    public static void open(ServerPlayer p, BlockPos terminal) {
        handle(p, new WheelPayloads.Bet(terminal, -1, 0));
    }

    public static void handle(ServerPlayer p, WheelPayloads.Bet req) {
        if (p.blockPosition().distSqr(req.terminal()) > 64 || !(p.level().getBlockState(req.terminal()).getBlock() instanceof WheelTerminalBlock)) return;
        ServerLevel level = p.serverLevel();
        BigWheelBlockEntity w = find(level, req.terminal());
        long lastWin = 0;
        if (w != null) {
            // いない間の当たりを受けとる
            Long pend = w.pending.remove(p.getUUID());
            if (pend != null && pend > 0) {
                Chips.give(p, pend);
                lastWin = pend;
                p.sendSystemMessage(Component.literal("§6ビッグホイール：いない間の当たり チップ +" + pend + "枚"));
                w.setChanged();
            }
            if (req.slot() >= 0 && req.slot() < 5 && w.phase == BigWheelBlockEntity.BETTING) {
                long[] mine = w.bets.getOrDefault(p.getUUID(), new long[5]);
                int amount = Math.max(0, Math.min(req.amount(), MAX_BET));
                long already = 0;
                for (long v : mine) already += v;
                if (amount > 0 && already + amount <= MAX_BET * 5L && Chips.take(p, amount)) {
                    mine[req.slot()] += amount;
                    w.bets.put(p.getUUID(), mine);
                    w.setChanged();
                } else if (amount > 0) p.displayClientMessage(Component.literal("チップが足りないよ"), true);
            } else if (req.slot() == -2 && w.phase == BigWheelBlockEntity.BETTING) {
                long[] mine = w.bets.remove(p.getUUID());
                if (mine != null) {
                    long sum = 0;
                    for (long v : mine) sum += v;
                    if (sum > 0) Chips.give(p, sum);
                    w.setChanged();
                }
            }
        }
        send(p, req.terminal(), w, lastWin);
    }

    static void send(ServerPlayer p, BlockPos terminal, BigWheelBlockEntity w, long lastWin) {
        List<Long> mine = new ArrayList<>(), totals = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            mine.add(0L);
            totals.add(0L);
        }
        if (w == null) {
            PacketDistributor.sendToPlayer(p, new WheelPayloads.State(terminal, false, 0, 0, mine, totals, List.of(), Chips.count(p), 0, -1, 0));
            return;
        }
        long[] m = w.bets.get(p.getUUID());
        for (int i = 0; i < 5; i++) {
            if (m != null) mine.set(i, m[i]);
            long t = 0;
            for (long[] b : w.bets.values()) t += b[i];
            totals.set(i, t);
        }
        PacketDistributor.sendToPlayer(p, new WheelPayloads.State(terminal, true, w.phase, w.ticksLeft(p.level().getGameTime()), mine, totals,
                new ArrayList<>(w.history), Chips.count(p), w.bets.size(), w.result, lastWin));
    }

    private WheelService() {}
}
