package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.enchanting.Runes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** 魔紋の書：左に魔紋の一覧（種類ごと）、右に選んだ魔紋の説明。 */
public class RuneBookScreen extends Screen {
    private static final int W = 360, H = 230, ROW = 12, LIST_W = 110;
    /** 一覧の行：見出し（rune = null）か魔紋 */
    private record Row(String header, Runes.Rune rune) {}

    private final List<Row> rows = new ArrayList<>();
    private int scroll = 0, selected = 0;

    public RuneBookScreen() {
        super(Component.literal("魔紋の書"));
        String last = "";
        List<Runes.Rune> sorted = new ArrayList<>(Runes.LIST);
        for (Runes.Rune r : sorted) {
            if (!r.category().equals(last)) {
                rows.add(new Row(r.category(), null));
                last = r.category();
            }
            rows.add(new Row(null, r));
        }
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }
    private int visible() { return (H - 30) / ROW; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int x = left(), y = top();
        // 本の見た目
        g.fill(x - 4, y - 4, x + W + 4, y + H + 4, 0xFF3A1E4A);
        g.fill(x, y, x + W, y + H, 0xFFEADBC0);
        g.fill(x + LIST_W + 6, y + 6, x + LIST_W + 7, y + H - 6, 0xFFB8A080);
        g.drawString(font, "魔紋の書（" + Runes.LIST.size() + "種）", x + 8, y + 8, 0xFF5A2A7A, false);

        int ly = y + 24;
        for (int i = scroll; i < rows.size() && i < scroll + visible(); i++) {
            Row row = rows.get(i);
            if (row.rune() == null) {
                g.drawString(font, "― " + row.header(), x + 6, ly + 2, 0xFF7A5A3A, false);
            } else {
                boolean sel = row.rune().index() == selected;
                boolean hover = mx >= x + 4 && mx < x + LIST_W + 4 && my >= ly && my < ly + ROW;
                if (sel) g.fill(x + 4, ly, x + LIST_W + 4, ly + ROW, 0x553A1E6A);
                else if (hover) g.fill(x + 4, ly, x + LIST_W + 4, ly + ROW, 0x22000000);
                g.drawString(font, "✦" + row.rune().name(), x + 12, ly + 2, sel ? 0xFF6A1E9A : 0xFF3A2A2A, false);
                g.drawString(font, Runes.roman(row.rune().maxLevel()), x + LIST_W - 12, ly + 2, 0xFF9A7A4A, false);
            }
            ly += ROW;
        }
        if (rows.size() > visible()) {
            int barH = Math.max(10, (H - 30) * visible() / rows.size());
            int barY = y + 24 + (H - 30 - barH) * scroll / Math.max(1, rows.size() - visible());
            g.fill(x + LIST_W + 2, barY, x + LIST_W + 4, barY + barH, 0xFF9A7A4A);
        }

        Runes.Rune r = Runes.get(selected);
        if (r != null) {
            int dx = x + LIST_W + 14, dy = y + 10, dw = W - LIST_W - 22;
            g.renderItem(r.catalyst().getDefaultInstance(), dx, dy);
            g.drawString(font, Component.literal("✦" + r.name()), dx + 20, dy + 4, 0xFF6A1E9A, false);
            dy += 22;
            List<Component> lines = RuneText.details(r, false);
            for (int i = 1; i < lines.size(); i++) {
                for (FormattedCharSequence seq : font.split(lines.get(i), dw)) {
                    g.drawString(font, seq, dx, dy, 0xFF2A2A2A, false);
                    dy += 10;
                }
                if (i >= 3 && i % 2 == 0) dy += 3;
            }
            dy = y + H - 34;
            for (FormattedCharSequence seq : font.split(Component.literal("魔紋は「魔紋の祭壇」で、付与術師が刻める。同じ魔紋をもう一度刻むとレベルが上がる。"), dw)) {
                g.drawString(font, seq, dx, dy, 0xFF7A6A5A, false);
                dy += 10;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = left(), y = top();
        if (mx >= x + 4 && mx < x + LIST_W + 4 && my >= y + 24) {
            int i = scroll + (int) ((my - y - 24) / ROW);
            if (i < rows.size() && i < scroll + visible() && rows.get(i).rune() != null) {
                selected = rows.get(i).rune().index();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, Math.min(Math.max(0, rows.size() - visible()), scroll - (int) Math.signum(sy) * 2));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
