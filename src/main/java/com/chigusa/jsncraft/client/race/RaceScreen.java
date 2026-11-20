package com.chigusa.jsncraft.client.race;

import com.chigusa.jsncraft.race.RaceNames;
import com.chigusa.jsncraft.race.RacePayloads;
import com.chigusa.jsncraft.race.RaceService;
import com.chigusa.jsncraft.race.RaceSim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

/** 競馬の台：出走表（オッズ・単勝・複勝）とレースの中継 */
public class RaceScreen extends Screen {
    private static final int W = 340, H = 236, ROW = 15;
    private static final int[] AMOUNTS = {1, 5, 10, 50, 100};
    private final BlockPos terminal;
    private RacePayloads.State st;
    private long receivedAt;
    private int left, top, ticks, amountIdx = 1;
    private Button amountBtn, cancelBtn;
    private long cachedSeed;
    private RaceSim.Result sim;
    private RaceSim.Horse[] horses;
    private String[] names = new String[8];
    private long namesSeed = Long.MIN_VALUE;
    private int lastPhase = -1;
    private long[] raceBets = new long[16];
    private static long closedAt;

    public RaceScreen(BlockPos terminal) {
        super(Component.literal("競馬"));
        this.terminal = terminal;
    }

    public static void onState(RacePayloads.State s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof RaceScreen r && r.terminal.equals(s.terminal())) r.update(s);
        else if (mc.screen == null && System.currentTimeMillis() - closedAt > 1500) {
            RaceScreen r = new RaceScreen(s.terminal());
            mc.setScreen(r);
            r.update(s);
        }
    }

    private void update(RacePayloads.State s) {
        if (lastPhase == RaceService.BETTING && s.phase() == RaceService.RACING)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 0.8F));
        lastPhase = s.phase();
        st = s;
        if (s.phase() != RaceService.RESULT) for (int i = 0; i < 16; i++) raceBets[i] = s.mine().get(i);
        receivedAt = System.currentTimeMillis();
        if (s.horseSeed() != namesSeed) {
            namesSeed = s.horseSeed();
            names = RaceNames.pick(s.horseSeed());
            horses = RaceSim.horses(s.horseSeed());
        }
        if (s.raceSeed() != 0 && s.raceSeed() != cachedSeed) {
            cachedSeed = s.raceSeed();
            sim = RaceSim.run(horses, s.raceSeed());
        }
        refresh();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        amountBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            amountIdx = (amountIdx + 1) % AMOUNTS.length;
            refresh();
        }).bounds(left + 10, top + H - 26, 120, 20).build());
        cancelBtn = addRenderableWidget(Button.builder(Component.literal("かけを取り消す"), b -> send(-2, 0, 0)).bounds(left + 136, top + H - 26, 100, 20).build());
        refresh();
    }

    private void refresh() {
        if (amountBtn == null) return;
        amountBtn.setMessage(Component.literal("1回にかける：§e" + AMOUNTS[amountIdx] + "枚"));
        long mine = 0;
        if (st != null) for (long v : st.mine()) mine += v;
        cancelBtn.active = st != null && st.phase() == RaceService.BETTING && mine > 0;
        cancelBtn.visible = st == null || st.phase() == RaceService.BETTING;
        amountBtn.visible = cancelBtn.visible;
    }

    private void send(int horse, int type, int amount) {
        PacketDistributor.sendToServer(new RacePayloads.Bet(terminal, horse, type, amount));
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks % 10 == 0) send(-1, 0, 0);
    }

    private int nowTick() {
        return st.elapsed() + (int) ((System.currentTimeMillis() - receivedAt) / 50);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.fill(left, top, left + W, top + H, 0xF0102018);
        g.renderOutline(left, top, W, H, 0xFF60C060);
        if (st == null) return;
        g.drawString(font, "§l競馬 §r§a第" + st.raceNo() + "レース §7芝 1000m", left + 10, top + 8, 0xFFFFFFFF);
        String chips = "チップ：" + st.chips() + "枚";
        g.drawString(font, "§7" + chips, left + W - 10 - font.width(chips), top + 8, 0xFFFFFF);
        int tl = Math.max(0, st.ticksLeft() - (int) ((System.currentTimeMillis() - receivedAt) / 50));
        if (st.phase() == RaceService.BETTING) {
            g.drawString(font, "§aかけ受付中 §f" + (tl / 20 + 1) + "秒 §7（" + st.players() + "人がかけてる）", left + 10, top + 20, 0xFFFFFF);
            table(g, mx, my);
        } else {
            track(g, pt);
            if (st.phase() == RaceService.RESULT && sim != null) results(g);
        }
        // 最近の勝ち馬
        StringBuilder h = new StringBuilder("§7前回：");
        var hs = st.history();
        for (int i = 0; i + 2 < hs.size() && i < 9; i += 3) h.append("§f").append(hs.get(i + 1) + 1).append("番§8(").append(hs.get(i + 2) / 10.0).append(") ");
        g.drawString(font, h.toString(), left + 244 - 2, top + H - 20, 0xFFFFFF);
    }

    /** 出走表 */
    private void table(GuiGraphics g, int mx, int my) {
        int y0 = top + 34;
        g.drawString(font, "§8番 馬の名前", left + 10, y0, 0xFFFFFF);
        g.drawString(font, "§8調子", left + 138, y0, 0xFFFFFF);
        g.drawString(font, "§8単勝", left + 176, y0, 0xFFFFFF);
        g.drawString(font, "§8複勝", left + 212, y0, 0xFFFFFF);
        g.drawString(font, "§8自分のかけ", left + 248, y0, 0xFFFFFF);
        // 人気
        Integer[] idx = new Integer[8];
        for (int i = 0; i < 8; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> Float.compare(st.oddsWin().get(a), st.oddsWin().get(b)));
        int[] pop = new int[8];
        for (int k = 0; k < 8; k++) pop[idx[k]] = k + 1;
        for (int i = 0; i < 8; i++) {
            int y = y0 + 12 + i * ROW;
            if (i % 2 == 0) g.fill(left + 6, y - 2, left + W - 6, y + ROW - 2, 0x20FFFFFF);
            int c = RaceNames.COLORS[i];
            g.fill(left + 9, y - 1, left + 21, y + 10, c);
            g.drawCenteredString(font, String.valueOf(i + 1), left + 15, y + 1, i == 0 || i == 4 || i == 7 ? 0xFF000000 : 0xFFFFFFFF);
            g.drawString(font, names[i], left + 25, y + 1, 0xFFFFFF);
            g.drawString(font, "§e" + "★".repeat(RaceSim.stars(horses[i])), left + 132, y + 1, 0xFFFFFF);
            g.drawString(font, (pop[i] == 1 ? "§c" : "§f") + st.oddsWin().get(i), left + 176, y + 1, 0xFFFFFF);
            g.drawString(font, "§f" + st.oddsPlace().get(i), left + 212, y + 1, 0xFFFFFF);
            long w = st.mine().get(i * 2), p = st.mine().get(i * 2 + 1);
            String mine = (w > 0 ? "§e単" + w + " " : "") + (p > 0 ? "§b複" + p : "");
            g.drawString(font, mine.isEmpty() ? "§8-" : mine, left + 248, y + 1, 0xFFFFFF);
            // かけるボタン（小さい）
            smallButton(g, left + W - 46, y - 1, "単", 0xFFB08020, mx, my);
            smallButton(g, left + W - 26, y - 1, "複", 0xFF2080B0, mx, my);
        }
        g.drawString(font, "§8単勝＝1着を当てる / 複勝＝3着までに入るか。数字はかけた枚数の何倍もどるか", left + 10, y0 + 12 + 8 * ROW + 2, 0xFFFFFF);
    }

    private void smallButton(GuiGraphics g, int x, int y, String label, int color, int mx, int my) {
        boolean hov = mx >= x && mx < x + 18 && my >= y && my < y + 12;
        g.fill(x, y, x + 18, y + 12, hov ? color | 0xFF000000 : (color & 0x00FFFFFF) | 0xA0000000);
        g.drawCenteredString(font, label, x + 9, y + 2, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (st != null && st.phase() == RaceService.BETTING) {
            int y0 = top + 34;
            for (int i = 0; i < 8; i++) {
                int y = y0 + 12 + i * ROW - 1;
                for (int t = 0; t < 2; t++) {
                    int x = left + W - 46 + t * 20;
                    if (mx >= x && mx < x + 18 && my >= y && my < y + 12) {
                        send(i, t, AMOUNTS[amountIdx]);
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHAIN_PLACE, 1.6F, 0.5F));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    /** レースの中継：先頭に合わせて画面が流れる */
    private void track(GuiGraphics g, float pt) {
        if (sim == null) {
            g.drawCenteredString(font, "§7レース中…", left + W / 2, top + 80, 0xFFFFFF);
            return;
        }
        int t = Math.min(sim.ticks(), Math.max(0, nowTick()));
        double[] pos = sim.pos()[Math.min(t, sim.pos().length - 1)];
        double[] next = sim.pos()[Math.min(t + 1, sim.pos().length - 1)];
        double f = Math.min(1, ((System.currentTimeMillis() - receivedAt) % 50) / 50.0);
        double lead = 0;
        double[] p = new double[8];
        for (int i = 0; i < 8; i++) {
            p[i] = pos[i] + (next[i] - pos[i]) * f;
            lead = Math.max(lead, p[i]);
        }
        int tx = left + 10, ty = top + 22, tw = W - 20, lane = 15, th = lane * 8 + 8;
        double view = 90;   // 画面に見える長さ(m)
        double cam = Math.max(view * 0.7, Math.min(RaceSim.LENGTH + 10, lead + 10)) - view * 0.75;
        // 芝
        g.fill(tx, ty, tx + tw, ty + th, 0xFF3A8A3A);
        for (int i = 0; i < 8; i += 2) g.fill(tx, ty + 4 + i * lane, tx + tw, ty + 4 + (i + 1) * lane, 0xFF358035);
        g.fill(tx, ty, tx + tw, ty + 3, 0xFFF0F0F0);
        g.fill(tx, ty + th - 3, tx + tw, ty + th, 0xFFF0F0F0);
        // 距離の目印
        for (int m = (int) (cam / 25) * 25; m < cam + view + 25; m += 25) {
            int x = tx + (int) ((m - cam) / view * tw);
            if (x < tx || x > tx + tw) continue;
            g.fill(x, ty + 3, x + 1, ty + th - 3, 0x30FFFFFF);
            if (m % 100 == 0) g.drawString(font, "§f" + (int) (RaceSim.LENGTH - m) + "m", x + 2, ty + th + 2, 0xFFFFFF);
        }
        // ゴール
        int gx = tx + (int) ((RaceSim.LENGTH - cam) / view * tw);
        if (gx >= tx && gx <= tx + tw) {
            for (int y = ty + 3; y < ty + th - 3; y += 4) g.fill(gx, y, gx + 3, y + 2, (y / 4 % 2 == 0) ? 0xFFFFFFFF : 0xFF000000);
            g.drawString(font, "§lGOAL", gx - 12, ty - 9, 0xFFFFFF);
        }
        // 馬
        for (int i = 0; i < 8; i++) {
            int x = tx + (int) ((p[i] - cam) / view * tw);
            int y = ty + 4 + i * lane + 2;
            if (x < tx - 20 || x > tx + tw + 4) continue;
            horse(g, x, y, i, (int) (p[i] * 0.8) % 2 == 0);
        }
        // 順位
        Integer[] idx = new Integer[8];
        for (int i = 0; i < 8; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> Double.compare(p[b], p[a]));
        int ry = ty + th + 14;
        g.drawString(font, "§7順位：", tx, ry, 0xFFFFFF);
        int rx = tx + 30;
        for (int k = 0; k < 8; k++) {
            int i = idx[k];
            g.fill(rx, ry - 1, rx + 12, ry + 9, RaceNames.COLORS[i]);
            g.drawCenteredString(font, String.valueOf(i + 1), rx + 6, ry, i == 0 || i == 4 || i == 7 ? 0xFF000000 : 0xFFFFFFFF);
            rx += 15;
        }
        double remain = Math.max(0, RaceSim.LENGTH - lead);
        g.drawString(font, "§fのこり " + (int) remain + "m", rx + 8, ry, 0xFFFFFF);
        // 自分のかけ
        long mine = 0;
        for (long v : st.mine()) mine += v;
        if (mine > 0) g.drawString(font, "§eあなたのかけ：" + mine + "枚", tx, ry + 12, 0xFFFFFF);
    }

    /** 小さな馬（ドット絵） */
    private void horse(GuiGraphics g, int x, int y, int i, boolean frame) {
        int body = 0xFF6A3A1A, dark = 0xFF3A1A08;
        // 体
        g.fill(x - 12, y + 2, x, y + 7, body);
        // 首と頭
        g.fill(x - 2, y - 1, x + 2, y + 4, body);
        g.fill(x, y - 2, x + 4, y + 1, body);
        // しっぽ
        g.fill(x - 14, y + 2, x - 12, y + 6, dark);
        // あし（2コマ）
        if (frame) {
            g.fill(x - 11, y + 7, x - 9, y + 11, dark);
            g.fill(x - 3, y + 7, x - 1, y + 11, dark);
        } else {
            g.fill(x - 9, y + 7, x - 7, y + 11, dark);
            g.fill(x - 5, y + 7, x - 3, y + 11, dark);
        }
        // 騎手（枠の色）
        g.fill(x - 8, y - 2, x - 3, y + 2, RaceNames.COLORS[i]);
        g.fill(x - 7, y - 4, x - 4, y - 2, 0xFFE8C8A0);
        g.drawString(font, String.valueOf(i + 1), x - 22, y, 0xFFFFFFFF);
    }

    private void results(GuiGraphics g) {
        int[] o = sim.order();
        int x = left + W / 2 - 90, y = top + 40, w = 180, h = 112;
        g.fill(x, y, x + w, y + h, 0xE0000000);
        g.renderOutline(x, y, w, h, 0xFFF0C030);
        g.drawCenteredString(font, "§6§l結果", x + w / 2, y + 6, 0xFFFFFF);
        for (int k = 0; k < 3; k++) {
            int i = o[k];
            int yy = y + 22 + k * 14;
            g.drawString(font, (k + 1) + "着", x + 10, yy, 0xFFFFFF);
            g.fill(x + 34, yy - 1, x + 46, yy + 9, RaceNames.COLORS[i]);
            g.drawCenteredString(font, String.valueOf(i + 1), x + 40, yy, i == 0 || i == 4 || i == 7 ? 0xFF000000 : 0xFFFFFFFF);
            g.drawString(font, names[i], x + 50, yy, 0xFFFFFF);
        }
        g.drawString(font, "§7単勝 " + (o[0] + 1) + "番 §f" + st.oddsWin().get(o[0]) + "倍", x + 10, y + 68, 0xFFFFFF);
        long won = (long) Math.floor(raceBets[o[0] * 2] * st.oddsWin().get(o[0]));
        for (int k = 0; k < 3; k++) won += (long) Math.floor(raceBets[o[k] * 2 + 1] * st.oddsPlace().get(o[k]));
        long mine = 0;
        for (long v : raceBets) mine += v;
        if (mine > 0) g.drawCenteredString(font, won > 0 ? "§a§l当たり！ +" + won + "枚" : "§7はずれ…", x + w / 2, y + 88, 0xFFFFFF);
        else g.drawCenteredString(font, "§8次のレースは まもなく受付", x + w / 2, y + 88, 0xFFFFFF);
    }

    @Override
    public void removed() {
        closedAt = System.currentTimeMillis();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
