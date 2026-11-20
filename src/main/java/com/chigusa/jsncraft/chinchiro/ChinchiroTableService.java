package com.chigusa.jsncraft.chinchiro;

import com.chigusa.jsncraft.casino.Chips;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class ChinchiroTableService {

    public static void handle(ServerPlayer p, ChinchiroPayloads.Act a) {
        if (p.blockPosition().distSqr(a.pos()) > 100 || !(p.level().getBlockEntity(a.pos()) instanceof ChinchiroTableBlockEntity t)) return;
        switch (a.action()) {
            case 1 -> t.sit(p);
            case 2 -> t.stand(p);
            case 3 -> t.start(p);
            case 4 -> t.bet(p, a.value());
            case 5 -> t.roll(p);
            case 6 -> {
                if (t.seatOf(p.getUUID()) >= 0) t.addCpu();
            }
            case 7 -> {
                if (t.seatOf(p.getUUID()) >= 0) t.removeCpu();
            }
            default -> {}
        }
        send(p, t);
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        handle(p, new ChinchiroPayloads.Act(pos, 0, 0));
    }

    static void send(ServerPlayer p, ChinchiroTableBlockEntity t) {
        List<String> names = new ArrayList<>(), hands = new ArrayList<>();
        List<Integer> flags = new ArrayList<>(), dice = new ArrayList<>();
        List<Long> bets = new ArrayList<>(), nets = new ArrayList<>();
        for (var s : t.seats) {
            if (s == null) {
                names.add("");
                flags.add(4);
                bets.add(0L);
                for (int k = 0; k < 3; k++) dice.add(0);
                hands.add("");
                nets.add(0L);
                continue;
            }
            names.add(s.name);
            flags.add((s.player == null ? 1 : 0) | (p.getUUID().equals(s.player) ? 2 : 0) | (s.hand != null ? 8 : 0));
            bets.add(s.bet);
            for (int k = 0; k < 3; k++) dice.add(s.dice[k]);
            hands.add(s.hand == null ? "" : s.hand.name());
            nets.add(s.net);
        }
        PacketDistributor.sendToPlayer(p, new ChinchiroPayloads.State(t.getBlockPos(), t.phase, t.timer, t.parent, t.turn, t.wait, t.rollId,
                names, flags, bets, dice, hands, nets, new ArrayList<>(t.log), Chips.count(p)));
    }

    private ChinchiroTableService() {}
}
