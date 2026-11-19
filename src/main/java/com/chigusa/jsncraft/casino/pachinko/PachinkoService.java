package com.chigusa.jsncraft.casino.pachinko;

import com.chigusa.jsncraft.JsnCraft;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * パチンコの中身（サーバー）。
 *  ・玉はチップ25枚で100玉。戻すときは4玉でチップ1枚。
 *  ・ヘソ（真ん中）に入ると図柄が回る（保留は4つまで）。通常 1/209、確変 1/16。
 *  ・大当たりは8ラウンド。右打ちでアタッカーに9個入れると1ラウンド（1個12玉）。
 *  ・大当たりの半分は確変（次の大当たりまで）、残りは時短50回。確変・時短のあいだは右の電チューが開く。
 *  ・ボーナスなしで、玉の数と確変・時短の状態はプレイヤーに保存する。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class PachinkoService {
    public static final int BALLS_PER_BUY = 100, CHIPS_PER_BUY = 25, BALLS_PER_CHIP = 4;
    static final int P_NORMAL = 314, P_KAKUHEN = 4096;   // 65536 分の（1/209、1/16）。出玉率 約96%
    static final int ROUNDS = 8, ROUND_COUNT = 9, ATTACKER_PAY = 12, ROUND_TICKS = 600, JITAN = 50;
    static final int[] PRIZE = {0, 3, 3, 2, ATTACKER_PAY, 1};
    private static final String KEY = "jsn_pachinko";

    static final class Session {
        BlockPos pos;
        final ArrayDeque<Boolean> holds = new ArrayDeque<>();
        int spin, spinTotal, digits;
        boolean reach, spinWin;
        int round, roundCount, roundTicks, interval;
        long nextShot;
        int lastWin;
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private static CompoundTag data(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    private static boolean near(ServerPlayer p, BlockPos pos) {
        return p.blockPosition().distSqr(pos) <= 64 && p.level().getBlockState(pos).getBlock() instanceof CasinoBlock b && b.game == CasinoGame.PACHINKO;
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        Session s = SESSIONS.computeIfAbsent(p.getUUID(), u -> new Session());
        s.pos = pos;
        send(p, s);
    }

    public static void balls(ServerPlayer p, PachinkoPayloads.Balls req) {
        if (!near(p, req.pos())) return;
        Session s = SESSIONS.computeIfAbsent(p.getUUID(), u -> new Session());
        s.pos = req.pos();
        CompoundTag d = data(p);
        int balls = d.getInt("balls");
        if (req.buy()) {
            if (!Chips.take(p, CHIPS_PER_BUY)) {
                p.displayClientMessage(Component.literal("チップが足りないよ"), true);
                return;
            }
            d.putInt("balls", balls + BALLS_PER_BUY);
        } else {
            int chips = balls / BALLS_PER_CHIP;
            if (chips > 0) Chips.give(p, chips);
            d.putInt("balls", balls - chips * BALLS_PER_CHIP);
        }
        p.containerMenu.broadcastChanges();
        send(p, s);
    }

    private static boolean electric(CompoundTag d) {
        return d.getInt("mode") != 0;
    }

    public static void shoot(ServerPlayer p, PachinkoPayloads.Shoot req) {
        if (!near(p, req.pos())) return;
        Session s = SESSIONS.computeIfAbsent(p.getUUID(), u -> new Session());
        s.pos = req.pos();
        long now = p.level().getGameTime();
        if (now < s.nextShot) return;
        s.nextShot = now + 10;   // 1秒に2玉まで
        CompoundTag d = data(p);
        int balls = d.getInt("balls");
        if (balls <= 0) return;
        balls--;
        double strength = Math.max(0, Math.min(1000, req.strength())) / 1000.0;
        long seed = p.getRandom().nextLong();
        boolean attacker = s.round > 0 && s.interval == 0;
        boolean denchu = electric(d) && s.round == 0;
        int res = Board.shoot(seed, strength, attacker, denchu, null);
        balls += PRIZE[res];
        d.putInt("balls", balls);
        if ((res == Board.HESO || res == Board.DENCHU) && s.holds.size() < 4) {
            // 保留：入った時点で当たりかどうかを決めておく
            int prob = d.getInt("mode") == 1 ? P_KAKUHEN : P_NORMAL;
            s.holds.add(p.getRandom().nextInt(65536) < prob);
        }
        if (res == Board.ATTACKER && s.round > 0) s.roundCount++;
        PacketDistributor.sendToPlayer(p, new PachinkoPayloads.Shot(seed, req.strength(), attacker, denchu, res));
        send(p, s);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Session s = SESSIONS.get(p.getUUID());
        if (s == null) return;
        CompoundTag d = data(p);
        RandomSource r = p.getRandom();
        boolean changed = false;
        // 大当たり中
        if (s.round > 0) {
            if (s.interval > 0) {
                if (--s.interval == 0) changed = true;
            } else if (s.roundCount >= ROUND_COUNT || ++s.roundTicks >= ROUND_TICKS) {
                s.round++;
                s.roundCount = 0;
                s.roundTicks = 0;
                s.interval = 30;
                if (s.round > ROUNDS) {
                    // 大当たり終わり
                    s.round = 0;
                    boolean kakuhen = s.digits % 2 == 1;   // 奇数の図柄は確変
                    d.putInt("mode", kakuhen ? 1 : 2);
                    d.putInt("jitan", kakuhen ? 0 : JITAN);
                    p.displayClientMessage(Component.literal(kakuhen ? "§c§l確変突入！" : "§b時短 " + JITAN + "回"), true);
                }
                changed = true;
            }
        } else if (s.spin > 0) {
            if (--s.spin == 0) {
                // 図柄が止まった
                if (s.spinWin) {
                    s.round = 1;
                    s.roundCount = 0;
                    s.roundTicks = 0;
                    s.interval = 40;
                    d.putInt("jackpots", d.getInt("jackpots") + 1);
                    p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1, 1);
                } else if (d.getInt("mode") == 2) {
                    int j = d.getInt("jitan") - 1;
                    d.putInt("jitan", j);
                    if (j <= 0) d.putInt("mode", 0);
                }
                changed = true;
            }
        } else if (!s.holds.isEmpty()) {
            // 次の保留を回す
            boolean win = s.holds.poll();
            s.spinWin = win;
            int a = 1 + r.nextInt(9);
            if (win) {
                s.digits = a * 111;
                s.reach = true;
            } else {
                int b = 1 + r.nextInt(9), c = 1 + r.nextInt(9);
                boolean reach = r.nextInt(6) == 0;   // はずれでもリーチになることがある
                if (reach) { b = a; c = a % 9 + 1; }
                if (a == b && b == c) c = a % 9 + 1;
                s.digits = a * 100 + b * 10 + c;
                s.reach = a == b;
            }
            boolean fast = electric(d);
            s.spinTotal = s.spin = (fast ? 30 : 90) + (s.reach ? 80 : 0);
            changed = true;
        }
        if (changed) send(p, s);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Session s = SESSIONS.remove(event.getEntity().getUUID());
        // 大当たりの途中で抜けたときは、残りのラウンドの分の玉をわたしておく
        if (s != null && s.round > 0 && event.getEntity() instanceof ServerPlayer p) {
            CompoundTag d = data(p);
            int left = ROUNDS - s.round + 1;
            int add = Math.max(0, left * ROUND_COUNT - s.roundCount) * ATTACKER_PAY;
            d.putInt("balls", d.getInt("balls") + add);
            boolean kakuhen = s.digits % 2 == 1;
            d.putInt("mode", kakuhen ? 1 : 2);
            d.putInt("jitan", kakuhen ? 0 : JITAN);
        }
    }

    private static void send(ServerPlayer p, Session s) {
        CompoundTag d = data(p);
        PacketDistributor.sendToPlayer(p, new PachinkoPayloads.State(d.getInt("balls"), Chips.count(p), s.holds.size(), s.spin, s.spinTotal, s.digits,
                s.reach, d.getInt("mode"), d.getInt("jitan"), s.round, s.roundCount, s.round > 0 && s.interval == 0, d.getInt("jackpots"), s.lastWin));
    }

    private PachinkoService() {}
}
