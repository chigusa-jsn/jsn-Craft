package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.alchemy.AlchemyMenu;
import com.chigusa.jsncraft.alchemy.AlchemyService;
import com.chigusa.jsncraft.alchemy.ElixirData;
import com.chigusa.jsncraft.alchemy.Elixirs;
import com.chigusa.jsncraft.alchemy.ModAlchemy;
import com.chigusa.jsncraft.alchemy.Transmutes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 調合台の画面。タブで「調合」と「錬金（錬金の賢者だけ）」を切りかえる。
 * 調合：ビン＋主素材＋追加素材で薬を作る。右に薬と追加素材の一覧。
 * 錬金：右の一覧からレシピを選んで、素材A・素材Bの枠に入れる。
 */
public class AlchemyScreen extends AbstractContainerScreen<AlchemyMenu> {
    private static final int LIST_X = 124, LIST_Y = 24, LIST_W = 124, ROW = 16, ROWS = 7;

    private int mode = 0, scroll = 0;
    private Button tabBrew, tabTrans, brewBtn, brewAllBtn, transBtn;

    public AlchemyScreen(AlchemyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 256;
        imageHeight = 228;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        tabBrew = addRenderableWidget(Button.builder(Component.literal("調合"), b -> setMode(0)).bounds(leftPos + 8, topPos + 4, 78, 15).build());
        tabTrans = addRenderableWidget(Button.builder(Component.literal("錬金"), b -> setMode(1)).bounds(leftPos + 90, topPos + 4, 78, 15).build());
        brewBtn = addRenderableWidget(Button.builder(Component.literal("作る"), b -> click(AlchemyMenu.BTN_BREW))
                .bounds(leftPos + 8, topPos + 123, 53, 15).build());
        brewAllBtn = addRenderableWidget(Button.builder(Component.literal("まとめて"), b -> click(AlchemyMenu.BTN_BREW_ALL))
                .bounds(leftPos + 65, topPos + 123, 53, 15).build());
        transBtn = addRenderableWidget(Button.builder(Component.literal("錬金する"), b -> click(AlchemyMenu.BTN_TRANSMUTE))
                .bounds(leftPos + 8, topPos + 123, 110, 15).build());
    }

    private void setMode(int m) {
        mode = m;
        scroll = 0;
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    // ---------------- 一覧 ----------------

    /** 調合の一覧の行：薬の種類（0〜）、見出し（-1）、追加素材（-2-n） */
    private List<Integer> brewRows() {
        List<Integer> out = new ArrayList<>();
        for (Elixirs.Type t : Elixirs.TYPES) out.add(t.index());
        out.add(-1);
        for (int i = 0; i < Elixirs.ADDITIVES.size(); i++) out.add(-2 - i);
        return out;
    }

    private int rowCount() {
        return mode == 0 ? brewRows().size() : Transmutes.LIST.size();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tabBrew.active = mode != 0;
        tabTrans.active = mode != 1;
        brewBtn.visible = brewAllBtn.visible = mode == 0;
        transBtn.visible = mode == 1;
        brewBtn.active = menu.brewStatus() == AlchemyService.OK;
        brewAllBtn.active = brewBtn.active && menu.brewMax() > 1;
        brewAllBtn.setMessage(Component.literal(menu.brewMax() > 1 ? "まとめて" + Math.min(16, menu.brewMax()) : "まとめて"));
        transBtn.active = menu.transStatus() == AlchemyService.OK;
        scroll = Math.max(0, Math.min(scroll, rowCount() - ROWS));
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A140C);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF5A4630);
        g.fill(x + 4, y + 22, x + 120, y + 141, 0xFF3E3020);
        g.fill(x + LIST_X - 2, y + LIST_Y - 2, x + LIST_X + LIST_W + 2, y + LIST_Y + ROW * ROWS + 2, 0xFF2A2016);
        int slots = menu.additiveSlots();
        for (Slot s : menu.slots) {
            boolean locked = s.index >= 2 && s.index <= 4 && (mode == 1 ? s.index > 2 : s.index - 2 >= slots);
            int c = s.index == 0 ? 0xFF6A9AB0 : s.index == 1 ? 0xFFB06A30 : s.index <= 4 ? (locked ? 0xFF3A3028 : 0xFF6AA050) : 0xFF2E2418;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, locked ? 0xFF4A4038 : 0xFF8B8070);
        }
        if (mode == 0) {
            if (menu.slotItem(0).isEmpty()) ghost(g, new ItemStack(Items.GLASS_BOTTLE), x + 16, y + 24);
            g.drawString(font, "+", x + 35, y + 28, 0xFFE8C080, false);
            g.drawString(font, "追加素材", x + 8, y + 43, 0xFFC8B898, false);
        } else {
            g.drawString(font, "A", x + 36, y + 28, 0xFFE8C080, false);
            g.drawString(font, "B", x + 8, y + 56, 0xFFE8C080, false);
        }

        int ly = y + LIST_Y;
        if (mode == 0) {
            List<Integer> rows = brewRows();
            int alv = menu.alchemyLevel();
            for (int i = scroll; i < rows.size() && i < scroll + ROWS; i++) {
                int r = rows.get(i);
                boolean hover = in(mouseX, mouseY, x + LIST_X, ly, LIST_W, ROW);
                if (r == -1) {
                    g.drawString(font, "― 追加素材 ―", x + LIST_X + 4, ly + 4, 0xFFC8B080, false);
                } else if (r >= 0) {
                    Elixirs.Type t = Elixirs.TYPES.get(r);
                    boolean cur = r == menu.brewType();
                    if (cur) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF7A5A30);
                    else if (hover) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF4A3A24);
                    g.renderItem(new ItemStack(ModAlchemy.ELIXIRS.get(r).get()), x + LIST_X, ly);
                    boolean locked = alv < t.unlock();
                    g.drawString(font, font.plainSubstrByWidth(t.name(), LIST_W - 40), x + LIST_X + 18, ly + 4, locked ? 0xFF7A7068 : 0xFFF0E8D8, false);
                    if (locked) {
                        String lv = RuneText.level(t.unlock());
                        g.drawString(font, lv, x + LIST_X + LIST_W - 2 - font.width(lv), ly + 4, 0xFF9A7A6A, false);
                    } else {
                        g.renderItem(new ItemStack(t.main().get()), x + LIST_X + LIST_W - 17, ly);
                    }
                } else {
                    Elixirs.Additive a = Elixirs.ADDITIVES.get(-2 - r);
                    if (hover) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF4A3A24);
                    g.renderItem(new ItemStack(a.item().get()), x + LIST_X, ly);
                    g.drawString(font, font.plainSubstrByWidth(a.desc(), LIST_W - 20), x + LIST_X + 18, ly + 4, 0xFFB8E0B0, false);
                }
                ly += ROW;
            }
        } else {
            int lv = menu.alchemyLevel() - 50;
            for (int i = scroll; i < Transmutes.LIST.size() && i < scroll + ROWS; i++) {
                Transmutes.Recipe r = Transmutes.LIST.get(i);
                boolean sel = i == menu.selected();
                boolean hover = in(mouseX, mouseY, x + LIST_X, ly, LIST_W, ROW);
                if (sel) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF7A5A30);
                else if (hover) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF4A3A24);
                boolean locked = !menu.sage() || lv < r.unlock();
                int ix = x + LIST_X;
                g.renderItem(new ItemStack(r.a().get()), ix, ly);
                g.renderItemDecorations(font, new ItemStack(r.a().get(), r.aCount()), ix, ly);
                ix += 18;
                if (r.hasB()) {
                    g.renderItem(new ItemStack(r.b().get()), ix, ly);
                    g.renderItemDecorations(font, new ItemStack(r.b().get(), r.bCount()), ix, ly);
                    ix += 18;
                }
                g.drawString(font, "→", ix + 2, ly + 4, 0xFFE8C080, false);
                ix += 14;
                g.renderItem(new ItemStack(r.out().get()), ix, ly);
                g.renderItemDecorations(font, new ItemStack(r.out().get(), r.outCount()), ix, ly);
                String tag = locked ? "Lv" + r.unlock() : r.group();
                g.drawString(font, tag, x + LIST_X + LIST_W - 2 - font.width(tag), ly + 4, locked ? 0xFF9A7A6A : 0xFFC8B080, false);
                if (locked) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0x66000000);
                ly += ROW;
            }
        }
        int n = rowCount();
        if (n > ROWS) {
            int h = ROW * ROWS, barH = Math.max(8, h * ROWS / n);
            int barY = y + LIST_Y + (h - barH) * scroll / Math.max(1, n - ROWS);
            g.fill(x + LIST_X + LIST_W, barY, x + LIST_X + LIST_W + 2, barY + barH, 0xFFE8C080);
        }
    }

    private static void ghost(GuiGraphics g, ItemStack stack, int x, int y) {
        g.renderFakeItem(stack, x, y);
        g.fill(x, y, x + 16, y + 16, 0xA08B8070);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------------- 左の説明 ----------------

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        int st;
        if (mode == 0) {
            st = menu.brewStatus();
            int ti = menu.brewType();
            if (ti >= 0) {
                Elixirs.Type t = Elixirs.TYPES.get(ti);
                int form = menu.brewForm();
                String name = form == ElixirData.DUST ? t.dustName() : form == ElixirData.THROW ? t.name() + "（投）" : t.name();
                lines.add(Component.literal(name).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (st == AlchemyService.OK) {
                    double power = 1, time = 1;
                    int strong = 0, lng = 0;
                    for (int i = 2; i <= 4; i++) {
                        Elixirs.Additive a = Elixirs.byAdditive(menu.slotItem(i).getItem());
                        if (a == null) continue;
                        if (a.kind().equals("strong") && strong++ < 2) power *= 1.5;
                        if (a.kind().equals("long") && lng++ < 2) time *= 1.5;
                    }
                    for (String s : Elixirs.describe(t, power, time)) lines.add(Component.literal(s).withStyle(ChatFormatting.WHITE));
                }
            } else {
                lines.add(Component.literal("ビン＋主素材で薬を作る").withStyle(ChatFormatting.GRAY));
            }
        } else {
            st = menu.transStatus();
            Transmutes.Recipe r = Transmutes.get(menu.selected());
            if (r != null) {
                lines.add(Component.literal("A：").append(r.a().get().getDescription()).append("×" + r.aCount()).withStyle(ChatFormatting.WHITE));
                if (r.hasB()) lines.add(Component.literal("B：").append(r.b().get().getDescription()).append("×" + r.bCount()).withStyle(ChatFormatting.WHITE));
                lines.add(Component.literal("→ ").append(r.out().get().getDescription()).append("×" + r.outCount()).withStyle(ChatFormatting.GOLD));
                lines.add(Component.literal("経験値Lv" + menu.transXp()).withStyle(ChatFormatting.AQUA));
            }
        }
        int ly = 74;
        for (Component line : lines) {
            for (FormattedCharSequence seq : font.split(line, 110)) {
                if (ly > 95) break;
                g.drawString(font, seq, 8, ly, 0xFFFFFFFF, false);
                ly += 10;
            }
        }
        ly = 105;
        for (FormattedCharSequence seq : font.split(status(st), 110)) {
            if (ly > 115) break;
            g.drawString(font, seq, 8, ly, 0xFFFFFFFF, false);
            ly += 10;
        }
    }

    private Component status(int st) {
        String s = switch (st) {
            case AlchemyService.OK -> mode == 0 ? "§a作れます" : "§a錬金できます";
            case AlchemyService.NOT_ALCHEMIST -> "§c使えるのは調合師だけです";
            case AlchemyService.NO_BOTTLE -> "§7ガラス瓶を置こう";
            case AlchemyService.NO_MAIN -> "§7主素材を置こう（右の一覧）";
            case AlchemyService.UNKNOWN_MAIN -> "§cこれは主素材にならない";
            case AlchemyService.LEVEL_LOW -> "§c調合の腕が足りない";
            case AlchemyService.BAD_ADDITIVE -> "§cこれは追加素材にならない";
            case AlchemyService.SLOT_LOCKED -> "§c3つめの追加素材の枠はまだ使えない";
            case AlchemyService.FORM_CONFLICT -> "§c火薬と粉塵の素は一緒に入れられない";
            case AlchemyService.NEED_DUST_ART -> "§c粉塵はスキル「粉塵の技」が必要";
            case AlchemyService.CANT_FORM -> "§cこの薬は投げたり粉塵にしたりできない";
            case AlchemyService.NOT_SAGE -> "§c錬金は錬金の賢者だけ";
            case AlchemyService.NO_SELECT -> "§7右の一覧からレシピを選ぼう";
            case AlchemyService.NO_MATERIAL -> "§7素材A・Bの枠に素材を置こう";
            case AlchemyService.NO_XP -> "§c経験値レベルが足りない";
            default -> "";
        };
        return Component.literal(s);
    }

    // ---------------- 操作 ----------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        if (mode == 1 && in(mx, my, x, y, LIST_W, ROW * ROWS)) {
            int i = scroll + (int) ((my - y) / ROW);
            if (i < Transmutes.LIST.size()) click(AlchemyMenu.BTN_RECIPE + i);
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
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        if (in(mouseX, mouseY, x, y, LIST_W, ROW * ROWS)) {
            int i = scroll + (mouseY - y) / ROW;
            List<Component> tip = new ArrayList<>();
            if (mode == 0) {
                List<Integer> rows = brewRows();
                if (i < rows.size() && rows.get(i) >= 0) {
                    Elixirs.Type t = Elixirs.TYPES.get(rows.get(i));
                    tip.add(Component.literal(t.name()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                    tip.add(Component.literal("主素材：").withStyle(ChatFormatting.GRAY).append(t.main().get().getDescription().copy().withStyle(ChatFormatting.AQUA)));
                    for (String s : Elixirs.describe(t, 1, 1)) tip.add(Component.literal(s).withStyle(ChatFormatting.WHITE));
                    if (!t.dustName().isEmpty()) tip.add(Component.literal("粉塵にすると「" + t.dustName() + "」").withStyle(ChatFormatting.DARK_GRAY));
                    tip.add(Component.literal("作れる：" + RuneText.level(t.unlock())).withStyle(ChatFormatting.GRAY));
                } else if (i < rows.size() && rows.get(i) <= -2) {
                    Elixirs.Additive a = Elixirs.ADDITIVES.get(-2 - rows.get(i));
                    tip.add(a.item().get().getDescription().copy().withStyle(ChatFormatting.AQUA));
                    tip.add(Component.literal(a.desc()).withStyle(ChatFormatting.WHITE));
                }
            } else if (i < Transmutes.LIST.size()) {
                Transmutes.Recipe r = Transmutes.LIST.get(i);
                tip.add(Component.literal("").append(r.a().get().getDescription()).append("×" + r.aCount())
                        .append(r.hasB() ? Component.literal(" ＋ ").append(r.b().get().getDescription()).append("×" + r.bCount()) : Component.empty())
                        .withStyle(ChatFormatting.WHITE));
                tip.add(Component.literal("→ ").append(r.out().get().getDescription()).append("×" + r.outCount()).withStyle(ChatFormatting.GOLD));
                tip.add(Component.literal("錬金の賢者 Lv" + r.unlock() + "・経験値Lv" + r.xp()).withStyle(ChatFormatting.GRAY));
            }
            if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 5) {
            String s = mode == 1 ? switch (hoveredSlot.index) {
                case 1 -> "素材A";
                case 2 -> "素材B";
                default -> "（錬金では使わない）";
            } : switch (hoveredSlot.index) {
                case 0 -> "ガラス瓶";
                case 1 -> "主素材（薬の種類が決まる）";
                default -> "追加素材（強化・延長・追加効果・形）";
            };
            g.renderTooltip(font, Component.literal(s), mouseX, mouseY);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(ModAlchemy.TABLE_MENU.get(), AlchemyScreen::new);
        }

        @SubscribeEvent
        public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModAlchemy.THROWN_ELIXIR.get(), ThrownItemRenderer::new);
        }

        /** 薬の形（0 飲む / 1 投げる / 2 粉塵）で絵を変える */
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                for (var item : ModAlchemy.ELIXIRS) {
                    ItemProperties.register(item.get(), JsnCraft.id("form"), (stack, level, entity, seed) -> ElixirData.form(stack));
                }
            });
        }
    }

    /** 主素材・追加素材のツールチップ */
    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Tooltips {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack.getItem() instanceof com.chigusa.jsncraft.alchemy.ElixirItem) return;
            Elixirs.Type t = Elixirs.byMain(stack.getItem());
            if (t != null) event.getToolTip().add(Component.literal("調合の主素材：" + t.name()).withStyle(ChatFormatting.GOLD));
            Elixirs.Additive a = Elixirs.byAdditive(stack.getItem());
            if (a != null) event.getToolTip().add(Component.literal("調合の追加素材：" + a.desc()).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
