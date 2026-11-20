package com.chigusa.jsncraft.client.wheel;

import com.chigusa.jsncraft.wheel.WheelLayout;
import com.chigusa.jsncraft.wheel.WheelPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

/** ビッグホイールのかけ台：1・3・5・10・20 のどれかにチップをかける。みんな同じ1回にかける */
public class WheelScreen extends Screen {
    private static final int W = 280, H = 190;
    private static final int[] AMOUNTS = {1, 5, 10, 50, 100};
    private final BlockPos terminal;
    private WheelPayloads.State state;
    private int left, top, ticks, amountIdx = 1;
    private long receivedAt;
    private final Button[] slots = new Button[5];
    private Button amountBtn, clearBtn;
    private int lastPhase = -1;

    public WheelScreen(BlockPos terminal) {
        super(Component.literal("ビッグホイール"));
        this.terminal = terminal;
    }

    public static void onState(WheelPayloads.State s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof WheelScreen w && w.terminal.equals(s.terminal())) w.update(s);
        else if (mc.screen == null && System.currentTimeMillis() - closedAt > 1500) {
            WheelScreen w = new WheelScreen(s.terminal());
            mc.setScreen(w);
            w.update(s);
        }
    }

    private void update(WheelPayloads.State s) {
        if (state != null && lastPhase == 1 && s.phase() == 2) {
            long won = 0;
            int k = s.history().isEmpty() ? -1 : WheelLayout.index(s.history().get(0));
            if (k >= 0) won = state.mine().get(k) * (WheelLayout.VALUES[k] + 1);
            if (won > 0) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.7F));
        }
        lastPhase = s.phase();
        state = s;
        receivedAt = System.currentTimeMillis();
        refresh();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bw = 48;
        for (int i = 0; i < 5; i++) {
            int k = i;
            slots[i] = addRenderableWidget(Button.builder(Component.literal("×" + WheelLayout.VALUES[i]), b -> bet(k))
                    .bounds(left + 10 + i * (bw + 5), top + 118, bw, 20).build());
        }
        amountIdx = Math.min(amountIdx, AMOUNTS.length - 1);
        amountBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            amountIdx = (amountIdx + 1) % AMOUNTS.length;
            refresh();
        }).bounds(left + 10, top + 160, 120, 20).build());
        clearBtn = addRenderableWidget(Button.builder(Component.literal("かけを取り消す"), b -> send(-2, 0)).bounds(left + 140, top + 160, 130, 20).build());
        refresh();
    }

    private void refresh() {
        if (amountBtn == null) return;
        amountBtn.setMessage(Component.literal("1回にかける：§e" + AMOUNTS[amountIdx] + "枚"));
        boolean open = state != null && state.linked() && state.phase() == 0;
        for (Button b : slots) b.active = open && state.chips() >= AMOUNTS[amountIdx];
        long mine = 0;
        if (state != null) for (long v : state.mine()) mine += v;
        clearBtn.active = open && mine > 0;
    }

    private void bet(int k) {
        send(k, AMOUNTS[amountIdx]);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHAIN_PLACE, 1.6F, 0.5F));
    }

    private void send(int slot, int amount) {
        PacketDistributor.sendToServer(new WheelPayloads.Bet(terminal, slot, amount));
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks % 10 == 0) send(-1, 0);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.fill(left, top, left + W, top + H, 0xF0201418);
        g.renderOutline(left, top, W, H, 0xFFF0C030);
        g.drawString(font, "§lビッグホイール", left + 10, top + 8, 0xFFF0D070);
        if (state == null) return;
        if (!state.linked()) {
            g.drawCenteredString(font, "§c近く（" + 12 + "マス以内）にビッグホイールがないよ", left + W / 2, top + 70, 0xFFFFFF);
            return;
        }
        String chips = "チップ：" + state.chips() + "枚";
        g.drawString(font, "§7" + chips, left + W - 10 - font.width(chips), top + 8, 0xFFFFFF);
        // 残り時間
        int left_t = Math.max(0, state.ticksLeft() - (int) ((System.currentTimeMillis() - receivedAt) / 50));
        String ph = switch (state.phase()) {
            case 0 -> "§aかけ受付中 §f" + (left_t / 20 + 1) + "秒";
            case 1 -> "§e回ってる…！";
            default -> state.history().isEmpty() ? "" : "§6結果：×" + state.history().get(0);
        };
        g.drawCenteredString(font, ph, left + W / 2, top + 24, 0xFFFFFF);
        if (state.phase() == 0) {
            int bw = W - 20;
            g.fill(left + 10, top + 36, left + 10 + bw, top + 39, 0xFF303030);
            g.fill(left + 10, top + 36, left + 10 + (int) (bw * left_t / 1200F), top + 39, 0xFF40C060);
        }
        g.drawString(font, "§7かけている人：" + state.players() + "人", left + 10, top + 44, 0xFFFFFF);
        // 最近の結果
        g.drawString(font, "§7最近：", left + 10, top + 56, 0xFFFFFF);
        int hx = left + 44;
        for (int v : state.history()) {
            int c = WheelLayout.COLORS[WheelLayout.index(v)];
            String s = String.valueOf(v);
            g.fill(hx - 1, top + 54, hx + font.width(s) + 3, top + 65, c);
            g.drawString(font, s, hx + 1, top + 56, 0xFF000000, false);
            hx += font.width(s) + 7;
        }
        // マスごと：色、出る数、自分のかけ、みんなのかけ
        int bw = 48;
        for (int i = 0; i < 5; i++) {
            int x = left + 10 + i * (bw + 5), y = top + 72;
            int c = WheelLayout.COLORS[i];
            g.fill(x, y, x + bw, y + 42, 0xFF181010);
            g.fill(x, y, x + bw, y + 12, c);
            g.drawCenteredString(font, "§0§l" + WheelLayout.VALUES[i], x + bw / 2, y + 2, 0xFFFFFF);
            int count = 0;
            for (int s : WheelLayout.SEGMENTS) if (s == WheelLayout.VALUES[i]) count++;
            g.drawCenteredString(font, "§8" + count + "/25", x + bw / 2, y + 14, 0xFFFFFF);
            long mine = state.mine().get(i);
            g.drawCenteredString(font, mine > 0 ? "§e自分 " + mine : "§8-", x + bw / 2, y + 23, 0xFFFFFF);
            g.drawCenteredString(font, "§7計 " + state.totals().get(i), x + bw / 2, y + 32, 0xFFFFFF);
            if (state.phase() == 2 && !state.history().isEmpty() && state.history().get(0) == WheelLayout.VALUES[i])
                g.renderOutline(x - 1, y - 1, bw + 2, 44, (ticks / 4 % 2 == 0) ? 0xFFFFFFFF : c);
        }
        g.drawCenteredString(font, "§8当たると かけた枚数 × (数字＋1) 枚もどる", left + W / 2, top + 145, 0xFFFFFF);
    }

    private static long closedAt;

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
