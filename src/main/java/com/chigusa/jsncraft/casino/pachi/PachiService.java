package com.chigusa.jsncraft.casino.pachi;

import com.chigusa.jsncraft.casino.CasinoBlock;
import com.chigusa.jsncraft.casino.CasinoGame;
import com.chigusa.jsncraft.casino.Chips;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * パチスロの中身。抽選・すべり・払い出しはすべてサーバー。
 * 台ごと（機種ごと）の状態はプレイヤーに保存する（ボーナスの持ち越しも消えない）。
 */
public final class PachiService {

    private static String key(Machine m) {
        return "jsn_pachi_" + m.name().toLowerCase();
    }

    public static Machine machineOf(CasinoGame g) {
        return g == CasinoGame.PACHI_TROPICAL ? Machine.TROPICAL : Machine.CROWN;
    }

    private static CompoundTag data(ServerPlayer p, Machine m) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(key(m))) root.put(key(m), new CompoundTag());
        return root.getCompound(key(m));
    }

    private static boolean near(ServerPlayer p, BlockPos pos, Machine m) {
        if (p.blockPosition().distSqr(pos) > 64) return false;
        return p.level().getBlockState(pos).getBlock() instanceof CasinoBlock b && b.game != null && (b.game == CasinoGame.PACHI_CROWN || b.game == CasinoGame.PACHI_TROPICAL)
                && machineOf(b.game) == m;
    }

    public static void open(ServerPlayer p, CasinoGame g) {
        Machine m = machineOf(g);
        CompoundTag d = data(p, m);
        if (d.getBoolean("inGame")) {
            // 前に回したまま閉じたリールは、そのまま続きから止められる
        }
        send(p, m, d, 0, 0, 0);
    }

    public static void lever(ServerPlayer p, PachiPayloads.Lever req) {
        Machine m = machineAt(p, req.pos());
        if (m == null) return;
        CompoundTag d = data(p, m);
        if (d.getBoolean("inGame")) return;
        boolean inBonus = d.getInt("bonus") > 0;
        int bet = Math.max(1, Math.min(3, req.bet()));
        if (inBonus) bet = 3;
        if (d.getBoolean("replay")) {
            bet = Math.max(1, d.getInt("bet"));
            d.putBoolean("replay", false);
        } else if (!Chips.take(p, bet)) {
            send(p, m, d, 3, 0, 0);
            return;
        }
        d.putInt("bet", bet);
        d.putBoolean("auto", req.auto());
        RandomSource r = p.getRandom();
        // 小役
        Reels.Role flag = Reels.Role.NONE;
        if (inBonus) {
            flag = Reels.Role.GRAPE;
        } else {
            int x = r.nextInt(65536);
            if ((x -= m.replay) < 0) flag = Reels.Role.REPLAY;
            else if ((x -= m.grape) < 0) flag = Reels.Role.GRAPE;
            else if ((x -= m.melon) < 0) flag = Reels.Role.MELON;
            else if ((x -= m.cherry) < 0) flag = Reels.Role.CHERRY;
            else if ((x -= m.bell) < 0) flag = Reels.Role.BELL;
            // ボーナス（持ち越していないときだけ）
            int held = d.getInt("held");
            if (held == 0) {
                int y = r.nextInt(65536);
                int newHeld = 0;
                double luck = 1 + com.chigusa.jsncraft.casino.CasinoLuck.edge(p);   // 遊び人の勝負勘
                if (m == Machine.CROWN) {
                    if (y < m.big * luck) newHeld = 1;
                    else if (y < (m.big + m.reg) * luck) newHeld = 2;
                } else {
                    int mode = d.getInt("mode");
                    if (y < Machine.tropicalBonus(mode) * luck) newHeld = r.nextDouble() < m.bigShare ? 1 : 2;
                }
                // 天井：ボーナスなしで 350 ゲームに来たら必ず当たる（クラウンはBIG、トロピカルはそのあと天国）
                if (newHeld == 0 && d.getInt("games") + 1 >= Machine.CEILING) {
                    newHeld = m == Machine.CROWN ? 1 : (r.nextDouble() < m.bigShare ? 1 : 2);
                    d.putBoolean("ceiling", true);
                }
                if (newHeld > 0) {
                    d.putInt("held", newHeld);
                    // ランプ：レバーで光る（先ペカ）か、3つ目を止めたら光る（後ペカ）
                    if (r.nextInt(4) == 0) d.putInt("lamp", 1 + r.nextInt(4));
                    else d.putBoolean("lampAfter", true);
                }
            }
            d.putInt("games", d.getInt("games") + 1);
            if (m == Machine.TROPICAL && d.getInt("mode") != Machine.MODE_NORMAL) {
                int mg = d.getInt("modeGames") + 1;
                d.putInt("modeGames", mg);
                if (mg >= Machine.MODE_GAMES && d.getInt("held") == 0) d.putInt("mode", Machine.MODE_NORMAL);
            }
        }
        d.putInt("flag", flag.ordinal());
        d.putBoolean("inGame", true);
        d.putIntArray("stops", new int[]{-1, -1, -1});
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.5F, 1.4F);
        send(p, m, d, 1, 0, 0);
    }

    public static void stop(ServerPlayer p, PachiPayloads.Stop req) {
        Machine m = machineAt(p, req.pos());
        if (m == null) return;
        CompoundTag d = data(p, m);
        if (!d.getBoolean("inGame")) return;
        int reel = req.reel();
        if (reel < 0 || reel > 2) return;
        int[] stops = d.getIntArray("stops");
        if (stops.length != 3 || stops[reel] >= 0) return;
        int pressed = Math.floorMod(req.pressed(), Reels.SIZE);
        Reels.Role flag = Reels.Role.values()[d.getInt("flag")];
        int held = d.getInt("held");
        Reels.Role bonusHeld = held == 1 ? Reels.Role.BIG : held == 2 ? Reels.Role.REG : Reels.Role.NONE;
        int bet = d.getInt("bet");
        // オートのときは、どこで押してもそろう（目押しいらず）
        int slip = d.getBoolean("auto") ? Reels.SIZE - 1 : Reels.SLIP;
        stops[reel] = Reels.control(reel, pressed, stops, bet, flag, bonusHeld, slip);
        d.putIntArray("stops", stops);
        if (stops[0] < 0 || stops[1] < 0 || stops[2] < 0) {
            send(p, m, d, 2, 0, 0);
            return;
        }
        // 全部止まった：払い出し
        d.putBoolean("inGame", false);
        boolean inBonus = d.getInt("bonus") > 0;
        List<Reels.Win> wins = Reels.wins(stops, bet);
        int pay = 0, lines = 0, event = 2;
        for (Reels.Win w : wins) {
            lines |= 1 << w.line();
            switch (w.role()) {
                case REPLAY -> d.putBoolean("replay", true);
                case BIG, REG -> {
                    int type = w.role() == Reels.Role.BIG ? 1 : 2;
                    d.putInt("held", 0);
                    d.putInt("lamp", 0);
                    d.putBoolean("lampAfter", false);
                    d.putInt("bonus", type);
                    d.putInt("bonusOut", 0);
                    d.putInt(type == 1 ? "big" : "reg", d.getInt(type == 1 ? "big" : "reg") + 1);
                    d.putInt("games", 0);
                    event = 4;
                    if (type == 1 && m == Machine.TROPICAL) d.putInt("mode", Machine.MODE_NORMAL);
                }
                default -> pay += Machine.pay(w.role(), inBonus);
            }
        }
        pay = pay * bet / 3;
        if (d.getBoolean("lampAfter") && d.getInt("held") > 0) {
            d.putInt("lamp", 1 + p.getRandom().nextInt(4));
            d.putBoolean("lampAfter", false);
        }
        if (inBonus) {
            int out = d.getInt("bonusOut") + pay;
            d.putInt("bonusOut", out);
            int target = d.getInt("bonus") == 1 ? m.bigOut : m.regOut;
            if (out >= target) {
                d.putInt("bonus", 0);
                event = 5;
                if (m == Machine.TROPICAL) {
                    double x = p.getRandom().nextDouble();
                    int mode = x < 0.06 ? Machine.MODE_DOKI : x < 0.5 ? Machine.MODE_HEAVEN : Machine.MODE_NORMAL;
                    if (d.getBoolean("ceiling") && mode == Machine.MODE_NORMAL) mode = Machine.MODE_HEAVEN;   // 天井のあとは天国
                    d.putInt("mode", mode);
                    d.putInt("modeGames", 0);
                }
                d.putBoolean("ceiling", false);
            }
        }
        if (pay > 0) Chips.give(p, pay);
        com.chigusa.jsncraft.casino.CasinoLuck.played(p, bet, pay);
        p.containerMenu.broadcastChanges();
        if (event == 4 && wins.stream().anyMatch(w -> w.role() == Reels.Role.BIG)) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        send(p, m, d, event, pay, lines);
    }

    private static Machine machineAt(ServerPlayer p, BlockPos pos) {
        if (p.blockPosition().distSqr(pos) > 64) return null;
        if (!(p.level().getBlockState(pos).getBlock() instanceof CasinoBlock b)) return null;
        if (b.game != CasinoGame.PACHI_CROWN && b.game != CasinoGame.PACHI_TROPICAL) return null;
        return machineOf(b.game);
    }

    private static void send(ServerPlayer p, Machine m, CompoundTag d, int event, int payout, int lines) {
        int[] s = d.getIntArray("stops");
        if (s.length != 3) s = new int[]{-1, -1, -1};
        int bonus = d.getInt("bonus");
        int target = bonus == 1 ? m.bigOut : bonus == 2 ? m.regOut : 0;
        PacketDistributor.sendToPlayer(p, new PachiPayloads.State(m.ordinal(), event, Chips.count(p), Math.max(1, d.getInt("bet")),
                s[0], s[1], s[2], d.getBoolean("inGame"), d.getInt("lamp"), bonus, d.getInt("bonusOut"), target,
                d.getInt("games"), d.getInt("big"), d.getInt("reg"), d.getBoolean("replay"), payout, lines));
        if (event == 3) p.displayClientMessage(Component.literal("チップが足りないよ（両替機でチップを買ってね）"), true);
    }

    private PachiService() {}
}
