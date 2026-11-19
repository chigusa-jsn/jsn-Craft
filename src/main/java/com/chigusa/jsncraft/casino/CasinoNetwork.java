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
        r.playToServer(CasinoPayloads.Spin.TYPE, CasinoPayloads.Spin.STREAM_CODEC,
                (p, c) -> c.enqueueWork(() -> {
                    if (c.player() instanceof ServerPlayer sp) CasinoService.spin(sp, p);
                }));
    }

    private CasinoNetwork() {}
}
