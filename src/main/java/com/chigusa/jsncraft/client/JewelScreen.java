package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.jewelry.JewelData;
import com.chigusa.jsncraft.jewelry.JewelMenu;
import com.chigusa.jsncraft.jewelry.JewelryItem;
import com.chigusa.jsncraft.jewelry.JewelryService;
import com.chigusa.jsncraft.jewelry.Jewels;
import com.chigusa.jsncraft.jewelry.ModJewelry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 細工台の画面。タブ：作る / 研磨 / はめ込み / 特別（宝飾師だけ）。
 * 左：枠（金属・原石・装飾品・素材A ＋ 宝石×3）と説明、右：一覧。
 */
public class JewelScreen extends AbstractContainerScreen<JewelMenu> {
    private static final int LIST_X = 124, LIST_Y = 24, LIST_W = 124, ROW = 16, ROWS = 7;
    private static final String[] TABS = {"作る", "研磨", "はめ込み", "特別"};
    private static final String[] ACTIONS = {"作る", "まとめて磨く", "はめ込む", "作る"};
    private static final int[] BUTTONS = {JewelMenu.BTN_MAKE, JewelMenu.BTN_POLISH, JewelMenu.BTN_SOCKET, JewelMenu.BTN_UNIQUE};

    private int mode = 0, scroll = 0;
    private final Button[] tabs = new Button[4];
    private Button action;

    public JewelScreen(JewelMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 256;
        imageHeight = 228;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 4; i++) {
            int m = i;
            tabs[i] = addRenderableWidget(Button.builder(Component.literal(TABS[i]), b -> {
                mode = m;
                scroll = 0;
            }).bounds(leftPos + 8 + i * 61, topPos + 4, 58, 15).build());
        }
        action = addRenderableWidget(Button.builder(Component.literal(ACTIONS[0]), b -> click(BUTTONS[mode]))
                .bounds(leftPos + 8, topPos + 123, 110, 15).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    // ---------------- 一覧 ----------------

    /** 行：0〜 種類 / 100〜 台座 / 200〜 宝石 / 300〜 特別 / -1 見出し「台座」/ -2 見出し「宝石」 */
    private List<Integer> rows() {
        List<Integer> out = new ArrayList<>();
        switch (mode) {
            case 0 -> {
                for (Jewels.Kind k : Jewels.KINDS) out.add(k.index());
                out.add(-1);
                for (Jewels.Tier t : Jewels.TIERS) out.add(100 + t.index());
                out.add(-2);
                for (Jewels.Gem g : Jewels.GEMS) out.add(200 + g.index());
            }
            case 1, 2 -> {
                for (Jewels.Gem g : Jewels.GEMS) out.add(200 + g.index());
            }
            default -> {
                for (Jewels.Unique u : Jewels.UNIQUES) out.add(300 + u.index());
            }
        }
        return out;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < 4; i++) tabs[i].active = mode != i;
        action.setMessage(Component.literal(ACTIONS[mode]));
        action.active = menu.status(mode) == JewelryService.OK;
        scroll = Math.max(0, Math.min(scroll, rows().size() - ROWS));
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF101820);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF34465A);
        g.fill(x + 4, y + 22, x + 120, y + 141, 0xFF26343F);
        g.fill(x + LIST_X - 2, y + LIST_Y - 2, x + LIST_X + LIST_W + 2, y + LIST_Y + ROW * ROWS + 2, 0xFF1A2430);
        for (Slot s : menu.slots) {
            boolean unused = (mode == 1 && s.index >= 1 && s.index <= 3) || (mode == 3 && s.index >= 2 && s.index <= 3);
            int c = s.index == 0 ? 0xFFC8A050 : s.index <= 3 ? (unused ? 0xFF2A3440 : 0xFF50B0D0) : 0xFF1E2A36;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, unused ? 0xFF3A4450 : 0xFF7A8490);
        }
        String label0 = switch (mode) {
            case 0 -> "台座の金属";
            case 1 -> "原石";
            case 2 -> "装飾品";
            default -> "素材A";
        };
        g.drawString(font, label0, x + 36, y + 28, 0xFFE0D0A0, false);
        g.drawString(font, mode == 3 ? "↑素材B" : mode == 1 ? "" : "宝石", x + 72, y + 56, 0xFFA0D8F0, false);

        int ly = y + LIST_Y;
        List<Integer> rows = rows();
        int lv = menu.jewelLevel();
        for (int i = scroll; i < rows.size() && i < scroll + ROWS; i++) {
            int r = rows.get(i);
            boolean hover = in(mouseX, mouseY, x + LIST_X, ly, LIST_W, ROW);
            if (r < 0) {
                g.drawString(font, r == -1 ? "― 台座（ランク）―" : "― 宝石 ―", x + LIST_X + 4, ly + 4, 0xFFC8B080, false);
            } else if (r < 100) {
                Jewels.Kind k = Jewels.KINDS.get(r);
                boolean sel = r == menu.kind();
                if (sel) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF3A6A8A);
                else if (hover) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF2A3A4A);
                g.renderItem(new ItemStack(ModJewelry.jewelry(Jewels.TIERS.get(2), k)), x + LIST_X, ly);
                g.drawString(font, k.name(), x + LIST_X + 18, ly + 4, 0xFFF0F0F0, false);
                String b = Jewels.format(k.base(), 1);
                g.drawString(font, font.plainSubstrByWidth(b, LIST_W - 56), x + LIST_X + 52, ly + 4, 0xFF9AB8D0, false);
            } else if (r < 200) {
                Jewels.Tier t = Jewels.TIERS.get(r - 100);
                boolean locked = lv < t.unlock();
                g.renderItem(new ItemStack(t.metal().get()), x + LIST_X, ly);
                g.drawString(font, t.name(), x + LIST_X + 18, ly + 4, locked ? 0xFF7A8490 : 0xFFF0F0F0, false);
                String info = locked ? RuneText.level(t.unlock()) : "穴" + t.sockets() + " ×" + Jewels.num(t.power());
                g.drawString(font, info, x + LIST_X + LIST_W - 2 - font.width(info), ly + 4, locked ? 0xFF9A7A6A : 0xFFE0C870, false);
            } else if (r < 300) {
                Jewels.Gem gem = Jewels.GEMS.get(r - 200);
                if (hover) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF2A3A4A);
                g.renderItem(new ItemStack((mode == 1 ? ModJewelry.RAW_GEMS : ModJewelry.GEMS).get(gem.index()).get()), x + LIST_X, ly);
                g.drawString(font, gem.name(), x + LIST_X + 18, ly + 4, 0xFFF0F0F0, false);
                String s = gem.stats().get(0).label();
                g.drawString(font, font.plainSubstrByWidth(s, 50), x + LIST_X + LIST_W - 2 - Math.min(50, font.width(s)), ly + 4, 0xFF9AB8D0, false);
            } else {
                Jewels.Unique u = Jewels.UNIQUES.get(r - 300);
                boolean sel = u.index() == menu.unique();
                boolean locked = !menu.master() || lv - 50 < u.unlock();
                if (sel) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF3A6A8A);
                else if (hover) g.fill(x + LIST_X, ly, x + LIST_X + LIST_W, ly + ROW, 0xFF2A3A4A);
                g.renderItem(new ItemStack(ModJewelry.UNIQUES.get(u.index()).get()), x + LIST_X, ly);
                g.drawString(font, font.plainSubstrByWidth(u.name(), LIST_W - 46), x + LIST_X + 18, ly + 4, locked ? 0xFF7A8490 : 0xFFF0D8FF, false);
                String tag = "Lv" + u.unlock();
                g.drawString(font, tag, x + LIST_X + LIST_W - 2 - font.width(tag), ly + 4, locked ? 0xFF9A7A6A : 0xFFE0C870, false);
            }
            ly += ROW;
        }
        int n = rows.size();
        if (n > ROWS) {
            int h = ROW * ROWS, barH = Math.max(8, h * ROWS / n);
            int barY = y + LIST_Y + (h - barH) * scroll / Math.max(1, n - ROWS);
            g.fill(x + LIST_X + LIST_W, barY, x + LIST_X + LIST_W + 2, barY + barH, 0xFFA0D8F0);
        }
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ---------------- 左の説明 ----------------

    private List<String> gemsInSlots() {
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Jewels.Gem gem = ModJewelry.gemOf(menu.slotItem(i).getItem(), false);
            if (gem != null) out.add(gem.id());
        }
        return out;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        int st = menu.status(mode);
        switch (mode) {
            case 0 -> {
                Jewels.Kind k = menu.kind() >= 0 ? Jewels.KINDS.get(menu.kind()) : null;
                Jewels.Tier t = Jewels.tierByMetal(menu.slotItem(0).getItem());
                if (k != null && t != null) {
                    lines.add(Component.literal(t.name() + "の" + k.name() + "（穴" + t.sockets() + "）").withStyle(ChatFormatting.AQUA));
                    lines.add(Component.literal(Jewels.format(k.base(), t.power())).withStyle(ChatFormatting.WHITE));
                    for (String id : gemsInSlots()) {
                        Jewels.Gem gem = Jewels.gem(id);
                        double m = t.power() * (gem.affinity().equals(k.id()) ? 1.25 : 1);
                        for (Jewels.Stat s : gem.stats()) lines.add(Component.literal(Jewels.format(s, m)).withStyle(ChatFormatting.WHITE));
                    }
                } else if (t != null) {
                    lines.add(Component.literal(t.name() + "×" + t.count() + "で作れる（穴" + t.sockets() + "）").withStyle(ChatFormatting.GRAY));
                }
            }
            case 1 -> {
                Jewels.Gem gem = ModJewelry.gemOf(menu.slotItem(0).getItem(), true);
                if (gem != null) lines.add(Component.literal(gem.name() + "の原石×" + menu.slotItem(0).getCount() + " を磨く").withStyle(ChatFormatting.AQUA));
            }
            case 2 -> {
                if (menu.slotItem(0).getItem() instanceof JewelryItem j && j.unique() == null) {
                    JewelData d = j.data(menu.slotItem(0));
                    lines.add(Component.literal("今：" + names(d.gems())).withStyle(ChatFormatting.GRAY));
                    List<String> now = gemsInSlots();
                    lines.add(Component.literal("→ " + (now.isEmpty() ? "宝石を外す" : names(now))).withStyle(ChatFormatting.AQUA));
                    lines.add(Component.literal("外した宝石は手元に戻る").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            default -> {
                Jewels.Unique u = menu.unique() >= 0 ? Jewels.UNIQUES.get(menu.unique()) : null;
                if (u != null) {
                    lines.add(Component.literal(u.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    lines.add(Component.literal("A：").append(u.a().get().getDescription()).append("×" + u.aCount()).withStyle(ChatFormatting.WHITE));
                    lines.add(Component.literal("B：").append(u.b().get().getDescription()).append("×" + u.bCount()).withStyle(ChatFormatting.WHITE));
                }
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

    private static String names(List<String> gems) {
        if (gems.isEmpty()) return "なし";
        StringBuilder sb = new StringBuilder();
        for (String id : gems) sb.append(sb.length() > 0 ? "・" : "").append(Jewels.gem(id).name());
        return sb.toString();
    }

    private Component status(int st) {
        String s = switch (st) {
            case JewelryService.OK -> switch (mode) {
                case 0 -> "§a作れます";
                case 1 -> "§a磨けます";
                case 2 -> "§aはめ込めます";
                default -> "§a作れます";
            };
            case JewelryService.NOT_JEWELER -> "§c使えるのは細工師だけです";
            case JewelryService.NO_SELECT -> mode == 3 ? "§7右の一覧から選ぼう" : "§7右の一覧から種類を選ぼう";
            case JewelryService.NO_METAL -> "§7台座の金属を置こう";
            case JewelryService.BAD_METAL -> "§cこれは台座にならない";
            case JewelryService.LEVEL_LOW -> "§c細工の腕が足りない";
            case JewelryService.TOO_MANY_GEMS -> "§c宝石が穴の数より多い";
            case JewelryService.BAD_GEM -> "§c宝石の枠には磨いた宝石だけ";
            case JewelryService.NO_RAW -> "§7原石を置こう";
            case JewelryService.NO_JEWELRY -> "§7装飾品を置こう（宝石なしで外す）";
            case JewelryService.NOT_MASTER -> "§c特別な装飾品は宝飾師だけ";
            case JewelryService.NO_MATERIAL -> "§7素材A・Bを置こう";
            case JewelryService.NEED_SKILL -> "§cスキル「至宝の細工」が必要";
            case JewelryService.NOT_ENOUGH_METAL -> "§c金属が足りない";
            case JewelryService.UNIQUE_ITEM -> "§c特別な装飾品には宝石をはめられない";
            default -> "";
        };
        return Component.literal(s);
    }

    // ---------------- 操作 ----------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        if (in(mx, my, x, y, LIST_W, ROW * ROWS)) {
            int i = scroll + (int) ((my - y) / ROW);
            List<Integer> rows = rows();
            if (i < rows.size()) {
                int r = rows.get(i);
                if (r >= 0 && r < 100) click(JewelMenu.BTN_KIND + r);
                else if (r >= 300) click(JewelMenu.BTN_PICK_UNIQUE + (r - 300));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (in(mx, my, leftPos + LIST_X, topPos + LIST_Y, LIST_W + 4, ROW * ROWS)) {
            scroll = Math.max(0, Math.min(Math.max(0, rows().size() - ROWS), scroll - (int) Math.signum(sy)));
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
            List<Integer> rows = rows();
            List<Component> tip = new ArrayList<>();
            if (i < rows.size()) {
                int r = rows.get(i);
                if (r >= 0 && r < 100) {
                    Jewels.Kind k = Jewels.KINDS.get(r);
                    tip.add(Component.literal(k.name()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                    tip.add(Component.literal("宝石なしでも：" + Jewels.format(k.base(), 1) + "（台座の倍率がかかる）").withStyle(ChatFormatting.WHITE));
                    StringBuilder sb = new StringBuilder();
                    for (Jewels.Gem gem : Jewels.GEMS) if (gem.affinity().equals(k.id())) sb.append(sb.length() > 0 ? "・" : "").append(gem.name());
                    tip.add(Component.literal("相性のいい宝石（×1.25）：" + sb).withStyle(ChatFormatting.GRAY));
                } else if (r >= 100 && r < 200) {
                    Jewels.Tier t = Jewels.TIERS.get(r - 100);
                    tip.add(Component.literal(t.name() + "の台座").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    tip.add(Component.literal("金属：").append(t.metal().get().getDescription()).append("×" + t.count()).withStyle(ChatFormatting.WHITE));
                    tip.add(Component.literal("宝石の穴 " + t.sockets() + "・効果 ×" + Jewels.num(t.power())).withStyle(ChatFormatting.WHITE));
                    tip.add(Component.literal("作れる：" + RuneText.level(t.unlock())).withStyle(ChatFormatting.GRAY));
                } else if (r >= 200 && r < 300) {
                    tip.addAll(gemTip(Jewels.GEMS.get(r - 200)));
                } else if (r >= 300) {
                    Jewels.Unique u = Jewels.UNIQUES.get(r - 300);
                    tip.add(Component.literal(u.name()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                    tip.add(Component.literal("特別な" + Jewels.KINDS.get(u.kind()).name()).withStyle(ChatFormatting.GRAY));
                    for (Jewels.Stat s : u.stats()) tip.add(Component.literal(Jewels.format(s, 1)).withStyle(ChatFormatting.WHITE));
                    if (!u.specialText().isEmpty()) tip.add(Component.literal("✦" + u.specialText()).withStyle(ChatFormatting.GOLD));
                    tip.add(Component.literal("").append(u.a().get().getDescription()).append("×" + u.aCount() + " ＋ ")
                            .append(u.b().get().getDescription()).append("×" + u.bCount()).withStyle(ChatFormatting.GRAY));
                    tip.add(Component.literal("輝石の宝飾師 Lv" + u.unlock()).withStyle(ChatFormatting.GRAY));
                }
            }
            if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    static List<Component> gemTip(Jewels.Gem gem) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal(gem.name()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        for (Jewels.Stat s : gem.stats()) tip.add(Component.literal(Jewels.format(s, 1) + "（台座の倍率がかかる）").withStyle(ChatFormatting.WHITE));
        tip.add(Component.literal("相性：" + Jewels.kind(gem.affinity()).name() + "（×1.25）").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(gem.rarity() == 2 ? "めずらしい" : gem.rarity() == 1 ? "ふつう" : "よく見つかる").withStyle(ChatFormatting.DARK_GRAY));
        return tip;
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(ModJewelry.TABLE_MENU.get(), JewelScreen::new);
        }

        /** 装飾品の宝石の部分に、1つめの宝石の色をつける（宝石なしは灰色） */
        @SubscribeEvent
        public static void onColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
            for (var item : ModJewelry.JEWELRY.values()) {
                event.register((stack, tint) -> {
                    if (tint != 1 || !(stack.getItem() instanceof JewelryItem j)) return -1;
                    var gems = j.data(stack).gems();
                    Jewels.Gem gem = gems.isEmpty() ? null : Jewels.gem(gems.get(0));
                    return 0xFF000000 | (gem == null ? 0x505860 : gem.color());
                }, item.get());
            }
        }
    }

    /** 宝石・原石のツールチップ */
    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Tooltips {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            var item = event.getItemStack().getItem();
            Jewels.Gem gem = ModJewelry.gemOf(item, false);
            if (gem != null) {
                List<Component> t = gemTip(gem);
                event.getToolTip().addAll(t.subList(1, t.size()));
            }
            Jewels.Gem raw = ModJewelry.gemOf(item, true);
            if (raw != null) event.getToolTip().add(Component.literal("細工台で磨くと" + raw.name() + "になる").withStyle(ChatFormatting.GRAY));
            Jewels.Tier tier = Jewels.tierByMetal(item);
            if (tier != null) event.getToolTip().add(Component.literal("装飾品の台座（" + tier.name() + "×" + tier.count() + "・穴" + tier.sockets() + "）").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
