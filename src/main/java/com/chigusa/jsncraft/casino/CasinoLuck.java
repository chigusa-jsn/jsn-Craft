package com.chigusa.jsncraft.casino;

import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 遊び人の「カジノの勝負勘」と、カジノで遊んだときの経験値。
 *  edge：0〜0.1 くらい。スロット・パチンコは当たりやすく、テーブルは負けた分が少しもどる。
 */
public final class CasinoLuck {

    public static double edge(ServerPlayer p) {
        double e = 0;
        for (String k : new String[]{"gad_casino_1", "gad_casino_2", "gad_casino_3"}) {
            SkillDefinition s = SkillService.passive(p, k);
            if (s != null) e += s.power();
        }
        return Math.min(0.15, e);
    }

    /** 負けたチップのうち、勝負勘の分だけもどす（テーブルゲーム） */
    public static void refund(ServerPlayer p, long lost) {
        if (lost <= 0) return;
        long back = (long) Math.floor(lost * edge(p));
        if (back <= 0 && lost >= 5 && p.getRandom().nextDouble() < lost * edge(p)) back = 1;
        if (back <= 0) return;
        Chips.give(p, back);
        p.displayClientMessage(Component.literal("§d遊び人のツキ：チップ " + back + " 枚がもどってきた"), false);
    }

    /** カジノで遊んだ：かけた枚数と、もどってきた枚数で経験値（遊び人のルール "casino_play" / "casino_jackpot"） */
    public static void played(ServerPlayer p, long bet, long won) {
        if (bet <= 0) return;
        double mult = Math.max(1, Math.min(20, bet / 5.0));
        JobService.onAction(p, r -> r.action().equals("casino_play"), mult);
        if (won >= bet * 10 && won >= 50) JobService.onAction(p, r -> r.action().equals("casino_jackpot"));
    }

    private CasinoLuck() {}
}
