package com.chigusa.jsncraft.client.casino;

import com.chigusa.jsncraft.casino.Blackjack;
import com.chigusa.jsncraft.casino.CasinoPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** ブラックジャック：ディーラーと1対1 */
public class BlackjackScreen extends Screen {
    private static final int W = 300, H = 210, CW = 22, CHh = 30;
    private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
    private static final String[] SUITS = {"♠", "♥", "♦", "♣"};

    private final BlockPos pos;
    private int left, top, betIndex = 1, ticks;
    private CasinoPayloads.BjState state;
    private String message = "";
    private Button deal, hit, stand, dbl, split, betButton;

    public BlackjackScreen(BlockPos pos) {
        super(Component.literal("ブラックジャック"));
        this.pos = pos;
        state = CasinoClient.lastBj;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int y = top + H - 26;
        betButton = addRenderableWidget(Button.builder(betLabel(), b -> {
            betIndex = (betIndex + 1) % Blackjack.BETS.length;
            b.setMessage(betLabel());
        }).bounds(left + 10, y, 64, 18).build());
        deal = addRenderableWidget(Button.builder(Component.literal("§l配る"), b -> act(Blackjack.ACT_DEAL)).bounds(left + 78, y, 40, 18).build());
        hit = addRenderableWidget(Button.builder(Component.literal("ヒット"), b -> act(Blackjack.ACT_HIT)).bounds(left + 122, y, 40, 18).build());
        stand = addRenderableWidget(Button.builder(Component.literal("スタンド"), b -> act(Blackjack.ACT_STAND)).bounds(left + 166, y, 44, 18).build());
        dbl = addRenderableWidget(Button.builder(Component.literal("ダブル"), b -> act(Blackjack.ACT_DOUBLE)).bounds(left + 214, y, 38, 18).build());
        split = addRenderableWidget(Button.builder(Component.literal("分ける"), b -> act(Blackjack.ACT_SPLIT)).bounds(left + 256, y, 36, 18).build());
    }

    private Component betLabel() {
        return Component.literal("かけ：§e" + Blackjack.BETS[betIndex]);
    }

    private void act(int a) {
        if (a == Blackjack.ACT_DEAL && CasinoClient.chips < Blackjack.BETS[betIndex]) {
            message = "§cチップが足りないよ";
            return;
        }
        message = "";
        CasinoUi.sound(SoundEvents.BOOK_PAGE_TURN, 1.4F);
        PacketDistributor.sendToServer(new CasinoPayloads.BjAction(pos, a, Blackjack.BETS[betIndex]));
    }

    void onState(CasinoPayloads.BjState s) {
        boolean wasPlaying = state != null && state.playing();
        state = s;
        if (!s.playing() && !s.hands().isEmpty() && (wasPlaying || s.payout() > 0 || anyResult(s))) {
            long bet = 0;
            for (int b : s.bets()) bet += b;
            if (s.payout() > bet) {
                message = "§a§l勝ち！ §e+" + CasinoUi.fmt(s.payout()) + " 枚";
                CasinoUi.sound(s.results().contains(Blackjack.R_BJ) ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 1.2F);
            } else if (s.payout() == bet && bet > 0) {
                message = "§e引き分け（かけは戻るよ）";
                CasinoUi.sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F);
            } else if (s.payout() > 0) {
                message = "§e+" + CasinoUi.fmt(s.payout()) + " 枚";
            } else {
                message = "§7負け…";
                CasinoUi.sound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F);
            }
        }
    }

    private static boolean anyResult(CasinoPayloads.BjState s) {
        for (int r : s.results()) if (r != Blackjack.R_NONE) return true;
        return false;
    }

    @Override
    public void tick() {
        ticks++;
    }

    private void drawCard(GuiGraphics g, int card, int x, int y) {
        g.fill(x - 1, y - 1, x + CW + 1, y + CHh + 1, 0xFF1A1A1A);
        if (card < 0) {
            g.fill(x, y, x + CW, y + CHh, 0xFF8A1A2A);
            g.fill(x + 2, y + 2, x + CW - 2, y + CHh - 2, 0xFFB02A3A);
            g.drawCenteredString(font, "§6✦", x + CW / 2, y + CHh / 2 - 4, 0xFFFFFF);
            return;
        }
        g.fill(x, y, x + CW, y + CHh, 0xFFF8F4EA);
        int suit = card / 13;
        String c = suit == 1 || suit == 2 ? "§c" : "§0";
        g.drawString(font, c + RANKS[card % 13], x + 2, y + 2, 0xFFFFFF, false);
        g.drawCenteredString(font, c + SUITS[suit], x + CW / 2, y + CHh / 2 - 1, 0xFFFFFF);
    }

    private void drawHand(GuiGraphics g, List<Integer> cards, int x, int y) {
        for (int i = 0; i < cards.size(); i++) drawCard(g, cards.get(i), x + i * (CW - 8), y);
    }

    private static String label(List<Integer> cards) {
        boolean hidden = cards.contains(-1);
        if (hidden) {
            List<Integer> shown = cards.stream().filter(c -> c >= 0).toList();
            return String.valueOf(Blackjack.total(shown)) + " + ?";
        }
        int t = Blackjack.total(cards);
        return t > 21 ? "§c" + t + " バースト" : cards.size() == 2 && t == 21 ? "§6§lブラックジャック" : String.valueOf(t);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        CasinoUi.panel(g, left, top, W, H);
        g.drawCenteredString(font, "§6§l♠ ブラックジャック ♥", left + W / 2, top + 10, 0xFFFFFF);
        g.drawString(font, "§7ディーラーは17以上で止まる・BJは1.5倍", left + 12, top + 24, 0xFFFFFF);
        boolean playing = state != null && state.playing();
        if (state != null && !state.dealer().isEmpty()) {
            g.drawString(font, "§fディーラー： §e" + label(state.dealer()), left + 12, top + 38, 0xFFFFFF);
            drawHand(g, state.dealer(), left + 14, top + 50);
            for (int h = 0; h < state.hands().size(); h++) {
                int x = left + 14 + h * 140, y = top + 112;
                boolean cur = playing && h == state.current() && state.hands().size() > 1;
                if (cur) g.fill(x - 4, y - 14, x + 130, y + CHh + 6, 0x30FFFF80);
                String res = switch (state.results().get(h)) {
                    case Blackjack.R_WIN -> " §a勝ち";
                    case Blackjack.R_LOSE -> " §c負け";
                    case Blackjack.R_PUSH -> " §e引き分け";
                    case Blackjack.R_BJ -> " §6§lBJ！";
                    default -> "";
                };
                g.drawString(font, "§fあなた： §e" + label(state.hands().get(h)) + " §7（" + state.bets().get(h) + "枚）" + res, x - 2, y - 12, 0xFFFFFF);
                drawHand(g, state.hands().get(h), x, y);
            }
        } else {
            g.drawCenteredString(font, "§7かけ枚数を選んで「配る」を押してね", left + W / 2, top + 90, 0xFFFFFF);
        }
        g.drawString(font, "チップ：§e" + CasinoUi.fmt(CasinoClient.chips), left + 12, top + H - 40, 0xFFFFFF);
        g.drawString(font, message, left + 120, top + H - 40, 0xFFFFFF);

        boolean canAct = playing;
        List<Integer> cur = playing ? state.hands().get(state.current()) : List.of();
        deal.active = !playing;
        betButton.active = !playing;
        hit.active = canAct;
        stand.active = canAct;
        dbl.active = canAct && cur.size() == 2;
        split.active = canAct && state.hands().size() == 1 && cur.size() == 2
                && Math.min(10, cur.get(0) % 13 + 1) == Math.min(10, cur.get(1) % 13 + 1);
        super.render(g, mx, my, pt);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
