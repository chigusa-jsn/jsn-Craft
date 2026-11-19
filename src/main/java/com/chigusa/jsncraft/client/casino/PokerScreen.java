package com.chigusa.jsncraft.client.casino;

import com.chigusa.jsncraft.casino.CasinoPayloads;
import com.chigusa.jsncraft.casino.VideoPoker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** ビデオポーカー：配る → カードをクリックして残す → 引き直し */
public class PokerScreen extends Screen {
    private static final int W = 300, H = 214, CW = 40, CHh = 56;
    private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
    private static final String[] SUITS = {"♠", "♥", "♦", "♣"};

    private final BlockPos pos;
    private int left, top, betIndex = 0, hold, ticks, flipTick = -100;
    private CasinoPayloads.PokerState state;
    private String message = "";
    private Button dealButton, betButton;

    public PokerScreen(BlockPos pos) {
        super(Component.literal("ポーカー"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        betButton = addRenderableWidget(Button.builder(betLabel(), b -> {
            betIndex = (betIndex + 1) % VideoPoker.BETS.length;
            b.setMessage(betLabel());
        }).bounds(left + 150, top + H - 26, 70, 18).build());
        dealButton = addRenderableWidget(Button.builder(Component.literal("§l配る"), b -> press()).bounds(left + 224, top + H - 26, 64, 18).build());
    }

    private Component betLabel() {
        return Component.literal("かけ：§e" + VideoPoker.BETS[betIndex]);
    }

    private boolean dealt() {
        return state != null && state.dealt();
    }

    private void press() {
        if (dealt()) {
            PacketDistributor.sendToServer(new CasinoPayloads.PokerAction(pos, true, 0, hold));
        } else {
            if (CasinoClient.chips < VideoPoker.BETS[betIndex]) {
                message = "§cチップが足りないよ";
                return;
            }
            hold = 0;
            PacketDistributor.sendToServer(new CasinoPayloads.PokerAction(pos, false, VideoPoker.BETS[betIndex], 0));
        }
        CasinoUi.sound(SoundEvents.BOOK_PAGE_TURN, 1.3F);
    }

    void onState(CasinoPayloads.PokerState s) {
        if (s.rank() < 0 && !s.dealt()) {
            message = "§cチップが足りないよ";
            return;
        }
        boolean wasDealt = dealt();
        state = s;
        flipTick = ticks;
        if (s.dealt()) {
            message = s.rank() > 0 ? "§7いまの手：§f" + VideoPoker.NAMES[s.rank()] : "§7残すカードをクリックしてね";
            if (!wasDealt) hold = 0;
        } else if (s.payout() > 0) {
            message = "§a§l" + VideoPoker.NAMES[s.rank()] + "！ §e+" + CasinoUi.fmt(s.payout()) + " 枚";
            CasinoUi.sound(s.rank() >= 7 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 1.2F);
        } else {
            message = "§7" + VideoPoker.NAMES[s.rank()] + "…";
        }
    }

    @Override
    public void tick() {
        ticks++;
    }

    private int cardX(int i) {
        return left + 22 + i * (CW + 12);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (dealt()) {
            int y = top + 60;
            for (int i = 0; i < 5; i++) {
                int x = cardX(i);
                if (mx >= x && mx < x + CW && my >= y && my < y + CHh) {
                    hold ^= 1 << i;
                    CasinoUi.sound(SoundEvents.UI_BUTTON_CLICK.value(), 1.6F);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_5 && dealt()) {
            hold ^= 1 << (key - GLFW.GLFW_KEY_1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE) {
            press();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        CasinoUi.panel(g, left, top, W, H);
        g.drawCenteredString(font, "§6§l♣ ビデオポーカー ♦", left + W / 2, top + 10, 0xFFFFFF);
        g.drawCenteredString(font, "§7ジャックス・オア・ベター（J以上のワンペアから）", left + W / 2, top + 22, 0xFFFFFF);

        int y = top + 60;
        for (int i = 0; i < 5; i++) {
            int x = cardX(i);
            int card = state == null ? -1 : state.cards()[i];
            boolean held = dealt() && (hold & (1 << i)) != 0;
            boolean hidden = card < 0 || (ticks - flipTick < 2 + i * 2 && !(dealt() && held));
            g.fill(x - 1, y - 1, x + CW + 1, y + CHh + 1, held ? 0xFFF0C040 : 0xFF1A1A1A);
            if (hidden) {
                g.fill(x, y, x + CW, y + CHh, 0xFF8A1A2A);
                g.fill(x + 3, y + 3, x + CW - 3, y + CHh - 3, 0xFFB02A3A);
                g.drawCenteredString(font, "§6✦", x + CW / 2, y + CHh / 2 - 4, 0xFFFFFF);
            } else {
                g.fill(x, y, x + CW, y + CHh, 0xFFF8F4EA);
                int suit = card / 13;
                String c = suit == 1 || suit == 2 ? "§c" : "§0";
                g.drawString(font, c + "§l" + RANKS[card % 13], x + 3, y + 3, 0xFFFFFF, false);
                g.pose().pushPose();
                g.pose().translate(x + CW / 2F, y + CHh / 2F - 6, 0);
                g.pose().scale(2F, 2F, 1);
                g.drawCenteredString(font, c + SUITS[suit], 0, 0, 0xFFFFFF);
                g.pose().popPose();
            }
            if (held) g.drawCenteredString(font, "§e§lHOLD", x + CW / 2, y + CHh + 4, 0xFFFFFF);
            else if (dealt()) g.drawCenteredString(font, "§8" + (i + 1), x + CW / 2, y + CHh + 4, 0xFFFFFF);
        }

        // 配当表
        int tx = left + 14, ty = top + 136;
        for (int k = 9; k >= 1; k--) {
            int row = 9 - k;
            int x = tx + (row / 5) * 132, yy = ty + (row % 5) * 9;
            boolean hit = state != null && state.rank() == k && (ticks / 5) % 2 == 0;
            g.pose().pushPose();
            g.pose().translate(x, yy, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, (hit ? "§e§l" : "§f") + VideoPoker.NAMES[k], 0, 0, 0xFFFFFF);
            g.drawString(font, "§6×" + VideoPoker.PAY[k], 150, 0, 0xFFFFFF);
            g.pose().popPose();
        }
        g.drawString(font, "チップ：§e" + CasinoUi.fmt(CasinoClient.chips), left + 14, top + H - 22, 0xFFFFFF);
        g.drawCenteredString(font, message, left + W / 2, top + 40, 0xFFFFFF);
        dealButton.setMessage(Component.literal(dealt() ? "§l引き直す" : "§l配る"));
        betButton.active = !dealt();
        super.render(g, mx, my, pt);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
