package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.loginbonus.LoginBonusClaimPayload;
import com.chigusa.jsncraft.loginbonus.LoginBonusPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** ログインボーナスのカレンダー：7列で日ごとのご褒美。押した日には赤いはんこ。 */
public class LoginBonusScreen extends Screen {
    private static final int CELL = 34, GAP = 3, COLS = 7;
    private static LoginBonusPayload data;
    private Button claim;
    private int left, top, w, h;
    /** 受け取った直後のはんこのアニメーション（tick） */
    private int stampAnim = -1, stampDay = -1;

    public LoginBonusScreen() {
        super(Component.literal("ログインボーナス"));
    }

    public static void receive(LoginBonusPayload p) {
        LoginBonusPayload before = data;
        data = p;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof LoginBonusScreen s) {
            if (before != null && before.canClaim() && !p.canClaim()) {
                s.stampDay = Math.max(0, p.stamps() - 1);
                s.stampAnim = 0;
            }
        } else if (p.open()) {
            mc.setScreen(new LoginBonusScreen());
        }
    }

    private int rows() {
        return data == null ? 1 : Math.max(1, (data.days().size() + COLS - 1) / COLS);
    }

    @Override
    protected void init() {
        w = COLS * (CELL + GAP) - GAP + 20;
        h = rows() * (CELL + GAP) - GAP + 76;
        left = (width - w) / 2;
        top = (height - h) / 2;
        claim = addRenderableWidget(Button.builder(Component.literal("今日のご褒美を受け取る"), b -> {
            PacketDistributor.sendToServer(LoginBonusClaimPayload.INSTANCE);
            b.active = false;
        }).bounds(left + w / 2 - 70, top + h - 26, 140, 18).build());
    }

    @Override
    public void tick() {
        if (stampAnim >= 0 && ++stampAnim > 12) stampAnim = -1;
        if (data != null && claim != null) {
            claim.active = data.canClaim();
            claim.setMessage(Component.literal(data.canClaim() ? "今日のご褒美を受け取る" : "また明日（朝5時に更新）"));
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        g.fill(left - 2, top - 2, left + w + 2, top + h + 2, 0xFF3A2410);
        g.fill(left, top, left + w, top + h, 0xFFF4E8C8);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        if (data == null) return;
        g.fill(left, top, left + w, top + 22, 0xFFC04030);
        g.drawCenteredString(font, Component.literal("ログインボーナス").withStyle(ChatFormatting.BOLD), left + w / 2, top + 7, 0xFFFFF4D0);
        String info = "連続 " + data.streak() + " 日  ・  合計 " + data.total() + " 日";
        g.drawString(font, info, left + 10, top + 27, 0xFF5A3A20, false);
        if (!data.streakRewards().isEmpty()) {
            int left7 = data.streakEvery() - (data.streak() % data.streakEvery());
            String s = "連続" + data.streakEvery() + "日ごとにおまけ（あと" + left7 + "日）";
            g.drawString(font, s, left + w - 10 - font.width(s), top + 27, 0xFFB04020, false);
        }

        int gx = left + 10, gy = top + 40;
        int today = data.canClaim() ? data.stamps() : -1;
        List<Component> tip = null;
        for (int i = 0; i < data.days().size(); i++) {
            int cx = gx + (i % COLS) * (CELL + GAP), cy = gy + (i / COLS) * (CELL + GAP);
            boolean done = i < data.stamps();
            boolean isToday = i == today;
            boolean big = (i + 1) % 7 == 0;
            int frame = isToday ? 0xFFFFC020 : big ? 0xFFC08030 : 0xFFB09070;
            g.fill(cx - 1, cy - 1, cx + CELL + 1, cy + CELL + 1, frame);
            g.fill(cx, cy, cx + CELL, cy + CELL, done ? 0xFFE0D4B4 : big ? 0xFFFFF0C8 : 0xFFFFFAEA);
            if (isToday) {
                int a = (int) (80 + 60 * Math.sin((Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime() + pt) / 4.0));
                g.fill(cx, cy, cx + CELL, cy + CELL, (a << 24) | 0xFFE060);
            }
            g.drawString(font, String.valueOf(i + 1), cx + 2, cy + 2, big ? 0xFFC03020 : 0xFF7A5A3A, false);
            LoginBonusPayload.Day day = data.days().get(i);
            if (!day.icons().isEmpty()) {
                ItemStack icon = day.icons().get(0);
                g.renderItem(icon, cx + (CELL - 16) / 2, cy + 11);
                g.renderItemDecorations(font, icon, cx + (CELL - 16) / 2, cy + 11);
                if (day.icons().size() > 1) g.drawString(font, "+" + (day.icons().size() - 1), cx + CELL - 11, cy + 2, 0xFF3A7A3A, false);
            }
            // はんこ
            boolean anim = stampAnim >= 0 && i == stampDay;
            if (done && !anim) stamp(g, cx, cy, 1.0F);
            if (anim) stamp(g, cx, cy, 1.0F + (12 - stampAnim) / 8F);
            if (mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL) {
                tip = new ArrayList<>();
                tip.add(Component.literal((i + 1) + "日目" + (done ? "（受け取りずみ）" : isToday ? "（今日）" : "")).withStyle(ChatFormatting.GOLD));
                for (String l : day.labels()) tip.add(Component.literal("・" + l).withStyle(ChatFormatting.WHITE));
            }
        }
        if (tip != null) g.renderComponentTooltip(font, tip, mx, my);
    }

    /** 赤い丸のはんこ */
    private void stamp(GuiGraphics g, int cx, int cy, float scale) {
        g.pose().pushPose();
        float c = CELL / 2F;
        g.pose().translate(cx + c, cy + c, 300);
        g.pose().scale(scale, scale, 1);
        int r = 12;
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                double d = Math.sqrt(x * x + y * y);
                if (d <= r && d >= r - 2) g.fill(x, y, x + 1, y + 1, 0xC0D02020);
            }
        }
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-15));
        g.drawCenteredString(font, Component.literal("済").withStyle(ChatFormatting.BOLD), 0, -4, 0xFFD02020);
        g.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
