package com.chigusa.jsncraft.casino;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ビデオポーカー（ジャックス・オア・ベター、8/5。うまく遊べば還元率 約97%）。
 * 5枚配る → 残したいカードを選ぶ → 残り引き直し → 役で配当。カードは 0〜51（÷13 マーク、%13 ランク 0=A）
 */
public final class VideoPoker {
    public static final int[] BETS = {1, 5, 10, 50, 100};
    /** 役：0=なし 1=ジャックス以上の1ペア 2=ツーペア 3=スリーカード 4=ストレート 5=フラッシュ 6=フルハウス 7=フォーカード 8=ストレートフラッシュ 9=ロイヤル */
    public static final String[] NAMES = {"役なし", "ジャックス・オア・ベター", "ツーペア", "スリーカード", "ストレート", "フラッシュ", "フルハウス", "フォーカード",
            "ストレートフラッシュ", "ロイヤルストレートフラッシュ"};
    public static final int[] PAY = {0, 1, 2, 3, 4, 5, 8, 25, 50, 800};

    static final class Game {
        int[] hand = new int[5];
        List<Integer> deck = new ArrayList<>();
        int bet;
        boolean dealt;   // 配ったあと（引き直し待ち）
    }

    private static final Map<UUID, Game> GAMES = new ConcurrentHashMap<>();

    /** 役を調べる */
    public static int rank(int[] cards) {
        int[] r = new int[5];
        for (int i = 0; i < 5; i++) r[i] = cards[i] % 13;
        boolean flush = true;
        for (int i = 1; i < 5; i++) if (cards[i] / 13 != cards[0] / 13) flush = false;
        int[] cnt = new int[13];
        for (int x : r) cnt[x]++;
        int[] sorted = r.clone();
        Arrays.sort(sorted);
        boolean distinct = true;
        for (int i = 1; i < 5; i++) if (sorted[i] == sorted[i - 1]) distinct = false;
        boolean royalRun = distinct && sorted[0] == 0 && sorted[1] == 9 && sorted[4] == 12;   // A,10,J,Q,K
        boolean straight = distinct && (sorted[4] - sorted[0] == 4 || royalRun);
        if (straight && flush) return royalRun ? 9 : 8;
        int pairs = 0, three = 0, four = 0, highPair = 0;
        for (int i = 0; i < 13; i++) {
            if (cnt[i] == 4) four++;
            if (cnt[i] == 3) three++;
            if (cnt[i] == 2) {
                pairs++;
                if (i == 0 || i >= 10) highPair++;
            }
        }
        if (four > 0) return 7;
        if (three > 0 && pairs > 0) return 6;
        if (flush) return 5;
        if (straight) return 4;
        if (three > 0) return 3;
        if (pairs == 2) return 2;
        if (highPair > 0) return 1;
        return 0;
    }

    private static void shuffle(Game g, RandomSource r) {
        g.deck.clear();
        for (int c = 0; c < 52; c++) g.deck.add(c);
        for (int i = 51; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = g.deck.get(i);
            g.deck.set(i, g.deck.get(j));
            g.deck.set(j, t);
        }
    }

    public static void action(ServerPlayer p, BlockPos pos, boolean draw, int bet, int hold) {
        if (!CasinoService.near(p, pos, CasinoGame.POKER)) return;
        Game g = GAMES.computeIfAbsent(p.getUUID(), u -> new Game());
        RandomSource r = p.getRandom();
        if (!draw) {
            if (g.dealt) return;
            boolean ok = false;
            for (int b : BETS) ok |= b == bet;
            if (!ok) return;
            if (!Chips.take(p, bet)) {
                PacketDistributor.sendToPlayer(p, new CasinoPayloads.PokerState(false, new int[]{-1, -1, -1, -1, -1}, -1, 0, Chips.count(p)));
                return;
            }
            shuffle(g, r);
            for (int i = 0; i < 5; i++) g.hand[i] = g.deck.remove(g.deck.size() - 1);
            g.bet = bet;
            g.dealt = true;
            p.containerMenu.broadcastChanges();
            PacketDistributor.sendToPlayer(p, new CasinoPayloads.PokerState(true, g.hand.clone(), rank(g.hand), 0, Chips.count(p)));
            return;
        }
        if (!g.dealt) return;
        for (int i = 0; i < 5; i++) if ((hold & (1 << i)) == 0) g.hand[i] = g.deck.remove(g.deck.size() - 1);
        g.dealt = false;
        int k = rank(g.hand);
        long pay = (long) g.bet * PAY[k];
        if (pay > 0) Chips.give(p, pay);
        p.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(p, new CasinoPayloads.PokerState(false, g.hand.clone(), k, pay, Chips.count(p)));
        if (k >= 8) p.server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6§l★ " + p.getName().getString() + " がポーカーで " + NAMES[k] + "！ チップ " + pay + " 枚！"), false);
    }

    /** 画面を開いたとき：引き直し待ちなら、その手を見せる */
    public static void open(ServerPlayer p) {
        Game g = GAMES.get(p.getUUID());
        if (g != null && g.dealt) PacketDistributor.sendToPlayer(p, new CasinoPayloads.PokerState(true, g.hand.clone(), rank(g.hand), 0, Chips.count(p)));
    }

    private VideoPoker() {}
}
