package com.chigusa.jsncraft.client.casino;

import com.chigusa.jsncraft.casino.CasinoPayloads;
import com.chigusa.jsncraft.casino.Roulette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** ルーレット：盤の上でボールが回り、止まった数字で配当。かけは左クリックで置く・右クリックで外す。 */
public class RouletteScreen extends Screen {
    private static final int W = 340, H = 228, CELL = 14;
    private static final int[] CHIP_VALUES = {1, 5, 10, 50, 100};
    private static final int SPIN_TICKS = 100;

    private final BlockPos pos;
    private int left, top, chipIndex = 0, ticks;
    /** かけ：キー＝type*100+value */
    private final Map<Integer, Integer> bets = new LinkedHashMap<>();
    private Map<Integer, Integer> lastBets = new LinkedHashMap<>();
    private CasinoPayloads.RouletteResult result;
    private int spinStart = -1;
    private boolean waiting;
    private final List<Integer> history = new ArrayList<>();
    private String message = "";
    private Button spinButton, chipButton;

    public RouletteScreen(BlockPos pos) {
        super(Component.literal("ルーレット"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        chipButton = addRenderableWidget(Button.builder(chipLabel(), b -> {
            chipIndex = (chipIndex + 1) % CHIP_VALUES.length;
            b.setMessage(chipLabel());
        }).bounds(left + 124, top + H - 26, 70, 18).build());
        addRenderableWidget(Button.builder(Component.literal("クリア"), b -> { if (!busy()) bets.clear(); })
                .bounds(left + 198, top + H - 26, 40, 18).build());
        addRenderableWidget(Button.builder(Component.literal("同じかけ"), b -> {
            if (!busy() && !lastBets.isEmpty()) { bets.clear(); bets.putAll(lastBets); }
        }).bounds(left + 242, top + H - 26, 46, 18).build());
        spinButton = addRenderableWidget(Button.builder(Component.literal("§l回す"), b -> spin())
                .bounds(left + 292, top + H - 26, 40, 18).build());
    }

    private Component chipLabel() {
        return Component.literal("チップ：§e" + CHIP_VALUES[chipIndex]);
    }

    private boolean busy() {
        return waiting || (spinStart >= 0 && ticks - spinStart < SPIN_TICKS);
    }

    private long totalBet() {
        long t = 0;
        for (int v : bets.values()) t += v;
        return t;
    }

    private void spin() {
        if (busy() || bets.isEmpty()) return;
        if (CasinoClient.chips < totalBet()) {
            message = "§cチップが足りないよ";
            return;
        }
        List<CasinoPayloads.Bet> list = new ArrayList<>();
        for (var e : bets.entrySet()) list.add(new CasinoPayloads.Bet(e.getKey() / 100, e.getKey() % 100, e.getValue()));
        lastBets = new LinkedHashMap<>(bets);
        waiting = true;
        result = null;
        message = "";
        CasinoClient.chips -= totalBet();
        CasinoUi.sound(SoundEvents.CHAIN_PLACE, 1.6F);
        PacketDistributor.sendToServer(new CasinoPayloads.RouletteSpin(pos, list));
    }

    void onResult(CasinoPayloads.RouletteResult r) {
        waiting = false;
        if (!r.ok()) {
            CasinoClient.chips = r.chips();
            message = "§cチップが足りないよ";
            return;
        }
        result = r;
        spinStart = ticks;
    }

    @Override
    public void tick() {
        ticks++;
        if (result != null && spinStart >= 0) {
            int t = ticks - spinStart;
            if (t > 0 && t < SPIN_TICKS && t % Math.max(1, t / 12 + 1) == 0) CasinoUi.sound(SoundEvents.NOTE_BLOCK_HAT.value(), 2.0F);
            if (t == SPIN_TICKS) {
                CasinoClient.chips = result.chips();
                history.add(0, result.number());
                if (history.size() > 10) history.remove(10);
                if (result.payout() > 0) {
                    message = "§a§l" + result.number() + "！ §e+" + CasinoUi.fmt(result.payout()) + " 枚";
                    CasinoUi.sound(result.payout() >= totalOf(lastBets) * 10 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 1.2F);
                } else {
                    message = "§7" + result.number() + "… はずれ";
                    CasinoUi.sound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F);
                }
                bets.clear();
            }
        }
    }

    private static long totalOf(Map<Integer, Integer> m) {
        long t = 0;
        for (int v : m.values()) t += v;
        return t;
    }

    // ---------------- 盤とボール ----------------
    private float ballAngle(float pt) {
        int idx = 0;
        if (result != null) for (int i = 0; i < Roulette.WHEEL.length; i++) if (Roulette.WHEEL[i] == result.number()) idx = i;
        float end = (float) (idx * 2 * Math.PI / 37);
        if (result == null || spinStart < 0) return (float) (ticks * 0.02);
        float u = Math.min(1F, (ticks - spinStart + pt) / SPIN_TICKS);
        return end + (float) (8 * Math.PI) * (1 - u) * (1 - u);
    }

    private float wheelAngle(float pt) {
        if (result == null || spinStart < 0) return 0;
        float u = Math.min(1F, (ticks - spinStart + pt) / SPIN_TICKS);
        return -(float) (3 * Math.PI) * (1 - u) * (1 - u);
    }

    private void drawWheel(GuiGraphics g, int cx, int cy, float pt) {
        int R = 46;
        // 外わく → ボールの走る溝 → 内側
        fillCircle(g, cx, cy, R + 8, 0xFF5A3A1A);
        fillCircle(g, cx, cy, R + 6, 0xFF2A1A0A);
        fillCircle(g, cx, cy, R - 7, 0xFF6A4A24);
        fillCircle(g, cx, cy, 10, 0xFFE6B836);
        float wa = wheelAngle(pt);
        for (int i = 0; i < 37; i++) {
            double a = wa + i * 2 * Math.PI / 37 - Math.PI / 2;
            int n = Roulette.WHEEL[i];
            int col = n == 0 ? 0xFF1A9A3A : Roulette.isRed(n) ? 0xFFC01818 : 0xFF181818;
            int px = cx + (int) Math.round(Math.cos(a) * (R - 3)), py = cy + (int) Math.round(Math.sin(a) * (R - 3));
            g.fill(px - 3, py - 3, px + 4, py + 4, col);
        }
        // ボール
        double ba = ballAngle(pt) + wa - Math.PI / 2;
        float u = result == null || spinStart < 0 ? 0 : Math.min(1F, (ticks - spinStart + pt) / SPIN_TICKS);
        double br = (R + 4) - (u > 0.7F ? (u - 0.7F) / 0.3F * 7 : 0);
        int bx = cx + (int) Math.round(Math.cos(ba) * br), by = cy + (int) Math.round(Math.sin(ba) * br);
        g.fill(bx - 2, by - 2, bx + 2, by + 2, 0xFFFFFFFF);
    }

    private static void fillCircle(GuiGraphics g, int cx, int cy, int r, int col) {
        for (int y = -r; y <= r; y++) {
            int w = (int) Math.sqrt(r * r - y * y);
            g.fill(cx - w, cy + y, cx + w + 1, cy + y + 1, col);
        }
    }

    // ---------------- かけの台 ----------------
    private record Spot(int type, int value, int x, int y, int w, int h, String label, int color) {}

    private List<Spot> spots() {
        List<Spot> s = new ArrayList<>();
        int bx = left + 124, by = top + 30;
        s.add(new Spot(Roulette.STRAIGHT, 0, bx, by, CELL, CELL * 3, "0", 0xFF1A9A3A));
        for (int col = 0; col < 12; col++) {
            for (int row = 0; row < 3; row++) {
                int n = col * 3 + (3 - row);
                s.add(new Spot(Roulette.STRAIGHT, n, bx + CELL + col * CELL, by + row * CELL, CELL, CELL, String.valueOf(n),
                        Roulette.isRed(n) ? 0xFFC01818 : 0xFF181818));
            }
        }
        for (int row = 0; row < 3; row++) s.add(new Spot(Roulette.COLUMN, 2 - row, bx + CELL * 13, by + row * CELL, 22, CELL, "2:1", 0xFF0E5A36));
        for (int d = 0; d < 3; d++) s.add(new Spot(Roulette.DOZEN, d, bx + CELL + d * CELL * 4, by + CELL * 3, CELL * 4, CELL, (d * 12 + 1) + "-" + (d * 12 + 12), 0xFF0E5A36));
        String[] out = {"1-18", "偶数", "赤", "黒", "奇数", "19-36"};
        int[] types = {Roulette.LOW, Roulette.EVEN, Roulette.RED, Roulette.BLACK, Roulette.ODD, Roulette.HIGH};
        int[] cols = {0xFF0E5A36, 0xFF0E5A36, 0xFFC01818, 0xFF181818, 0xFF0E5A36, 0xFF0E5A36};
        for (int i = 0; i < 6; i++) s.add(new Spot(types[i], 0, bx + CELL + i * CELL * 2, by + CELL * 4, CELL * 2, CELL, out[i], cols[i]));
        return s;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!busy()) {
            for (Spot s : spots()) {
                if (mx >= s.x && mx < s.x + s.w && my >= s.y && my < s.y + s.h) {
                    int key = s.type * 100 + s.value;
                    if (button == 1) bets.remove(key);
                    else if (totalBet() + CHIP_VALUES[chipIndex] <= Roulette.MAX_TOTAL) {
                        bets.merge(key, CHIP_VALUES[chipIndex], Integer::sum);
                        CasinoUi.sound(SoundEvents.CHAIN_HIT, 1.8F);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        CasinoUi.panel(g, left, top, W, H);
        g.drawCenteredString(font, "§6§l♦ ルーレット ♦", left + W / 2, top + 10, 0xFFFFFF);
        drawWheel(g, left + 64, top + 84, pt);

        boolean done = result != null && spinStart >= 0 && ticks - spinStart >= SPIN_TICKS;
        for (Spot s : spots()) {
            int key = s.type * 100 + s.value;
            boolean win = done && Roulette.multiplier(s.type, s.value, result.number()) > 0;
            g.fill(s.x, s.y, s.x + s.w, s.y + s.h, 0xFFE6D8A0);
            g.fill(s.x + 1, s.y + 1, s.x + s.w - 1, s.y + s.h - 1, win && (ticks / 5) % 2 == 0 ? 0xFFE6B836 : s.color);
            g.pose().pushPose();
            g.pose().translate(s.x + s.w / 2F, s.y + s.h / 2F - 3, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawCenteredString(font, s.label, 0, 0, 0xFFFFFF);
            g.pose().popPose();
            Integer amt = bets.get(key);
            if (amt == null && done && lastBets.containsKey(key)) amt = lastBets.get(key);
            if (amt != null) {
                int cx = s.x + s.w - 5, cy = s.y + 3;
                g.fill(cx - 4, cy - 3, cx + 4, cy + 4, 0xFF1A1018);
                g.fill(cx - 3, cy - 2, cx + 3, cy + 3, 0xFFF0C040);
                g.pose().pushPose();
                g.pose().translate(cx, cy - 1, 200);
                g.pose().scale(0.5F, 0.5F, 1);
                g.drawCenteredString(font, "§0" + amt, 0, 0, 0xFFFFFF);
                g.pose().popPose();
            }
        }

        int iy = top + 112;
        g.drawString(font, "チップ：§e" + CasinoUi.fmt(CasinoClient.chips) + "  §fかけ：§e" + CasinoUi.fmt(totalBet()), left + 124, top + 106, 0xFFFFFF);
        // 当たった数字
        if (done) {
            int n = result.number();
            int col = n == 0 ? 0xFF1A9A3A : Roulette.isRed(n) ? 0xFFC01818 : 0xFF181818;
            g.fill(left + 50, top + 146, left + 78, top + 168, 0xFFE6B836);
            g.fill(left + 51, top + 147, left + 77, top + 167, col);
            g.drawCenteredString(font, "§l" + n, left + 64, top + 153, 0xFFFFFF);
        }
        g.drawString(font, message, left + 124, iy + 10, 0xFFFFFF);
        // 履歴
        g.drawString(font, "§7これまで：", left + 124, iy + 30, 0xFFFFFF);
        for (int i = 0; i < history.size(); i++) {
            int n = history.get(i);
            int x = left + 170 + i * 16;
            g.fill(x, iy + 28, x + 14, iy + 40, n == 0 ? 0xFF1A9A3A : Roulette.isRed(n) ? 0xFFC01818 : 0xFF181818);
            g.pose().pushPose();
            g.pose().translate(x + 7, iy + 31, 0);
            g.pose().scale(0.7F, 0.7F, 1);
            g.drawCenteredString(font, String.valueOf(n), 0, 0, 0xFFFFFF);
            g.pose().popPose();
        }
        g.drawString(font, "§8左クリック：置く  右クリック：外す", left + 124, iy + 48, 0xFFFFFF);
        g.drawString(font, "§8配当：数字36倍・ダズン/列3倍・赤黒など2倍", left + 124, iy + 60, 0xFFFFFF);
        spinButton.active = !busy() && !bets.isEmpty();
        super.render(g, mx, my, pt);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
