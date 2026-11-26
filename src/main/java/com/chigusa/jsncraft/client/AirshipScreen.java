package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.airship.Airship;
import com.chigusa.jsncraft.airship.AirshipMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** 飛空艇の画面：上に燃料と「仕舞う」、真ん中に荷物、下に持ち物。 */
public class AirshipScreen extends AbstractContainerScreen<AirshipMenu> {
    private Button packBtn;

    public AirshipScreen(AirshipMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 44 + menu.rows() * 18 + 14 + 76 + 6;
        inventoryLabelY = 44 + menu.rows() * 18 + 3;
    }

    @Override
    protected void init() {
        super.init();
        packBtn = addRenderableWidget(Button.builder(Component.literal("仕舞う"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("飛空艇をアイテムに戻す（荷物・燃料・色はそのまま）")))
                .bounds(leftPos + 118, topPos + 19, 50, 18).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        packBtn.active = menu.owner();
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E22);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF8A7A62);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 40, 0xFF5A4A36);
        for (Slot s : menu.slots) {
            int c = s.index == 0 ? 0xFFC07020 : s.index <= menu.rows() * 9 ? 0xFF6A5A40 : 0xFF3A3A42;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
        // 燃料のゲージ
        int gx = x + 30, gy = y + 24, gw = 82;
        g.fill(gx - 1, gy - 1, gx + gw + 1, gy + 9, 0xFF000000);
        g.fill(gx, gy, gx + gw, gy + 8, 0xFF2A2A2E);
        int fill = (int) Math.round(gw * Math.min(1.0, menu.fuelSeconds() * 20.0 / Airship.MAX_FUEL));
        if (fill > 0) g.fill(gx, gy, gx + fill, gy + 8, 0xFFE8A040);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 4, 0xFFF0E0C0, false);
        int sec = menu.fuelSeconds();
        String s = sec > 0 ? String.format("燃料 %d:%02d", sec / 60, sec % 60) : "§c燃料なし";
        g.drawString(font, s, 30, 14, 0xFFFFFFFF, false);
        g.drawString(font, "荷物", 8, 34, 0xFFE8E0D0, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF2A2A2A, false);
        if (!menu.owner()) g.drawString(font, "§7持ち主だけ仕舞える", 92, 6, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        if (mx >= leftPos + 8 && mx < leftPos + 24 && my >= topPos + 20 && my < topPos + 36 && menu.slots.get(0).getItem().isEmpty())
            g.renderTooltip(font, Component.literal("燃料（石炭・木炭・ブレイズロッドなど）"), mx, my);
    }
}
