package com.chigusa.jsncraft.client.casino;

import com.chigusa.jsncraft.casino.CasinoGame;
import com.chigusa.jsncraft.casino.CasinoPayloads;
import net.minecraft.client.Minecraft;

/** カジノの画面を開く・届いた結果を画面に渡す（クライアントだけ） */
public final class CasinoClient {
    public static long chips, wallet;
    public static boolean lightman;

    public static void open(CasinoPayloads.Open p) {
        chips = p.chips();
        wallet = p.wallet();
        lightman = p.lightman();
        CasinoGame g = CasinoGame.values()[Math.max(0, Math.min(CasinoGame.values().length - 1, p.game()))];
        Minecraft.getInstance().setScreen(switch (g) {
            case EXCHANGE -> new ExchangeScreen(p.pos());
            case SLOT -> new SlotScreen(p.pos());
        });
    }

    public static void balance(CasinoPayloads.Balance p) {
        chips = p.chips();
        wallet = p.wallet();
    }

    public static void spinResult(CasinoPayloads.SpinResult p) {
        if (Minecraft.getInstance().screen instanceof SlotScreen s) s.onResult(p);
        else chips = p.chips();
    }

    private CasinoClient() {}
}
