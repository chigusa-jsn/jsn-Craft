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
 * ブラックジャック（ディーラーと1対1、6デッキ）。
 * カード：0〜51（÷13 がマーク、%13 が 0=A 1〜8=2〜9 9=10 10=J 11=Q 12=K）
 * ディーラーはソフト17で止まる。ブラックジャックは 1.5倍。ダブル・スプリット（1回）あり。
 */
public final class Blackjack {
    public static final int[] BETS = {1, 5, 10, 50, 100, 500};
    public static final int ACT_DEAL = 0, ACT_HIT = 1, ACT_STAND = 2, ACT_DOUBLE = 3, ACT_SPLIT = 4;
    /** 結果：0=まだ 1=勝ち 2=負け 3=引き分け 4=ブラックジャック */
    public static final int R_NONE = 0, R_WIN = 1, R_LOSE = 2, R_PUSH = 3, R_BJ = 4;

    static final class Hand {
        final List<Integer> cards = new ArrayList<>();
        int bet;
        boolean done, doubled;
        int result;
    }

    static final class Table {
        final List<Integer> shoe = new ArrayList<>();
        final List<Integer> dealer = new ArrayList<>();
        final List<Hand> hands = new ArrayList<>();
        int current;
        boolean playing;
        BlockPos pos;
    }

    private static final Map<UUID, Table> TABLES = new ConcurrentHashMap<>();

    private static Table table(ServerPlayer p) {
        return TABLES.computeIfAbsent(p.getUUID(), u -> new Table());
    }

    public static int rank(int card) {
        return card % 13;
    }

    /** 手の合計（A は 11 か 1 の得な方）と、ソフト（Aを11で数えている）か */
    public static int total(List<Integer> cards) {
        int sum = 0, aces = 0;
        for (int c : cards) {
            int r = rank(c);
            if (r == 0) { aces++; sum += 11; }
            else sum += Math.min(10, r + 1);
        }
        while (sum > 21 && aces > 0) { sum -= 10; aces--; }
        return sum;
    }

    static boolean soft(List<Integer> cards) {
        int sum = 0, aces = 0;
        for (int c : cards) {
            int r = rank(c);
            if (r == 0) { aces++; sum += 11; }
            else sum += Math.min(10, r + 1);
        }
        while (sum > 21 && aces > 0) { sum -= 10; aces--; }
        return aces > 0;
    }

    static boolean blackjack(List<Integer> cards) {
        return cards.size() == 2 && total(cards) == 21;
    }

    private static int draw(Table t, RandomSource r) {
        if (t.shoe.size() < 52) {
            t.shoe.clear();
            for (int d = 0; d < 6; d++) for (int c = 0; c < 52; c++) t.shoe.add(c);
            for (int i = t.shoe.size() - 1; i > 0; i--) {
                int j = r.nextInt(i + 1);
                int tmp = t.shoe.get(i);
                t.shoe.set(i, t.shoe.get(j));
                t.shoe.set(j, tmp);
            }
        }
        return t.shoe.remove(t.shoe.size() - 1);
    }

    public static void open(ServerPlayer p) {
        send(p, table(p));
    }

    public static void action(ServerPlayer p, BlockPos pos, int act, int bet) {
        if (!CasinoService.near(p, pos, CasinoGame.BLACKJACK)) return;
        Table t = table(p);
        RandomSource r = p.getRandom();
        if (act == ACT_DEAL) {
            if (t.playing) return;
            boolean ok = false;
            for (int b : BETS) ok |= b == bet;
            if (!ok) return;
            if (!Chips.take(p, bet)) {
                send(p, t);
                return;
            }
            t.dealer.clear();
            t.hands.clear();
            Hand h = new Hand();
            h.bet = bet;
            t.hands.add(h);
            t.current = 0;
            t.pos = pos;
            h.cards.add(draw(t, r));
            t.dealer.add(draw(t, r));
            h.cards.add(draw(t, r));
            t.dealer.add(draw(t, r));
            t.playing = true;
            // どちらかがブラックジャックなら、すぐに決着
            if (blackjack(h.cards) || blackjack(t.dealer)) {
                h.done = true;
                finish(p, t, false);
                return;
            }
            send(p, t);
            return;
        }
        if (!t.playing) return;
        Hand h = t.hands.get(t.current);
        switch (act) {
            case ACT_HIT -> {
                h.cards.add(draw(t, r));
                if (total(h.cards) >= 21) h.done = true;
            }
            case ACT_STAND -> h.done = true;
            case ACT_DOUBLE -> {
                if (h.cards.size() != 2 || !Chips.take(p, h.bet)) break;
                h.bet *= 2;
                h.doubled = true;
                h.cards.add(draw(t, r));
                h.done = true;
            }
            case ACT_SPLIT -> {
                if (t.hands.size() != 1 || h.cards.size() != 2) break;
                if (Math.min(10, rank(h.cards.get(0)) + 1) != Math.min(10, rank(h.cards.get(1)) + 1)) break;
                if (!Chips.take(p, h.bet)) break;
                Hand h2 = new Hand();
                h2.bet = h.bet;
                h2.cards.add(h.cards.remove(1));
                h.cards.add(draw(t, r));
                h2.cards.add(draw(t, r));
                t.hands.add(h2);
                // Aのスプリットは1枚ずつで終わり
                if (rank(h.cards.get(0)) == 0) {
                    h.done = true;
                    h2.done = true;
                }
            }
            default -> {}
        }
        // 次の手へ
        while (t.current < t.hands.size() && t.hands.get(t.current).done) t.current++;
        if (t.current >= t.hands.size()) {
            t.current = t.hands.size() - 1;
            finish(p, t, true);
            return;
        }
        p.containerMenu.broadcastChanges();
        send(p, t);
    }

    private static void finish(ServerPlayer p, Table t, boolean dealerDraws) {
        RandomSource r = p.getRandom();
        boolean allBust = true;
        for (Hand h : t.hands) if (total(h.cards) <= 21) allBust = false;
        if (dealerDraws && !allBust) {
            while (total(t.dealer) < 17 || (total(t.dealer) == 17 && soft(t.dealer))) {
                if (total(t.dealer) == 17 && soft(t.dealer)) break;   // ソフト17で止まる
                t.dealer.add(draw(t, r));
            }
        }
        int d = total(t.dealer);
        boolean dealerBj = blackjack(t.dealer);
        long pay = 0;
        for (Hand h : t.hands) {
            int v = total(h.cards);
            boolean bj = blackjack(h.cards) && t.hands.size() == 1;
            if (bj && !dealerBj) { h.result = R_BJ; pay += h.bet * 5L / 2; }
            else if (bj) { h.result = R_PUSH; pay += h.bet; }
            else if (dealerBj) h.result = R_LOSE;
            else if (v > 21) h.result = R_LOSE;
            else if (d > 21 || v > d) { h.result = R_WIN; pay += h.bet * 2L; }
            else if (v == d) { h.result = R_PUSH; pay += h.bet; }
            else h.result = R_LOSE;
        }
        if (pay > 0) Chips.give(p, pay);
        long bet = 0;
        for (Hand h : t.hands) bet += h.bet;
        CasinoLuck.played(p, bet, pay);
        CasinoLuck.refund(p, bet - pay);
        t.playing = false;
        p.containerMenu.broadcastChanges();
        send(p, t, pay);
    }

    private static void send(ServerPlayer p, Table t) {
        send(p, t, 0);
    }

    private static void send(ServerPlayer p, Table t, long pay) {
        List<Integer> dealer = new ArrayList<>(t.dealer);
        if (t.playing && dealer.size() >= 2) dealer.set(1, -1);   // 2枚目は伏せたまま
        List<List<Integer>> hands = new ArrayList<>();
        List<Integer> bets = new ArrayList<>(), results = new ArrayList<>();
        for (Hand h : t.hands) {
            hands.add(new ArrayList<>(h.cards));
            bets.add(h.bet);
            results.add(h.result);
        }
        PacketDistributor.sendToPlayer(p, new CasinoPayloads.BjState(t.playing, dealer, hands, bets, results, t.current, pay, Chips.count(p)));
    }

    private Blackjack() {}
}
