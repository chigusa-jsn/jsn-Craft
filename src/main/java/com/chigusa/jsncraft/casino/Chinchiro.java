package com.chigusa.jsncraft.casino;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * チンチロリン：親（ディーラー）が先に振り、子（プレイヤー）が3回まで振って勝負。
 * 役：ピンゾロ（1・1・1）5倍、ゾロ目 3倍、シゴロ（4・5・6）2倍、目（2つ同じ＋1つ）1倍、
 *     目なし（3回とも役なし）は負け、ヒフミ（1・2・3）は2倍払い。同じ目は引き分け。
 * 負けたときに最大5倍払うので、始めるときにかけの5倍を預かり、使わなかった分は返す。
 */
public final class Chinchiro {
    public static final int[] BETS = {1, 5, 10, 50, 100};
    public static final int DEPOSIT = 5;

    static final class Game {
        int bet;
        boolean playerTurn;
        final List<int[]> dealer = new ArrayList<>(), player = new ArrayList<>();
        int dealerHand, playerHand;
    }

    private static final Map<UUID, Game> GAMES = new ConcurrentHashMap<>();

    /** 役の強さ：1000=ピンゾロ 901〜906=ゾロ目 800=シゴロ 101〜106=目 0=目なし -1=ヒフミ */
    public static int hand(int[] d) {
        int[] s = d.clone();
        java.util.Arrays.sort(s);
        if (s[0] == 1 && s[2] == 1) return 1000;
        if (s[0] == s[2]) return 900 + s[0];
        if (s[0] == 4 && s[1] == 5 && s[2] == 6) return 800;
        if (s[0] == 1 && s[1] == 2 && s[2] == 3) return -1;
        if (s[0] == s[1]) return 100 + s[2];
        if (s[1] == s[2]) return 100 + s[0];
        return 0;
    }

    public static String name(int h) {
        if (h == 1000) return "ピンゾロ";
        if (h > 900) return (h - 900) + "のゾロ目";
        if (h == 800) return "シゴロ";
        if (h > 100) return (h - 100) + "の目";
        if (h == -1) return "ヒフミ";
        return "目なし";
    }

    static int mult(int h) {
        return h == 1000 ? 5 : h > 900 ? 3 : h == 800 ? 2 : 1;
    }

    private static int[] roll(RandomSource r) {
        return new int[]{1 + r.nextInt(6), 1 + r.nextInt(6), 1 + r.nextInt(6)};
    }

    public static void action(ServerPlayer p, BlockPos pos, boolean rollAction, int bet) {
        if (!CasinoService.near(p, pos, CasinoGame.CHINCHIRO)) return;
        Game g = GAMES.computeIfAbsent(p.getUUID(), u -> new Game());
        RandomSource r = p.getRandom();
        if (!rollAction) {
            if (g.playerTurn) return;
            boolean ok = false;
            for (int b : BETS) ok |= b == bet;
            if (!ok) return;
            if (!Chips.take(p, (long) bet * DEPOSIT)) {
                send(p, g, 0, 0, 0, true);
                return;
            }
            g.bet = bet;
            g.dealer.clear();
            g.player.clear();
            g.dealerHand = 0;
            g.playerHand = 0;
            // 親が振る（3回まで）
            for (int i = 0; i < 3; i++) {
                int[] d = roll(r);
                g.dealer.add(d);
                g.dealerHand = hand(d);
                if (g.dealerHand != 0) break;
            }
            int dh = g.dealerHand;
            if (dh >= 800) { settle(p, g, -mult(dh)); return; }   // 親の強い役：子の負け
            if (dh == -1) { settle(p, g, 2); return; }            // 親のヒフミ：子の勝ち（2倍）
            if (dh == 0) { settle(p, g, 1); return; }             // 親の目なし：子の勝ち
            g.playerTurn = true;
            send(p, g, 0, 0, 0, false);
            return;
        }
        if (!g.playerTurn || g.player.size() >= 3) return;
        int[] d = roll(r);
        g.player.add(d);
        g.playerHand = hand(d);
        int ph = g.playerHand;
        if (ph == 0 && g.player.size() < 3) {
            send(p, g, 0, 0, 0, false);
            return;
        }
        int dh = g.dealerHand;
        int result;
        if (ph == -1) result = -2;
        else if (ph == 0) result = -1;
        else if (ph > dh) result = mult(ph);
        else if (ph == dh) result = 0;
        else result = -1;
        settle(p, g, result);
    }

    /** result：子から見た勝ち負けの倍率（プラスで勝ち、マイナスで負け、0で引き分け） */
    private static void settle(ServerPlayer p, Game g, int result) {
        g.playerTurn = false;
        long deposit = (long) g.bet * DEPOSIT;
        long back = deposit + (long) g.bet * result;   // 預かり分から増減
        if (back > 0) Chips.give(p, back);
        CasinoLuck.played(p, g.bet, Math.max(0, (long) g.bet * (result + 1)));
        if (result < 0) CasinoLuck.refund(p, (long) g.bet * -result);
        p.containerMenu.broadcastChanges();
        send(p, g, result, back, 1, false);
    }

    private static void send(ServerPlayer p, Game g, int result, long back, int done, boolean noChips) {
        List<Integer> dd = new ArrayList<>(), pp = new ArrayList<>();
        for (int[] d : g.dealer) for (int x : d) dd.add(x);
        for (int[] d : g.player) for (int x : d) pp.add(x);
        PacketDistributor.sendToPlayer(p, new CasinoPayloads.ChinchiroState(g.playerTurn, dd, pp, g.dealerHand, g.playerHand,
                result, back, done == 1, noChips, g.bet, Chips.count(p)));
    }

    public static void open(ServerPlayer p) {
        Game g = GAMES.get(p.getUUID());
        if (g != null && g.playerTurn) send(p, g, 0, 0, 0, false);
    }

    private Chinchiro() {}
}
