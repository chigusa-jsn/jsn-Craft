package com.chigusa.jsncraft.casino;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** カジノの台の処理（抽選はすべてサーバーで行う） */
public final class CasinoService {

    /** 回しすぎ防止：次に回せる時刻（ゲームの時刻） */
    private static final Map<UUID, Long> NEXT_SPIN = new ConcurrentHashMap<>();

    public static void open(ServerPlayer p, CasinoGame game, BlockPos pos) {
        PacketDistributor.sendToPlayer(p, new CasinoPayloads.Open(game.ordinal(), pos, Chips.count(p), Money.walletInChips(p), Money.hasLightman()));
        if (game == CasinoGame.BLACKJACK) Blackjack.open(p);
        if (game == CasinoGame.POKER) VideoPoker.open(p);
        if (game == CasinoGame.PACHINKO) com.chigusa.jsncraft.casino.pachinko.PachinkoService.open(p, pos);
        if (game == CasinoGame.CHINCHIRO) Chinchiro.open(p);
        if (game == CasinoGame.GACHA) com.chigusa.jsncraft.gacha.GachaService.open(p, pos);
        if (game == CasinoGame.PACHI_CROWN || game == CasinoGame.PACHI_TROPICAL) com.chigusa.jsncraft.casino.pachi.PachiService.open(p, game);
    }

    /** その台の近く（8ブロック）にいるか */
    static boolean near(ServerPlayer p, BlockPos pos, CasinoGame game) {
        if (p.blockPosition().distSqr(pos) > 64) return false;
        return p.level().getBlockState(pos).getBlock() instanceof CasinoBlock b && b.game == game;
    }

    private static void balance(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new CasinoPayloads.Balance(Chips.count(p), Money.walletInChips(p)));
    }

    public static void exchange(ServerPlayer p, CasinoPayloads.Exchange req) {
        if (!near(p, req.pos(), CasinoGame.EXCHANGE)) return;
        int amount = req.amount();
        if (amount <= 0 || amount > 100_000 || amount % Money.unit() != 0) return;
        if (req.buy()) {
            if (!Money.pay(p, amount)) {
                p.displayClientMessage(Component.literal("お金が足りないよ"), true);
            } else {
                Chips.give(p, amount);
                p.level().playSound(null, p.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
        } else {
            if (!Chips.take(p, amount)) {
                p.displayClientMessage(Component.literal("チップが足りないよ"), true);
            } else {
                Money.give(p, amount);
                p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
            }
        }
        p.containerMenu.broadcastChanges();
        balance(p);
    }

    public static void spin(ServerPlayer p, CasinoPayloads.Spin req) {
        if (!near(p, req.pos(), CasinoGame.SLOT)) return;
        int bet = req.bet();
        boolean valid = false;
        for (int b : Slots.BETS) valid |= b == bet;
        if (!valid) return;
        long now = p.level().getGameTime();
        if (NEXT_SPIN.getOrDefault(p.getUUID(), 0L) > now) return;
        if (!Chips.take(p, bet)) {
            PacketDistributor.sendToPlayer(p, new CasinoPayloads.SpinResult(false, 0, 0, 0, 0, Chips.count(p)));
            return;
        }
        NEXT_SPIN.put(p.getUUID(), now + 30);   // 1.5秒に1回まで（リールが止まるまで）
        var r = p.getRandom();
        int a = Slots.roll(r), b = Slots.roll(r), c = Slots.roll(r);
        // 遊び人の勝負勘：はずれたら、たまにもう一回
        if (Slots.multiplier(a, b, c) == 0 && r.nextDouble() < CasinoLuck.edge(p)) {
            a = Slots.roll(r); b = Slots.roll(r); c = Slots.roll(r);
        }
        long payout = (long) bet * Slots.multiplier(a, b, c);
        CasinoLuck.played(p, bet, payout);
        if (payout > 0) Chips.give(p, payout);
        p.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(p, new CasinoPayloads.SpinResult(true, a, b, c, payout, Chips.count(p)));
        if (a == 0 && b == 0 && c == 0) {
            // 大当たりはまわりにも知らせる
            p.server.getPlayerList().broadcastSystemMessage(Component.literal("§6§l★ " + p.getName().getString() + " がスロットで 7 7 7 ！ チップ " + payout + " 枚を当てた！"), false);
        }
    }

    private CasinoService() {}
}
