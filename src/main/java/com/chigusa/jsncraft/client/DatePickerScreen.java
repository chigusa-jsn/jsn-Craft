package com.chigusa.jsncraft.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

/**
 * カレンダーで日付を選び、▲▼で時刻を合わせる画面。
 * 決定すると "2026-12-24 18:00"、指定なしなら "" を返す。
 */
public class DatePickerScreen extends Screen {

    private static final String[] WEEK = {"日", "月", "火", "水", "木", "金", "土"};
    private static final int CELL_W = 26, CELL_H = 16;

    private final Screen parent;
    private final boolean isEnd;
    private final Consumer<String> onDone;

    @Nullable private LocalDate date;
    private int hour, minute;
    private YearMonth month;
    private int left, top, w, h, gridX, gridY;

    public DatePickerScreen(Screen parent, boolean isEnd, String current, Consumer<String> onDone) {
        super(Component.literal(isEnd ? "終了日時" : "開始日時"));
        this.parent = parent;
        this.isEnd = isEnd;
        this.onDone = onDone;
        LocalDateTime t = parse(current);
        if (t != null) {
            date = t.toLocalDate();
            hour = t.getHour();
            minute = t.getMinute();
        } else {
            date = null;
            hour = isEnd ? 23 : 0;
            minute = isEnd ? 59 : 0;
        }
        month = YearMonth.from(date != null ? date : LocalDate.now());
    }

    @Nullable
    public static LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim().replace('/', '-').replace(' ', 'T');
        if (!v.contains("T")) v += "T00:00";
        try {
            return LocalDateTime.parse(v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 表示用：「12/24(木) 18:00」 */
    public static String pretty(String s) {
        LocalDateTime t = parse(s);
        if (t == null) return "";
        String wd = WEEK[t.getDayOfWeek().getValue() % 7];
        return String.format("%d/%d(%s) %02d:%02d", t.getMonthValue(), t.getDayOfMonth(), wd, t.getHour(), t.getMinute());
    }

    @Override
    protected void init() {
        w = CELL_W * 7 + 20;
        h = 230;
        left = (width - w) / 2;
        top = Math.max(4, (height - h) / 2);
        gridX = left + 10;
        gridY = top + 42;

        // 月の切り替え
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> month = month.minusMonths(1))
                .bounds(left + 6, top + 18, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> month = month.plusMonths(1))
                .bounds(left + w - 26, top + 18, 20, 16).build());

        // 時刻（時は1時間、分は5分ずつ）
        int ty = gridY + CELL_H * 7 + 4;
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> hour = (hour + 1) % 24).bounds(left + 40, ty, 16, 12).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> hour = (hour + 23) % 24).bounds(left + 40, ty + 13, 16, 12).build());
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> minute = (minute + 5) % 60).bounds(left + 98, ty, 16, 12).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> minute = (minute + 55) % 60).bounds(left + 98, ty + 13, 16, 12).build());

        // 下のボタン
        int by = top + h - 22;
        int bw = (w - 16) / 4;
        addRenderableWidget(Button.builder(Component.literal(isEnd ? "期限なし" : "今すぐ"), b -> finish(""))
                .bounds(left + 4, by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("今日"), b -> {
            date = LocalDate.now();
            month = YearMonth.from(date);
        }).bounds(left + 6 + bw, by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("決定").withStyle(ChatFormatting.GREEN), b -> {
            if (date == null) finish("");
            else finish(String.format("%s %02d:%02d", date, hour, minute));
        }).bounds(left + 8 + bw * 2, by, bw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("やめる"), b -> onClose())
                .bounds(left + 10 + bw * 3, by, bw, 18).build());
    }

    private void finish(String value) {
        onDone.accept(value);
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + w, top + h, 0xF0101018);
        g.fill(left, top, left + w, top + 1, 0xFF5A5A78);
        g.fill(left, top + h - 1, left + w, top + h, 0xFF5A5A78);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, Component.literal(isEnd ? "いつまで受けられる？" : "いつから受けられる？").withStyle(ChatFormatting.YELLOW),
                left + w / 2, top + 6, 0xFFFFFF);
        g.drawCenteredString(font, month.getYear() + "年 " + month.getMonthValue() + "月", left + w / 2, top + 22, 0xFFFFFF);

        // 曜日
        for (int i = 0; i < 7; i++) {
            int color = i == 0 ? 0xFF6666 : i == 6 ? 0x6699FF : 0xAAAAAA;
            g.drawCenteredString(font, WEEK[i], gridX + i * CELL_W + CELL_W / 2, gridY - 2, color);
        }
        // 日付
        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() % 7; // 日曜はじまり
        LocalDate today = LocalDate.now();
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            int idx = offset + d - 1;
            int x = gridX + (idx % 7) * CELL_W, y = gridY + 9 + (idx / 7) * CELL_H;
            LocalDate day = month.atDay(d);
            boolean sel = day.equals(date);
            boolean hover = mouseX >= x && mouseX < x + CELL_W - 2 && mouseY >= y && mouseY < y + CELL_H - 2;
            int bg = sel ? 0xFF3A7BD8 : hover ? 0xFF4A4A6A : 0xFF22222E;
            g.fill(x, y, x + CELL_W - 2, y + CELL_H - 2, bg);
            if (day.equals(today)) {
                g.fill(x, y + CELL_H - 3, x + CELL_W - 2, y + CELL_H - 2, 0xFFFFD700);
            }
            DayOfWeek dow = day.getDayOfWeek();
            int color = dow == DayOfWeek.SUNDAY ? 0xFF8888 : dow == DayOfWeek.SATURDAY ? 0x88AAFF : 0xFFFFFF;
            g.drawCenteredString(font, String.valueOf(d), x + (CELL_W - 2) / 2, y + 3, color);
        }

        // 時刻
        int ty = gridY + CELL_H * 7 + 4;
        g.drawString(font, "時刻", left + 8, ty + 8, 0xAAAAAA);
        g.drawString(font, String.format("%02d 時", hour), left + 60, ty + 8, 0xFFFFFF);
        g.drawString(font, String.format("%02d 分", minute), left + 118, ty + 8, 0xFFFFFF);

        // 今の選択
        String summary = date == null
                ? (isEnd ? "期限なし（ずっと受けられる）" : "すぐに受けられる")
                : String.format("%d/%d(%s) %02d:%02d %s", date.getMonthValue(), date.getDayOfMonth(),
                WEEK[date.getDayOfWeek().getValue() % 7], hour, minute, isEnd ? "まで" : "から");
        g.drawCenteredString(font, Component.literal(summary).withStyle(ChatFormatting.GREEN), left + w / 2, top + h - 34, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int offset = month.atDay(1).getDayOfWeek().getValue() % 7;
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            int idx = offset + d - 1;
            int x = gridX + (idx % 7) * CELL_W, y = gridY + 9 + (idx / 7) * CELL_H;
            if (mouseX >= x && mouseX < x + CELL_W - 2 && mouseY >= y && mouseY < y + CELL_H - 2) {
                LocalDate day = month.atDay(d);
                date = day.equals(date) ? null : day; // もう一度押すと選択解除
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
