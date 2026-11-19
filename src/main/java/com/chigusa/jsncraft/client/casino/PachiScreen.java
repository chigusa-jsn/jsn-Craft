package com.chigusa.jsncraft.client.casino;

import com.chigusa.jsncraft.casino.pachi.Machine;
import com.chigusa.jsncraft.casino.pachi.PachiPayloads;
import com.chigusa.jsncraft.casino.pachi.Reels;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * パチスロの画面。レバー（スペース）で回して、ストップ（Z・X・C）で左・中・右を止める。
 * 止まる位置はサーバーが決める（押した所から最大4コマすべる）。
 */
public class PachiScreen extends Screen {
    private static final int W = 300, H = 214, CW = 34, CH = 26;
    private static final float SPEED = 0.8F;   // 1ティックに流れるコマ数
    private static final ItemStack[] ICON = {ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.BELL), new ItemStack(Items.CHORUS_FRUIT),
            new ItemStack(Items.SWEET_BERRIES), ItemStack.EMPTY, new ItemStack(Items.MELON_SLICE)};
    private static final int[][] LINE_COLOR = {{0xFFFF4040}, {0xFF40C0FF}, {0xFF40FF60}, {0xFFFFE040}, {0xFFFF80FF}};

    private final BlockPos pos;
    private final Machine machine;
    private int left, top, bet = 3, ticks;
    private PachiPayloads.State state;
    /** リールの位置（流れたコマ数）。中段のコマ ＝ floorMod(-floor(pos), 21) */
    private final float[] reelPos = new float[3];
    private final float[] target = {-1, -1, -1};
    private final boolean[] spinning = new boolean[3], waiting = new boolean[3];
    /** 押したときの位置（整数）と、中段にあったコマ */
    private final int[] pressBase = new int[3], pressedIndex = new int[3];
    private int lampOnTick = -1, flashTicks, winLines;
    private String message = "";
    private Button[] stopButtons = new Button[3];
    private Button leverButton;

    public PachiScreen(BlockPos pos, Machine machine) {
        super(Component.literal("パチスロ"));
        this.pos = pos;
        this.machine = machine;
        for (int i = 0; i < 3; i++) reelPos[i] = 3 + i * 5;
    }

    private boolean crown() {
        return machine == Machine.CROWN;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int rx = left + 16, by = top + 150;
        for (int i = 0; i < 3; i++) {
            int reel = i;
            stopButtons[i] = addRenderableWidget(Button.builder(Component.literal(new String[]{"Z", "X", "C"}[i]), b -> stop(reel))
                    .bounds(rx + i * (CW + 6) + 4, by, CW - 8, 16).build());
        }
        addRenderableWidget(Button.builder(Component.literal("1枚"), b -> bet = 1).bounds(left + 16, top + H - 28, 34, 16).build());
        addRenderableWidget(Button.builder(Component.literal("MAX"), b -> bet = 3).bounds(left + 54, top + H - 28, 34, 16).build());
        leverButton = addRenderableWidget(Button.builder(Component.literal("§lレバー（Space）"), b -> lever())
                .bounds(left + 92, top + H - 28, 92, 16).build());
    }

    void onState(PachiPayloads.State s) {
        PachiPayloads.State old = state;
        state = s;
        switch (s.event()) {
            case 1 -> {   // レバーOK：回し始める
                for (int i = 0; i < 3; i++) {
                    spinning[i] = true;
                    waiting[i] = false;
                    target[i] = -1;
                }
                winLines = 0;
                message = "";
                CasinoUi.sound(SoundEvents.NOTE_BLOCK_HAT.value(), 1.6F);
            }
            case 3 -> message = "§cチップが足りないよ";
            default -> {}
        }
        // 止まったリールの位置
        for (int i = 0; i < 3; i++) {
            int st = s.stop(i);
            if (st >= 0 && spinning[i] && waiting[i]) {
                // 押した位置から何コマすべったか（0〜4）
                int k = Math.min(Reels.SLIP, Math.floorMod(pressedIndex[i] - st, Reels.SIZE));
                target[i] = pressBase[i] + k;
                if (target[i] < reelPos[i]) reelPos[i] = target[i];
            } else if (st >= 0 && !spinning[i] && s.event() == 0) {
                setCenter(i, st);
            }
        }
        if (s.event() == 0 && !s.inGame()) {
            for (int i = 0; i < 3; i++) if (s.stop(i) >= 0) setCenter(i, s.stop(i));
        }
        if (s.event() == 0 && s.inGame()) {
            // 回したまま閉じていた：止まっていないリールはまた回す
            for (int i = 0; i < 3; i++) {
                if (s.stop(i) < 0) spinning[i] = true;
                else setCenter(i, s.stop(i));
            }
        }
        boolean lampWasOff = old == null || old.lamp() == 0;
        if (s.lamp() > 0 && lampWasOff && old != null) {
            lampOnTick = ticks;
            CasinoUi.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F);
            CasinoUi.sound(SoundEvents.NOTE_BLOCK_BELL.value(), 2.0F);
        }
        if (s.event() == 2 || s.event() == 4 || s.event() == 5) {
            if (!s.inGame()) pendingResult = s;
        }
    }

    private PachiPayloads.State pendingResult;

    private void setCenter(int reel, int index) {
        reelPos[reel] = Math.floorMod(-index, Reels.SIZE);
        target[reel] = -1;
        spinning[reel] = false;
    }

    private int center(int reel) {
        return Math.floorMod(-Mth.floor(reelPos[reel]), Reels.SIZE);
    }

    private boolean anySpinning() {
        return spinning[0] || spinning[1] || spinning[2];
    }

    private void lever() {
        if (state == null || state.inGame() || anySpinning()) return;
        boolean free = state.replay();
        if (!free && CasinoClient.chips < (state.bonus() > 0 ? 3 : bet)) {
            message = "§cチップが足りないよ";
            return;
        }
        CasinoUi.sound(SoundEvents.LEVER_CLICK, 0.8F);
        PacketDistributor.sendToServer(new PachiPayloads.Lever(pos, bet));
    }

    private void stop(int reel) {
        if (!spinning[reel] || waiting[reel] || state == null || !state.inGame()) return;
        waiting[reel] = true;
        pressBase[reel] = Mth.floor(reelPos[reel]);
        pressedIndex[reel] = center(reel);
        CasinoUi.sound(SoundEvents.STONE_BUTTON_CLICK_ON, 1.4F);
        PacketDistributor.sendToServer(new PachiPayloads.Stop(pos, reel, pressedIndex[reel]));
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_DOWN -> { lever(); return true; }
            case GLFW.GLFW_KEY_Z -> { stop(0); return true; }
            case GLFW.GLFW_KEY_X -> { stop(1); return true; }
            case GLFW.GLFW_KEY_C -> { stop(2); return true; }
            case GLFW.GLFW_KEY_1 -> { bet = 1; return true; }
            case GLFW.GLFW_KEY_2 -> { bet = 2; return true; }
            case GLFW.GLFW_KEY_3 -> { bet = 3; return true; }
            default -> { return super.keyPressed(key, scan, mods); }
        }
    }

    @Override
    public void tick() {
        ticks++;
        for (int i = 0; i < 3; i++) {
            if (!spinning[i]) continue;
            if (target[i] >= 0) {
                reelPos[i] = Math.min(target[i], reelPos[i] + SPEED);
                if (reelPos[i] >= target[i]) {
                    reelPos[i] = target[i];
                    spinning[i] = false;
                    waiting[i] = false;
                    target[i] = -1;
                    CasinoUi.sound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.0F + i * 0.1F);
                }
            } else if (waiting[i]) {
                // 返事を待つあいだは、すべれる所（4コマ）までゆっくり進む
                reelPos[i] = Math.min(pressBase[i] + Reels.SLIP, reelPos[i] + SPEED * 0.5F);
            } else {
                reelPos[i] += SPEED;
            }
        }
        if (pendingResult != null && !anySpinning()) {
            showResult(pendingResult);
            pendingResult = null;
        }
        if (flashTicks > 0) flashTicks--;
    }

    private void showResult(PachiPayloads.State s) {
        winLines = s.wins();
        if (s.event() == 4) {
            message = s.bonus() == 1 ? "§6§lBIG BONUS！" : "§b§lREGULAR BONUS！";
            flashTicks = 60;
            CasinoUi.sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
        } else if (s.event() == 5) {
            message = "§eボーナス終了！";
            CasinoUi.sound(SoundEvents.PLAYER_LEVELUP, 1.0F);
        } else if (s.payout() > 0) {
            message = "§a+" + s.payout() + " 枚";
            CasinoUi.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F + Math.min(1, s.payout() / 15F));
        } else if (s.replay()) {
            message = "§bリプレイ（次は無料）";
            CasinoUi.sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.2F);
        } else {
            message = "";
        }
    }

    // ---------------- 描く ----------------
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        int frame = crown() ? 0xFF1A2A6A : 0xFF0A5A4A;
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF0A0A10);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, CasinoUi.GOLD);
        g.fill(left, top, left + W, top + H, frame);
        g.fill(left + 4, top + 4, left + W - 4, top + H - 4, crown() ? 0xFF22348A : 0xFF0E7A62);

        // 看板
        String title = crown() ? "§e§l♛ ピカッとクラウン ♛" : "§f§l☀ トロピカル・ドキドキ ☀";
        g.drawCenteredString(font, title, left + 116, top + 10, 0xFFFFFF);
        if (!crown()) drawTropicalLamps(g, pt);

        // リール
        int rx = left + 16, ry = top + 34, rw = CW * 3 + 12, rh = CH * 3;
        g.fill(rx - 4, ry - 4, rx + rw + 4, ry + rh + 4, 0xFF101010);
        for (int i = 0; i < 3; i++) {
            int x = rx + i * (CW + 6);
            g.fill(x, ry, x + CW, ry + rh, 0xFFF8F4E8);
            g.enableScissor(x, ry, x + CW, ry + rh);
            float p = reelPos[i];
            if (spinning[i]) {
                if (target[i] >= 0) p += Math.min(SPEED * pt, Math.max(0, target[i] - reelPos[i]));
                else if (waiting[i]) p = Math.min(pressBase[i] + Reels.SLIP, p + SPEED * 0.5F * pt);
                else p += SPEED * pt;
            }
            int base = Mth.floor(p);
            float frac = p - base;
            int c = Math.floorMod(-base, Reels.SIZE);
            for (int k = -2; k <= 2; k++) {
                int sym = Reels.at(i, c + k);
                drawSymbol(g, sym, x, (int) (ry + CH + k * CH + frac * CH));
            }
            g.disableScissor();
            // 回っているときは少し暗くして速さを出す
            if (spinning[i]) g.fill(x, ry, x + CW, ry + rh, 0x22000000);
        }
        // ライン
        int lines = Reels.lines(state != null && state.bonus() > 0 ? 3 : bet);
        for (int l = 0; l < lines; l++) {
            boolean hit = (winLines & (1 << l)) != 0 && (ticks / 4) % 2 == 0;
            int col = LINE_COLOR[l][0];
            int ly0 = ry + CH * Reels.LINES[l][0] + CH / 2, ly2 = ry + CH * Reels.LINES[l][2] + CH / 2;
            g.fill(rx - 9, ly0 - 1, rx - 5, ly0 + 1, col);
            g.fill(rx + rw + 5, ly2 - 1, rx + rw + 9, ly2 + 1, col);
            if (hit) drawLine(g, rx, ly0, rx + rw, ly2, col);
        }

        // クラウンのランプ（リールの左下）
        if (crown()) drawCrownLamp(g, left + 16, top + 120, pt);

        // 情報
        int ix = left + 160, iy = top + 34;
        g.fill(ix - 4, iy - 4, left + W - 12, iy + 104, 0xAA000000);
        g.drawString(font, "§7チップ", ix, iy, 0xFFFFFF);
        g.drawString(font, "§e" + CasinoUi.fmt(CasinoClient.chips), ix + 50, iy, 0xFFFFFF);
        g.drawString(font, "§7かけ", ix, iy + 12, 0xFFFFFF);
        g.drawString(font, state != null && state.replay() ? "§bリプレイ" : "§f" + (state != null && state.bonus() > 0 ? 3 : bet) + "枚（" + lines + "ライン）", ix + 50, iy + 12, 0xFFFFFF);
        if (state != null) {
            g.drawString(font, "§7ゲーム数", ix, iy + 28, 0xFFFFFF);
            g.drawString(font, "§f" + state.games(), ix + 50, iy + 28, 0xFFFFFF);
            g.drawString(font, "§6BIG §f" + state.big() + "  §bREG §f" + state.reg(), ix, iy + 40, 0xFFFFFF);
            if (state.bonus() > 0) {
                boolean blink = (ticks / 6) % 2 == 0;
                g.drawString(font, (state.bonus() == 1 ? "§6§lBIG" : "§b§lREG") + (blink ? " §e★" : ""), ix, iy + 58, 0xFFFFFF);
                g.drawString(font, "§f" + state.bonusOut() + " / " + state.bonusTarget() + " 枚", ix, iy + 70, 0xFFFFFF);
            }
        }
        g.drawString(font, message, ix, iy + 88, 0xFFFFFF);
        g.drawString(font, "§8Space：レバー  Z/X/C：ストップ  1〜3：かけ枚数", left + 16, top + H - 44, 0xFFFFFF);

        boolean canLever = state != null && !state.inGame() && !anySpinning();
        leverButton.active = canLever;
        for (int i = 0; i < 3; i++) stopButtons[i].active = spinning[i] && !waiting[i] && state != null && state.inGame();
        if (flashTicks > 0 && (flashTicks / 3) % 2 == 0) g.fill(left, top, left + W, top + H, 0x30FFFF80);
        super.render(g, mx, my, pt);
    }

    private void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        int n = Math.max(1, Math.abs(x1 - x0));
        for (int i = 0; i <= n; i += 2) {
            int x = x0 + (x1 - x0) * i / n, y = y0 + (y1 - y0) * i / n;
            g.fill(x, y - 1, x + 2, y + 1, col);
        }
    }

    private void drawCrownLamp(GuiGraphics g, int x, int y, float pt) {
        boolean on = state != null && state.lamp() > 0;
        int w = 52, h = 22;
        g.fill(x, y, x + w, y + h, 0xFF101018);
        int body = on ? lampColor(state.lamp(), pt) : 0xFF3A3A48;
        // 王冠の形
        g.fill(x + 4, y + 10, x + w - 4, y + h - 3, body);
        for (int i = 0; i < 3; i++) {
            int cx = x + 8 + i * 18;
            g.fill(cx, y + 4, cx + 4, y + 10, body);
        }
        if (on) {
            g.drawCenteredString(font, "§l§fピカッ", x + w / 2, y + 11, 0xFFFFFF);
            if (ticks - lampOnTick < 30 && (ticks / 2) % 2 == 0) g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0x40FFFFFF);
        }
    }

    private void drawTropicalLamps(GuiGraphics g, float pt) {
        boolean on = state != null && state.lamp() > 0;
        int y = top + 22;
        for (int i = 0; i < 5; i++) {
            int x = left + 52 + i * 24;
            int col = 0xFF2A3A30;
            if (on) {
                int phase = (ticks / 3 + i) % 5;
                col = state.lamp() == 4 ? rainbow(i * 50 + ticks * 12) : phase < 3 ? lampColor(state.lamp(), pt) : 0xFF603030;
            }
            g.fill(x, y, x + 18, y + 8, 0xFF101010);
            g.fill(x + 1, y + 1, x + 17, y + 7, col);
        }
    }

    private int lampColor(int kind, float pt) {
        return switch (kind) {
            case 1 -> 0xFFFF3030;
            case 2 -> 0xFF30A0FF;
            case 3 -> 0xFF40FF60;
            default -> rainbow(ticks * 12);
        };
    }

    private static int rainbow(int deg) {
        float h = (deg % 360) / 360F;
        return 0xFF000000 | java.awt.Color.HSBtoRGB(h, 0.8F, 1F) & 0xFFFFFF;
    }

    private void drawSymbol(GuiGraphics g, int sym, int x, int y) {
        int cx = x + CW / 2;
        switch (sym) {
            case Reels.SEVEN -> {
                g.pose().pushPose();
                g.pose().translate(cx, y + 5, 0);
                g.pose().scale(2.2F, 2.2F, 1);
                g.drawCenteredString(font, "§4§l7", 1, 1, 0xFFFFFF);
                g.drawCenteredString(font, "§c§l7", 0, 0, 0xFFFFFF);
                g.pose().popPose();
            }
            case Reels.BAR -> {
                g.fill(x + 4, y + 7, x + CW - 4, y + CH - 7, 0xFF101010);
                g.drawCenteredString(font, "§f§lBAR", cx, y + 9, 0xFFFFFF);
            }
            case Reels.REPLAY -> {
                g.fill(x + 5, y + 6, x + CW - 5, y + CH - 6, 0xFF2A6AE0);
                g.drawCenteredString(font, "§f§lRE", cx, y + 9, 0xFFFFFF);
            }
            default -> {
                g.pose().pushPose();
                g.pose().translate(cx - 11, y + 2, 0);
                g.pose().scale(1.375F, 1.375F, 1);
                g.renderItem(ICON[sym], 0, 0);
                g.pose().popPose();
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
