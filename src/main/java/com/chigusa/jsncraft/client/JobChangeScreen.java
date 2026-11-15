package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.network.JobChangePayloads;
import com.chigusa.jsncraft.network.JobChangePayloads.JobEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 転職の神官の画面。上のタブで生活／戦闘を切り替え、左の一覧から職業を選んで「転職する」。
 * 一覧は 基本職 → 上位職 → 合体職 の順。★＝今の職業、✔＝転職できる、灰色＝条件不足。
 */
public class JobChangeScreen extends Screen {

    private static final int LIST_W = 130, ROW = 12;
    private static String lastType = "life";

    private List<JobEntry> jobs;
    private String type = lastType;
    @Nullable private String selected;
    private Button changeBtn, resetBtn, resetCombatBtn, resetLifeBtn;
    private boolean resetMode;
    private int left, top, w, h, scroll;

    public JobChangeScreen(List<JobEntry> jobs) {
        super(Component.literal("転職"));
        this.jobs = jobs;
    }

    public void update(List<JobEntry> jobs) {
        this.jobs = jobs;
    }

    @Override
    protected void init() {
        w = Math.min(width - 20, 340);
        h = Math.min(height - 20, 220);
        left = (width - w) / 2;
        top = (height - h) / 2;
        int tw = (w - 12) / 2;
        addRenderableWidget(Button.builder(Component.literal("生活ジョブ").withStyle(ChatFormatting.GREEN), b -> setType("life"))
                .bounds(left + 4, top + 4, tw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("戦闘ジョブ").withStyle(ChatFormatting.RED), b -> setType("combat"))
                .bounds(left + 8 + tw, top + 4, tw, 18).build());
        changeBtn = addRenderableWidget(Button.builder(Component.literal("転職する").withStyle(ChatFormatting.GOLD), b -> {
            JobEntry e = selectedEntry();
            if (e != null) PacketDistributor.sendToServer(new JobChangePayloads.Change(e.id()));
        }).bounds(left + LIST_W + 16, top + h - 24, 90, 18).build());

        // 忘却の宝珠でボードをリセット
        resetBtn = addRenderableWidget(Button.builder(Component.literal("ボードをリセット").withStyle(ChatFormatting.LIGHT_PURPLE),
                b -> resetMode = !resetMode).bounds(left + w - 96, top + h - 24, 90, 18).build());
        int rx = left + LIST_W + 16, ry = top + h - 48;
        resetCombatBtn = addRenderableWidget(Button.builder(Component.literal("戦闘ボード").withStyle(ChatFormatting.RED), b -> {
            PacketDistributor.sendToServer(new JobChangePayloads.ResetBoard("combat"));
            resetMode = false;
        }).bounds(rx, ry, 80, 18).build());
        resetLifeBtn = addRenderableWidget(Button.builder(Component.literal("生活ボード").withStyle(ChatFormatting.GREEN), b -> {
            PacketDistributor.sendToServer(new JobChangePayloads.ResetBoard("life"));
            resetMode = false;
        }).bounds(rx + 84, ry, 80, 18).build());
    }

    /** 持っている忘却の宝珠の数 */
    private int orbCount() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return 0;
        int n = 0;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(com.chigusa.jsncraft.npc.ModEntities.OBLIVION_ORB.get())) n += inv.getItem(i).getCount();
        }
        return n;
    }

    private void setType(String t) {
        type = t;
        lastType = t;
        selected = null;
        scroll = 0;
    }

    private List<JobEntry> shown() {
        List<JobEntry> out = new ArrayList<>();
        for (String tier : List.of("basic", "advanced", "hybrid")) {
            for (JobEntry e : jobs) if (e.type().equals(type) && e.tier().equals(tier)) out.add(e);
        }
        return out;
    }

    @Nullable
    private JobEntry selectedEntry() {
        List<JobEntry> list = shown();
        for (JobEntry e : list) if (e.id().equals(selected)) return e;
        // 何も選んでいなければ、今の職業を選んでおく
        for (JobEntry e : list) if (e.current()) {
            selected = e.id();
            return e;
        }
        if (!list.isEmpty()) {
            selected = list.get(0).id();
            return list.get(0);
        }
        return null;
    }

    private static String tierName(String tier) {
        return switch (tier) {
            case "advanced" -> "上位職";
            case "hybrid" -> "合体職";
            default -> "基本職";
        };
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top + 26, left + w, top + h, 0xE0101018);
        g.fill(left, top + 26, left + w, top + 27, 0xFF5A5A78);
        g.fill(left + LIST_W + 8, top + 30, left + LIST_W + 9, top + h - 4, 0xFF5A5A78);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        JobEntry sel = selectedEntry();
        changeBtn.visible = sel != null && !resetMode;
        changeBtn.active = sel != null && sel.canChange();
        boolean creative = minecraft != null && minecraft.player != null && minecraft.player.isCreative();
        int orbs = orbCount();
        resetBtn.setMessage(Component.literal(resetMode ? "やめる" : "ボードをリセット").withStyle(ChatFormatting.LIGHT_PURPLE));
        resetCombatBtn.visible = resetLifeBtn.visible = resetMode;
        resetCombatBtn.active = resetLifeBtn.active = orbs > 0 || creative;
        super.render(g, mouseX, mouseY, partialTick);

        // 一覧
        int lx = left + 6, ly = top + 32 - scroll;
        g.enableScissor(left + 2, top + 30, left + LIST_W + 6, top + h - 4);
        String lastTier = "";
        for (JobEntry e : shown()) {
            if (!e.tier().equals(lastTier)) {
                lastTier = e.tier();
                g.drawString(font, "― " + tierName(e.tier()) + " ―", lx, ly, 0x8888AA);
                ly += ROW;
            }
            boolean isSel = e.id().equals(selected);
            boolean hover = mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ly - 2 && mouseY < ly + ROW - 2;
            if (isSel) g.fill(lx, ly - 2, lx + LIST_W, ly + ROW - 2, 0x60FFFFFF);
            else if (hover) g.fill(lx, ly - 2, lx + LIST_W, ly + ROW - 2, 0x30FFFFFF);
            String mark = e.current() ? "★ " : e.canChange() ? "✔ " : "  ";
            int color = e.current() ? 0xFFD700 : e.canChange() ? 0xFFFFFF : 0x777777;
            String name = font.plainSubstrByWidth(mark + e.name().getString(), LIST_W - 34);
            g.drawString(font, name, lx + 2, ly, color);
            if (e.level() > 0) {
                String lv = "Lv." + e.level();
                g.drawString(font, lv, lx + LIST_W - 2 - font.width(lv), ly, 0x88AACC);
            }
            ly += ROW;
        }
        g.disableScissor();

        // 詳細
        if (sel == null) {
            g.drawString(font, "職業がありません", left + LIST_W + 16, top + 36, 0xAAAAAA);
            return;
        }
        int x = left + LIST_W + 16, y = top + 34;
        g.drawString(font, sel.name().copy().withStyle(ChatFormatting.BOLD), x, y, 0xFFFFFF);
        y += 12;
        g.drawString(font, ("life".equals(sel.type()) ? "生活ジョブ" : "戦闘ジョブ") + "・" + tierName(sel.tier()), x, y, 0x8888AA);
        y += 16;
        String lv = sel.level() > 0 ? "Lv." + sel.level() + " / " + sel.maxLevel() : "まだ就いたことがありません";
        g.drawString(font, "あなたのレベル：" + lv, x, y, 0xDDDDDD);
        y += 16;
        if (!sel.requirements().isEmpty()) {
            g.drawString(font, "転職の条件", x, y, 0xFFFF55);
            y += 11;
            for (Component r : sel.requirements()) {
                g.drawString(font, Component.literal("・").append(r.copy()), x + 2, y, 0xFFFFFF);
                y += 10;
            }
            y += 6;
        } else {
            g.drawString(font, "条件なしで転職できます", x, y, 0x888899);
            y += 16;
        }
        int reasonColor = sel.current() ? 0xFFD700 : sel.canChange() ? 0x55FF55 : 0xFF5555;
        g.drawString(font, sel.reason(), x, y, reasonColor);
        if (resetMode) {
            int ry = top + h - 74;
            g.fill(left + LIST_W + 12, ry - 4, left + w - 6, top + h - 28, 0xC0201030);
            g.drawString(font, "忘却の宝珠を1つ使って、ボードを", left + LIST_W + 16, ry, 0xE0B0FF);
            g.drawString(font, "リセットします（所持：" + orbs + "個）", left + LIST_W + 16, ry + 10, orbs > 0 || creative ? 0xE0B0FF : 0xFF5555);
        } else {
            g.drawString(font, "転職しても、前の職業のレベルとボードは残ります", left + LIST_W + 16, top + h - 36, 0x666677);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int lx = left + 6, ly = top + 32 - scroll;
        if (mouseX < lx || mouseX >= lx + LIST_W || mouseY < top + 30 || mouseY >= top + h - 4) return false;
        String lastTier = "";
        for (JobEntry e : shown()) {
            if (!e.tier().equals(lastTier)) {
                lastTier = e.tier();
                ly += ROW;
            }
            if (mouseY >= ly - 2 && mouseY < ly + ROW - 2) {
                selected = e.id();
                return true;
            }
            ly += ROW;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int rows = shown().size() + 3;
        int max = Math.max(0, rows * ROW - (h - 36));
        scroll = (int) Math.max(0, Math.min(max, scroll - scrollY * ROW));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
