package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.network.CookingRecipesPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * 調理台・酒樽の画面の右に出す、レシピの一覧。
 * 作れるレベル順に並べる。カーソルを当てると材料、クリックすると持ち物から材料を枠に入れる。
 */
public final class RecipePanel {
    public static final int W = 150, ROW = 18;

    private final Supplier<List<CookingRecipesPayload.Entry>> source;
    private final IntConsumer onPick;
    private final int frame, fill, text;
    private int scroll = 0;
    private String query = "";

    public RecipePanel(Supplier<List<CookingRecipesPayload.Entry>> source, IntConsumer onPick, int frame, int fill, int text) {
        this.source = source;
        this.onPick = onPick;
        this.frame = frame;
        this.fill = fill;
        this.text = text;
    }

    /** 表示する順（元の番号のリスト）：レベル順・名前の絞り込み */
    private List<Integer> order() {
        List<CookingRecipesPayload.Entry> list = source.get();
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (!query.isEmpty() && !list.get(i).result().getHoverName().getString().contains(query)) continue;
            out.add(i);
        }
        out.sort(Comparator.comparingInt((Integer i) -> list.get(i).level()).thenComparing(i -> list.get(i).result().getHoverName().getString()));
        return out;
    }

    private int rows(int h) {
        return (h - 22) / ROW;
    }

    public void render(GuiGraphics g, Font font, int x, int y, int h, int mx, int my) {
        g.fill(x, y, x + W, y + h, frame);
        g.fill(x + 2, y + 2, x + W - 2, y + h - 2, fill);
        List<CookingRecipesPayload.Entry> list = source.get();
        List<Integer> ord = order();
        g.drawString(font, "レシピ（" + list.size() + "）", x + 6, y + 6, text, false);
        if (list.isEmpty()) {
            g.drawString(font, "§8読み込み中…", x + 6, y + 24, text, false);
            return;
        }
        int n = rows(h);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, ord.size() - n)));
        int ly = y + 18;
        for (int k = scroll; k < ord.size() && k < scroll + n; k++) {
            var e = list.get(ord.get(k));
            boolean hover = mx >= x + 2 && mx < x + W - 4 && my >= ly && my < ly + ROW;
            if (hover) g.fill(x + 3, ly, x + W - 5, ly + ROW, 0x30FFFFFF);
            g.renderItem(e.result(), x + 4, ly + 1);
            g.drawString(font, font.plainSubstrByWidth(e.result().getHoverName().getString(), W - 50), x + 22, ly + 5, text, false);
            String lv = "Lv" + e.level();
            g.drawString(font, lv, x + W - 8 - font.width(lv), ly + 5, 0xFF8A7A6A, false);
            ly += ROW;
        }
        if (ord.size() > n) {
            int barH = Math.max(8, (h - 22) * n / ord.size());
            int barY = y + 18 + (h - 22 - barH) * scroll / Math.max(1, ord.size() - n);
            g.fill(x + W - 5, barY, x + W - 3, barY + barH, 0xFF8A7A6A);
        }
    }

    /** カーソルの下のレシピ（元の番号、なければ -1） */
    private int at(int x, int y, int h, double mx, double my) {
        if (mx < x + 2 || mx >= x + W - 4 || my < y + 18) return -1;
        int k = scroll + (int) ((my - y - 18) / ROW);
        List<Integer> ord = order();
        if (k >= ord.size() || k >= scroll + rows(h)) return -1;
        return ord.get(k);
    }

    public void renderTooltip(GuiGraphics g, Font font, int x, int y, int h, int mx, int my) {
        int i = at(x, y, h, mx, my);
        if (i < 0) return;
        var e = source.get().get(i);
        List<Component> tip = new ArrayList<>();
        tip.add(e.result().getHoverName().copy().withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("必要なレベル：Lv" + e.level()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("材料：").withStyle(ChatFormatting.WHITE));
        var player = Minecraft.getInstance().player;
        for (String spec : e.ingredients()) {
            Component name = nameOf(spec);
            boolean have = player != null && player.getInventory().items.stream().anyMatch(s -> com.chigusa.jsncraft.cooking.CookingRecipe.matchesOne(spec, s));
            tip.add(Component.literal(have ? " ✔ " : " ・ ").withStyle(have ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                    .append(name.copy().withStyle(have ? ChatFormatting.GREEN : ChatFormatting.WHITE)));
        }
        tip.add(Component.literal("クリックで持ち物から材料を入れる").withStyle(ChatFormatting.DARK_GRAY));
        g.renderComponentTooltip(font, tip, mx, my);
    }

    private static Component nameOf(String spec) {
        if (spec.startsWith("#")) return Component.literal("（" + spec.substring(1) + " のどれか）");
        ResourceLocation id = ResourceLocation.tryParse(spec);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? Component.literal(spec) : new ItemStack(item).getHoverName();
    }

    public boolean click(int x, int y, int h, double mx, double my) {
        int i = at(x, y, h, mx, my);
        if (i < 0) return false;
        onPick.accept(i);
        return true;
    }

    public boolean scroll(int x, int y, int h, double mx, double my, double sy) {
        if (mx < x || mx >= x + W || my < y || my >= y + h) return false;
        scroll = Math.max(0, scroll - (int) Math.signum(sy) * 2);
        return true;
    }
}
