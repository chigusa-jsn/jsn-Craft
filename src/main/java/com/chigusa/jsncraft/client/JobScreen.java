package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.network.BoardSnapshotPayload;
import com.chigusa.jsncraft.network.BoardSnapshotPayload.BoardView;
import com.chigusa.jsncraft.network.BoardSnapshotPayload.NodeView;
import com.chigusa.jsncraft.network.BoardSnapshotPayload.StatLine;
import com.chigusa.jsncraft.network.ClientJobCache;
import com.chigusa.jsncraft.network.JobSyncPayload;
import com.chigusa.jsncraft.network.QuestActionPayload;
import com.chigusa.jsncraft.network.QuestListPayload;
import com.chigusa.jsncraft.network.QuestListPayload.ObjectiveView;
import com.chigusa.jsncraft.network.QuestListPayload.QuestView;
import com.chigusa.jsncraft.network.UnlockNodePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 職業メニュー。タブで「ステータス」「戦闘ボード」「生活ボード」「依頼」を切り替える。
 * ボードはドラッグで動かせる（盤面型の大きなボードにも対応できるように）。
 */
public class JobScreen extends Screen {

    private static final int TAB_STATUS = 0, TAB_COMBAT = 1, TAB_LIFE = 2, TAB_QUEST = 3, TAB_SKILL = 4;
    private static final int LIST_W = 120, ROW_H = 12;
    private static final int NODE = 22, GAP_X = 38, GAP_Y = 34;

    private static int lastTab = TAB_STATUS;

    public static void startOnTab(int tab) {
        if (tab >= TAB_STATUS && tab <= TAB_SKILL) lastTab = tab;
    }

    private BoardSnapshotPayload data;
    private int tab = lastTab;
    private int boardIndex = 0;
    private Button prevBtn, nextBtn;
    private Button acceptBtn, turnInBtn, abandonBtn, claimBtn;
    private Button titleBtn;
    private boolean titleMenu;
    @Nullable private String selectedQuest;
    /** 選択報酬で選んだ候補の番号（どの依頼のものか picksFor で覚える） */
    private final java.util.Set<Integer> picks = new java.util.LinkedHashSet<>();
    @Nullable private String picksFor;
    private int choiceX = -1000, choiceY = -1000;
    private int questScroll;
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

        int tabW = (panelW - 24) / 5;
        addRenderableWidget(Button.builder(Component.literal("ステータス"), b -> setTab(TAB_STATUS))
                .bounds(left + 4, top + 4, tabW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("戦闘ボード").withStyle(ChatFormatting.RED), b -> setTab(TAB_COMBAT))
                .bounds(left + 8 + tabW, top + 4, tabW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("生活ボード").withStyle(ChatFormatting.GREEN), b -> setTab(TAB_LIFE))
                .bounds(left + 12 + tabW * 2, top + 4, tabW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("依頼").withStyle(ChatFormatting.YELLOW), b -> setTab(TAB_QUEST))
                .bounds(left + 16 + tabW * 3, top + 4, tabW, 18).build());
        addRenderableWidget(Button.builder(Component.literal("スキル").withStyle(ChatFormatting.LIGHT_PURPLE), b -> setTab(TAB_SKILL))
                .bounds(left + 20 + tabW * 4, top + 4, tabW, 18).build());

        // 肩書の変更（ステータスタブ）
        titleBtn = addRenderableWidget(Button.builder(Component.literal("肩書を変更"), b -> titleMenu = !titleMenu)
                .bounds(left + panelW - 80, top + 32, 70, 14).build());

        // 依頼のボタン（詳細の下）
        int qx = left + LIST_W + 14, qy = top + panelH - 22;
        acceptBtn = addRenderableWidget(Button.builder(Component.literal("受ける"), b -> questAction(QuestActionPayload.ACCEPT))
                .bounds(qx, qy, 60, 16).build());
        turnInBtn = addRenderableWidget(Button.builder(Component.literal("報告する"), b -> questAction(QuestActionPayload.TURN_IN))
                .bounds(qx, qy, 60, 16).build());
        claimBtn = addRenderableWidget(Button.builder(Component.literal("報酬を受け取る"), b -> questAction(QuestActionPayload.CLAIM))
                .bounds(qx, qy, 84, 16).build());
        abandonBtn = addRenderableWidget(Button.builder(Component.literal("あきらめる"), b -> questAction(QuestActionPayload.ABANDON))
                .bounds(left + panelW - 72, qy, 64, 16).build());

        // 引き継いだボード（基本職のボードなど）との切り替え
        int by = top + panelH - 17;
        prevBtn = addRenderableWidget(Button.builder(Component.literal("◀"), b -> boardIndex--)
                .bounds(left + panelW - 62, by, 16, 14).build());
        nextBtn = addRenderableWidget(Button.builder(Component.literal("▶"), b -> boardIndex++)
                .bounds(left + panelW - 20, by, 16, 14).build());
    }

    private List<BoardView> boards() {
        if (tab == TAB_COMBAT) return data.combat();
        if (tab == TAB_LIFE) return data.life();
        return List.of();
    }

    private boolean isBoardTab() {
        return tab == TAB_COMBAT || tab == TAB_LIFE;
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
        titleBtn.visible = tab == TAB_STATUS;
        if (tab != TAB_STATUS) titleMenu = false;
        boolean multi = isBoardTab() && boards().size() > 1;
        QuestView selected = selectedQuest();
        QuestListPayload ql = QuestListPayload.client;
        boolean questTab = tab == TAB_QUEST && selected != null;
        acceptBtn.visible = questTab && selected.isAvailable();
        acceptBtn.active = ql.atReceptionist() && ql.activeCount() < ql.maxActive();
        turnInBtn.visible = questTab && selected.isActive();
        turnInBtn.active = selected != null && selected.canTurnIn();
        abandonBtn.visible = questTab && selected.isActive();
        claimBtn.visible = questTab && selected.isReported();
        if (selected == null || !selected.id().equals(picksFor)) {
            picks.clear();
            picksFor = selected == null ? null : selected.id();
        }
        claimBtn.active = ql.atReceptionist() && selected != null && picks.size() == selected.pickCount();
        currentBoard();
        prevBtn.visible = multi;
        nextBtn.visible = multi;
        prevBtn.active = boardIndex > 0;
        nextBtn.active = boardIndex < boards().size() - 1;

        super.render(g, mouseX, mouseY, partialTick); // 背景・パネル・ボタン

        if (tab == TAB_STATUS) {
            renderStatus(g);
            if (titleMenu) renderTitleMenu(g, mouseX, mouseY);
        } else if (tab == TAB_SKILL) {
            renderSkills(g, mouseX, mouseY);
        } else if (tab == TAB_QUEST) {
            renderQuests(g, mouseX, mouseY);
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
        // 名前と肩書
        Component name = mc.player != null ? mc.player.getName().copy().withStyle(ChatFormatting.BOLD) : Component.empty();
        String title = own != null ? own.title() : "";
        Component titleText = title.isEmpty()
                ? Component.literal("肩書なし").withStyle(ChatFormatting.DARK_GRAY)
                : Component.literal("《" + title + "》").withStyle(ChatFormatting.GOLD);
        g.drawString(font, Component.empty().append(name).append("  ").append(titleText), x, y + 3, 0xFFFFFF);
        y += 16;
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

    // ---- 肩書の一覧（ステータスタブの「肩書を変更」） ----

    private List<String> titleOptions() {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(data.titles());
        return out;
    }

    private int titleMenuX() {
        return left + panelW - 172;
    }

    private void renderTitleMenu(GuiGraphics g, int mouseX, int mouseY) {
        int x = titleMenuX(), y = top + 48, w = 162;
        List<String> opts = titleOptions();
        int h = opts.size() * 12 + 4 + (data.titles().isEmpty() ? 12 : 0);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF8888AA);
        g.fill(x, y, x + w, y + h, 0xF0181828);
        int ry = y + 2;
        for (String t : opts) {
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + 12;
            if (hover) g.fill(x, ry, x + w, ry + 12, 0x40FFFFFF);
            Component label = t.isEmpty() ? Component.literal("（肩書をはずす）").withStyle(ChatFormatting.GRAY)
                    : Component.literal("《" + t + "》").withStyle(ChatFormatting.GOLD);
            g.drawString(font, label, x + 4, ry + 2, 0xFFFFFF);
            ry += 12;
        }
        if (data.titles().isEmpty()) {
            g.drawString(font, "依頼で肩書を手に入れよう", x + 4, ry + 2, 0x888899);
        }
        g.pose().popPose();
    }

    /** 肩書の一覧が開いているときのクリック。一覧の外を押したら閉じる。 */
    private boolean clickTitleMenu(double mouseX, double mouseY) {
        int x = titleMenuX(), y = top + 48, w = 162;
        int ry = y + 2;
        for (String t : titleOptions()) {
            if (mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + 12) {
                PacketDistributor.sendToServer(new com.chigusa.jsncraft.network.SetTitlePayload(t));
                titleMenu = false;
                return true;
            }
            ry += 12;
        }
        if (titleBtn.isMouseOver(mouseX, mouseY)) return false; // ボタン自身は普通に押させる
        titleMenu = false;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (titleMenu && clickTitleMenu(mouseX, mouseY)) return true;
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && tab == TAB_QUEST) return clickChoice(mouseX, mouseY) || clickQuestList(mouseX, mouseY);
        if (tab == TAB_SKILL) return clickSkills(mouseX, mouseY, button);
        if (button != 0 || !isBoardTab()) return false;

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
        if (isBoardTab() && button == 0) {
            panX += dragX;
            panY += dragY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // ---- 依頼タブ ----

    private List<QuestView> quests() {
        return QuestListPayload.client.quests();
    }

    @Nullable
    private QuestView selectedQuest() {
        List<QuestView> list = quests();
        if (list.isEmpty()) return null;
        for (QuestView q : list) if (q.id().equals(selectedQuest)) return q;
        selectedQuest = list.get(0).id(); // 見つからなければ先頭を選ぶ
        return list.get(0);
    }

    private void questAction(int action) {
        QuestView q = selectedQuest();
        if (q == null) return;
        List<Integer> chosen = action == QuestActionPayload.CLAIM ? new ArrayList<>(picks) : List.of();
        PacketDistributor.sendToServer(new QuestActionPayload(q.id(), action, chosen));
    }

    /** 選択報酬のアイコンをクリック：受付嬢のそばで、報酬待ちの依頼なら選ぶ／外す。 */
    private boolean clickChoice(double mouseX, double mouseY) {
        QuestView q = selectedQuest();
        if (q == null || q.choices().isEmpty()) return false;
        for (int i = 0; i < q.choices().size(); i++) {
            int ix = choiceX + i * 20;
            if (mouseX >= ix && mouseX < ix + 18 && mouseY >= choiceY && mouseY < choiceY + 18) {
                if (!q.isReported() || !QuestListPayload.client.atReceptionist()) return true;
                if (!picks.remove(i)) {
                    if (q.pickCount() == 1) picks.clear(); // 1つだけなら選び直し
                    if (picks.size() < q.pickCount()) picks.add(i);
                }
                return true;
            }
        }
        return false;
    }

    private static int categoryColor(String category) {
        return switch (category) {
            case "special" -> 0xFFAA00;
            case "job" -> 0x55FF55;
            default -> 0xFFFFFF;
        };
    }

    private static String categoryMark(String category) {
        return switch (category) {
            case "special" -> "[特別] ";
            case "job" -> "[職業] ";
            default -> "[通常] ";
        };
    }

    /** 一覧の行の位置（見出しの分をずらす）。-1 なら見出し。 */
    private record Row(int y, @Nullable QuestView quest, @Nullable String header) {}

    private List<Row> questRows() {
        List<Row> rows = new ArrayList<>();
        int y = top + 32 - questScroll;
        QuestListPayload d = QuestListPayload.client;
        boolean anyReported = false;
        for (QuestView q : quests()) {
            if (!q.isReported()) continue;
            if (!anyReported) { rows.add(new Row(y, null, "報酬受け取り待ち")); y += ROW_H; anyReported = true; }
            rows.add(new Row(y, q, null));
            y += ROW_H;
        }
        if (anyReported) y += 4;
        rows.add(new Row(y, null, "受注中 " + d.activeCount() + "/" + d.maxActive()));
        y += ROW_H;
        boolean any = false;
        for (QuestView q : quests()) if (q.isActive()) { rows.add(new Row(y, q, null)); y += ROW_H; any = true; }
        if (!any) { rows.add(new Row(y, null, "  なし")); y += ROW_H; }
        y += 4;
        rows.add(new Row(y, null, "受けられる依頼"));
        y += ROW_H;
        any = false;
        for (QuestView q : quests()) if (q.isAvailable()) { rows.add(new Row(y, q, null)); y += ROW_H; any = true; }
        if (!any) rows.add(new Row(y, null, "  なし"));
        return rows;
    }

    private void renderQuests(GuiGraphics g, int mouseX, int mouseY) {
        int listL = left + 6, listT = top + 30, listR = left + 6 + LIST_W, listB = top + panelH - 4;
        g.fill(listR + 3, listT, listR + 4, listB, 0xFF5A5A78);

        // 一覧
        g.enableScissor(listL, listT, listR, listB);
        QuestView selected = selectedQuest();
        for (Row r : questRows()) {
            if (r.header() != null) {
                g.drawString(font, r.header(), listL, r.y(), 0x8888AA);
                continue;
            }
            QuestView q = r.quest();
            boolean sel = selected != null && q.id().equals(selected.id());
            boolean hover = mouseX >= listL && mouseX < listR && mouseY >= r.y() - 1 && mouseY < r.y() + ROW_H - 1;
            if (sel) g.fill(listL, r.y() - 2, listR, r.y() + ROW_H - 2, 0x60FFFFFF);
            else if (hover) g.fill(listL, r.y() - 2, listR, r.y() + ROW_H - 2, 0x30FFFFFF);
            String mark = q.isReported() ? "★ " : q.canTurnIn() ? "✔ " : "";
            int color = q.isReported() ? 0xFFD700 : q.canTurnIn() ? 0x55FF55 : categoryColor(q.category());
            String text = font.plainSubstrByWidth(mark + q.title().getString(), LIST_W - 6);
            g.drawString(font, text, listL + 3, r.y(), color);
        }
        g.disableScissor();

        // 詳細
        int x = listR + 10, y = top + 32, w = left + panelW - 8 - x;
        if (selected == null) {
            g.drawString(font, "今受けられる依頼はありません", x, y, 0xAAAAAA);
            return;
        }
        g.drawString(font, Component.literal(categoryMark(selected.category())).withStyle(s -> s.withColor(categoryColor(selected.category())))
                .append(selected.title().copy().withStyle(ChatFormatting.BOLD)), x, y, 0xFFFFFF);
        y += 11;
        g.drawString(font, selected.period(), x, y, 0x888899);
        y += 12;
        for (FormattedCharSequence line : font.split(selected.description(), w)) {
            g.drawString(font, line, x, y, 0xCCCCCC);
            y += 10;
        }
        y += 4;
        g.drawString(font, "目標", x, y, 0xFFFF55);
        y += 11;
        for (ObjectiveView o : selected.objectives()) {
            String count = " " + o.progress() + "/" + o.count();
            Component line = Component.literal(o.done() ? "✔ " : "・").append(o.label().copy()).append(count);
            for (FormattedCharSequence part : font.split(line, w)) {
                g.drawString(font, part, x + 2, y, o.done() ? 0x55FF55 : 0xFFFFFF);
                y += 10;
            }
        }
        y += 4;
        if (!selected.rewards().isEmpty()) {
            g.drawString(font, "報酬", x, y, 0xFFFF55);
            y += 11;
            for (Component r : selected.rewards()) {
                g.drawString(font, Component.literal("・").append(r.copy()), x + 2, y, 0xFFFFFF);
                y += 10;
            }
        }

        // 選択報酬
        choiceX = -1000;
        ItemStack hoveredChoice = null;
        if (!selected.choices().isEmpty()) {
            y += 2;
            g.drawString(font, "選択報酬（" + selected.choices().size() + "つの中から" + selected.pickCount() + "つ）", x, y, 0xFFFF55);
            y += 11;
            choiceX = x + 2;
            choiceY = y;
            boolean choosing = selected.isReported() && QuestListPayload.client.atReceptionist();
            for (int i = 0; i < selected.choices().size(); i++) {
                ItemStack c = selected.choices().get(i);
                int ix = choiceX + i * 20;
                boolean picked = picks.contains(i);
                g.fill(ix - 1, y - 1, ix + 19, y + 19, picked ? 0xFFFFC14D : choosing ? 0xFF55AA77 : 0xFF3A3A44);
                g.fill(ix, y, ix + 18, y + 18, 0xFF1A1A22);
                if (selected.hidden()) {
                    g.drawCenteredString(font, "?", ix + 9, y + 5, 0xFFD27FFF);
                    if (c.getCount() > 1) g.drawString(font, String.valueOf(c.getCount()), ix + 17 - font.width(String.valueOf(c.getCount())), y + 10, 0xFFFFFF);
                } else {
                    g.renderItem(c, ix + 1, y + 1);
                    g.renderItemDecorations(font, c, ix + 1, y + 1);
                }
                if (mouseX >= ix && mouseX < ix + 18 && mouseY >= y && mouseY < y + 18) hoveredChoice = c;
            }
            if (choosing) {
                g.drawString(font, picks.size() + "/" + selected.pickCount(), choiceX + selected.choices().size() * 20 + 4, y + 5,
                        picks.size() == selected.pickCount() ? 0x55FF55 : 0xFFFF55);
            }
            y += 22;
        }

        // 受付嬢から離れているときの案内（ボタンの横）
        boolean away = !QuestListPayload.client.atReceptionist();
        String hint = null;
        if (selected.isAvailable() && away) hint = "受付嬢から受けられます";
        else if (selected.isReported() && away) hint = "受付嬢から受け取れます";
        if (hint == null && selected.isReported() && !away && picks.size() < selected.pickCount()) {
            hint = "選択報酬を選んでね";
        }
        if (hint != null) {
            int hx = left + LIST_W + 14 + (selected.isReported() ? 88 : 64);
            g.drawString(font, hint, hx, top + panelH - 18, 0xAAAAAA);
        }
        if (hoveredChoice != null) {
            List<Component> tip = selected.hidden()
                    ? new ArrayList<>(List.of(Component.literal("？？？？？ ×" + hoveredChoice.getCount()).withStyle(ChatFormatting.LIGHT_PURPLE)))
                    : new ArrayList<>(Screen.getTooltipFromItem(minecraft, hoveredChoice));
            if (selected.isReported() && !away) tip.add(Component.literal("クリックで選ぶ").withStyle(ChatFormatting.GREEN));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    private boolean clickQuestList(double mouseX, double mouseY) {
        int listL = left + 6, listT = top + 30, listR = left + 6 + LIST_W, listB = top + panelH - 4;
        if (mouseX < listL || mouseX >= listR || mouseY < listT || mouseY >= listB) return false;
        for (Row r : questRows()) {
            if (r.quest() != null && mouseY >= r.y() - 2 && mouseY < r.y() + ROW_H - 2) {
                selectedQuest = r.quest().id();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == TAB_SKILL) {
            int max = Math.max(0, ClientSkills.state().skills().size() * SKILL_ROW - (panelH - 92));
            skillScroll = (int) Math.max(0, Math.min(max, skillScroll - scrollY * SKILL_ROW));
            return true;
        }
        if (tab == TAB_QUEST) {
            int rows = questRows().size();
            int max = Math.max(0, rows * ROW_H + 10 - (panelH - 34));
            questScroll = (int) Math.max(0, Math.min(max, questScroll - scrollY * ROW_H));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---- スキルタブ ----

    private static final int SKILL_ROW = 22;
    @Nullable private String pickedSkill;
    private int skillScroll;

    private int skillSlotX(int i) {
        return left + panelW / 2 - (ClientSkills.SLOTS * 26) / 2 + i * 26;
    }

    private void renderSkills(GuiGraphics g, int mouseX, int mouseY) {
        int slotY = top + 44;
        g.drawString(font, "スキルバー（Rでスキルモード → 1〜5で使う）", left + 10, top + 32, 0xFFFF55);
        List<Component> tip = null;
        for (int i = 0; i < ClientSkills.SLOTS; i++) {
            int x = skillSlotX(i);
            boolean hover = mouseX >= x && mouseX < x + 24 && mouseY >= slotY && mouseY < slotY + 24;
            g.fill(x - 1, slotY - 1, x + 25, slotY + 25, pickedSkill != null && hover ? 0xFF55DD77 : 0xFF5A5A78);
            g.fill(x, slotY, x + 24, slotY + 24, 0xFF1A1A2A);
            g.drawString(font, String.valueOf(i + 1), x + 2, slotY + 1, 0xFFFF55);
            var v = ClientSkills.find(ClientSkills.slot(i));
            if (v != null) {
                g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(v.icon())), x + 4, slotY + 5);
                if (hover) tip = List.of(v.name(), Component.literal("右クリックで外す").withStyle(ChatFormatting.GRAY));
            }
        }

        // スキル一覧
        int listT = slotY + 32, listB = top + panelH - 14;
        g.drawString(font, pickedSkill == null ? "アクティブスキルをクリック → 上の枠をクリックでセット"
                : "セットする枠をクリックしてね（もう一度スキルを押すとやめる）", left + 10, listT - 2, pickedSkill == null ? 0x888899 : 0x55FF55);
        listT += 10;
        g.enableScissor(left + 2, listT, left + panelW - 2, listB);
        int y = listT - skillScroll;
        for (var v : ClientSkills.state().skills()) {
            boolean hover = mouseX >= left + 6 && mouseX < left + panelW - 6 && mouseY >= y && mouseY < y + SKILL_ROW - 2
                    && mouseY >= listT && mouseY < listB;
            boolean picked = v.id().equals(pickedSkill);
            int bg = picked ? 0xFF3A5A3A : hover ? 0xFF2A2A40 : 0xFF181826;
            g.fill(left + 6, y, left + panelW - 6, y + SKILL_ROW - 2, bg);
            g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(v.icon())), left + 9, y + 2);
            if (!v.learned()) g.fill(left + 9, y + 2, left + 25, y + 18, 0xA0000000);
            int nameColor = !v.learned() ? 0x777777 : v.active() ? 0xFFFFFF : 0xAADDFF;
            g.drawString(font, v.name(), left + 30, y + 2, nameColor);
            String kind = v.active() ? "アクティブ" : "パッシブ";
            String info = v.learned() ? kind : "Lv." + v.level() + " で習得";
            g.drawString(font, Component.literal(info).withStyle(v.learned() ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_GRAY),
                    left + 30, y + 11, 0xFFFFFF);
            if (v.active()) {
                StringBuilder cost = new StringBuilder();
                if (v.stamina() > 0) cost.append("ST ").append((int) v.stamina()).append("  ");
                if (v.mana() > 0) cost.append("MP ").append((int) v.mana()).append("  ");
                if (v.cooldown() > 0) cost.append("CT ").append(fmtSec(v.cooldown())).append("秒");
                String c = cost.toString().trim();
                g.drawString(font, c, left + panelW - 10 - font.width(c), y + 6, 0xAAAAAA);
            }
            if (hover) {
                List<Component> t = new ArrayList<>();
                t.add(v.name().copy().withStyle(ChatFormatting.BOLD));
                t.add(Component.empty().append(v.jobName()).append(Component.literal(" Lv." + v.level()).withStyle(ChatFormatting.GRAY)));
                for (var line : font.split(v.description(), 200)) t.add(Component.literal(toStr(line)));
                if (!v.learned()) t.add(Component.literal("まだ覚えていません").withStyle(ChatFormatting.RED));
                else if (v.active()) t.add(Component.literal("クリックでスキルバーにセット").withStyle(ChatFormatting.GREEN));
                else t.add(Component.literal("覚えている間、常に効果があります").withStyle(ChatFormatting.AQUA));
                tip = t;
            }
            y += SKILL_ROW;
        }
        g.disableScissor();
        if (ClientSkills.state().skills().isEmpty()) {
            g.drawCenteredString(font, "今の職業にはスキルがありません", left + panelW / 2, listT + 20, 0xAAAAAA);
        }
        if (tip != null) g.renderComponentTooltip(font, tip, mouseX, mouseY);
    }

    private static String toStr(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    private static String fmtSec(float s) {
        return s == Math.floor(s) ? String.valueOf((int) s) : String.format("%.1f", s);
    }

    private boolean clickSkills(double mouseX, double mouseY, int button) {
        int slotY = top + 44;
        for (int i = 0; i < ClientSkills.SLOTS; i++) {
            int x = skillSlotX(i);
            if (mouseX >= x && mouseX < x + 24 && mouseY >= slotY && mouseY < slotY + 24) {
                if (button == 1) {
                    PacketDistributor.sendToServer(new com.chigusa.jsncraft.network.SkillPayloads.Assign(i, ""));
                } else if (pickedSkill != null) {
                    PacketDistributor.sendToServer(new com.chigusa.jsncraft.network.SkillPayloads.Assign(i, pickedSkill));
                    pickedSkill = null;
                }
                return true;
            }
        }
        if (button != 0) return false;
        int listT = slotY + 32 + 10, listB = top + panelH - 14;
        if (mouseY < listT || mouseY >= listB) return false;
        int y = listT - skillScroll;
        for (var v : ClientSkills.state().skills()) {
            if (mouseX >= left + 6 && mouseX < left + panelW - 6 && mouseY >= y && mouseY < y + SKILL_ROW - 2) {
                if (v.active() && v.learned()) pickedSkill = v.id().equals(pickedSkill) ? null : v.id();
                return true;
            }
            y += SKILL_ROW;
        }
        return false;
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
