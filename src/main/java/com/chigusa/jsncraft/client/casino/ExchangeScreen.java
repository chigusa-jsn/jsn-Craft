package com.chigusa.jsncraft.client.casino;

import com.chigusa.jsncraft.casino.CasinoPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** 両替機：お金 ⇔ チップ */
public class ExchangeScreen extends Screen {
    private static final int W = 240, H = 150;
    private final BlockPos pos;
    private int left, top;

    public ExchangeScreen(BlockPos pos) {
        super(Component.literal("両替機"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int[] amounts = CasinoClient.lightman ? new int[]{1, 10, 100, 1000} : new int[]{10, 100, 1000, 10000};
        for (int i = 0; i < 4; i++) {
            int a = amounts[i];
            addRenderableWidget(Button.builder(Component.literal("+" + CasinoUi.fmt(a)), b -> send(true, a))
                    .bounds(left + 14 + i * 54, top + 72, 50, 18).build());
            addRenderableWidget(Button.builder(Component.literal("-" + CasinoUi.fmt(a)), b -> send(false, a))
                    .bounds(left + 14 + i * 54, top + 108, 50, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("全部お金に戻す"), b -> {
            long unit = CasinoClient.lightman ? 1 : 10;
            long n = CasinoClient.chips / unit * unit;
            if (n > 0) send(false, (int) Math.min(n, 100_000));
        }).bounds(left + W - 104, top + 128, 92, 16).build());
    }

    private void send(boolean buy, int amount) {
        PacketDistributor.sendToServer(new CasinoPayloads.Exchange(pos, buy, amount));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        CasinoUi.panel(g, left, top, W, H);
        g.drawCenteredString(font, "§6§l両替機", left + W / 2, top + 10, 0xFFFFFF);
        g.drawString(font, "チップ： §e" + CasinoUi.fmt(CasinoClient.chips) + " 枚", left + 14, top + 28, 0xFFFFFF);
        String money = CasinoClient.lightman ? "お金：§a" + CasinoUi.fmt(CasinoClient.wallet) + " §f枚ぶん（鉄コイン1枚＝チップ1枚）"
                : "エメラルド：§a" + CasinoUi.fmt(CasinoClient.wallet / 10) + " §f個（1個＝チップ10枚）";
        g.drawString(font, money, left + 14, top + 40, 0xFFFFFF);
        g.drawString(font, "§bお金 → チップ", left + 14, top + 60, 0xFFFFFF);
        g.drawString(font, "§dチップ → お金", left + 14, top + 96, 0xFFFFFF);
        super.render(g, mx, my, pt);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
