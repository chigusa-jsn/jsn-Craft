package com.chigusa.jsncraft.client.casino;

import com.chigusa.jsncraft.casino.CasinoPayloads;
import com.chigusa.jsncraft.casino.Chinchiro;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** チンチロリン：親が振ったあと、子（あなた）が3回まで振る */
public class ChinchiroScreen extends Screen {
    private static final int W = 280, H = 210, ROLL_TICKS = 14;
    private final BlockPos pos;
    private int left, top, betIndex = 0, ticks, rollStart = -100;
    private CasinoPayloads.ChinchiroState state, shown;
    private String message = "";
    private Button startButton, rollButton, betButton;
    private final RandomSource rnd = RandomSource.create();
    /** 親の分を見せ終わったか（親が何回も振るときは順番に見せる） */
    private int dealerShown = 0, playerShown = 0;
    private boolean pendingNew;

    public ChinchiroScreen(BlockPos pos) {
        super(Component.literal("チンチロリン"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int y = top + H - 26;
        betButton = addRenderableWidget(Button.builder(betLabel(), b -> {
            betIndex = (betIndex + 1) % Chinchiro.BETS.length;
            b.setMessage(betLabel());
        }).bounds(left + 12, y, 70, 18).build());
        startButton = addRenderableWidget(Button.builder(Component.literal("§l勝負！"), b -> start()).bounds(left + 88, y, 60, 18).build());
        rollButton = addRenderableWidget(Button.builder(Component.literal("§lさいころを振る"), b -> roll()).bounds(left + 154, y, 114, 18).build());
    }

    private Component betLabel() {
        return Component.literal("かけ：§e" + Chinchiro.BETS[betIndex]);
    }

    private boolean animating() {
        return ticks - rollStart < ROLL_TICKS;
    }

    private void start() {
        if (state != null && state.playerTurn()) return;
        int bet = Chinchiro.BETS[betIndex];
        if (CasinoClient.chips < (long) bet * Chinchiro.DEPOSIT) {
            message = "§cかけの5倍のチップが必要だよ";
            return;
        }
        message = "";
        pendingNew = true;
        PacketDistributor.sendToServer(new CasinoPayloads.ChinchiroAction(pos, false, bet));
    }

    private void roll() {
        if (state == null || !state.playerTurn() || animating()) return;
        PacketDistributor.sendToServer(new CasinoPayloads.ChinchiroAction(pos, true, 0));
    }

    void onState(CasinoPayloads.ChinchiroState s) {
        if (s.noChips()) {
            message = "§cかけの5倍のチップが必要だよ";
            CasinoClient.chips = s.chips();
            return;
        }
        if (pendingNew || state == null) {
            dealerShown = 0;
            playerShown = 0;
            pendingNew = false;
        }
        state = s;
        rollStart = ticks;
        CasinoUi.sound(SoundEvents.BAMBOO_HIT, 1.2F);
    }

    @Override
    public void tick() {
        ticks++;
        if (state == null) return;
        if (!animating()) {
            int dRolls = state.dealer().size() / 3, pRolls = state.player().size() / 3;
            if (dealerShown < dRolls) {
                dealerShown++;
                if (dealerShown < dRolls) rollStart = ticks;   // 親がもう一回振る
                CasinoUi.sound(SoundEvents.BAMBOO_WOOD_HIT, 1.6F);
            } else if (playerShown < pRolls) {
                playerShown++;
                CasinoUi.sound(SoundEvents.BAMBOO_WOOD_HIT, 1.6F);
            }
            if (dealerShown == dRolls && playerShown == pRolls && shown != state) {
                shown = state;
                CasinoClient.chips = state.chips();
                if (state.done()) {
                    int r = state.result();
                    if (r > 0) {
                        message = "§a§l勝ち！ §e+" + CasinoUi.fmt((long) state.bet() * r) + " 枚";
                        CasinoUi.sound(r >= 3 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, 1.2F);
                    } else if (r == 0) {
                        message = "§e引き分け";
                    } else {
                        message = "§c負け… §7-" + CasinoUi.fmt((long) state.bet() * -r) + " 枚";
                        CasinoUi.sound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F);
                    }
                } else if (state.playerTurn()) {
                    message = pRolls == 0 ? "§fあなたの番！（3回まで振れる）" : "§7目なし… もう一回！（あと" + (3 - pRolls) + "回）";
                }
            }
        }
    }

    private void drawDice(GuiGraphics g, List<Integer> dice, int shownRolls, boolean rolling, int x, int y) {
        int rolls = dice.size() / 3;
        int idx = Math.min(shownRolls, rolls) - 1;
        for (int i = 0; i < 3; i++) {
            int v;
            if (rolling) v = 1 + rnd.nextInt(6);
            else if (idx >= 0) v = dice.get(idx * 3 + i);
            else v = 0;
            drawDie(g, x + i * 30, y + (rolling ? rnd.nextInt(3) : 0), v);
        }
    }

    private void drawDie(GuiGraphics g, int x, int y, int v) {
        g.fill(x - 1, y - 1, x + 25, y + 25, 0xFF1A1A1A);
        g.fill(x, y, x + 24, y + 24, v == 0 ? 0xFF6A6A6A : 0xFFF8F4EA);
        if (v == 0) return;
        int col = v == 1 ? 0xFFD01818 : 0xFF1A1A1A;
        int s = v == 1 ? 6 : 4;
        int[][] pips = switch (v) {
            case 1 -> new int[][]{{12, 12}};
            case 2 -> new int[][]{{6, 6}, {18, 18}};
            case 3 -> new int[][]{{6, 6}, {12, 12}, {18, 18}};
            case 4 -> new int[][]{{6, 6}, {18, 6}, {6, 18}, {18, 18}};
            case 5 -> new int[][]{{6, 6}, {18, 6}, {12, 12}, {6, 18}, {18, 18}};
            default -> new int[][]{{6, 5}, {18, 5}, {6, 12}, {18, 12}, {6, 19}, {18, 19}};
        };
        for (int[] p : pips) g.fill(x + p[0] - s / 2, y + p[1] - s / 2, x + p[0] + (s + 1) / 2, y + p[1] + (s + 1) / 2, col);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        CasinoUi.panel(g, left, top, W, H);
        g.drawCenteredString(font, "§6§l⚀ チンチロリン ⚅", left + W / 2, top + 10, 0xFFFFFF);
        g.drawCenteredString(font, "§7ピンゾロ5倍・ゾロ目3倍・シゴロ2倍・ヒフミは2倍払い", left + W / 2, top + 22, 0xFFFFFF);
        // どんぶり
        g.fill(left + 60, top + 36, left + W - 60, top + 150, 0xFF2A1A10);
        g.fill(left + 64, top + 40, left + W - 64, top + 146, 0xFFE8E0D0);
        if (state != null) {
            int dRolls = state.dealer().size() / 3, pRolls = state.player().size() / 3;
            boolean dRolling = animating() && dealerShown < dRolls;
            boolean pRolling = animating() && dealerShown >= dRolls && playerShown < pRolls;
            g.drawString(font, "§0親", left + 72, top + 46, 0xFFFFFF, false);
            drawDice(g, state.dealer(), dealerShown, dRolling, left + 96, top + 46);
            if (dealerShown >= dRolls && dRolls > 0) g.drawString(font, "§4" + Chinchiro.name(state.dealerHand()), left + 194, top + 54, 0xFFFFFF, false);
            g.drawString(font, "§0あなた", left + 68, top + 100, 0xFFFFFF, false);
            drawDice(g, state.player(), playerShown, pRolling, left + 96, top + 96);
            if (playerShown >= pRolls && pRolls > 0) g.drawString(font, "§1" + Chinchiro.name(state.playerHand()), left + 194, top + 104, 0xFFFFFF, false);
        } else {
            g.drawCenteredString(font, "§0かけを選んで「勝負！」", left + W / 2, top + 88, 0xFFFFFF);
        }
        g.drawString(font, "チップ：§e" + CasinoUi.fmt(CasinoClient.chips), left + 12, top + 160, 0xFFFFFF);
        g.drawString(font, message, left + 12, top + 172, 0xFFFFFF);
        boolean myTurn = state != null && state.playerTurn() && shown == state && !animating();
        startButton.active = (state == null || !state.playerTurn()) && !animating();
        betButton.active = startButton.active;
        rollButton.active = myTurn;
        super.render(g, mx, my, pt);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
