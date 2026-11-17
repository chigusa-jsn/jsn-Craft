package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.enchanting.EnchantingService;
import com.chigusa.jsncraft.enchanting.ModEnchanting;
import com.chigusa.jsncraft.enchanting.RuneAltarMenu;
import com.chigusa.jsncraft.enchanting.RuneData;
import com.chigusa.jsncraft.enchanting.Runes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 魔紋の祭壇の画面。上のタブで「魔紋を刻む／上限突破／付け替え」を切りかえる。
 * 左：刻む物・ラピス・触媒と説明、右：魔紋（またはエンチャント）の一覧。
 */
public class RuneAltarScreen extends AbstractContainerScreen<RuneAltarMenu> {
    private static final int LIST_X = 124, LIST_Y = 24, LIST_W = 124, ROW = 16, ROWS = 7;
    private static final String[] TABS = {"魔紋を刻む", "上限突破", "付け替え"};
    private static final String[] ACTIONS = {"刻む", "上限突破する", "付け替える"};

    private int mode = 0, scroll = 0;
    private final Button[] tabs = new Button[3];
    private Button action;

    public RuneAltarScreen(RuneAltarMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 256;
        imageHeight = 228;
        inventoryLabelY = 1000; // 持ち物の見出しは出さない
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 3; i++) {
            int m = i;
            tabs[i] = addRenderableWidget(Button.builder(Component.literal(TABS[i]), b -> {
                mode = m;
                scroll = 0;
            }).bounds(leftPos + 8 + i * 82, topPos + 4, 78, 15).build());
        }
        action = addRenderableWidget(Button.builder(Component.literal(ACTIONS[0]), b -> click(mode))
                .bounds(leftPos + 8, topPos + 123, 110, 15).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    // ---------------- 一覧 ----------------

    /** 魔紋の一覧（物を置いていれば、その物に刻めるものだけ） */
    private List<Runes.Rune> runeRows() {
        String kinds = RuneData.kinds(menu.item());
        List<Runes.Rune> out = new ArrayList<>();
        for (Runes.Rune r : Runes.LIST) if (menu.item().isEmpty() || r.fits(kinds)) out.add(r);
        return out;
    }

    private List<Map.Entry<Holder<Enchantment>, Integer>> enchRows() {
        return RuneData.enchantments(menu.item());
    }

    private int rowCount() {
        return mode == 0 ? runeRows().size() : enchRows().size();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < 3; i++) tabs[i].active = mode != i;
        action.setMessage(Component.literal(ACTIONS[mode]));
        action.active = menu.check(mode, 0) == EnchantingService.OK;
        scroll = Math.max(0, Math.min(scroll, rowCount() - ROWS));
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF140A1E);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF3A2A52);
        g.fill(x + 4, y + 22, x + 120, y + 141, 0xFF2A1E3C);
        g.fill(x + LIST_X - 2, y + LIST_Y - 2, x + LIST_X + LIST_W + 2, y + LIST_Y + ROW * ROWS + 2, 0xFF1E1430);
        for (Slot s : menu.slots) {
            int c = s.index == 0 ? 0xFF8A4AC0 : s.index == 1 ? 0xFF2A4AB0 : s.index == 2 ? 0xFF2A9A9A : 0xFF241A30;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF6A6A7A);
        }
        // 空の枠に薄く目印
        if (menu.getSlot(1).getItem().isEmpty()) ghost(g, new ItemStack(Items.LAPIS_LAZULI), x + menu.getSlot(1).x, y + menu.getSlot(1).y);
        if (menu.getSlot(2).getItem().isEmpty()) {
            ItemStack hint = mode == 2 ? new ItemStack(Items.BOOK) : mode == 1 ? new ItemStack(Items.DIAMOND)
                    : Runes.get(menu.selectedRune()) != null ? new ItemStack(Runes.get(menu.selectedRune()).catalyst()) : ItemStack.EMPTY;
            if (!hint.isEmpty()) ghost(g, hint, x + menu.getSlot(2).x, y + menu.getSlot(2).y);
        }
        g.drawString(font, "+", x + 36, y + 38, 0xFFB080FF, false);

        // 右の一覧
        int ly = y + LIST_Y;
        if (mode == 0) {
            List<Runes.Rune> rows = runeRows();
            Map<String, Integer> has = RuneData.runes(menu.item());
            int elv = menu.enchantLevel();
            for (int i = scroll; i < rows.size() && i < scroll + ROWS; i++) {
                Runes.Rune r = rows.get(i);
                boolean sel = r.index() == menu.selectedRune();
                boolean hover = in(mouseX, mouseY, x + LIST_X, ly, LIST_W, ROW);
                if (sel) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF6A3A9A);
                else if (hover) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF34244A);
                g.renderItem(r.catalyst().getDefaultInstance(), x + LIST_X, ly);
                int cur = has.getOrDefault(r.id(), 0);
                boolean locked = elv < r.unlock();
                int col = locked ? 0xFF6A6A7A : cur > 0 ? 0xFFE0A0FF : 0xFFE8E8F0;
                g.drawString(font, r.name(), x + LIST_X + 19, ly + 4, col, false);
                String lv = locked ? RuneText.level(r.unlock()) : cur > 0 ? Runes.roman(cur) + "/" + Runes.roman(r.maxLevel()) : "";
                g.drawString(font, lv, x + LIST_X + LIST_W - 2 - font.width(lv), ly + 4, locked ? 0xFF8A6A6A : 0xFFC0A0E0, false);
                ly += ROW;
            }
            if (rows.isEmpty()) g.drawString(font, "§7刻める魔紋がない", x + LIST_X + 4, ly + 4, 0xFFFFFF, false);
        } else {
            var rows = enchRows();
            for (int i = scroll; i < rows.size() && i < scroll + ROWS; i++) {
                var e = rows.get(i);
                boolean sel = i == Math.floorMod(menu.selectedEnch(), Math.max(1, rows.size()));
                boolean hover = in(mouseX, mouseY, x + LIST_X, ly, LIST_W, ROW);
                if (sel) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF6A3A9A);
                else if (hover) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF34244A);
                Component name = Enchantment.getFullname(e.getKey(), e.getValue());
                int over = e.getValue() - e.getKey().value().getMaxLevel();
                g.drawString(font, font.plainSubstrByWidth(name.getString(), LIST_W - 26), x + LIST_X + 4, ly + 4,
                        over > 0 ? 0xFFFFD060 : 0xFFE8E8F0, false);
                if (over > 0) g.drawString(font, "+" + over, x + LIST_X + LIST_W - 4 - font.width("+" + over), ly + 4, 0xFFFFB030, false);
                ly += ROW;
            }
            if (rows.isEmpty()) g.drawString(font, "§7エンチャントがない", x + LIST_X + 4, ly + 4, 0xFFFFFF, false);
        }
        int n = rowCount();
        if (n > ROWS) {
            int h = ROW * ROWS, barH = Math.max(8, h * ROWS / n);
            int barY = y + LIST_Y + (h - barH) * scroll / Math.max(1, n - ROWS);
            g.fill(x + LIST_X + LIST_W, barY, x + LIST_X + LIST_W + 2, barY + barH, 0xFFB080FF);
        }
    }

    private static void ghost(GuiGraphics g, ItemStack stack, int x, int y) {
        g.renderFakeItem(stack, x, y);
        g.fill(x, y, x + 16, y + 16, 0xA06A6A7A);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------------- 左の説明 ----------------

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        int st = menu.check(mode, 0), lapis = menu.check(mode, 1), xp = menu.check(mode, 2), cat = menu.check(mode, 3), target = menu.check(mode, 4);
        boolean costs = st == EnchantingService.OK || st == EnchantingService.NO_LAPIS || st == EnchantingService.NO_CATALYST || st == EnchantingService.NO_XP;
        if (mode == 0) {
            Runes.Rune r = Runes.get(menu.selectedRune());
            if (r != null) {
                int cur = RuneData.runes(menu.item()).getOrDefault(r.id(), 0);
                int show = Math.max(1, Math.min(r.maxLevel(), costs || st == EnchantingService.LEVEL_LOW ? target : Math.max(cur, 1)));
                lines.add(Component.literal("✦" + r.name() + " " + (cur > 0 && cur < show ? Runes.roman(cur) + " → " : "") + Runes.roman(show))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                lines.add(Component.literal(r.effect().apply(show)).withStyle(ChatFormatting.WHITE));
            }
            if (costs) {
                lines.add(Component.literal("ラピス×" + lapis + "  経験値Lv" + xp).withStyle(ChatFormatting.AQUA));
                if (r != null && cat > 0) lines.add(Component.literal("触媒：").withStyle(ChatFormatting.GRAY)
                        .append(r.catalyst().getDescription().copy()).append("×" + cat));
            }
        } else {
            var rows = enchRows();
            ItemStack c = menu.catalyst();
            if (mode == 2 && c.is(Items.ENCHANTED_BOOK)) {
                lines.add(Component.literal("本のエンチャントを付ける").withStyle(ChatFormatting.LIGHT_PURPLE));
                lines.add(Component.literal("（上限突破したレベルのまま）").withStyle(ChatFormatting.GRAY));
            } else if (!rows.isEmpty()) {
                var e = rows.get(Math.floorMod(menu.selectedEnch(), rows.size()));
                Component name = Enchantment.getFullname(e.getKey(), e.getValue());
                if (mode == 1 && costs) {
                    lines.add(Component.literal("").append(name).append(" →").withStyle(ChatFormatting.LIGHT_PURPLE));
                    lines.add(Component.literal("   ").append(Enchantment.getFullname(e.getKey(), target)).withStyle(ChatFormatting.GOLD));
                } else {
                    lines.add(Component.literal(mode == 2 ? "本に抜き出す：" : "").append(name).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                if (mode == 1) lines.add(Component.literal("今の上限：+" + menu.limitBonus() + " まで").withStyle(ChatFormatting.GRAY));
            }
            if (costs) {
                lines.add(Component.literal("ラピス×" + lapis + "  経験値Lv" + xp).withStyle(ChatFormatting.AQUA));
                if (mode == 1 && !rows.isEmpty()) {
                    var e = rows.get(Math.floorMod(menu.selectedEnch(), rows.size()));
                    int bonus = target - e.getKey().value().getMaxLevel();
                    lines.add(Component.literal("触媒：").withStyle(ChatFormatting.GRAY)
                            .append(EnchantingService.limitCatalyst(bonus).getDescription().copy()).append("×1"));
                }
            }
        }
        int ly = 65;
        for (Component line : lines) {
            for (FormattedCharSequence seq : font.split(line, 110)) {
                if (ly > 95) break;
                g.drawString(font, seq, 8, ly, 0xFFFFFFFF, false);
                ly += 10;
            }
        }
        ly = 105;
        for (FormattedCharSequence seq : font.split(status(st, mode), 110)) {
            if (ly > 115) break;
            g.drawString(font, seq, 8, ly, 0xFFFFFFFF, false);
            ly += 10;
        }
        if (menu.enchantLevel() >= 0) {
            g.drawString(font, "魔紋の枠 " + menu.runeSlots(), 74, 26, 0xFFB0A0C8, false);
            g.drawString(font, "上限 +" + menu.limitBonus(), 74, 38, 0xFFB0A0C8, false);
        }
    }

    private Component status(int st, int mode) {
        String s = switch (st) {
            case EnchantingService.OK -> mode == 0 ? "§a刻めます" : mode == 1 ? "§a上限突破できます" : "§a付け替えできます";
            case EnchantingService.NO_ITEM -> mode == 0 ? "§7武器・防具・道具を置こう" : "§7エンチャントのついた物を置こう";
            case EnchantingService.NOT_ENCHANTER -> "§c使えるのは付与術師だけです";
            case EnchantingService.WRONG_ITEM -> "§cこの物には使えない";
            case EnchantingService.LEVEL_LOW -> "§c付与の腕が足りない";
            case EnchantingService.MAX -> "§6最大レベルです";
            case EnchantingService.SLOTS_FULL -> "§c魔紋の枠がいっぱい（" + menu.runeSlots() + "つまで）";
            case EnchantingService.NO_LAPIS -> "§cラピスが足りない";
            case EnchantingService.NO_CATALYST -> "§c触媒が足りない";
            case EnchantingService.NO_XP -> "§c経験値レベルが足りない";
            case EnchantingService.NOT_AT_MAX -> "§7まず最大レベルまで上げよう";
            case EnchantingService.OVER_LIMIT -> "§c今はこれ以上超えられない";
            case EnchantingService.NO_ENCH -> "§7エンチャントがついていない";
            case EnchantingService.CURSE -> "§c呪いは扱えない";
            case EnchantingService.INCOMPATIBLE -> "§cこの本は付けられない";
            case EnchantingService.NO_SELECT -> "§7右の一覧から魔紋を選ぼう";
            case EnchantingService.NO_BOOK -> "§7触媒の枠に、本（抜き出す）かエンチャント本（付ける）を置こう";
            default -> "";
        };
        if (st == EnchantingService.LEVEL_LOW && mode == 0) {
            Runes.Rune r = Runes.get(menu.selectedRune());
            if (r != null) s += "（" + RuneText.level(r.requiredLevel(Math.max(1, menu.check(0, 4)))) + "）";
        }
        return Component.literal(s);
    }

    // ---------------- 操作 ----------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        if (in(mx, my, x, y, LIST_W, ROW * ROWS)) {
            int i = scroll + (int) ((my - y) / ROW);
            if (mode == 0) {
                List<Runes.Rune> rows = runeRows();
                if (i < rows.size()) click(RuneAltarMenu.BTN_RUNE + rows.get(i).index());
            } else if (i < enchRows().size()) {
                click(RuneAltarMenu.BTN_ENCH + i);
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (in(mx, my, leftPos + LIST_X, topPos + LIST_Y, LIST_W + 4, ROW * ROWS)) {
            scroll = Math.max(0, Math.min(Math.max(0, rowCount() - ROWS), scroll - (int) Math.signum(sy)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        renderTooltip(g, mouseX, mouseY);
        // 一覧の魔紋にカーソルを当てると説明
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        if (mode == 0 && in(mouseX, mouseY, x, y, LIST_W, ROW * ROWS)) {
            int i = scroll + (mouseY - y) / ROW;
            List<Runes.Rune> rows = runeRows();
            if (i < rows.size()) g.renderComponentTooltip(font, RuneText.details(rows.get(i), true), mouseX, mouseY);
        }
        // 空の枠の説明
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 3) {
            String s = switch (hoveredSlot.index) {
                case 0 -> "刻む物（武器・防具・道具、上限突破なら本も）";
                case 1 -> "ラピスラズリ";
                default -> mode == 2 ? "本（抜き出す）／エンチャント本（付ける）" : "触媒";
            };
            g.renderTooltip(font, Component.literal(s), mouseX, mouseY);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(ModEnchanting.ALTAR_MENU.get(), RuneAltarScreen::new);
        }
    }

    /** 刻まれた魔紋と、触媒の説明をツールチップに出す */
    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Tooltips {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            List<Component> tip = event.getToolTip();
            Map<String, Integer> runes = RuneData.runes(stack);
            if (!runes.isEmpty()) {
                int at = Math.min(1, tip.size());
                for (var e : runes.entrySet()) {
                    Runes.Rune r = Runes.get(e.getKey());
                    tip.add(at++, Component.literal("✦" + r.name() + " " + Runes.roman(e.getValue())).withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.literal("  " + r.effect().apply(e.getValue())).withStyle(ChatFormatting.DARK_PURPLE)));
                }
            }
            List<Runes.Rune> by = Runes.byCatalyst(stack.getItem());
            if (!by.isEmpty()) {
                StringBuilder sb = new StringBuilder("魔紋の触媒：");
                for (int i = 0; i < by.size(); i++) sb.append(i > 0 ? "・" : "").append(by.get(i).name());
                tip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }
}
