package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.smithing.ForgeMenu;
import com.chigusa.jsncraft.smithing.ModSmithing;
import com.chigusa.jsncraft.smithing.SmithData;
import com.chigusa.jsncraft.smithing.SmithTraits;
import com.chigusa.jsncraft.smithing.SmithingService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

/** 鍛冶台の画面：左に鍛える物、真ん中に素材。右のボタンで強化・鍛え直し・修理。 */
public class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    private Button enhanceBtn, reforge1Btn, reforge2Btn, repairBtn, craftBtn, makeBtn, backBtn;
    /** 製作の画面（true）／鍛冶の画面（false） */
    private boolean craftMode;

    public ForgeScreen(ForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(4, (width - imageWidth - PANEL_W - 4) / 2);   // 右のレシピ一覧の分、左に寄せる
        int x = leftPos + 98;
        enhanceBtn = addRenderableWidget(Button.builder(Component.literal("強化する"), b -> startGame()).bounds(x, topPos + 4, 72, 12).build());
        reforge1Btn = addRenderableWidget(Button.builder(Component.literal("鍛え直す"), b -> click(1)).bounds(x, topPos + 17, 72, 12).build());
        reforge2Btn = addRenderableWidget(Button.builder(Component.literal("鍛え直す(2)"), b -> click(2)).bounds(x, topPos + 30, 72, 12).build());
        repairBtn = addRenderableWidget(Button.builder(Component.literal("修理する"), b -> click(3)).bounds(x, topPos + 43, 72, 12).build());
        craftBtn = addRenderableWidget(Button.builder(Component.literal("製作する ▶"), b -> setCraftMode(true)).bounds(x, topPos + 56, 72, 12).build());
        makeBtn = addRenderableWidget(Button.builder(Component.literal("作る"), b -> click(hasShiftDown() ? 5 : 4))
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Shift＋クリックで、材料がある分だけまとめて作る")))
                .bounds(leftPos + 124, topPos + 22, 46, 16).build());
        backBtn = addRenderableWidget(Button.builder(Component.literal("◀ 鍛冶へ"), b -> setCraftMode(false)).bounds(leftPos + 124, topPos + 42, 46, 12).build());
        setCraftMode(craftMode);
    }

    private void setCraftMode(boolean on) {
        craftMode = on;
        setButtons(!game);
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    // ================= 製作レシピ（画面の右の一覧） =================

    private static final int PANEL_W = 150, ROW = 18;
    private int recipeScroll;
    /** 一覧のタブ：false 鍛冶師 / true 名工（上位職） */
    private boolean masterTab;

    /** 今のタブに出すレシピ（元の番号） */
    private List<Integer> shown() {
        List<Integer> out = new java.util.ArrayList<>();
        var list = SmithingService.CRAFTS;
        for (int i = 0; i < list.size(); i++) if ((list.get(i).minLevel() > 50) == masterTab) out.add(i);
        return out;
    }

    private int panelX() {
        return leftPos + imageWidth + 4;
    }

    private int panelRows() {
        return (imageHeight - 22) / ROW;
    }

    private static int count(net.minecraft.world.item.Item item) {
        var p = net.minecraft.client.Minecraft.getInstance().player;
        if (p == null || item == null) return 0;
        int n = 0;
        for (ItemStack st : p.getInventory().items) if (st.is(item)) n += st.getCount();
        return n;
    }

    private void renderRecipes(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX(), y = topPos, h = imageHeight;
        g.fill(x, y, x + PANEL_W, y + h, 0xFF1E1E22);
        g.fill(x + 2, y + 2, x + PANEL_W - 2, y + h - 2, 0xFF7A7A82);
        var list = SmithingService.CRAFTS;
        List<Integer> ord = shown();
        // タブ：鍛冶師／名工
        String a = "鍛冶師", b = "名工（上位職）";
        int aw = font.width(a), bw = font.width(b);
        g.fill(x + 4, y + 4, x + 10 + aw, y + 16, masterTab ? 0x00000000 : 0xFF3A3A42);
        g.fill(x + 14 + aw, y + 4, x + 20 + aw + bw, y + 16, masterTab ? 0xFF6A4A10 : 0x00000000);
        g.drawString(font, a, x + 7, y + 6, masterTab ? 0xFF4A4A52 : 0xFFE8E8E8, false);
        g.drawString(font, b, x + 17 + aw, y + 6, masterTab ? 0xFFFFD870 : 0xFF4A4A52, false);
        int n = panelRows();
        recipeScroll = Math.max(0, Math.min(recipeScroll, Math.max(0, ord.size() - n)));
        int lv = menu.smithLevel();
        int ly = y + 18;
        for (int k = recipeScroll; k < ord.size() && k < recipeScroll + n; k++) {
            var c = list.get(ord.get(k));
            boolean hover = mouseX >= x + 2 && mouseX < x + PANEL_W - 4 && mouseY >= ly && mouseY < ly + ROW;
            boolean have = count(c.input()) >= c.count() && (c.base() == null || count(c.base()) >= c.baseCount());
            if (hover) g.fill(x + 3, ly, x + PANEL_W - 5, ly + ROW, 0x30FFFFFF);
            g.renderItem(new ItemStack(c.output().get()), x + 4, ly + 1);
            int col = lv < c.minLevel() ? 0xFF6A3A3A : have ? 0xFF1A1A1A : 0xFF4A4A52;
            g.drawString(font, font.plainSubstrByWidth(c.name(), PANEL_W - 56), x + 22, ly + 5, col, false);
            String lvs = "Lv" + (c.minLevel() > 50 ? c.minLevel() - 50 : c.minLevel());
            g.drawString(font, lvs, x + PANEL_W - 8 - font.width(lvs), ly + 5, lv < c.minLevel() ? 0xFFA03030 : 0xFF3A3A42, false);
            ly += ROW;
        }
        if (ord.size() > n) {
            int barH = Math.max(8, (h - 22) * n / ord.size());
            int barY = y + 18 + (h - 22 - barH) * recipeScroll / Math.max(1, ord.size() - n);
            g.fill(x + PANEL_W - 5, barY, x + PANEL_W - 3, barY + barH, 0xFF3A3A42);
        }
    }

    /** タブを押したら true */
    private boolean clickTab(double mx, double my) {
        int x = panelX(), y = topPos;
        if (my < y + 4 || my >= y + 16 || mx < x + 4 || mx >= x + PANEL_W) return false;
        boolean m = mx >= x + 12 + font.width("鍛冶師");
        if (m != masterTab) {
            masterTab = m;
            recipeScroll = 0;
        }
        return true;
    }

    private int recipeAt(double mx, double my) {
        int x = panelX(), y = topPos;
        if (mx < x + 2 || mx >= x + PANEL_W - 4 || my < y + 18 || my >= y + imageHeight - 4) return -1;
        int k = recipeScroll + (int) ((my - y - 18) / ROW);
        List<Integer> ord = shown();
        if (k >= ord.size() || k >= recipeScroll + panelRows()) return -1;
        return ord.get(k);
    }

    private void renderRecipeTooltip(GuiGraphics g, int mx, int my) {
        int i = recipeAt(mx, my);
        if (i < 0) return;
        var c = SmithingService.CRAFTS.get(i);
        List<Component> tip = new java.util.ArrayList<>();
        tip.add(Component.literal(c.name() + " ×" + c.outCount()).withStyle(ChatFormatting.GOLD));
        boolean lvOk = menu.smithLevel() >= c.minLevel();
        tip.add(Component.literal("必要なレベル：" + SmithingService.levelText(c.minLevel()) + (c.minLevel() > 50 ? "（上位職）" : "")).withStyle(lvOk ? ChatFormatting.GRAY : ChatFormatting.RED));
        if (c.base() != null) tip.add(need("土台（左の枠）", c.base(), c.baseCount()));
        tip.add(need("素材（右の枠）", c.input(), c.count()));
        if (c.quality()) tip.add(Component.literal("品質 ★1〜3（鍛冶の腕が高いほど★が出やすい）").withStyle(ChatFormatting.AQUA));
        if (minecraft != null && minecraft.level != null) {
            var lines = new ItemStack(c.output().get()).getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, net.minecraft.world.item.TooltipFlag.NORMAL);
            for (int k = 1; k < lines.size(); k++) tip.add(lines.get(k));
        }
        tip.add(Component.literal("クリックで持ち物から材料を入れる").withStyle(ChatFormatting.DARK_GRAY));
        g.renderComponentTooltip(font, tip, mx, my);
    }

    private static Component need(String label, net.minecraft.world.item.Item item, int n) {
        int have = count(item);
        boolean ok = have >= n;
        return Component.literal((ok ? " ✔ " : " ・ ") + label + "：").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.WHITE)
                .append(new ItemStack(item).getHoverName().copy()).append(" ×" + n + "（持っている：" + have + "）");
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int x = panelX();
        if (mx >= x && mx < x + PANEL_W && my >= topPos && my < topPos + imageHeight) {
            recipeScroll = Math.max(0, recipeScroll - (int) Math.signum(sy) * 2);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ================= 強化ミニゲーム =================
    // 火床の上を行き来する印を、光っている所でタイミングよく止めて5回叩く。
    // 真ん中（会心）なら PERFECT、光の中なら GOOD、外れたら MISS。
    // MISS 2回以上 → 失敗 / MISS なしで PERFECT 4回以上 → 大成功 / それ以外 → 成功

    private static final int STRIKES = 5;
    private boolean game;
    private final int[] marks = new int[STRIKES];
    private int strike;
    private long phaseStart, finishAt;
    private double zone, goodHalf, perfectHalf, period;
    private String flash = "";
    private long flashUntil;
    private final java.util.Random rnd = new java.util.Random();

    private void startGame() {
        if ((menu.flags() & ForgeMenu.CAN_ENHANCE) == 0 || game || craftMode) return;
        click(0);
        game = true;
        strike = 0;
        finishAt = 0;
        java.util.Arrays.fill(marks, -1);
        double rate = Math.max(0.03, menu.ratePermille() / 1000.0);
        int plus = SmithData.plus(menu.item());
        goodHalf = 0.04 + 0.21 * rate;
        perfectHalf = goodHalf * Math.min(0.7, 0.3 + menu.greatPermille() / 1000.0 * 2);
        period = Math.max(0.55, 1.6 - 0.035 * plus);
        newZone();
        phaseStart = System.nanoTime();
        setButtons(false);
    }

    private void newZone() {
        double m = goodHalf + 0.04;
        zone = m + rnd.nextDouble() * Math.max(0, 1 - 2 * m);
    }

    /** 印の位置（0〜1を往復） */
    private double cursor() {
        double t = (System.nanoTime() - phaseStart) / 1e9 / period;
        double f = t - Math.floor(t);
        return f < 0.5 ? f * 2 : 2 - f * 2;
    }

    private void hit() {
        if (!game || strike >= STRIKES) return;
        double d = Math.abs(cursor() - zone);
        int m = d <= perfectHalf ? 2 : d <= goodHalf ? 1 : 0;
        marks[strike++] = m;
        flash = m == 2 ? "§6§lPERFECT!" : m == 1 ? "§aGOOD" : "§7MISS…";
        flashUntil = System.currentTimeMillis() + 450;
        if (minecraft != null) {
            var snd = m == 0 ? net.minecraft.sounds.SoundEvents.ANVIL_LAND : net.minecraft.sounds.SoundEvents.ANVIL_PLACE;
            float pitch = m == 2 ? 1.6F : m == 1 ? 1.2F : 0.6F;
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(snd, pitch, m == 0 ? 0.3F : 0.45F));
        }
        if (strike >= STRIKES) {
            finishAt = System.currentTimeMillis() + 700;
        } else {
            newZone();
            phaseStart = System.nanoTime() - (long) (rnd.nextDouble() * period * 1e9);
        }
    }

    private int outcome() {
        int miss = 0, perfect = 0;
        for (int m : marks) {
            if (m == 0) miss++;
            if (m == 2) perfect++;
        }
        if (miss >= 2) return 0;
        if (miss == 0 && perfect >= 4) return 2;
        return 1;
    }

    private void setButtons(boolean on) {
        for (Button b : new Button[]{enhanceBtn, reforge1Btn, reforge2Btn, repairBtn, craftBtn}) b.visible = on && !craftMode;
        if (on && !craftMode) reforge2Btn.visible = menu.traitSlots() >= 2;
        makeBtn.visible = on && craftMode;
        backBtn.visible = on && craftMode;
    }

    private void renderGame(GuiGraphics g) {
        int x = leftPos + 4, y = topPos + 4, w = imageWidth - 8, h = 68;
        g.fill(x, y, x + w, y + h, 0xF0201814);
        g.drawString(font, "§6⚒ 強化！ §7光った所でクリック", x + 6, y + 4, 0xFFFFFF, false);
        // 火床のバー
        int bx = x + 8, by = y + 20, bw = w - 16, bh = 14;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF000000);
        g.fill(bx, by, bx + bw, by + bh, 0xFF3A2A22);
        if (strike < STRIKES) {
            int gz0 = bx + (int) ((zone - goodHalf) * bw), gz1 = bx + (int) ((zone + goodHalf) * bw);
            int pz0 = bx + (int) ((zone - perfectHalf) * bw), pz1 = bx + (int) ((zone + perfectHalf) * bw);
            g.fill(Math.max(bx, gz0), by, Math.min(bx + bw, gz1), by + bh, 0xFFE07828);
            g.fill(Math.max(bx, pz0), by, Math.min(bx + bw, pz1), by + bh, 0xFFFFE070);
            int cx = bx + (int) (cursor() * bw);
            g.fill(cx - 1, by - 4, cx + 2, by + bh + 4, 0xFFFFFFFF);
        }
        // 5回の結果
        for (int i = 0; i < STRIKES; i++) {
            int ix = x + w / 2 - STRIKES * 11 + i * 22, iy = y + 40;
            int c = marks[i] == 2 ? 0xFFFFD040 : marks[i] == 1 ? 0xFF60D060 : marks[i] == 0 ? 0xFF808080 : 0xFF4A4038;
            g.fill(ix, iy, ix + 18, iy + 10, 0xFF000000);
            g.fill(ix + 1, iy + 1, ix + 17, iy + 9, c);
            if (i == strike && strike < STRIKES) g.fill(ix, iy + 11, ix + 18, iy + 12, 0xFFFFFFFF);
        }
        String msg = System.currentTimeMillis() < flashUntil ? flash : "";
        if (strike >= STRIKES) {
            int o = outcome();
            msg = o == 2 ? "§6§l大成功！" : o == 1 ? "§a§l成功！" : "§c§l失敗…";
        }
        if (!msg.isEmpty()) g.drawCenteredString(font, msg, x + w / 2, y + 55, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!game && clickTab(mx, my)) return true;
        if (!game) {
            int r = recipeAt(mx, my);
            if (r >= 0) {
                setCraftMode(true);
                click(200 + r);
                return true;
            }
        }
        if (game) {
            if (mx >= leftPos && mx < leftPos + imageWidth && my >= topPos && my < topPos + 72) {
                hit();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (game && key == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE) {
            hit();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (game) {
            if (finishAt > 0 && System.currentTimeMillis() >= finishAt) {
                click(10 + outcome());
                game = false;
                setButtons(true);
            } else {
                return;
            }
        }
        if (!craftMode && menu.item().isEmpty() && menu.craftInfo() != 0) setCraftMode(true);
        int f = menu.flags();
        enhanceBtn.active = (f & ForgeMenu.CAN_ENHANCE) != 0;
        reforge1Btn.active = (f & ForgeMenu.CAN_REFORGE1) != 0;
        reforge2Btn.visible = menu.traitSlots() >= 2;
        reforge2Btn.active = (f & ForgeMenu.CAN_REFORGE2) != 0;
        repairBtn.active = (f & ForgeMenu.CAN_REPAIR) != 0;
        craftBtn.active = true;
        makeBtn.active = (f & ForgeMenu.CAN_CRAFT) != 0;
        reforge1Btn.setMessage(Component.literal(menu.traitSlots() >= 2 ? "鍛え直す(1)" : "鍛え直す"));
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E22);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF7A7A82);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 72, 0xFF5A5A62);
        for (Slot s : menu.slots) {
            int c = s.index == 0 ? 0xFFB06A20 : s.index == 1 ? 0xFF4A6A9A : 0xFF3A3A42;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
        g.drawString(font, "＋", x + 46, y + 30, 0xFFE8C060, false);
        if (craftMode) {
            g.drawString(font, "→", x + 84, y + 30, 0xFFE8C060, false);
            g.fill(x + 97, y + 25, x + 115, y + 43, 0xFF3A7A3A);
            g.fill(x + 98, y + 26, x + 114, y + 42, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFFE8E8E8, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF2A2A2A, false);
        g.drawString(font, craftMode ? "土台" : "鍛える物", craftMode ? 26 : 14, 16, 0xFFD8D8D8, false);
        g.drawString(font, "素材", 60, 16, 0xFFD8D8D8, false);
        if (craftMode) {
            g.drawString(font, "出来上がり", 84, 16, 0xFFD8D8D8, false);
            int ci = menu.craftInfo();
            String l1, l2 = "";
            if (menu.status() == ForgeMenu.ST_NOT_SMITH || menu.smithLevel() < 0) {
                l1 = "§c作れるのは鍛冶師だけです";
            } else if (ci == 0) {
                l1 = "§7右の一覧から選ぼう";
                l2 = "§7（材料が自動で入る）";
            } else {
                var c = SmithingService.CRAFTS.get(Math.abs(ci) - 1);
                ItemStack out = new ItemStack(c.output().get(), c.outCount());
                g.renderItem(out, 98, 26);
                g.renderItemDecorations(font, out, 98, 26);
                l1 = (ci > 0 ? "§f" : "§c") + c.name() + " ×" + c.outCount();
                int haveMat = menu.slots.get(1).getItem().getCount(), haveBase = menu.slots.get(0).getItem().getCount();
                if (ci < 0) l2 = "§c" + SmithingService.levelText(c.minLevel()) + " から";
                else if (haveMat < c.count()) l2 = "§c素材が足りない（×" + c.count() + " 必要）";
                else if (c.base() != null && haveBase < c.baseCount()) l2 = "§c土台が足りない（×" + c.baseCount() + " 必要）";
                else l2 = c.quality() ? "§b★は鍛冶の腕次第" : "§a作れます";
            }
            g.drawString(font, l1, 8, 48, 0xFFFFFF, false);
            if (!l2.isEmpty()) g.drawString(font, l2, 8, 59, 0xFFFFFF, false);
            return;
        }
        ItemStack item = menu.item();
        int st = menu.status();
        String line1, line2 = "";
        int craftInfo = menu.craftInfo();

        switch (st) {
            case ForgeMenu.ST_EMPTY -> {
                line1 = "§7鍛える物を置こう";
            }
            case ForgeMenu.ST_NOT_SMITH -> line1 = "§c鍛えられるのは鍛冶師だけです";
            case ForgeMenu.ST_CANT -> line1 = "§7これは強化できない（修理のみ）";
            case ForgeMenu.ST_MAX -> line1 = "§6+" + SmithData.plus(item) + "（今の上限 +" + menu.maxPlus() + "）";
            default -> {
                int plus = SmithData.plus(item);
                int stars = Math.max(1, Math.min(5, 5 - (int) Math.round(menu.ratePermille() / 1000.0 * 4)));
                line1 = "§f+" + plus + " → +" + (plus + 1) + " §e" + "★".repeat(stars) + "§8" + "★".repeat(5 - stars);
                line2 = "§7必要：" + SmithingService.TIER_NAMES[SmithingService.tierFor(plus + 1)] + "の強化石";
            }
        }
        if (!item.isEmpty() && item.getMaxDamage() > 0 && st != ForgeMenu.ST_NOT_SMITH && line2.isEmpty()) {
            line2 = com.chigusa.jsncraft.smithing.Durability.broken(item) ? "§c壊れている"
                    : "§7耐久 " + Math.round(com.chigusa.jsncraft.smithing.Durability.ratio(item) * 100) + "%";
        }
        g.drawString(font, line1, 8, 48, 0xFFFFFF, false);
        if (!line2.isEmpty()) g.drawString(font, line2, 8, 59, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        renderRecipes(g, mouseX, mouseY);
        if (game) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            renderGame(g);
            g.pose().popPose();
        }
        renderTooltip(g, mouseX, mouseY);
        if (!game) renderRecipeTooltip(g, mouseX, mouseY);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(ModSmithing.FORGE_MENU.get(), ForgeScreen::new);
        }
    }

    /** 切れ味ゲージ（MHの切れ味みたいに、耐久が減ると色が変わって弱くなる） */
    static void sharpTooltip(ItemStack stack, List<Component> tip) {
        if (!com.chigusa.jsncraft.smithing.Durability.isGear(stack)) return;
        String kinds = SmithData.kinds(stack);
        int at = Math.min(1, tip.size());
        int plate = com.chigusa.jsncraft.smithing.Durability.plate(stack);
        if (plate > 0) tip.add(at, Component.literal("▣ " + (com.chigusa.jsncraft.smithing.Durability.plateLevel(stack) >= 2 ? "鉄の当て板" : "当て板") + " あと" + plate + "回").withStyle(ChatFormatting.GOLD));
        if (com.chigusa.jsncraft.smithing.Durability.broken(stack)) {
            tip.add(at, Component.literal("✖ 壊れている（鍛冶師に鍛冶台で直してもらおう）").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            return;
        }
        double r = com.chigusa.jsncraft.smithing.Durability.ratio(stack);
        int band = com.chigusa.jsncraft.smithing.Durability.band(stack);
        int hone = com.chigusa.jsncraft.smithing.Durability.hone(stack);
        boolean honed = band == 3 && hone > 0;
        ChatFormatting col = honed ? (com.chigusa.jsncraft.smithing.Durability.honeLevel(stack) >= 2 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE)
                : band == 3 ? ChatFormatting.AQUA : band == 2 ? ChatFormatting.GREEN : band == 1 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        if (honed) tip.add(at, Component.literal("  研ぎ澄まし あと" + hone + "回").withStyle(col));
        int filled = (int) Math.ceil(r * 10);
        String label = kinds.contains("A") ? "耐久 " : "切れ味 ";
        String effect;
        if (kinds.contains("A")) {
            effect = "防御 " + Math.round(com.chigusa.jsncraft.smithing.Durability.armorFactor(stack) * 100) + "%";
        } else if (kinds.contains("W")) {
            effect = (SmithData.ranged(stack) ? "威力 " : "攻撃 ") + Math.round(com.chigusa.jsncraft.smithing.Durability.weaponFactor(stack) * 100) + "%";
        } else {
            effect = Math.round(r * 100) + "%";
        }
        tip.add(at, Component.literal(label).withStyle(ChatFormatting.GRAY)
                .append(Component.literal("■".repeat(filled)).withStyle(col))
                .append(Component.literal("■".repeat(10 - filled)).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(" " + effect).withStyle(col)));
    }

    /** 特性の説明と、触媒の説明をツールチップに出す */
    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Tooltips {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            List<Component> tip = event.getToolTip();
            int at = Math.min(1, tip.size());
            for (SmithData.TraitRoll r : SmithData.traits(stack)) {
                SmithTraits.Trait t = SmithTraits.TRAITS.get(r.id());
                if (t == null) continue;
                String g = SmithTraits.grade(r.ratio());
                String v = t.percent() ? String.format(Locale.ROOT, "+%.1f%%", r.value() * 100) : String.format(Locale.ROOT, "+%.1f", r.value());
                tip.add(at++, Component.literal("◆" + t.name() + " " + g + "  " + v).withStyle(SmithTraits.gradeColor(g)));
            }
            var cat = SmithTraits.CATALYSTS.get(stack.getItem());
            if (cat != null) {
                StringBuilder sb = new StringBuilder("鍛え直しの触媒：");
                if (stack.is(net.minecraft.world.item.Items.ECHO_SHARD)) sb.append("どの特性でも（強めに出る）");
                else for (int i = 0; i < cat.size(); i++) sb.append(i > 0 ? "・" : "").append(SmithTraits.TRAITS.get(cat.get(i)).name());
                tip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.DARK_AQUA));
            }
            sharpTooltip(stack, tip);
            int tier = ModSmithing.stoneTier(stack);
            if (tier >= 0) {
                int from = tier * 6 + 1;
                tip.add(Component.literal("+" + from + "〜+" + (from + 5) + " の強化に使う").withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
