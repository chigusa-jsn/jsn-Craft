package com.chigusa.jsncraft.casino;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** カジノの通信の登録 */
public final class CasinoNetwork {
    public static void register(PayloadRegistrar r) {
        r.playToClient(CasinoPayloads.Open.TYPE, CasinoPayloads.Open.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.casino.CasinoClient.open(p)));
        r.playToClient(CasinoPayloads.Balance.TYPE, CasinoPayloads.Balance.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.casino.CasinoClient.balance(p)));
        r.playToClient(CasinoPayloads.SpinResult.TYPE, CasinoPayloads.SpinResult.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.casino.CasinoClient.spinResult(p)));
        r.playToServer(CasinoPayloads.Exchange.TYPE, CasinoPayloads.Exchange.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) CasinoService.exchange(sp, p);
                }));
        r.playToClient(com.chigusa.jsncraft.casino.pachi.PachiPayloads.State.TYPE, com.chigusa.jsncraft.casino.pachi.PachiPayloads.State.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.casino.CasinoClient.pachiState(p)));
        r.playToServer(com.chigusa.jsncraft.casino.pachi.PachiPayloads.Lever.TYPE, com.chigusa.jsncraft.casino.pachi.PachiPayloads.Lever.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.casino.pachi.PachiService.lever(sp, p);
                }));
        r.playToServer(com.chigusa.jsncraft.casino.pachi.PachiPayloads.Stop.TYPE, com.chigusa.jsncraft.casino.pachi.PachiPayloads.Stop.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) com.chigusa.jsncraft.casino.pachi.PachiService.stop(sp, p);
                }));
        r.playToClient(CasinoPayloads.RouletteResult.TYPE, CasinoPayloads.RouletteResult.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.casino.CasinoClient.roulette(p)));
        r.playToClient(CasinoPayloads.BjState.TYPE, CasinoPayloads.BjState.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.casino.CasinoClient.blackjack(p)));
        r.playToServer(CasinoPayloads.RouletteSpin.TYPE, CasinoPayloads.RouletteSpin.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) Roulette.spin(sp, p.pos(), p.bets());
                }));
        r.playToServer(CasinoPayloads.BjAction.TYPE, CasinoPayloads.BjAction.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) Blackjack.action(sp, p.pos(), p.action(), p.bet());
                }));
        r.playToClient(CasinoPayloads.PokerState.TYPE, CasinoPayloads.PokerState.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.casino.CasinoClient.poker(p)));
        r.playToClient(CasinoPayloads.ChinchiroState.TYPE, CasinoPayloads.ChinchiroState.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> com.chigusa.jsncraft.client.casino.CasinoClient.chinchiro(p)));
        r.playToServer(CasinoPayloads.PokerAction.TYPE, CasinoPayloads.PokerAction.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) VideoPoker.action(sp, p.pos(), p.draw(), p.bet(), p.hold());
                }));
        r.playToServer(CasinoPayloads.ChinchiroAction.TYPE, CasinoPayloads.ChinchiroAction.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) Chinchiro.action(sp, p.pos(), p.roll(), p.bet());
                }));
        r.playToServer(CasinoPayloads.Spin.TYPE, CasinoPayloads.Spin.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) CasinoService.spin(sp, p);
                }));
    }

    private CasinoNetwork() {}
}
