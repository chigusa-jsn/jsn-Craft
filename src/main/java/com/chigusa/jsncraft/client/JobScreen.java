package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.network.BoardSnapshotPayload;
import com.chigusa.jsncraft.network.BoardSnapshotPayload.BoardView;
import com.chigusa.jsncraft.network.BoardSnapshotPayload.NodeView;
import com.chigusa.jsncraft.network.BoardSnapshotPayload.StatLine;
import com.chigusa.jsncraft.network.ClientJobCache;
import com.chigusa.jsncraft.network.JobSyncPayload;
import com.chigusa.jsncraft.network.UnlockNodePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 職業メニュー。タブで「ステータス」「戦闘ボード」「生活ボード」を切り替える。
 * ボードはドラッグで動かせる（盤面型の大きなボードにも対応できるように）。
 */
public class JobScreen extends Screen {

    private static final int TAB_STATUS = 0, TAB_COMBAT = 1, TAB_LIFE = 2;
    private static final int NODE = 22, GAP_X = 38, GAP_Y = 34;

    private static int lastTab = TAB_STATUS;

    private BoardSnapshotPayload data;
    private int tab = lastTab;
    private int boardIndex = 0;
    private Button prevBtn, nextBtn;
    private double panX, panY;

    private int left, top, panelW, panelH;

    public JobScreen(BoardSnapshotPayload data) {
        super(Component.literal("職業メニュー"));
        this.data = data;
    }

    public void update(BoardSnapshotPayload data) {
        this.data = data;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 340);
        panelH = Math.min(height - 20, 230);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;

        int tabW = (panelW - 16) / 3;
        addRenderableWidget(Button.builder(Component.literal("ステータス"), b -> setTab(TAB_STATUS))
                .bounds(left + 4, top + 4, tabW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("戦闘ボード").withStyle(ChatFormatting.RED), b -> setTab(TAB_COMBAT))
                .bounds(left + 8 + tabW, top + 4, tabW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("生活ボード").withStyle(ChatFormatting.GREEN), b -> setTab(TAB_LIFE))
                .bounds(left + 12 + tabW * 2, top + 4, tabW, 18).build());

        // 引き継いだボード（基本職のボードなど）との切り替え
        int by = top + panelH - 17;
        prevBtn = addRenderableWidget(Button.builder(Component.literal("◀"), b -> boardIndex--)
                .bounds(left + panelW - 62, by, 16, 14).build());
        nextBtn = addRenderableWidget(Button.builder(Component.literal("▶"), b -> boardIndex++)
                .bounds(left + panelW - 20, by, 16, 14).build());
    }

    private List<BoardView> boards() {
        return tab == TAB_COMBAT ? data.combat() : data.life();
    }

    private Optional<BoardView> currentBoard() {
        List<BoardView> list = boards();
        if (list.isEmpty()) return Optional.empty();
        boardIndex = Math.max(0, Math.min(boardIndex, list.size() - 1));
        return Optional.of(list.get(boardIndex));
    }

    private void setTab(int t) {
        tab = t;
        lastTab = t;
        boardIndex = 0;
        panX = 0;
        panY = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- 描画 ----

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean multi = tab != TAB_STATUS && boards().size() > 1;
        currentBoard();
        prevBtn.visible = multi;
        nextBtn.visible = multi;
        prevBtn.active = boardIndex > 0;
        nextBtn.active = boardIndex < boards().size() - 1;

        super.render(g, mouseX, mouseY, partialTick); // 背景・パネル・ボタン

        if (tab == TAB_STATUS) {
            renderStatus(g);
        } else {
            Optional<BoardView> board = currentBoard();
            if (board.isEmpty()) {
                g.drawCenteredString(font, (tab == TAB_COMBAT ? "戦闘" : "生活") + "ジョブに就いていません（/job change）",
                        left + panelW / 2, top + panelH / 2, 0xAAAAAA);
            } else if (board.get().nodes().isEmpty()) {
                renderBoardHeader(g, board.get());
                g.drawCenteredString(font, "この職業のスキルボードはまだありません", left + panelW / 2, top + panelH / 2, 0xAAAAAA);
            } else {
                renderBoard(g, board.get(), mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        // パネル（ボタンより先に描く）
        g.fill(left, top + 26, left + panelW, top + panelH, 0xE0101018);
        outline(g, left, top + 26, panelW, panelH - 26, 0xFF5A5A78);
    }

    private void renderStatus(GuiGraphics g) {
        int x = left + 12;
        int y = top + 34;
        Minecraft mc = Minecraft.getInstance();
        JobSyncPayload own = mc.player != null ? ClientJobCache.get(mc.player.getId()) : null;
        if (own != null && !own.title().isEmpty()) {
            g.drawString(font, Component.literal("《" + own.title() + "》").withStyle(ChatFormatting.GOLD), x, y, 0xFFFFFF);
            y += 12;
        }
        y = jobLine(g, x, y, "[戦闘] ", ChatFormatting.RED, data.combat().stream().findFirst());
        y = jobLine(g, x, y, "[生活] ", ChatFormatting.GREEN, data.life().stream().findFirst());
        y += 4;
        g.fill(x, y, left + panelW - 12, y + 1, 0xFF5A5A78);
        y += 6;

        // 能力値は2列で並べる（左列の下まで行ったら右列へ）
        List<StatLine> stats = data.stats();
        int perColumn = (stats.size() + 1) / 2;
        int colW = (panelW - 24) / 2;
        int startY = y;
        for (int i = 0; i < stats.size(); i++) {
            StatLine s = stats.get(i);
            int col = i / perColumn;
            int cx = x + col * (colW + 4);
            int cy = startY + (i % perColumn) * 11;
            if (cy > top + panelH - 12) continue;
            int valueX = cx + colW - 44;
            g.drawString(font, s.name(), cx, cy, 0xDDDDDD);
            g.drawString(font, s.value(), valueX - font.width(s.value()), cy, 0xFFFFFF);
            if (!s.bonus().isEmpty()) {
                g.drawString(font, s.bonus(), valueX + 4, cy, 0x55FF55);
            }
        }
    }

    private int jobLine(GuiGraphics g, int x, int y, String label, ChatFormatting color, Optional<BoardView> view) {
        Component line;
        if (view.isEmpty()) {
            line = Component.literal(label).withStyle(color).append(Component.literal("なし").withStyle(ChatFormatting.GRAY));
        } else {
            BoardView v = view.get();
            line = Component.literal(label).withStyle(color).append(v.jobName().copy())
                    .append(Component.literal(" Lv." + v.level()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("   SP " + v.points()).withStyle(ChatFormatting.AQUA));
        }
        g.drawString(font, line, x, y, 0xFFFFFF);
        return y + 12;
    }

    private void renderBoardHeader(GuiGraphics g, BoardView v) {
        Component head = v.jobName().copy().append(Component.literal(" Lv." + v.level()).withStyle(ChatFormatting.WHITE));
        if (v.inherited()) head = head.copy().append(Component.literal(" (引き継ぎ)").withStyle(ChatFormatting.GRAY));
        g.drawString(font, head, left + 10, top + 32, 0xFFFFFF);
        if (boards().size() > 1) {
            g.drawCenteredString(font, (boardIndex + 1) + "/" + boards().size(), left + panelW - 33, top + panelH - 14, 0xAAAAAA);
        }
        String sp = "スキルポイント " + v.points();
        g.drawString(font, sp, left + panelW - 10 - font.width(sp), top + 32, 0x55FFFF);
    }

    private void renderBoard(GuiGraphics g, BoardView v, int mouseX, int mouseY) {
        renderBoardHeader(g, v);

        int areaL = left + 2, areaT = top + 44, areaR = left + panelW - 2, areaB = top + panelH - 20;
        int originX = (areaL + areaR) / 2 + (int) panX;
        int originY = areaT + 14 + (int) panY;

        g.enableScissor(areaL, areaT, areaR, areaB);

        // 線
        for (NodeView n : v.nodes()) {
            for (String linkId : n.links()) {
                NodeView other = find(v, linkId);
                if (other == null) continue;
                boolean lit = v.isUnlocked(n.id()) && v.isUnlocked(other.id());
                int color = lit ? 0xFFFFC14D : 0xFF4A4A5A;
                connect(g, cx(originX, other), cy(originY, other), cx(originX, n), cy(originY, n), color);
            }
        }

        // マス
        NodeView hovered = null;
        for (NodeView n : v.nodes()) {
            int x = cx(originX, n) - NODE / 2;
            int y = cy(originY, n) - NODE / 2;
            boolean unlocked = v.isUnlocked(n.id());
            boolean reachable = v.isReachable(n);
            boolean affordable = v.points() >= n.cost();

            int border = unlocked ? 0xFFFFC14D
                    : reachable && affordable ? 0xFF55DD77
                    : reachable ? 0xFF5588AA
                    : 0xFF3A3A44;
            g.fill(x - 1, y - 1, x + NODE + 1, y + NODE + 1, border);
            g.fill(x + 1, y + 1, x + NODE - 1, y + NODE - 1, unlocked ? 0xFF3A2E10 : 0xFF1A1A22);

            Item item = n.icon().map(BuiltInRegistries.ITEM::get).orElse(Items.AIR);
            if (item != Items.AIR) {
                g.renderItem(new ItemStack(item), x + 3, y + 3);
                if (!unlocked) g.fill(x + 1, y + 1, x + NODE - 1, y + NODE - 1, 0x88000000); // 未解放は暗く
            } else {
                String c = n.name().getString().isEmpty() ? "?" : n.name().getString().substring(0, 1);
                g.drawCenteredString(font, c, x + NODE / 2, y + 7, unlocked ? 0xFFE08A : 0x888888);
            }

            boolean inArea = mouseX >= areaL && mouseX < areaR && mouseY >= areaT && mouseY < areaB;
            if (inArea && mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) hovered = n;
        }

        g.disableScissor();

        g.drawString(font, "ドラッグで移動 / クリックで解放", areaL + 6, top + panelH - 14, 0x666677);

        if (hovered != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(hovered.name().copy().withStyle(ChatFormatting.BOLD));
            tip.addAll(hovered.effects());
            tip.add(Component.literal("コスト: " + hovered.cost() + " SP").withStyle(ChatFormatting.GRAY));
            if (v.isUnlocked(hovered.id())) {
                tip.add(Component.literal("解放済み").withStyle(ChatFormatting.GOLD));
            } else if (!v.isReachable(hovered)) {
                tip.add(Component.literal("つながったマスを先に解放").withStyle(ChatFormatting.DARK_GRAY));
            } else if (v.points() < hovered.cost()) {
                tip.add(Component.literal("ポイントが足りません").withStyle(ChatFormatting.RED));
            } else {
                tip.add(Component.literal("クリックで解放").withStyle(ChatFormatting.GREEN));
            }
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    // ---- 操作 ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || tab == TAB_STATUS) return false;

        Optional<BoardView> board = currentBoard();
        if (board.isEmpty()) return false;
        BoardView v = board.get();

        int areaL = left + 2, areaT = top + 44, areaR = left + panelW - 2, areaB = top + panelH - 20;
        if (mouseX < areaL || mouseX >= areaR || mouseY < areaT || mouseY >= areaB) return false;
        int originX = (areaL + areaR) / 2 + (int) panX;
        int originY = areaT + 14 + (int) panY;

        for (NodeView n : v.nodes()) {
            int x = cx(originX, n) - NODE / 2;
            int y = cy(originY, n) - NODE / 2;
            if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                if (!v.isUnlocked(n.id()) && v.isReachable(n) && v.points() >= n.cost()) {
                    PacketDistributor.sendToServer(new UnlockNodePayload(v.jobId(), n.id()));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (tab != TAB_STATUS && button == 0) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // ---- 小道具 ----

    private static int cx(int originX, NodeView n) {
        return originX + n.x() * GAP_X;
    }

    private static int cy(int originY, NodeView n) {
        return originY + n.y() * GAP_Y;
    }

    @Nullable
    private static NodeView find(BoardView v, String id) {
        for (NodeView n : v.nodes()) if (n.id().equals(id)) return n;
        return null;
    }

    /** 2点をカギ型の線（縦→横→縦）でつなぐ。隣どうしならまっすぐになる。 */
    private static void connect(GuiGraphics g, int ax, int ay, int bx, int by, int color) {
        int midY = (ay + by) / 2;
        vline(g, ax, ay, midY, color);
        hline(g, ax, bx, midY, color);
        vline(g, bx, midY, by, color);
    }

    private static void hline(GuiGraphics g, int x1, int x2, int y, int color) {
        g.fill(Math.min(x1, x2) - 1, y - 1, Math.max(x1, x2) + 1, y + 1, color);
    }

    private static void vline(GuiGraphics g, int x, int y1, int y2, int color) {
        g.fill(x - 1, Math.min(y1, y2) - 1, x + 1, Math.max(y1, y2) + 1, color);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
