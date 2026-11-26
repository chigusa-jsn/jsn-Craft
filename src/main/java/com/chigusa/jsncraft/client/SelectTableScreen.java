package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.enchanting.SelectEnchant;
import com.chigusa.jsncraft.enchanting.SelectTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

/** 選べる付与台の画面：左に物とラピス、右にエンチャントの一覧。選んでレベルを決めて「付与する」。 */
public class SelectTableScreen extends AbstractContainerScreen<SelectTableMenu> {
    private static final int LX = 40, LY = 16, LW = 130, ROW = 10, ROWS = 6;
    private Button minus, plus, apply;
    private List<Holder<Enchantment>> list = List.of();
    private ItemStack lastTarget = ItemStack.EMPTY;
    private int scroll, selected = -1, level = 1;

    public SelectTableScreen(SelectTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 202;
        inventoryLabelY = 109;
    }

    @Override
    protected void init() {
        super.init();
        minus = addRenderableWidget(Button.builder(Component.literal("<"), b -> level--).bounds(leftPos + LX, topPos + 80, 14, 14).build());
        plus = addRenderableWidget(Button.builder(Component.literal(">"), b -> level++).bounds(leftPos + LX + 44, topPos + 80, 14, 14).build());
        apply = addRenderableWidget(Button.builder(Component.literal("付与する"), b -> click()).bounds(leftPos + 110, topPos + 80, 60, 14).build());
    }

    private void click() {
        if (selected < 0 || minecraft == null || minecraft.gameMode == null) return;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected * 8 + level);
    }

    private Holder<Enchantment> sel() {
        return selected >= 0 && selected < list.size() ? list.get(selected) : null;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack t = menu.target();
        if (!ItemStack.isSameItemSameComponents(t, lastTarget)) {
            Holder<Enchantment> before = sel();
            lastTarget = t.copy();
            list = minecraft != null && minecraft.level != null ? SelectEnchant.candidates(minecraft.level.registryAccess(), t) : List.of();
            selected = before != null ? list.indexOf(before) : -1;
            scroll = Math.max(0, Math.min(scroll, list.size() - ROWS));
        }
        Holder<Enchantment> e = sel();
        int max = e == null ? 1 : Math.min(e.value().getMaxLevel(), Math.max(1, SelectEnchant.maxLevel(menu.jobLevel())));
        level = Math.max(1, Math.min(level, max));
        minus.active = e != null && level > 1;
        plus.active = e != null && level < max;
        apply.active = e != null && SelectEnchant.problem(t, e, level, menu.jobLevel()) == null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        double x = mx - leftPos - LX, y = my - topPos - LY;
        if (x >= 0 && x < LW && y >= 0 && y < ROW * ROWS) {
            int i = scroll + (int) (y / ROW);
            if (i < list.size()) {
                selected = i;
                level = 1;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (list.size() > ROWS) {
            scroll = Math.max(0, Math.min(list.size() - ROWS, scroll - (int) Math.signum(sy)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A1426);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF7A68A8);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 106, 0xFF2E2448);
        g.fill(x + LX - 1, y + LY - 1, x + LX + LW + 1, y + LY + ROW * ROWS + 1, 0xFF120E1E);
        for (int r = 0; r < ROWS; r++) {
            int i = scroll + r;
            if (i >= list.size()) break;
            int ry = y + LY + r * ROW;
            boolean hover = mx >= x + LX && mx < x + LX + LW && my >= ry && my < ry + ROW;
            int c = i == selected ? 0xFF6A4AA8 : hover ? 0xFF3A2E5A : (r % 2 == 0 ? 0xFF221A36 : 0xFF1C1630);
            g.fill(x + LX, ry, x + LX + LW, ry + ROW, c);
        }
        if (list.size() > ROWS) {
            int h = ROW * ROWS, bar = Math.max(6, h * ROWS / list.size());
            int by = (h - bar) * scroll / Math.max(1, list.size() - ROWS);
            g.fill(x + LX + LW - 2, y + LY + by, x + LX + LW, y + LY + by + bar, 0xFFB8A8E8);
        }
        for (Slot s : menu.slots) {
            int c = s.index == 0 ? 0xFFC8A0F0 : s.index == 1 ? 0xFF3050C0 : 0xFF3A3A42;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
    }

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII"};

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 5, 0xFFE8E0F8, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF2A2A2A, false);
        g.drawString(font, "§7物", 12, 20, 0xFFFFFF, false);
        g.drawString(font, "§7ラピス", 6, 47, 0xFFFFFF, false);
        int jl = menu.jobLevel();
        Holder<Enchantment> e = sel();
        if (list.isEmpty()) {
            String s = menu.target().isEmpty() ? "§7左に物を置いてね" : "§7付けられるものがない";
            g.drawString(font, s, LX + 4, LY + 4, 0xFFFFFF, false);
        }
        for (int r = 0; r < ROWS; r++) {
            int i = scroll + r;
            if (i >= list.size()) break;
            Holder<Enchantment> h = list.get(i);
            String name = h.value().description().getString();
            int maxLv = h.value().getMaxLevel();
            boolean ok = SelectEnchant.problem(menu.target(), h, 1, jl) == null;
            String right = "~" + ROMAN[Math.min(maxLv, 7)];
            int rw = font.width(right);
            String n = font.plainSubstrByWidth(name, LW - rw - 10);
            g.drawString(font, n, LX + 3, LY + r * ROW + 1, ok ? 0xFFF0E8FF : 0xFF7A7090, false);
            g.drawString(font, right, LX + LW - rw - 4, LY + r * ROW + 1, 0xFF9A90C0, false);
        }
        // レベル
        String lvText = e == null ? "-" : ROMAN[Math.min(level, 7)];
        g.drawString(font, lvText, LX + 29 - font.width(lvText) / 2, 83, 0xFFFFE080, false);
        // 費用・状態
        String info;
        if (jl < 0) info = "§c付けられるのは付与術師・刻印師だけ";
        else if (e == null) info = "§7腕：レベル " + SelectEnchant.maxLevel(jl) + " まで付けられる";
        else {
            String why = SelectEnchant.problem(menu.target(), e, level, jl);
            int xp = SelectEnchant.xpCost(level, menu.master()), lap = SelectEnchant.lapisCost(level);
            boolean xpOk = minecraft != null && minecraft.player != null && (minecraft.player.experienceLevel >= xp || minecraft.player.getAbilities().instabuild);
            if (why != null) info = "§c" + why;
            else info = (xpOk ? "§a" : "§c") + "経験値Lv " + xp + "  " + (menu.lapis() >= lap ? "§9" : "§c") + "ラピス " + lap;
        }
        g.drawString(font, font.plainSubstrByWidth(info, imageWidth - 12), 8, 97, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(SelectEnchant.MENU.get(), SelectTableScreen::new);
        }
    }
}
