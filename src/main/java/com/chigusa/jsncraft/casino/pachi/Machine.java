package com.chigusa.jsncraft.casino.pachi;

/**
 * パチスロの機種。
 *  CROWN：ペカ系（ボーナスが成立するとランプが光る。小役とボーナスだけのシンプルな台、出玉率 約97%）
 *  TROPICAL：連チャン系（ボーナスのあと32ゲームの「天国」に入ると、すぐに次が当たりやすい。出玉率 約97%）
 *  確率は 65536 分の いくつ。
 */
public enum Machine {
    CROWN(8978, 10923, 0, 1820, 60, 240, 149, 300, 120, 0.62),
    TROPICAL(8978, 10082, 819, 1638, 0, 0, 0, 255, 105, 0.6);

    public final int replay, grape, melon, cherry, bell, big, reg;
    /** ボーナスでもらえる枚数（これを超えたら終わり） */
    public final int bigOut, regOut;
    /** 連チャン系：ボーナスのうち BIG の割合 */
    public final double bigShare;

    Machine(int replay, int grape, int melon, int cherry, int bell, int big, int reg, int bigOut, int regOut, double bigShare) {
        this.replay = replay;
        this.grape = grape;
        this.melon = melon;
        this.cherry = cherry;
        this.bell = bell;
        this.big = big;
        this.reg = reg;
        this.bigOut = bigOut;
        this.regOut = regOut;
        this.bigShare = bigShare;
    }

    /** 3枚がけのときの払い出し */
    public static int pay(Reels.Role r, boolean inBonus) {
        return switch (r) {
            case GRAPE -> inBonus ? 15 : 8;
            case BELL -> 14;
            case MELON -> 5;
            case CHERRY -> 2;
            default -> 0;
        };
    }

    /** 天井：ボーナスなしでこのゲーム数に来たら、必ずボーナスが当たる */
    public static final int CEILING = 350;

    /** 連チャン系のモード：0=通常 1=天国 2=ドキドキ（どちらも32ゲームまで） */
    public static final int MODE_NORMAL = 0, MODE_HEAVEN = 1, MODE_DOKI = 2, MODE_GAMES = 32;

    public static int tropicalBonus(int mode) {
        return switch (mode) {
            case MODE_HEAVEN -> 4096;   // 1/16
            case MODE_DOKI -> 13107;    // 1/5
            default -> 285;             // 1/230
        };
    }
}
