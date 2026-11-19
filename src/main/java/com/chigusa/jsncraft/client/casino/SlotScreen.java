package com.chigusa.jsncraft.client.casino;

import com.chigusa.jsncraft.casino.CasinoPayloads;
import com.chigusa.jsncraft.casino.Slots;
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

/** スロットマシン：3つのリール。結果はサーバーで決まり、画面は止まる演出だけ。 */
public class SlotScreen extends Screen {
    private static final int W = 260, H = 176, CELL = 26;
    /** 回っているときに流れる絵柄の並び */
    private static final int[] STRIP = {6, 5, 3, 6, 4, 1, 6, 5, 2, 4, 6, 3, 0, 5, 6, 4, 2, 6, 3, 5, 1, 4};
    private static final ItemStack[] ICONS = {ItemStack.EMPTY, new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD),
            new ItemStack(Items.GOLDEN_APPLE), new ItemStack(Items.MELON_SLICE), new ItemStack(Items.SWEET_BERRIES), new ItemStack(Items.BONE)};

    private final BlockPos pos;
    private int left, top, betIndex = 0;
    private Button spinButton, betButton;
    /** リールの位置（絵柄いくつ分流れたか）、止まる時刻、止まった絵柄 */
    private final float[] reel = new float[3];
    private final int[] stopAt = {-1, -1, -1};
    private final int[] result = {6, 6, 6};
    private final boolean[] stopped = {true, true, true};
    private boolean spinning;
    private int ticks, resultTick = -1;
    private long lastPayout = -1;
    private String message = "";

    public SlotScreen(BlockPos pos) {
        super(Component.literal("スロットマシン"));
        this.pos = pos;
        result[0] = 0; result[1] = 0; result[2] = 0;   // 最初は 7 7 7 を見せておく
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        betButton = addRenderableWidget(Button.builder(betLabel(), b -> {
            if (spinning) return;
            betIndex = (betIndex + 1) % Slots.BETS.length;
            b.setMessage(betLabel());
        }).bounds(left + 16, top + H - 30, 80, 20).build());
        spinButton = addRenderableWidget(Button.builder(Component.literal("§l回す！"), b -> spin())
                .bounds(left + 102, top + H - 30, 70, 20).build());
    }

    private Component betLabel() {
        return Component.literal("かけ：§e" + Slots.BETS[betIndex] + "枚");
    }

    private void spin() {
        if (spinning) return;
        int bet = Slots.BETS[betIndex];
        if (CasinoClient.chips < bet) {
            message = "§cチップが足りないよ（両替機でチップを買ってね）";
            return;
        }
        spinning = true;
        resultTick = -1;
        lastPayout = -1;
        message = "";
        for (int i = 0; i < 3; i++) {
            stopped[i] = false;
            stopAt[i] = -1;
        }
        CasinoClient.chips -= bet;
        CasinoUi.sound(SoundEvents.LEVER_CLICK, 1.0F);
        PacketDistributor.sendToServer(new CasinoPayloads.Spin(pos, bet));
    }

    void onResult(CasinoPayloads.SpinResult p) {
        if (!p.ok()) {
            spinning = false;
            for (int i = 0; i < 3; i++) stopped[i] = true;
            CasinoClient.chips = p.chips();
            message = "§cチップが足りないよ";
            return;
        }
        result[0] = p.a(); result[1] = p.b(); result[2] = p.c();
        lastPayout = p.payout();
        CasinoClient.chips = p.chips() - p.payout();   // 当たりの分は止まってから足す
        pendingChips = p.chips();
        resultTick = ticks;
        for (int i = 0; i < 3; i++) stopAt[i] = ticks + 10 + i * 8;
    }

    private long pendingChips;

    @Override
    public void tick() {
        ticks++;
        for (int i = 0; i < 3; i++) {
            if (stopped[i]) continue;
            reel[i] += 0.55F + i * 0.05F;
            if (stopAt[i] >= 0 && ticks >= stopAt[i]) {
                stopped[i] = true;
                CasinoUi.sound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.2F + i * 0.1F);
                if (i == 2) finish();
            }
        }
    }

    private void finish() {
        spinning = false;
        CasinoClient.chips = pendingChips;
        int mult = Slots.multiplier(result[0], result[1], result[2]);
        if (lastPayout > 0) {
            if (mult >= 100) {
                message = "§6§l大当たり！ §e+" + CasinoUi.fmt(lastPayout) + " 枚";
                CasinoUi.sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
            } else if (mult >= 10) {
                message = "§a§lあたり！ §e+" + CasinoUi.fmt(lastPayout) + " 枚";
                CasinoUi.sound(SoundEvents.PLAYER_LEVELUP, 1.2F);
            } else {
                message = "§a+" + CasinoUi.fmt(lastPayout) + " 枚";
                CasinoUi.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F);
            }
        } else {
            message = "§7はずれ…";
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        CasinoUi.panel(g, left, top, W, H);
        g.drawCenteredString(font, "§6§l★ ラッキースロット ★", left + 90, top + 10, 0xFFFFFF);

        // リールの窓
        int rx = left + 16, ry = top + 26, rw = CELL * 3 + 16, rh = CELL * 3;
        g.fill(rx - 3, ry - 3, rx + rw + 3, ry + rh + 3, 0xFF2A1A0A);
        for (int i = 0; i < 3; i++) {
            int x = rx + i * (CELL + 8);
            g.fill(x, ry, x + CELL, ry + rh, 0xFFF4EEDC);
            g.enableScissor(x, ry, x + CELL, ry + rh);
            if (stopped[i]) {
                drawSymbol(g, neighbor(result[i], -1), x, ry);
                drawSymbol(g, result[i], x, ry + CELL);
                drawSymbol(g, neighbor(result[i], 1), x, ry + CELL * 2);
            } else {
                float p = reel[i] + (spinning ? pt * 0.55F : 0);
                int base = Mth.floor(p);
                float frac = p - base;
                for (int k = -1; k < 4; k++) {
                    int sym = STRIP[Math.floorMod(base - k, STRIP.length)];
                    drawSymbol(g, sym, x, (int) (ry + (k + frac - 1) * CELL + CELL));
                }
            }
            g.disableScissor();
        }
        // 当たりの線
        g.fill(rx - 6, ry + CELL + CELL / 2, rx - 1, ry + CELL + CELL / 2 + 2, 0xFFFF3030);
        g.fill(rx + rw + 1, ry + CELL + CELL / 2, rx + rw + 6, ry + CELL + CELL / 2 + 2, 0xFFFF3030);

        // 持ちチップとメッセージ
        g.drawString(font, "チップ：§e" + CasinoUi.fmt(CasinoClient.chips) + " 枚", left + 16, top + 112, 0xFFFFFF);
        g.drawString(font, message, left + 16, top + 126, 0xFFFFFF);

        // 配当表
        int tx = left + 184, ty = top + 24;
        g.drawString(font, "§6配当（かけ×）", tx, ty, 0xFFFFFF);
        for (int s = 0; s <= 5; s++) {
            int y = ty + 12 + s * 15;
            for (int k = 0; k < 3; k++) drawSmall(g, s, tx + k * 11, y);
            g.drawString(font, "§f" + Slots.THREE[s], tx + 36, y + 2, 0xFFFFFF);
        }
        int y = ty + 12 + 6 * 15;
        g.drawString(font, "§7宝石バラバラ §f" + Slots.MIXED_GEMS, tx, y, 0xFFFFFF);
        g.drawString(font, "§7ベリー2つ §f3", tx, y + 10, 0xFFFFFF);
        g.drawString(font, "§7ベリー1つ §f1", tx, y + 20, 0xFFFFFF);

        spinButton.active = !spinning;
        betButton.active = !spinning;
        super.render(g, mx, my, pt);
    }

    private static int neighbor(int sym, int d) {
        for (int i = 0; i < STRIP.length; i++) if (STRIP[i] == sym) return STRIP[Math.floorMod(i + d, STRIP.length)];
        return 6;
    }

    private void drawSymbol(GuiGraphics g, int sym, int x, int y) {
        if (sym == 0) {
            g.pose().pushPose();
            g.pose().translate(x + CELL / 2F, y + 4, 0);
            g.pose().scale(2.2F, 2.2F, 1);
            g.drawCenteredString(font, "§c§l7", 0, 0, 0xFFFFFF);
            g.pose().popPose();
        } else {
            g.pose().pushPose();
            g.pose().translate(x + 1, y + 1, 0);
            g.pose().scale(1.5F, 1.5F, 1);
            g.renderItem(ICONS[sym], 0, 0);
            g.pose().popPose();
        }
    }

    private void drawSmall(GuiGraphics g, int sym, int x, int y) {
        if (sym == 0) {
            g.drawString(font, "§c§l7", x + 2, y + 2, 0xFFFFFF);
        } else {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(0.7F, 0.7F, 1);
            g.renderItem(ICONS[sym], 0, 0);
            g.pose().popPose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
