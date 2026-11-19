package com.chigusa.jsncraft.casino;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ルーレット（ヨーロピアン：0〜36、還元率 約97.3%）。
 * かけ方：0=数字1つ（36倍）、1=赤 2=黒 3=奇数 4=偶数 5=前半(1-18) 6=後半(19-36)（2倍）、7=ダズン 8=列（3倍）
 */
public final class Roulette {
    public static final int STRAIGHT = 0, RED = 1, BLACK = 2, ODD = 3, EVEN = 4, LOW = 5, HIGH = 6, DOZEN = 7, COLUMN = 8;
    /** 盤の並び（ヨーロピアン） */
    public static final int[] WHEEL = {0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26};
    private static final int[] REDS = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};
    public static final int MAX_TOTAL = 10_000;
    private static final Map<UUID, Long> NEXT = new ConcurrentHashMap<>();

    public static boolean isRed(int n) {
        for (int r : REDS) if (r == n) return true;
        return false;
    }

    /** かけが当たったときに戻る倍率（0ならはずれ） */
    public static int multiplier(int type, int value, int n) {
        if (type == STRAIGHT) return value == n ? 36 : 0;
        if (n == 0) return 0;
        return switch (type) {
            case RED -> isRed(n) ? 2 : 0;
            case BLACK -> !isRed(n) ? 2 : 0;
            case ODD -> n % 2 == 1 ? 2 : 0;
            case EVEN -> n % 2 == 0 ? 2 : 0;
            case LOW -> n <= 18 ? 2 : 0;
            case HIGH -> n >= 19 ? 2 : 0;
            case DOZEN -> (n - 1) / 12 == value ? 3 : 0;
            case COLUMN -> (n - 1) % 3 == value ? 3 : 0;
            default -> 0;
        };
    }

    static boolean valid(CasinoPayloads.Bet b) {
        if (b.amount() <= 0) return false;
        return switch (b.type()) {
            case STRAIGHT -> b.value() >= 0 && b.value() <= 36;
            case DOZEN, COLUMN -> b.value() >= 0 && b.value() <= 2;
            case RED, BLACK, ODD, EVEN, LOW, HIGH -> true;
            default -> false;
        };
    }

    public static void spin(ServerPlayer p, BlockPos pos, List<CasinoPayloads.Bet> bets) {
        if (!CasinoService.near(p, pos, CasinoGame.ROULETTE)) return;
        long now = p.level().getGameTime();
        if (NEXT.getOrDefault(p.getUUID(), 0L) > now) return;
        if (bets.isEmpty() || bets.size() > 60) return;
        long total = 0;
        for (CasinoPayloads.Bet b : bets) {
            if (!valid(b)) return;
            total += b.amount();
        }
        if (total > MAX_TOTAL) return;
        if (!Chips.take(p, total)) {
            PacketDistributor.sendToPlayer(p, new CasinoPayloads.RouletteResult(false, 0, 0, Chips.count(p)));
            return;
        }
        NEXT.put(p.getUUID(), now + 100);   // 盤が止まるまで（5秒）
        int n = p.getRandom().nextInt(37);
        long payout = 0;
        for (CasinoPayloads.Bet b : bets) payout += (long) b.amount() * multiplier(b.type(), b.value(), n);
        if (payout > 0) Chips.give(p, payout);
        CasinoLuck.played(p, total, payout);
        // 結果を見せてから（盤が止まってから）もどす
        long lost = total - payout;
        if (lost > 0) com.chigusa.jsncraft.skill.SkillEffects.later(p, 100, () -> {
            CasinoLuck.refund(p, lost);
            p.containerMenu.broadcastChanges();
        });
        p.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(p, new CasinoPayloads.RouletteResult(true, n, payout, Chips.count(p)));
        if (payout >= 1000) p.server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6§l★ " + p.getName().getString() + " がルーレットで チップ " + payout + " 枚を当てた！"), false);
    }

    private Roulette() {}
}
