package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.network.QuestEditorPayloads.JobOption;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * アイコンの一覧から選ぶ画面。上の検索欄で絞り込める。
 * BLOCK（壊す）/ MOB（倒す）/ ITEM（納品）/ REWARD（報酬アイテム・個数つき）/ JOB（職業）
 */
public class PickerScreen extends Screen {

    public enum Kind { BLOCK, MOB, ITEM, REWARD, JOB }

    /** 一覧の1つ。id は目標の対象として入る文字（タグなら #付き）、stack は報酬で渡すアイテム。 */
    public record Entry(String id, Component name, ItemStack icon, ItemStack stack) {}

    private static final int CELL = 18;

    private final Screen parent;
    private final Kind kind;
    private final List<Entry> all;
    private final BiConsumer<Entry, Integer> onPick;
    private List<Entry> shown;
    private String query = "";
    private String countText = "1";
    private int scroll;
    private int left, top, w, h, cols, gridTop, gridRows;

    public PickerScreen(Screen parent, Kind kind, List<JobOption> jobs, BiConsumer<Entry, Integer> onPick) {
        super(Component.literal("選ぶ"));
        this.parent = parent;
        this.kind = kind;
        this.onPick = onPick;
        this.all = build(kind, jobs);
        this.shown = all;
    }

    // ---- 候補を作る ----

    private static List<Entry> build(Kind kind, List<JobOption> jobs) {
        List<Entry> out = new ArrayList<>();
        switch (kind) {
            case BLOCK -> {
                tag(out, "#minecraft:logs", "原木（全種類）", Items.OAK_LOG);
                tag(out, "#minecraft:leaves", "葉（全種類）", Items.OAK_LEAVES);
                tag(out, "#c:ores", "鉱石（全種類）", Items.IRON_ORE);
                tag(out, "#minecraft:crops", "作物（全種類）", Items.WHEAT);
                tag(out, "#minecraft:base_stone_overworld", "石（全種類）", Items.STONE);
                tag(out, "#minecraft:dirt", "土（全種類）", Items.DIRT);
                tag(out, "#minecraft:sand", "砂（全種類）", Items.SAND);
                for (Block b : BuiltInRegistries.BLOCK) {
                    Item item = b.asItem();
                    if (item == Items.AIR) continue;
                    out.add(new Entry(BuiltInRegistries.BLOCK.getKey(b).toString(), b.getName(), new ItemStack(item), ItemStack.EMPTY));
                }
            }
            case MOB -> {
                tag(out, "*", "モンスター（何でも）", Items.ZOMBIE_HEAD);
                for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                    SpawnEggItem egg = SpawnEggItem.byId(type);
                    boolean mob = type.getCategory() != MobCategory.MISC || egg != null;
                    if (!mob || type == EntityType.PLAYER) continue;
                    ItemStack icon = egg != null ? new ItemStack(egg) : ItemStack.EMPTY;
                    out.add(new Entry(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), type.getDescription(), icon, ItemStack.EMPTY));
                }
            }
            case ITEM -> {
                tag(out, "#minecraft:logs", "原木（全種類）", Items.OAK_LOG);
                tag(out, "#minecraft:planks", "木材（全種類）", Items.OAK_PLANKS);
                tag(out, "#minecraft:wool", "羊毛（全色）", Items.WHITE_WOOL);
                tag(out, "#minecraft:fishes", "魚（全種類）", Items.COD);
                tag(out, "#c:ingots", "インゴット（全種類）", Items.IRON_INGOT);
                tag(out, "#c:gems", "宝石（全種類）", Items.DIAMOND);
                Map<String, Entry> unique = new LinkedHashMap<>();
                for (ItemStack s : creativeItems()) {
                    String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                    unique.putIfAbsent(id, new Entry(id, s.getItem().getDescription(), new ItemStack(s.getItem()), ItemStack.EMPTY));
                }
                out.addAll(unique.values());
            }
            case REWARD -> {
                for (ItemStack s : creativeItems()) {
                    out.add(new Entry(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getHoverName(), s, s));
                }
            }
            case JOB -> {
                for (JobOption j : jobs) out.add(new Entry(j.id(), j.name(), ItemStack.EMPTY, ItemStack.EMPTY));
            }
        }
        return out;
    }

    private static void tag(List<Entry> out, String id, String label, Item icon) {
        out.add(new Entry(id, Component.literal(label).withStyle(ChatFormatting.AQUA), new ItemStack(icon), ItemStack.EMPTY));
    }

    /** クリエイティブの検索タブと同じ並び（エンチャントの本やポーションの種類も入る）。 */
    private static List<ItemStack> creativeItems() {
        Minecraft mc = Minecraft.getInstance();
        List<ItemStack> list = new ArrayList<>();
        try {
            if (mc.player != null && mc.level != null) {
                CreativeModeTabs.tryRebuildTabContents(mc.player.connection.enabledFeatures(), true, mc.level.registryAccess());
                list.addAll(CreativeModeTabs.searchTab().getDisplayItems());
            }
        } catch (RuntimeException ignored) {
            // うまく取れなかったら、登録されている全アイテムで代わりにする
        }
        if (list.isEmpty()) {
            for (Item item : BuiltInRegistries.ITEM) if (item != Items.AIR) list.add(new ItemStack(item));
        }
        return list;
    }

    // ---- 画面 ----

    @Override
    protected void init() {
        w = Math.min(width - 20, 340);
        h = Math.min(height - 20, 240);
        left = (width - w) / 2;
        top = (height - h) / 2;

        int searchW = kind == Kind.REWARD ? w - 76 : w - 12;
        EditBox search = new EditBox(font, left + 6, top + 18, searchW, 16, Component.literal("検索"));
        search.setHint(Component.literal("検索（名前やID）").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(query);
        search.setResponder(v -> {
            query = v;
            scroll = 0;
            filter();
        });
        addRenderableWidget(search);
        setInitialFocus(search);

        if (kind == Kind.REWARD) {
            EditBox count = new EditBox(font, left + w - 66, top + 18, 60, 16, Component.literal("個数"));
            count.setHint(Component.literal("個数").withStyle(ChatFormatting.DARK_GRAY));
            count.setValue(countText);
            count.setMaxLength(4);
            count.setResponder(v -> countText = v);
            addRenderableWidget(count);
        }

        addRenderableWidget(Button.builder(Component.literal("もどる"), b -> onClose())
                .bounds(left + w - 66, top + h - 22, 60, 18).build());

        gridTop = top + 40;
        cols = Math.max(1, (w - 12) / CELL);
        gridRows = Math.max(1, (h - 40 - 26) / CELL);
        filter();
    }

    private void filter() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            shown = all;
            return;
        }
        List<Entry> out = new ArrayList<>();
        for (Entry e : all) {
            if (e.name().getString().toLowerCase(Locale.ROOT).contains(q) || e.id().toLowerCase(Locale.ROOT).contains(q)) out.add(e);
        }
        shown = out;
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
        g.drawString(font, title(), left + 6, top + 6, 0xFFFF55);

        Entry hovered = null;
        int start = scroll * cols;
        int gx = left + 6;
        for (int i = 0; i < cols * gridRows && start + i < shown.size(); i++) {
            Entry e = shown.get(start + i);
            int x = gx + (i % cols) * CELL, y = gridTop + (i / cols) * CELL;
            boolean hover = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            g.fill(x, y, x + CELL - 1, y + CELL - 1, hover ? 0xFF4A4A6A : 0xFF22222E);
            if (!e.icon().isEmpty()) {
                g.renderItem(e.icon(), x + 1, y + 1);
                if (kind == Kind.REWARD) g.renderItemDecorations(font, e.icon(), x + 1, y + 1);
            } else {
                String c = e.name().getString().isEmpty() ? "?" : e.name().getString().substring(0, 1);
                g.drawCenteredString(font, c, x + CELL / 2, y + 5, 0xDDDDDD);
            }
            if (hover) hovered = e;
        }

        int pages = Math.max(1, (shown.size() + cols - 1) / cols - gridRows + 1);
        g.drawString(font, shown.size() + " 件" + (pages > 1 ? "（ホイールでスクロール）" : ""), left + 6, top + h - 17, 0x888899);

        if (hovered != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(hovered.name());
            tip.add(Component.literal(hovered.id()).withStyle(ChatFormatting.DARK_GRAY));
            tip.add(Component.literal("クリックで選ぶ").withStyle(ChatFormatting.GREEN));
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    private Component title() {
        return Component.literal(switch (kind) {
            case BLOCK -> "壊すブロックを選ぶ";
            case MOB -> "倒すモブを選ぶ";
            case ITEM -> "納品するアイテムを選ぶ";
            case REWARD -> "報酬のアイテムを選ぶ（個数は右上）";
            case JOB -> "職業を選ぶ";
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int gx = left + 6;
        int col = (int) ((mouseX - gx) / CELL), row = (int) ((mouseY - gridTop) / CELL);
        if (mouseX < gx || mouseY < gridTop || col >= cols || row >= gridRows) return false;
        int index = scroll * cols + row * cols + col;
        if (index < 0 || index >= shown.size()) return false;
        int count;
        try {
            count = Math.max(1, Math.min(9999, Integer.parseInt(countText.trim())));
        } catch (NumberFormatException e) {
            count = 1;
        }
        onPick.accept(shown.get(index), count);
        onClose();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalRows = (shown.size() + cols - 1) / cols;
        int max = Math.max(0, totalRows - gridRows);
        scroll = (int) Math.max(0, Math.min(max, scroll - Math.signum(scrollY) * 2));
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 目標の対象IDから、画面に出す名前を作る（エディターのボタン用）。type は目標の種類。 */
    public static Component nameOf(String type, String id, List<JobOption> jobs) {
        if (id == null || id.isBlank()) return Component.literal("対象を選ぶ").withStyle(ChatFormatting.GRAY);
        if (id.equals("*")) return Component.literal(type.equals("kill_entity") ? "モンスター（何でも）" : "何でも");
        if (id.startsWith("#")) {
            String label = TAG_NAMES.get(id);
            return Component.literal(label != null ? label : id).withStyle(ChatFormatting.AQUA);
        }
        if (type.equals("reach_level")) {
            for (JobOption j : jobs) if (j.id().equals(id)) return j.name();
            return Component.literal(id);
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return Component.literal(id);
        if (type.equals("kill_entity") && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            return BuiltInRegistries.ENTITY_TYPE.get(rl).getDescription();
        }
        if (type.equals("break_block") && BuiltInRegistries.BLOCK.containsKey(rl)) {
            return BuiltInRegistries.BLOCK.get(rl).getName();
        }
        if (BuiltInRegistries.ITEM.containsKey(rl)) return BuiltInRegistries.ITEM.get(rl).getDescription();
        return Component.literal(id);
    }

    private static final Map<String, String> TAG_NAMES = Map.ofEntries(
            Map.entry("#minecraft:logs", "原木（全種類）"),
            Map.entry("#minecraft:leaves", "葉（全種類）"),
            Map.entry("#c:ores", "鉱石（全種類）"),
            Map.entry("#minecraft:crops", "作物（全種類）"),
            Map.entry("#minecraft:base_stone_overworld", "石（全種類）"),
            Map.entry("#minecraft:dirt", "土（全種類）"),
            Map.entry("#minecraft:sand", "砂（全種類）"),
            Map.entry("#minecraft:planks", "木材（全種類）"),
            Map.entry("#minecraft:wool", "羊毛（全色）"),
            Map.entry("#minecraft:fishes", "魚（全種類）"),
            Map.entry("#c:ingots", "インゴット（全種類）"),
            Map.entry("#c:gems", "宝石（全種類）"));
}
