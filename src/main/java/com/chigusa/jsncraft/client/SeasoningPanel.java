package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.cooking.FoodBuff;
import com.chigusa.jsncraft.cooking.Seasoning;
import com.chigusa.jsncraft.network.SeasoningsPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** 調理台の画面の右に出す、隠し味の一覧。カーソルで効果、クリックで持ち物から隠し味の枠に入れる。 */
public final class SeasoningPanel {
    public static final int ROW = 18;
    private final IntConsumer onPick;
    private int scroll = 0;

    public SeasoningPanel(IntConsumer onPick) {
        this.onPick = onPick;
    }

    private static List<Seasoning> list() {
        return SeasoningsPayload.client.seasonings();
    }

    static ItemStack icon(Seasoning s) {
        if (s.item().startsWith("#")) return ItemStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(s.item());
        return id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    private static String stars(double q) {
        return q >= 0.3 ? "品質↑↑" : q > 0 ? "品質↑" : "";
    }

    private int rows(int h) {
        return (h - 22) / ROW;
    }

    public void render(GuiGraphics g, Font font, int x, int y, int h, int mx, int my, int text) {
        List<Seasoning> l = list();
        if (l.isEmpty()) {
            g.drawString(font, "§8読み込み中…", x + 6, y + 24, text, false);
            return;
        }
        int n = rows(h);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, l.size() - n)));
        int ly = y + 18;
        var player = Minecraft.getInstance().player;
        for (int i = scroll; i < l.size() && i < scroll + n; i++) {
            Seasoning s = l.get(i);
            boolean hover = mx >= x + 2 && mx < x + RecipePanel.W - 4 && my >= ly && my < ly + ROW;
            if (hover) g.fill(x + 3, ly, x + RecipePanel.W - 5, ly + ROW, 0x30FFFFFF);
            g.renderItem(icon(s), x + 4, ly + 1);
            boolean have = player != null && player.getInventory().items.stream().anyMatch(st -> com.chigusa.jsncraft.cooking.CookingRecipe.matchesOne(s.item(), st));
            g.drawString(font, font.plainSubstrByWidth(s.name(), RecipePanel.W - 66), x + 22, ly + 5, have ? text : 0xFF8A7A6A, false);
            String q = stars(s.quality());
            if (!q.isEmpty()) g.drawString(font, q, x + RecipePanel.W - 8 - font.width(q), ly + 5, 0xFFB07A10, false);
            ly += ROW;
        }
        if (l.size() > n) {
            int barH = Math.max(8, (h - 22) * n / l.size());
            int barY = y + 18 + (h - 22 - barH) * scroll / Math.max(1, l.size() - n);
            g.fill(x + RecipePanel.W - 5, barY, x + RecipePanel.W - 3, barY + barH, 0xFF8A7A6A);
        }
    }

    private int at(int x, int y, int h, double mx, double my) {
        if (mx < x + 2 || mx >= x + RecipePanel.W - 4 || my < y + 18) return -1;
        int i = scroll + (int) ((my - y - 18) / ROW);
        return i < list().size() && i < scroll + rows(h) ? i : -1;
    }

    public void renderTooltip(GuiGraphics g, Font font, int x, int y, int h, int mx, int my) {
        int i = at(x, y, h, mx, my);
        if (i < 0) return;
        Seasoning s = list().get(i);
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal("隠し味：" + s.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
        ItemStack ic = icon(s);
        if (!ic.isEmpty()) tip.add(Component.literal("材料：").withStyle(ChatFormatting.GRAY).append(ic.getHoverName()));
        for (FoodBuff b : s.buffs()) tip.add(Component.literal(" ").append(b.describe()));
        if (s.quality() > 0) tip.add(Component.literal("品質（★）が上がりやすい（+" + Math.round(s.quality() * 100) + "%）").withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("クリックで持ち物から隠し味の枠に入れる").withStyle(ChatFormatting.DARK_GRAY));
        g.renderComponentTooltip(font, tip, mx, my);
    }

    public boolean click(int x, int y, int h, double mx, double my) {
        int i = at(x, y, h, mx, my);
        if (i < 0) return false;
        onPick.accept(i);
        return true;
    }

    public boolean scroll(int x, int y, int h, double mx, double my, double sy) {
        if (mx < x || mx >= x + RecipePanel.W || my < y || my >= y + h) return false;
        scroll = Math.max(0, scroll - (int) Math.signum(sy) * 2);
        return true;
    }

    /** アイテムのツールチップに「隠し味：○○」 */
    public static Seasoning of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (Seasoning s : list()) if (com.chigusa.jsncraft.cooking.CookingRecipe.matchesOne(s.item(), stack)) return s;
        return null;
    }
}
