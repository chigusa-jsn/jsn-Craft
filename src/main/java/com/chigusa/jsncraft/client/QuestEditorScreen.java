package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.network.QuestEditorPayloads;
import com.chigusa.jsncraft.network.QuestEditorPayloads.JobOption;
import com.chigusa.jsncraft.quest.QuestEditorData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 依頼エディター（OP用）。左に「基本」、右に「目標・報酬」。
 * 目標の [✋] を押すと、手に持っているアイテム（スポーンエッグならそのモンスター）が対象に入る。
 * 報酬の [手持ちを追加] で、手に持っているアイテムをそのまま報酬に登録できる。
 */
public class QuestEditorScreen extends Screen {

    private static final List<String> CATEGORIES = List.of("normal", "job", "special");
    private static final List<String> REPEATS = List.of("once", "daily", "always");
    private static final List<String> OBJ_TYPES = List.of("break_block", "kill_entity", "deliver_item", "reach_level");
    private static final List<String> XP_JOBS = List.of("life", "combat");
    private static final int ROW = 22;

    private final List<JobOption> jobs;
    private final List<String> jobChoices = new ArrayList<>(); // "" = なし

    // 入力中の値（画面サイズが変わって作り直しても消えないように、ここに持つ）
    private final String id;
    private String title, description, category, repeat, requiredJob, minLevel, start, end;
    private final String[] objType = new String[QuestEditorData.MAX_OBJECTIVES];
    private final String[] objTarget = new String[QuestEditorData.MAX_OBJECTIVES];
    private final String[] objCount = new String[QuestEditorData.MAX_OBJECTIVES];
    private String xpJob, xpAmount, rewardTitle;
    private final List<ItemStack> items = new ArrayList<>();

    private final EditBox[] targetBoxes = new EditBox[QuestEditorData.MAX_OBJECTIVES];
    private Component status = Component.empty();
    private int left, top, panelW, colW;

    public QuestEditorScreen(QuestEditorData data, List<JobOption> jobs) {
        super(Component.literal("依頼エディター"));
        this.jobs = jobs;
        jobChoices.add("");
        for (JobOption j : jobs) jobChoices.add(j.id());

        id = data.id();
        title = data.title();
        description = data.description();
        category = data.category();
        repeat = data.repeat();
        requiredJob = data.requiredJob();
        minLevel = data.minLevel() > 0 ? String.valueOf(data.minLevel()) : "";
        start = data.start();
        end = data.end();
        for (int i = 0; i < QuestEditorData.MAX_OBJECTIVES; i++) {
            QuestEditorData.Obj o = i < data.objectives().size() ? data.objectives().get(i) : new QuestEditorData.Obj("break_block", "", 1);
            objType[i] = o.type();
            objTarget[i] = o.target();
            objCount[i] = String.valueOf(o.count());
        }
        xpJob = data.xpJob();
        xpAmount = data.xpAmount() > 0 ? String.valueOf(data.xpAmount()) : "";
        rewardTitle = data.rewardTitle();
        for (ItemStack s : data.items()) items.add(s.copy());
    }

    /** サーバーから保存結果が届いたとき */
    public void onResult(boolean ok, Component message) {
        if (ok) onClose();
        else status = message;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 10, 420);
        colW = (panelW - 18) / 2;
        left = (width - panelW) / 2;
        int H = 22 + ROW * 6 + 34;
        top = Math.max(4, (height - H) / 2);

        int lx = left + 6, rx = left + 12 + colW;
        int y0 = top + 20;
        int half = (colW - 4) / 2;

        // ---- 左：基本 ----
        box(lx, y0, colW, "タイトル", title, 64, v -> title = v);
        box(lx, y0 + ROW, colW, "説明文", description, 256, v -> description = v);
        addRenderableWidget(CycleButton.<String>builder(QuestEditorScreen::categoryName)
                .withValues(CATEGORIES).withInitialValue(category).displayOnlyValue()
                .create(lx, y0 + ROW * 2, half, 20, Component.literal("種類"), (b, v) -> category = v));
        addRenderableWidget(CycleButton.<String>builder(QuestEditorScreen::repeatName)
                .withValues(REPEATS).withInitialValue(repeat).displayOnlyValue()
                .create(lx + half + 4, y0 + ROW * 2, half, 20, Component.literal("繰り返し"), (b, v) -> repeat = v));
        String initialJob = jobChoices.contains(requiredJob) ? requiredJob : "";
        addRenderableWidget(CycleButton.<String>builder(this::jobName)
                .withValues(jobChoices).withInitialValue(initialJob).displayOnlyValue()
                .create(lx, y0 + ROW * 3, colW - 48, 20, Component.literal("職業条件"), (b, v) -> requiredJob = v));
        box(lx + colW - 44, y0 + ROW * 3, 44, "Lv", minLevel, 4, v -> minLevel = v);
        box(lx, y0 + ROW * 4, colW, "開始 例: 2026-12-24 18:00", start, 20, v -> start = v);
        box(lx, y0 + ROW * 5, colW, "終了 例: 2026-12-31", end, 20, v -> end = v);

        // ---- 右：目標 ----
        for (int i = 0; i < QuestEditorData.MAX_OBJECTIVES; i++) {
            final int n = i;
            int y = y0 + ROW * i;
            addRenderableWidget(CycleButton.<String>builder(QuestEditorScreen::objTypeName)
                    .withValues(OBJ_TYPES).withInitialValue(objType[i]).displayOnlyValue()
                    .create(rx, y, 46, 20, Component.literal("目標"), (b, v) -> objType[n] = v));
            int tw = colW - 46 - 36 - 22 - 6;
            targetBoxes[i] = box(rx + 48, y, tw, "対象 (例 minecraft:oak_log)", objTarget[i], 128, v -> objTarget[n] = v);
            box(rx + 50 + tw, y, 34, "数", objCount[i], 7, v -> objCount[n] = v);
            addRenderableWidget(Button.builder(Component.literal("✋"), b -> fillFromHand(n))
                    .bounds(rx + colW - 20, y, 20, 20).build());
        }

        // ---- 右：報酬 ----
        int ry = y0 + ROW * 3;
        addRenderableWidget(CycleButton.<String>builder(QuestEditorScreen::xpJobName)
                .withValues(XP_JOBS).withInitialValue(XP_JOBS.contains(xpJob) ? xpJob : "life").displayOnlyValue()
                .create(rx, ry, 70, 20, Component.literal("経験値"), (b, v) -> xpJob = v));
        box(rx + 72, ry, colW - 72, "経験値の量", xpAmount, 7, v -> xpAmount = v);
        box(rx, ry + ROW, colW, "肩書（もらえる称号）", rewardTitle, 32, v -> rewardTitle = v);
        addRenderableWidget(Button.builder(Component.literal("手持ちを追加"), b -> addHeldItem())
                .bounds(rx, ry + ROW * 2, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("消す"), b -> items.clear())
                .bounds(rx + 72, ry + ROW * 2, 30, 20).build());

        // ---- 下：保存 ----
        int by = y0 + ROW * 6 + 12;
        addRenderableWidget(Button.builder(Component.literal(id.isEmpty() ? "作成する" : "更新する").withStyle(ChatFormatting.GREEN),
                b -> save()).bounds(left + panelW / 2 - 104, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("キャンセル"), b -> onClose())
                .bounds(left + panelW / 2 + 4, by, 100, 20).build());
    }

    private EditBox box(int x, int y, int w, String hint, String value, int maxLen, java.util.function.Consumer<String> onChange) {
        EditBox b = new EditBox(font, x + 1, y + 1, w - 2, 18, Component.literal(hint));
        b.setMaxLength(maxLen);
        b.setValue(value == null ? "" : value);
        b.setHint(Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY));
        b.setResponder(onChange);
        addRenderableWidget(b);
        return b;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int lx = left + 6, rx = left + 12 + colW;
        g.drawString(font, Component.literal(id.isEmpty() ? "新しい依頼" : "依頼の編集  " + id).withStyle(ChatFormatting.AQUA),
                lx, top + 6, 0xFFFFFF);
        g.drawString(font, "目標（✋で手持ちから入力）・報酬", rx, top + 6, 0xFFFF55);
        int y0 = top + 20;

        // 報酬アイテムのアイコン
        int ix = rx + 106, iy = y0 + ROW * 5 + 2;
        for (ItemStack s : items) {
            g.renderItem(s, ix, iy);
            g.renderItemDecorations(font, s, ix, iy);
            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                g.renderTooltip(font, s, mouseX, mouseY);
            }
            ix += 18;
        }
        if (items.isEmpty()) g.drawString(font, "アイテムなし", rx + 106, iy + 4, 0x777777);

        if (!status.getString().isEmpty()) {
            g.drawCenteredString(font, status, left + panelW / 2, y0 + ROW * 6 + 1, 0xFF5555);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int H = 22 + ROW * 6 + 34;
        g.fill(left, top, left + panelW, top + H, 0xE0101018);
        g.fill(left, top, left + panelW, top + 1, 0xFF5A5A78);
        g.fill(left, top + H - 1, left + panelW, top + H, 0xFF5A5A78);
    }

    // ---- 手持ちから入力 ----

    private void fillFromHand(int n) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) {
            status = Component.literal("手に何も持っていません");
            return;
        }
        String target;
        if (objType[n].equals("kill_entity") && held.getItem() instanceof SpawnEggItem egg) {
            EntityType<?> type = egg.getType(held);
            target = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        } else {
            target = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        }
        targetBoxes[n].setValue(target); // responder で objTarget も更新される
    }

    private void addHeldItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) {
            status = Component.literal("報酬にしたいアイテムを手に持ってください");
        } else if (items.size() >= QuestEditorData.MAX_ITEMS) {
            status = Component.literal("報酬アイテムは " + QuestEditorData.MAX_ITEMS + " 種類までです");
        } else {
            items.add(held.copy());
            status = Component.empty();
        }
    }

    // ---- 保存 ----

    private void save() {
        List<QuestEditorData.Obj> objs = new ArrayList<>();
        for (int i = 0; i < QuestEditorData.MAX_OBJECTIVES; i++) {
            objs.add(new QuestEditorData.Obj(objType[i], objTarget[i] == null ? "" : objTarget[i], parse(objCount[i], 1)));
        }
        QuestEditorData data = new QuestEditorData(id, title, description, category, repeat,
                requiredJob == null ? "" : requiredJob, parse(minLevel, 0), start, end, objs,
                xpJob, parse(xpAmount, 0), rewardTitle, List.copyOf(items));
        status = Component.literal("保存中…").withStyle(ChatFormatting.GRAY);
        PacketDistributor.sendToServer(new QuestEditorPayloads.Save(data));
    }

    private static int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s == null ? "" : s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- 表示名 ----

    private static Component categoryName(String v) {
        return switch (v) {
            case "job" -> Component.literal("職業依頼").withStyle(ChatFormatting.GREEN);
            case "special" -> Component.literal("特別依頼").withStyle(ChatFormatting.GOLD);
            default -> Component.literal("通常依頼");
        };
    }

    private static Component repeatName(String v) {
        return Component.literal(switch (v) {
            case "daily" -> "1日1回";
            case "always" -> "何度でも";
            default -> "1回のみ";
        });
    }

    private Component jobName(String v) {
        if (v.isEmpty()) return Component.literal("職業条件なし").withStyle(ChatFormatting.GRAY);
        for (JobOption j : jobs) if (j.id().equals(v)) return j.name();
        return Component.literal(v);
    }

    private static Component objTypeName(String v) {
        return Component.literal(switch (v) {
            case "kill_entity" -> "倒す";
            case "deliver_item" -> "納品";
            case "reach_level" -> "Lv";
            default -> "壊す";
        });
    }

    private static Component xpJobName(String v) {
        return Component.literal(v.equals("combat") ? "戦闘経験値" : "生活経験値");
    }
}
