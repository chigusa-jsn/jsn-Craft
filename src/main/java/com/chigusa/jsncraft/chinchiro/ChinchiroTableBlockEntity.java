package com.chigusa.jsncraft.chinchiro;

import com.chigusa.jsncraft.casino.CasinoLuck;
import com.chigusa.jsncraft.casino.Chips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * チンチロ対戦の卓（4人まで・親は順番に交代）。
 * 子がかける → 親がふる（3回まで）→ 親の目が決まらなければ子が順にふる → 精算。
 */
public class ChinchiroTableBlockEntity extends BlockEntity {
    public static final int WAITING = 0, BETTING = 1, PARENT = 2, CHILD = 3, SETTLE = 4;
    public static final int SEATS = 4, BET_TICKS = 400, ROLL_TICKS = 300, ANIM = 30, SETTLE_TICKS = 120;
    public static final int[] BETS = {10, 50, 100, 500};

    public static final class Seat {
        public UUID player;   // null なら CPU
        public String name;
        public long bet;
        public int[] dice = new int[3];
        public int rolls;
        public Hand hand;
        public long net;
        public boolean done;

        boolean cpu() {
            return player == null;
        }
    }

    public final Seat[] seats = new Seat[SEATS];
    public int phase = WAITING, parent = 0, turn = -1, timer, wait, rollId;
    public final List<String> log = new ArrayList<>();
    private int cpuCount;

    public ChinchiroTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModChinchiro.TABLE_BE.get(), pos, state);
    }

    public int seated() {
        int n = 0;
        for (Seat s : seats) if (s != null) n++;
        return n;
    }

    public int seatOf(UUID u) {
        for (int i = 0; i < SEATS; i++) if (seats[i] != null && u.equals(seats[i].player)) return i;
        return -1;
    }

    void say(String s) {
        log.add(s);
        while (log.size() > 7) log.remove(0);
    }

    // ================= 操作 =================
    public void sit(ServerPlayer p) {
        if (seatOf(p.getUUID()) >= 0) return;
        for (int i = 0; i < SEATS; i++) if (seats[i] == null) {
            Seat s = new Seat();
            s.player = p.getUUID();
            s.name = p.getName().getString();
            s.done = phase != WAITING;   // 途中から座った人は次の勝負から
            seats[i] = s;
            say(s.name + " が座った");
            return;
        }
    }

    public void stand(ServerPlayer p) {
        int i = seatOf(p.getUUID());
        if (i < 0) return;
        if (phase != WAITING && phase != SETTLE && (i == parent || seats[i].bet > 0)) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("勝負の途中は立てないよ"), true);
            return;
        }
        say(seats[i].name + " が立った");
        seats[i] = null;
        if (seated() < 2 && phase == BETTING) phase = WAITING;
    }

    public void addCpu() {
        if (phase != WAITING) return;
        for (int i = 0; i < SEATS; i++) if (seats[i] == null) {
            Seat s = new Seat();
            s.name = "CPU" + (++cpuCount);
            seats[i] = s;
            say(s.name + " が座った");
            return;
        }
    }

    public void removeCpu() {
        if (phase != WAITING) return;
        for (int i = SEATS - 1; i >= 0; i--) if (seats[i] != null && seats[i].cpu()) {
            say(seats[i].name + " が立った");
            seats[i] = null;
            return;
        }
    }

    public void start(ServerPlayer p) {
        if (phase != WAITING || seated() < 2 || seatOf(p.getUUID()) < 0) return;
        if (seats[parent] == null) parent = nextSeat(parent);
        beginBetting();
    }

    public void bet(ServerPlayer p, int amount) {
        int i = seatOf(p.getUUID());
        if (phase != BETTING || i < 0 || i == parent || seats[i].bet > 0) return;
        boolean ok = false;
        for (int b : BETS) ok |= b == amount;
        if (!ok) return;
        if (!Chips.take(p, amount)) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("チップが足りないよ"), true);
            return;
        }
        seats[i].bet = amount;
        say(seats[i].name + " が " + amount + "枚かけた");
        changed();
    }

    public void roll(ServerPlayer p) {
        int i = seatOf(p.getUUID());
        if (i < 0 || i != turn || wait > 0) return;
        if ((phase == PARENT || phase == CHILD) && !seats[i].done) doRoll(i);
    }

    // ================= 進行 =================
    private void beginBetting() {
        phase = BETTING;
        timer = BET_TICKS;
        turn = -1;
        for (Seat s : seats) if (s != null) {
            s.bet = 0;
            s.dice = new int[3];
            s.rolls = 0;
            s.hand = null;
            s.net = 0;
            s.done = false;
        }
        say("―― 親は " + seats[parent].name + " ――");
        RandomSource r = level.random;
        for (int i = 0; i < SEATS; i++) if (seats[i] != null && seats[i].cpu() && i != parent) seats[i].bet = BETS[r.nextInt(3)];
        changed();
    }

    private int nextSeat(int from) {
        for (int k = 1; k <= SEATS; k++) {
            int j = (from + k) % SEATS;
            if (seats[j] != null) return j;
        }
        return from;
    }

    private void doRoll(int i) {
        Seat s = seats[i];
        RandomSource r = level.random;
        for (int k = 0; k < 3; k++) s.dice[k] = 1 + r.nextInt(6);
        s.rolls++;
        rollId++;
        wait = ANIM;
        level.playSound(null, worldPosition, SoundEvents.BONE_BLOCK_HIT, SoundSource.BLOCKS, 0.8F, 1.6F);
        changed();
    }

    /** ふったあと：役が決まったか */
    private void afterRoll(int i) {
        Seat s = seats[i];
        Hand h = Hand.of(s.dice[0], s.dice[1], s.dice[2]);
        if (h == null && s.rolls < 3) {
            timer = ROLL_TICKS;
            return;
        }
        s.hand = h == null ? Hand.NONE : h;
        s.done = true;
        say(s.name + "：" + s.dice[0] + "・" + s.dice[1] + "・" + s.dice[2] + " → " + s.hand.name());
        if (phase == PARENT) {
            int inst = s.hand.parentInstant();
            if (inst != 0) {
                say(inst > 0 ? "親の勝ち！（即決）" : "親の負け！（即決）");
                settle();
            } else nextChild(parent);
        } else nextChild(i);
    }

    private void nextChild(int from) {
        for (int k = 1; k < SEATS; k++) {
            int j = (from + k) % SEATS;
            if (j == parent) break;
            if (seats[j] != null && seats[j].bet > 0 && !seats[j].done) {
                phase = CHILD;
                turn = j;
                timer = ROLL_TICKS;
                changed();
                return;
            }
        }
        settle();
    }

    private void settle() {
        Seat ps = seats[parent];
        ServerLevel sl = (ServerLevel) level;
        ServerPlayer pp = ps.cpu() ? null : sl.getServer().getPlayerList().getPlayer(ps.player);
        int inst = ps.hand == null ? 0 : ps.hand.parentInstant();
        for (int i = 0; i < SEATS; i++) {
            Seat c = seats[i];
            if (c == null || i == parent || c.bet <= 0) continue;
            int res;   // 1=子の勝ち -1=子の負け 0=分け
            int mult;
            if (inst > 0) {
                res = -1;
                mult = ps.hand.mult();
            } else if (inst < 0) {
                res = 1;
                mult = ps.hand.rank() < 0 ? 2 : 1;
            } else {
                Hand ch = c.hand == null ? Hand.NONE : c.hand;
                if (ch.rank() > ps.hand.rank()) {
                    res = 1;
                    mult = ch.mult();
                } else if (ch.rank() < ps.hand.rank()) {
                    res = -1;
                    mult = Math.max(ps.hand.mult(), ch.rank() < 0 ? 2 : 1);
                } else {
                    res = 0;
                    mult = 0;
                }
            }
            ServerPlayer cp = c.cpu() ? null : sl.getServer().getPlayerList().getPlayer(c.player);
            long amount = c.bet * mult;
            if (res > 0) {
                // 親が払う（CPUの親なら店が払う）
                long paid = amount;
                if (pp != null) {
                    paid = Math.min(amount, Chips.count(pp));
                    Chips.take(pp, paid);
                }
                if (cp != null) Chips.give(cp, c.bet + paid);
                c.net = paid;
                ps.net -= paid;
            } else if (res < 0) {
                // かけた分はもう預かっている。倍率の分は子の手持ちから
                long extra = amount - c.bet, got = c.bet;
                if (cp != null && extra > 0) {
                    long e = Math.min(extra, Chips.count(cp));
                    Chips.take(cp, e);
                    got += e;
                } else if (cp == null) got = amount;
                if (pp != null) Chips.give(pp, got);
                c.net = -got;
                ps.net += got;
            } else if (cp != null) Chips.give(cp, c.bet);
            if (cp != null) CasinoLuck.played(cp, c.bet, Math.max(0, c.bet + c.net));
        }
        if (pp != null) CasinoLuck.played(pp, 0, Math.max(0, ps.net));
        for (int i = 0; i < SEATS; i++) if (seats[i] != null && seats[i].net != 0)
            say(seats[i].name + "：" + (seats[i].net > 0 ? "§a+" : "§c") + seats[i].net + "枚");
        phase = SETTLE;
        timer = SETTLE_TICKS;
        turn = -1;
        level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1F, 1.2F);
        changed();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChinchiroTableBlockEntity be) {
        be.tickServer((ServerLevel) level);
    }

    private void tickServer(ServerLevel sl) {
        // いなくなった人・遠くへ行った人は、次の区切りで席を立つ
        if (sl.getGameTime() % 20 == 0) for (int i = 0; i < SEATS; i++) {
            Seat s = seats[i];
            if (s == null || s.cpu()) continue;
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(s.player);
            boolean gone = p == null || p.level() != sl || p.blockPosition().distSqr(worldPosition) > 16 * 16;
            if (gone && (phase == WAITING || phase == SETTLE || (i != parent && s.bet == 0))) {
                say(s.name + " が席をはなれた");
                seats[i] = null;
                changed();
            }
        }
        if (phase == WAITING) return;
        if (seats[parent] == null) {
            // 親がいなくなった：かけを返して待ちに
            refundAll(sl);
            phase = WAITING;
            changed();
            return;
        }
        if (wait > 0) {
            if (--wait == 0) afterRoll(turn);
            return;
        }
        if (timer > 0) timer--;
        switch (phase) {
            case BETTING -> {
                boolean all = true, any = false;
                for (int i = 0; i < SEATS; i++) if (seats[i] != null && i != parent) {
                    all &= seats[i].bet > 0;
                    any |= seats[i].bet > 0;
                }
                if ((all && any) || timer == 0) {
                    if (!any) {
                        say("だれもかけなかった");
                        phase = WAITING;
                        changed();
                        return;
                    }
                    phase = PARENT;
                    turn = parent;
                    timer = ROLL_TICKS;
                    changed();
                }
            }
            case PARENT, CHILD -> {
                Seat s = seats[turn];
                if (s == null) {
                    nextChild(turn);
                    return;
                }
                // CPU はすぐふる。人は時間切れで自動
                if ((s.cpu() && timer < ROLL_TICKS - 25) || timer == 0) doRoll(turn);
            }
            case SETTLE -> {
                if (timer == 0) {
                    parent = nextSeat(parent);
                    if (seated() >= 2) beginBetting();
                    else {
                        phase = WAITING;
                        changed();
                    }
                }
            }
            default -> {}
        }
    }

    private void refundAll(ServerLevel sl) {
        for (Seat s : seats) if (s != null && !s.cpu() && s.bet > 0 && !(phase == SETTLE)) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(s.player);
            if (p != null) Chips.give(p, s.bet);
            s.bet = 0;
        }
    }

    public void removedFromWorld() {
        if (level instanceof ServerLevel sl && phase != WAITING && phase != SETTLE) refundAll(sl);
    }

    private void changed() {
        setChanged();
    }

    // 卓の勝負は保存しない（再起動したら空の卓にもどる。かけは止めるときに返す）
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
    }

    @Override
    public void setRemoved() {
        removedFromWorld();
        super.setRemoved();
    }
}
