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
            case PACHI_CROWN -> new PachiScreen(p.pos(), com.chigusa.jsncraft.casino.pachi.Machine.CROWN);
            case PACHI_TROPICAL -> new PachiScreen(p.pos(), com.chigusa.jsncraft.casino.pachi.Machine.TROPICAL);
            case ROULETTE -> new RouletteScreen(p.pos());
            case BLACKJACK -> new BlackjackScreen(p.pos());
            case POKER -> new PokerScreen(p.pos());
            case CHINCHIRO -> new ChinchiroScreen(p.pos());
            case PACHINKO -> new PachinkoScreen(p.pos());
            case GACHA -> new com.chigusa.jsncraft.client.gacha.GachaScreen(p.pos());
            case CARD -> new com.chigusa.jsncraft.client.card.CardMachineScreen(p.pos());
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

    public static void pachiState(com.chigusa.jsncraft.casino.pachi.PachiPayloads.State s) {
        chips = s.chips();
        if (Minecraft.getInstance().screen instanceof PachiScreen ps) ps.onState(s);
    }

    public static void roulette(CasinoPayloads.RouletteResult r) {
        if (Minecraft.getInstance().screen instanceof RouletteScreen s) s.onResult(r);
        else chips = r.chips();
    }

    public static void blackjack(CasinoPayloads.BjState b) {
        chips = b.chips();
        if (Minecraft.getInstance().screen instanceof BlackjackScreen s) s.onState(b);
        else lastBj = b;
    }

    public static void poker(CasinoPayloads.PokerState s) {
        chips = s.chips();
        if (Minecraft.getInstance().screen instanceof PokerScreen ps) ps.onState(s);
    }

    public static void chinchiro(CasinoPayloads.ChinchiroState s) {
        if (Minecraft.getInstance().screen instanceof ChinchiroScreen cs) cs.onState(s);
        else chips = s.chips();
    }

    public static void pachinkoState(com.chigusa.jsncraft.casino.pachinko.PachinkoPayloads.State s) {
        chips = s.chips();
        if (Minecraft.getInstance().screen instanceof PachinkoScreen ps) ps.onState(s);
    }

    public static void pachinkoShot(com.chigusa.jsncraft.casino.pachinko.PachinkoPayloads.Shot s) {
        if (Minecraft.getInstance().screen instanceof PachinkoScreen ps) ps.onShot(s);
    }

    /** 画面が開く前に届いた卓の様子 */
    public static CasinoPayloads.BjState lastBj;

    private CasinoClient() {}
}
