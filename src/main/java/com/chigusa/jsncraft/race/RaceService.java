package com.chigusa.jsncraft.race;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.casino.CasinoLuck;
import com.chigusa.jsncraft.casino.Chips;
import com.chigusa.jsncraft.compat.PhoneBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 競馬：サーバー全体で1つのレースを、2分の受付 → 約30秒のレース → 10秒の結果 でくり返す。
 * どの競馬の台からでも、同じレースにかけられる。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class RaceService {
    public static final int BETTING = 0, RACING = 1, RESULT = 2;
    public static final int BET_TICKS = 2400, RESULT_TICKS = 200, MAX_BET = 1000;
    public static final int WIN = 0, PLACE = 1;

    static int raceNo = 1, phase = BETTING;
    static long phaseStart = -1, horseSeed, raceSeed;
    static RaceSim.Horse[] horses;
    static RaceSim.Result result;
    static final float[] oddsWin = new float[8], oddsPlace = new float[8];
    /** かけ：プレイヤー → [馬×2 + 種類] の枚数 */
    static final Map<UUID, long[]> BETS = new LinkedHashMap<>();
    static final List<int[]> HISTORY = new ArrayList<>();   // {レース番号, 1着, 単勝×10}

    static void newRace(MinecraftServer server) {
        horseSeed = server.overworld().random.nextLong();
        horses = RaceSim.horses(horseSeed);
        // オッズ：何回も走らせてみて、勝ちやすさから決める（2割は胴元）
        int sims = 800;
        int[] w = new int[8], pl = new int[8];
        for (int s = 0; s < sims; s++) {
            RaceSim.Result r = RaceSim.run(horses, horseSeed * 1000 + s * 7919L + 1);
            w[r.order()[0]]++;
            for (int k = 0; k < 3; k++) pl[r.order()[k]]++;
        }
        for (int i = 0; i < 8; i++) {
            double pw = (w[i] + 0.5) / (sims + 4.0), pp = (pl[i] + 0.5) / (sims + 4.0);
            oddsWin[i] = (float) Math.max(1.1, Math.min(300, Math.floor(0.8 / pw * 10) / 10));
            oddsPlace[i] = (float) Math.max(1.0, Math.min(60, Math.floor(0.8 / pp * 10) / 10));
        }
        result = null;
        raceSeed = 0;
        phase = BETTING;
        phaseStart = server.getTickCount();
    }

    static int phaseLength() {
        return phase == BETTING ? BET_TICKS : phase == RACING ? (result == null ? 600 : result.ticks() + 40) : RESULT_TICKS;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (horses == null) newRace(server);
        long now = server.getTickCount();
        if (now - phaseStart < phaseLength()) return;
        if (phase == BETTING) {
            raceSeed = server.overworld().random.nextLong() | 1;
            result = RaceSim.run(horses, raceSeed);
            phase = RACING;
            phaseStart = now;
        } else if (phase == RACING) {
            payout(server);
            phase = RESULT;
            phaseStart = now;
        } else {
            raceNo++;
            newRace(server);
        }
    }

    private static void payout(MinecraftServer server) {
        int[] o = result.order();
        HISTORY.add(0, new int[]{raceNo, o[0], Math.round(oddsWin[o[0]] * 10)});
        while (HISTORY.size() > 8) HISTORY.remove(HISTORY.size() - 1);
        Pending pend = Pending.get(server);
        for (var e : BETS.entrySet()) {
            long[] b = e.getValue();
            long total = 0, won = 0;
            for (long v : b) total += v;
            won += (long) Math.floor(b[o[0] * 2 + WIN] * oddsWin[o[0]]);
            for (int k = 0; k < 3; k++) won += (long) Math.floor(b[o[k] * 2 + PLACE] * oddsPlace[o[k]]);
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                if (won > 0) {
                    Chips.give(p, won);
                    p.sendSystemMessage(Component.literal("§6競馬 第" + raceNo + "レース：当たり！ チップ +" + won + "枚"));
                    if (won >= 500 && won >= total * 10)
                        PhoneBridge.news(p, p.getName().getString() + " が競馬で大穴を当てた！（チップ " + won + "枚）", "casino");
                } else if (total > 0) p.displayClientMessage(Component.literal("§7競馬 第" + raceNo + "レース：はずれ…"), true);
                CasinoLuck.played(p, total, won);
                if (won < total) CasinoLuck.refund(p, total - won);
            } else if (won > 0) pend.add(e.getKey(), won);
        }
        BETS.clear();
    }

    /** 台の画面から：horse=-1 は様子だけ、-2 は自分のかけを取り消す */
    public static void handle(ServerPlayer p, RacePayloads.Bet req) {
        if (p.blockPosition().distSqr(req.terminal()) > 64 || !(p.level().getBlockState(req.terminal()).getBlock() instanceof RaceTerminalBlock)) return;
        if (horses == null) newRace(p.server);
        long lastWin = Pending.get(p.server).take(p.getUUID());
        if (lastWin > 0) {
            Chips.give(p, lastWin);
            p.sendSystemMessage(Component.literal("§6競馬：いない間の当たり チップ +" + lastWin + "枚"));
        }
        if (phase == BETTING && req.horse() >= 0 && req.horse() < 8 && (req.type() == WIN || req.type() == PLACE)) {
            long[] mine = BETS.getOrDefault(p.getUUID(), new long[16]);
            long sum = 0;
            for (long v : mine) sum += v;
            int amount = Math.max(0, Math.min(MAX_BET, req.amount()));
            if (amount > 0 && sum + amount <= MAX_BET * 5L && Chips.take(p, amount)) {
                mine[req.horse() * 2 + req.type()] += amount;
                BETS.put(p.getUUID(), mine);
            } else if (amount > 0) p.displayClientMessage(Component.literal("チップが足りないよ"), true);
        } else if (phase == BETTING && req.horse() == -2) {
            long[] mine = BETS.remove(p.getUUID());
            if (mine != null) {
                long sum = 0;
                for (long v : mine) sum += v;
                if (sum > 0) Chips.give(p, sum);
            }
        }
        send(p, req.terminal(), lastWin);
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        handle(p, new RacePayloads.Bet(pos, -1, 0, 0));
    }

    static void send(ServerPlayer p, BlockPos terminal, long lastWin) {
        List<Long> mine = new ArrayList<>();
        long[] m = BETS.get(p.getUUID());
        for (int i = 0; i < 16; i++) mine.add(m == null ? 0 : m[i]);
        List<Float> ow = new ArrayList<>(), op = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ow.add(oddsWin[i]);
            op.add(oddsPlace[i]);
        }
        List<Integer> hist = new ArrayList<>();
        for (int[] h : HISTORY) for (int v : h) hist.add(v);
        long now = p.server.getTickCount();
        PacketDistributor.sendToPlayer(p, new RacePayloads.State(terminal, raceNo, phase, (int) Math.max(0, phaseStart + phaseLength() - now),
                (int) (now - phaseStart), horseSeed, phase == BETTING ? 0 : raceSeed, ow, op, mine, hist, Chips.count(p), lastWin, BETS.size()));
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent e) {
        // 止めるときは、かけをお返しする（いない人は次に台を開いたとき）
        Pending pend = Pending.get(e.getServer());
        for (var b : BETS.entrySet()) {
            long sum = 0;
            for (long v : b.getValue()) sum += v;
            if (sum > 0) pend.add(b.getKey(), sum);
        }
        BETS.clear();
        horses = null;
    }

    /** いなかった人の当たり（ワールドに保存） */
    public static final class Pending extends SavedData {
        final Map<UUID, Long> map = new HashMap<>();

        static Pending get(MinecraftServer s) {
            return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Pending::new, Pending::load), "jsn_craft_race");
        }

        static Pending load(CompoundTag t, HolderLookup.Provider reg) {
            Pending p = new Pending();
            for (String k : t.getAllKeys()) p.map.put(UUID.fromString(k), t.getLong(k));
            return p;
        }

        void add(UUID u, long v) {
            map.merge(u, v, Long::sum);
            setDirty();
        }

        long take(UUID u) {
            Long v = map.remove(u);
            if (v != null) setDirty();
            return v == null ? 0 : v;
        }

        @Override
        public CompoundTag save(CompoundTag t, HolderLookup.Provider reg) {
            map.forEach((k, v) -> t.putLong(k.toString(), v));
            return t;
        }
    }

    private RaceService() {}
}
