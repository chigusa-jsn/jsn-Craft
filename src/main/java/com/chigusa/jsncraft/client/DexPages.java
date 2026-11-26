package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.dex.MonsterDex;
import com.chigusa.jsncraft.gacha.FigureItem;
import com.chigusa.jsncraft.network.MonsterDexPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 職業メニューの図鑑タブ：モンスター図鑑とフィギュア図鑑のページ。 */
public final class DexPages {
    private static final int CELL = 20;
    private static final String[] RARITY = {"ノーマル", "レア", "スーパーレア", "ウルトラレア", "レジェンド"};
    private static final int[] FRAME = {0xFF707070, 0xFF3A78C0, 0xFF9040C0, 0xFFD09020, 0xFFE04060};
    private static final ChatFormatting[] COLOR = {ChatFormatting.GRAY, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD, ChatFormatting.RED};
    private static final Map<String, ItemStack> ICONS = new HashMap<>();

    private static EntityType<?> type(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl) ? null : BuiltInRegistries.ENTITY_TYPE.get(rl);
    }

    private static Component name(String id) {
        EntityType<?> t = type(id);
        return t == null ? Component.literal(id) : t.getDescription();
    }

    private static ItemStack monsterIcon(String id) {
        return ICONS.computeIfAbsent(id, k -> {
            EntityType<?> t = type(k);
            if (t == EntityType.ENDER_DRAGON) return new ItemStack(Items.DRAGON_HEAD);
            if (t == EntityType.WITHER) return new ItemStack(Items.WITHER_SKELETON_SKULL);
            SpawnEggItem egg = t == null ? null : SpawnEggItem.byId(t);
            return egg != null ? new ItemStack(egg) : new ItemStack(Items.ZOMBIE_HEAD);
        });
    }

    private static String nextReward(List<MonsterDex.Milestone> list, int have, int total) {
        for (MonsterDex.Milestone m : list) {
            int need = m.count() < 0 ? total : Math.min(m.count(), total);
            if (have < need) return "次のご褒美：" + (m.count() < 0 ? "全部" : need + "種類") + " → " + m.desc();
        }
        return "ご褒美を全部もらった！";
    }

    public static void render(Screen screen, GuiGraphics g, Font font, int mx, int my, boolean monsters,
                              int left, int top, int panelW, int panelH, int cols, int visible, int scroll, int maxScroll) {
        MonsterDexPayload data = MonsterDexPayload.client;
        int x0 = left + 10, y0 = top + 30;
        int total = monsters ? data.monsters().size() : data.figures().size();
        int have = 0;
        if (monsters) { for (var m : data.monsters()) if (m.kills() > 0) have++; }
        else { for (var f : data.figures()) if (f.have()) have++; }
        int pct = total == 0 ? 0 : have * 100 / total;
        g.drawString(font, Component.literal((monsters ? "モンスター図鑑  " : "フィギュア図鑑  ") + have + " / " + total + "（" + pct + "%）")
                .withStyle(monsters ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE), x0, y0, 0xFFFFFF);
        String next = nextReward(monsters ? MonsterDex.MONSTER_REWARDS : MonsterDex.FIGURE_REWARDS, have, total);
        g.drawString(font, font.plainSubstrByWidth(next, panelW - 150), left + 140, y0, 0xFFE0C060, false);

        int gx = left + (panelW - cols * CELL) / 2, gy = top + 44;
        List<Component> tip = null;
        int size = monsters ? data.monsters().size() : data.figures().size();
        for (int i = 0; i < size; i++) {
            int row = i / cols - scroll;
            if (row < 0 || row >= visible) continue;
            int cx = gx + (i % cols) * CELL, cy = gy + row * CELL;
            boolean hover = mx >= cx && mx < cx + CELL - 2 && my >= cy && my < cy + CELL - 2;
            if (monsters) {
                MonsterDexPayload.Mon m = data.monsters().get(i);
                boolean got = m.kills() > 0;
                int frame = m.kills() >= 100 ? 0xFFD09020 : got ? 0xFF8A3030 : 0xFF3A3A44;
                g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, frame);
                g.fill(cx + 1, cy + 1, cx + CELL - 3, cy + CELL - 3, got ? 0xFF2E2226 : 0xFF151520);
                if (got) g.renderItem(monsterIcon(m.id()), cx + 1, cy + 1);
                else g.drawCenteredString(font, "?", cx + (CELL - 2) / 2, cy + 5, 0x666677);
                if (hover) {
                    tip = new ArrayList<>();
                    tip.add(got ? name(m.id()).copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD) : Component.literal("？？？").withStyle(ChatFormatting.DARK_GRAY));
                    tip.add(Component.literal("No." + (i + 1)).withStyle(ChatFormatting.GRAY));
                    if (got) {
                        tip.add(Component.literal("倒した数 " + m.kills() + (m.kills() >= 100 ? "  ★討伐の達人" : "")).withStyle(ChatFormatting.WHITE));
                        if (m.hp() > 0) tip.add(Component.literal("HP " + fmt(m.hp()) + (m.atk() > 0 ? "  攻撃 " + fmt(m.atk()) : "")).withStyle(ChatFormatting.YELLOW));
                        if (!m.weakness().isEmpty()) tip.add(Component.literal("弱点：" + m.weakness()).withStyle(ChatFormatting.AQUA));
                    } else {
                        tip.add(Component.literal("倒すと登録される").withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
            } else {
                MonsterDexPayload.Fig f = data.figures().get(i);
                int r = Math.max(0, Math.min(4, f.rarity()));
                g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, FRAME[r]);
                g.fill(cx + 1, cy + 1, cx + CELL - 3, cy + CELL - 3, f.have() ? 0xFF2A2236 : 0xFF151520);
                if (f.have()) g.renderItem(FigureItem.of(f.id()), cx + 1, cy + 1);
                else g.drawCenteredString(font, "?", cx + (CELL - 2) / 2, cy + 5, 0x666677);
                if (hover) {
                    tip = new ArrayList<>();
                    tip.add(f.have() ? Component.literal("フィギュア：").append(name(f.id())).withStyle(COLOR[r], ChatFormatting.BOLD)
                            : Component.literal("？？？").withStyle(ChatFormatting.DARK_GRAY));
                    tip.add(Component.literal("No." + (i + 1) + "  ").withStyle(ChatFormatting.GRAY).append(Component.literal(RARITY[r]).withStyle(COLOR[r])));
                    if (!f.have()) tip.add(Component.literal("フィギュアガチャで出る。持つと登録される").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }
        if (maxScroll > 0) g.drawString(font, "↕ " + (scroll + 1) + "/" + (maxScroll + 1), left + panelW - 40, top + panelH - 12, 0x888899);
        if (tip != null) g.renderComponentTooltip(font, tip, mx, my);
    }

    private static String fmt(float v) {
        return Math.abs(v - Math.round(v)) < 0.01 ? String.valueOf(Math.round(v)) : String.format("%.1f", v);
    }

    private DexPages() {}
}
