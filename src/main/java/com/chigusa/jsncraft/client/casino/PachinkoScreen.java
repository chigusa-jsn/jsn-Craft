package com.chigusa.jsncraft.client.casino;

import com.chigusa.jsncraft.casino.pachinko.Board;
import com.chigusa.jsncraft.casino.pachinko.PachinkoPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * パチンコの画面。ハンドルの強さ（ホイール／←→）を決めて「打つ」。
 * 玉の動きはサーバーと同じ計算（Board）で、送られてきた種から再現する。
 */
public class PachinkoScreen extends Screen {
    private static final int W = 350, H = 252;
    private static final String[] MODE = {"通常", "§c確変", "§b時短"};

    private final BlockPos pos;
    private int left, top, bx, by, ticks;
    private int strength = 300;
    private boolean firing;
    private int nextFire;
    private PachinkoPayloads.State state;
    private int localSpin;
    private Button fireButton;

    /** 動いている玉 */
    private static final class Ball {
        final List<float[]> path = new ArrayList<>();
        float t;
        int result;
    }

    private final List<Ball> balls = new ArrayList<>();
    private final List<int[]> flashes = new ArrayList<>();   // x, y, 残り

    public PachinkoScreen(BlockPos pos) {
        super(Component.literal("パチンコ"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        bx = left + 10;
        by = top + 12;
        int x = left + 222;
        fireButton = addRenderableWidget(Button.builder(fireLabel(), b -> {
            firing = !firing;
            b.setMessage(fireLabel());
        }).bounds(x, top + 168, 118, 18).build());
        addRenderableWidget(Button.builder(Component.literal("玉を借りる（100玉）"), b -> PacketDistributor.sendToServer(new PachinkoPayloads.Balls(pos, true)))
                .bounds(x, top + 190, 118, 16).build());
        addRenderableWidget(Button.builder(Component.literal("玉をチップに戻す"), b -> PacketDistributor.sendToServer(new PachinkoPayloads.Balls(pos, false)))
                .bounds(x, top + 208, 118, 16).build());
    }

    private Component fireLabel() {
        return Component.literal(firing ? "§c§l■ 止める（Space）" : "§a§l▶ 打つ（Space）");
    }

    void onState(PachinkoPayloads.State s) {
        PachinkoPayloads.State old = state;
        state = s;
        if (old == null || s.spin() != old.spin() || s.spinTotal() != old.spinTotal()) localSpin = s.spin();
        if (old != null && old.round() == 0 && s.round() > 0) CasinoUi.sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
        if (old != null && old.spin() == 0 && s.spin() > 0) CasinoUi.sound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.6F);
        if (s.balls() <= 0 && firing) {
            firing = false;
            if (fireButton != null) fireButton.setMessage(fireLabel());
        }
    }

    void onShot(PachinkoPayloads.Shot s) {
        Ball b = new Ball();
        b.result = Board.shoot(s.seed(), s.strength() / 1000.0, s.attacker(), s.denchu(), b.path);
        b.result = s.result();
        balls.add(b);
    }

    @Override
    public void tick() {
        ticks++;
        if (localSpin > 0) {
            localSpin--;
            if (state != null && localSpin == 0) CasinoUi.sound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F);
        }
        if (firing && ticks >= nextFire && state != null && state.balls() > 0) {
            nextFire = ticks + 11;
            PacketDistributor.sendToServer(new PachinkoPayloads.Shoot(pos, strength));
            CasinoUi.sound(SoundEvents.NOTE_BLOCK_HAT.value(), 2.0F);
        }
        for (Iterator<Ball> it = balls.iterator(); it.hasNext(); ) {
            Ball b = it.next();
            b.t += 6;
            if (b.t >= b.path.size() - 1) {
                float[] last = b.path.isEmpty() ? new float[]{100, 220} : b.path.get(b.path.size() - 1);
                if (b.result != Board.OUT) {
                    flashes.add(new int[]{(int) last[0], (int) last[1], 10});
                    CasinoUi.sound(b.result == Board.ATTACKER ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.NOTE_BLOCK_CHIME.value(),
                            b.result == Board.HESO || b.result == Board.DENCHU ? 1.8F : 1.2F);
                }
                it.remove();
            }
        }
        flashes.removeIf(f -> --f[2] <= 0);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        strength = Mth.clamp(strength + (int) (sy * 20), 0, 1000);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_SPACE -> { fireButton.onPress(); return true; }
            case GLFW.GLFW_KEY_LEFT -> { strength = Math.max(0, strength - 20); return true; }
            case GLFW.GLFW_KEY_RIGHT -> { strength = Math.min(1000, strength + 20); return true; }
            default -> { return super.keyPressed(key, scan, mods); }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (dragHandle(mx, my)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragHandle(mx, my)) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    /** ハンドルのゲージ（右の帯） */
    private boolean dragHandle(double mx, double my) {
        int x = left + 222, y = top + 140, w = 118;
        if (mx >= x && mx <= x + w && my >= y - 2 && my <= y + 10) {
            strength = Mth.clamp((int) ((mx - x) / w * 1000), 0, 1000);
            return true;
        }
        return false;
    }

    // ---------------- 描く ----------------
    private void fillCircle(GuiGraphics g, double cx, double cy, double r, int col, boolean topOnly) {
        for (int y = (int) -r; y <= r; y++) {
            if (topOnly && y > 0) break;
            int w = (int) Math.sqrt(r * r - y * y);
            g.fill((int) (cx - w), (int) (cy + y), (int) (cx + w + 1), (int) (cy + y + 1), col);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF0A0A10);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, CasinoUi.GOLD);
        g.fill(left, top, left + W, top + H, 0xFF3A1A4A);
        // 盤
        fillCircle(g, bx + Board.CX, by + Board.CY, Board.R + 4, 0xFFB0A090, true);
        g.fill(bx + 2, by + (int) Board.CY, bx + Board.W - 2, by + Board.H, 0xFFB0A090);
        fillCircle(g, bx + Board.CX, by + Board.CY, Board.R, 0xFF2A6AA0, true);
        g.fill(bx + 6, by + (int) Board.CY, bx + Board.W - 6, by + Board.H - 4, 0xFF2A6AA0);
        // 右の道の壁
        g.fill(bx + (int) Board.WALL_X - 1, by + (int) Board.WALL_Y0, bx + (int) Board.WALL_X + 1, by + (int) Board.WALL_Y1, 0xFFE0E0E0);
        // 液晶
        drawLcd(g, bx + 64, by + 50, 72, 64, pt);
        // 釘
        for (double[] p : Board.PEGS) g.fill(bx + (int) p[0] - 1, by + (int) p[1] - 1, bx + (int) p[0] + 1, by + (int) p[1] + 1, 0xFFE8E8F0);
        // ポケット
        boolean electric = state != null && state.mode() != 0 && state.round() == 0;
        boolean attacker = state != null && state.attacker();
        drawPocket(g, Board.HESO, 0xFFF0C040, "");
        drawPocket(g, Board.LEFT, 0xFF40C060, "");
        drawPocket(g, Board.RIGHT, 0xFF40C060, "");
        drawPocket(g, Board.DENCHU, electric ? ((ticks / 4) % 2 == 0 ? 0xFFFF6060 : 0xFFFFC040) : 0xFF707070, "");
        double[] ap = Board.POCKET[Board.ATTACKER];
        g.fill(bx + (int) ap[0], by + (int) ap[2] - 2, bx + (int) ap[1], by + (int) ap[2] + 3, attacker ? ((ticks / 3) % 2 == 0 ? 0xFFFF3030 : 0xFFFFE040) : 0xFF606060);
        // 光
        for (int[] f : flashes) g.fill(bx + f[0] - 4, by + f[1] - 4, bx + f[0] + 4, by + f[1] + 4, 0x80FFFF80);
        // 玉
        for (Ball b : balls) {
            if (b.path.isEmpty()) continue;
            float t = Math.min(b.path.size() - 1, b.t + pt * 6);
            int i = (int) t;
            float f = t - i;
            float[] a = b.path.get(i), c = b.path.get(Math.min(b.path.size() - 1, i + 1));
            float x = a[0] + (c[0] - a[0]) * f, y = a[1] + (c[1] - a[1]) * f;
            g.fill(bx + (int) x - 2, by + (int) y - 2, bx + (int) x + 2, by + (int) y + 2, 0xFFD8D8E0);
            g.fill(bx + (int) x - 1, by + (int) y - 2, bx + (int) x, by + (int) y - 1, 0xFFFFFFFF);
        }

        // 右の情報
        int x = left + 222, y = top + 14;
        g.drawString(font, "§6§l★ JSNパチンコ ★", x, y, 0xFFFFFF);
        if (state != null) {
            g.drawString(font, "玉：§e" + CasinoUi.fmt(state.balls()), x, y + 16, 0xFFFFFF);
            g.drawString(font, "チップ：§e" + CasinoUi.fmt(CasinoClient.chips), x, y + 28, 0xFFFFFF);
            g.drawString(font, "状態：" + MODE[Math.min(2, state.mode())] + (state.mode() == 2 ? " §7残り" + state.jitan() + "回" : ""), x, y + 44, 0xFFFFFF);
            g.drawString(font, "大当たり：§e" + state.jackpots() + " §f回", x, y + 56, 0xFFFFFF);
            if (state.round() > 0) {
                boolean blink = (ticks / 6) % 2 == 0;
                g.drawString(font, (blink ? "§c§l" : "§6§l") + "大当たり！", x, y + 74, 0xFFFFFF);
                g.drawString(font, "§fラウンド " + Math.min(8, state.round()) + "/8  §e" + state.roundCount() + "/9", x, y + 86, 0xFFFFFF);
                g.drawString(font, "§c§l→ 右打ち！", x, y + 98, 0xFFFFFF);
            } else if (state.mode() != 0) {
                g.drawString(font, "§c§l→ 右打ち！（電チュー）", x, y + 80, 0xFFFFFF);
            } else {
                g.drawString(font, "§7← 左打ちでヘソをねらおう", x, y + 80, 0xFFFFFF);
            }
        }
        // ハンドル
        g.drawString(font, "ハンドル（ホイール・←→）", x, top + 128, 0xFFFFFF);
        int gx = left + 222, gy = top + 140, gw = 118;
        g.fill(gx, gy, gx + gw, gy + 8, 0xFF1A1A1A);
        int split = (int) (gw * 0.47);
        g.fill(gx + 1, gy + 1, gx + split, gy + 7, 0xFF3A6AA0);
        g.fill(gx + split, gy + 1, gx + gw - 1, gy + 7, 0xFFA03A3A);
        int hx = gx + strength * gw / 1000;
        g.fill(hx - 2, gy - 2, hx + 2, gy + 10, 0xFFF0C040);
        g.drawString(font, "§7左打ち", gx, gy + 12, 0xFFFFFF);
        g.drawString(font, "§7右打ち", gx + gw - 30, gy + 12, 0xFFFFFF);
        super.render(g, mx, my, pt);
    }

    private void drawPocket(GuiGraphics g, int k, int col, String label) {
        double[] p = Board.POCKET[k];
        int x0 = bx + (int) p[0], x1 = bx + (int) Math.ceil(p[1]), y = by + (int) p[2];
        g.fill(x0 - 1, y - 1, x1 + 1, y + 4, 0xFF1A1A1A);
        g.fill(x0, y, x1, y + 3, col);
    }

    private void drawLcd(GuiGraphics g, int x, int y, int w, int h, float pt) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFFE6B836);
        g.fill(x, y, x + w, y + h, 0xFF0A0A20);
        if (state == null) return;
        int d = state.digits();
        int[] dig = {d / 100 % 10, d / 10 % 10, d % 10};
        float total = Math.max(1, state.spinTotal());
        float prog = state.spin() > 0 || localSpin > 0 ? 1 - Math.max(0, localSpin - pt) / total : 1;
        // 左 → 右 → 真ん中 の順に止まる
        float[] stopAt = {0.35F, 0.55F, 1.0F};
        int[] order = {0, 2, 1};
        for (int k = 0; k < 3; k++) {
            int idx = order[k];
            boolean stopped = prog >= stopAt[k];
            int v = stopped ? dig[idx] : 1 + (int) ((ticks + pt) * (k == 2 && state.reach() && prog > 0.55F ? 0.3F : 1.2F) + idx * 3) % 9;
            int cx = x + 12 + idx * 24, cy = y + 18;
            boolean odd = v % 2 == 1;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().scale(2.4F, 2.4F, 1);
            g.drawCenteredString(font, (odd ? "§c" : "§b") + "§l" + v, 0, 0, 0xFFFFFF);
            g.pose().popPose();
        }
        if (state.reach() && prog > 0.55F && prog < 1) g.drawCenteredString(font, (ticks / 4) % 2 == 0 ? "§e§lリーチ！" : "§6§lリーチ！", x + w / 2, y + 4, 0xFFFFFF);
        // 保留
        for (int i = 0; i < 4; i++) {
            int col = i < state.holds() ? 0xFFF0F0F0 : 0xFF303040;
            g.fill(x + 14 + i * 12, y + h - 9, x + 22 + i * 12, y + h - 3, col);
        }
        if (state.round() > 0) {
            g.fill(x, y + 40, x + w, y + 52, 0xC0000000);
            g.drawCenteredString(font, "§c§lROUND " + Math.min(8, state.round()), x + w / 2, y + 42, 0xFFFFFF);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
