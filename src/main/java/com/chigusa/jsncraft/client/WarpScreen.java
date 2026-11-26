package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.warp.WarpGoPayload;
import com.chigusa.jsncraft.warp.WarpListPayload;
import com.chigusa.jsncraft.warp.Warps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** ワープの行き先を選ぶ画面 */
public class WarpScreen extends Screen {
    private static final int ROW = 22, W = 240;
    private final WarpListPayload data;
    private int scroll, left, top, rows;

    public WarpScreen(WarpListPayload data) {
        super(Component.literal("ワープ"));
        this.data = data;
    }

    public static void open(WarpListPayload p) {
        Minecraft.getInstance().setScreen(new WarpScreen(p));
    }

    @Override
    protected void init() {
        rows = Math.max(3, Math.min(8, (height - 80) / ROW));
        left = (width - W) / 2;
        top = (height - (rows * ROW + 40)) / 2;
    }

    private static int dimColor(String dim) {
        return switch (dim) {
            case "minecraft:overworld" -> 0xFF60C060;
            case "minecraft:the_nether" -> 0xFFE05030;
            case "minecraft:the_end" -> 0xFFC080F0;
            default -> 0xFF60A0E0;
        };
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        int h = rows * ROW + 40;
        g.fill(left - 2, top - 2, left + W + 2, top + h + 2, 0xFF102838);
        g.fill(left, top, left + W, top + h, 0xFF1C3C50);
        g.fill(left, top, left + W, top + 18, 0xFF2A6A8A);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, Component.literal("ワープの行き先").withStyle(ChatFormatting.BOLD), left + W / 2, top + 5, 0xFFE8F8FF);
        if (data.entries().isEmpty()) {
            g.drawCenteredString(font, "まだワープ地点がありません", left + W / 2, top + 40, 0xFFA0B8C8);
            return;
        }
        for (int r = 0; r < rows; r++) {
            int i = scroll + r;
            if (i >= data.entries().size()) break;
            WarpListPayload.Entry e = data.entries().get(i);
            int x = left + 6, y = top + 24 + r * ROW;
            boolean hover = mx >= x && mx < x + W - 12 && my >= y && my < y + ROW - 2;
            g.fill(x, y, x + W - 12, y + ROW - 2, hover ? 0xFF3A7090 : 0xFF244A60);
            g.fill(x, y, x + 3, y + ROW - 2, dimColor(e.dim()));
            g.drawString(font, e.name(), x + 7, y + 2, 0xFFFFFFFF, false);
            String sub = Warps.dimName(e.dim()) + "  (" + e.x() + ", " + e.y() + ", " + e.z() + ")" + (e.dim().equals(data.here()) ? "" : "  ※別のディメンション");
            g.drawString(font, sub, x + 7, y + 11, 0xFF9AB8C8, false);
            String cost = e.cost() > 0 ? "使用料 " + e.cost() : "無料";
            g.drawString(font, cost, x + W - 16 - font.width(cost), y + 2, e.cost() > 0 ? 0xFFFFD060 : 0xFF80E080, false);
        }
        int max = Math.max(0, data.entries().size() - rows);
        if (max > 0) g.drawString(font, "↕ " + (scroll + 1) + "/" + (max + 1), left + W - 40, top + rows * ROW + 28, 0xFF88A0B0, false);
        g.drawString(font, "§7クリックでワープ", left + 6, top + rows * ROW + 28, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int r = 0; r < rows; r++) {
            int i = scroll + r;
            if (i >= data.entries().size()) break;
            int x = left + 6, y = top + 24 + r * ROW;
            if (mx >= x && mx < x + W - 12 && my >= y && my < y + ROW - 2) {
                PacketDistributor.sendToServer(new WarpGoPayload(i));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int max = Math.max(0, data.entries().size() - rows);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(sy)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
