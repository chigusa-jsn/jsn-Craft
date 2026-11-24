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
    private Button enhanceBtn, reforge1Btn, reforge2Btn, repairBtn, craftBtn;

    public ForgeScreen(ForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 98;
        enhanceBtn = addRenderableWidget(Button.builder(Component.literal("強化する"), b -> startGame()).bounds(x, topPos + 4, 72, 12).build());
        reforge1Btn = addRenderableWidget(Button.builder(Component.literal("鍛え直す"), b -> click(1)).bounds(x, topPos + 17, 72, 12).build());
        reforge2Btn = addRenderableWidget(Button.builder(Component.literal("鍛え直す(2)"), b -> click(2)).bounds(x, topPos + 30, 72, 12).build());
        repairBtn = addRenderableWidget(Button.builder(Component.literal("修理する"), b -> click(3)).bounds(x, topPos + 43, 72, 12).build());
        craftBtn = addRenderableWidget(Button.builder(Component.literal("製作する"), b -> click(4)).bounds(x, topPos + 56, 72, 12).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    // ================= 製作レシピ（画面の右に表示） =================

    private static final int ROWS = 8;
    private int recipeScroll;

    private int recipeX() {
        return leftPos + imageWidth + 4;
    }

    private void renderRecipes(GuiGraphics g, int mouseX, int mouseY) {
        int x = recipeX(), y = topPos;
        int w = 150, total = SmithingService.CRAFTS.size();
        int h = 30 + ROWS * 18;
        if (x + w > width) return;   // 画面が狭いときは出さない
        recipeScroll = Math.max(0, Math.min(total - ROWS, recipeScroll));
        g.fill(x, y, x + w, y + h, 0xFF1E1E22);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF5A5A62);
        g.drawString(font, "製作レシピ", x + 6, y + 5, 0xFFE8C060, false);
        g.drawString(font, "§7(ホイールで送る)", x + 62, y + 5, 0xFFFFFF, false);
        g.drawString(font, "§8左の枠 ＋ 素材 → 出来上がり", x + 6, y + 16, 0xFFFFFF, false);
        int lv = menu.smithLevel();
        ItemStack hover = ItemStack.EMPTY;
        for (int r = 0; r < ROWS && recipeScroll + r < total; r++) {
            var c = SmithingService.CRAFTS.get(recipeScroll + r);
            int ry = y + 28 + r * 18;
            boolean ok = lv >= c.minLevel();
            ItemStack base = c.base() == null ? ItemStack.EMPTY : new ItemStack(c.base(), c.baseCount());
            ItemStack in = new ItemStack(c.input(), c.count());
            ItemStack out = new ItemStack(c.output().get(), c.outCount());
            ItemStack[] icons = {base, in, out};
            int[] xs = {x + 6, x + 30, x + 60};
            if (!base.isEmpty()) g.drawString(font, "+", x + 23, ry + 4, 0xFFFFFFFF, false);
            g.drawString(font, "→", x + 48, ry + 4, 0xFFFFFFFF, false);
            for (int i = 0; i < 3; i++) {
                if (icons[i].isEmpty()) continue;
                g.renderItem(icons[i], xs[i], ry);
                g.renderItemDecorations(font, icons[i], xs[i], ry);
                if (mouseX >= xs[i] && mouseX < xs[i] + 16 && mouseY >= ry && mouseY < ry + 16) hover = icons[i];
            }
            String label = ok ? "§f" + c.name() : "§cLv" + c.minLevel() + "〜";
            g.drawString(font, label, x + 80, ry + 4, 0xFFFFFF, false);
        }
        if (total > ROWS) {
            int barH = ROWS * 18, th = Math.max(10, barH * ROWS / total);
            int ty = y + 28 + (barH - th) * recipeScroll / (total - ROWS);
            g.fill(x + w - 5, y + 28, x + w - 3, y + 28 + barH, 0xFF3A3A42);
            g.fill(x + w - 5, ty, x + w - 3, ty + th, 0xFFC8C8C8);
        }
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (mx >= recipeX() && mx < recipeX() + 150 && my >= topPos && my < topPos + 30 + ROWS * 18) {
            recipeScroll -= (int) Math.signum(sy);
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
        if ((menu.flags() & ForgeMenu.CAN_ENHANCE) == 0 || game) return;
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
        for (Button b : new Button[]{enhanceBtn, reforge1Btn, reforge2Btn, repairBtn, craftBtn}) b.visible = on;
        if (on) reforge2Btn.visible = menu.traitSlots() >= 2;
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
        int f = menu.flags();
        enhanceBtn.active = (f & ForgeMenu.CAN_ENHANCE) != 0;
        reforge1Btn.active = (f & ForgeMenu.CAN_REFORGE1) != 0;
        reforge2Btn.visible = menu.traitSlots() >= 2;
        reforge2Btn.active = (f & ForgeMenu.CAN_REFORGE2) != 0;
        repairBtn.active = (f & ForgeMenu.CAN_REPAIR) != 0;
        craftBtn.active = (f & ForgeMenu.CAN_CRAFT) != 0;
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
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFFE8E8E8, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF2A2A2A, false);
        g.drawString(font, "鍛える物", 14, 16, 0xFFD8D8D8, false);
        g.drawString(font, "素材", 60, 16, 0xFFD8D8D8, false);
        ItemStack item = menu.item();
        int st = menu.status();
        String line1, line2 = "";
        int craftInfo = menu.craftInfo();
        if (craftInfo != 0 && st != ForgeMenu.ST_NOT_SMITH) st = ForgeMenu.ST_EMPTY;
        switch (st) {
            case ForgeMenu.ST_EMPTY -> {
                int ci = menu.craftInfo();
                if (ci == 0) {
                    line1 = "§7鍛える物を置こう";
                    line2 = "§7素材だけ→製作";
                } else {
                    var c = SmithingService.CRAFTS.get(Math.abs(ci) - 1);
                    line1 = (ci > 0 ? "§f製作：" : "§c製作：") + c.name();
                    line2 = ci > 0 ? "§7→ ×" + c.outCount() + (c.quality() ? "（★は腕次第）" : "") : "§c鍛冶師Lv" + c.minLevel() + " から";
                }
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
        if (!game) renderRecipes(g, mouseX, mouseY);
        if (game) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            renderGame(g);
            g.pose().popPose();
        }
        renderTooltip(g, mouseX, mouseY);
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
        if (plate > 0) tip.add(at, Component.literal("▣ 当て板 あと" + plate + "回").withStyle(ChatFormatting.GOLD));
        if (com.chigusa.jsncraft.smithing.Durability.broken(stack)) {
            tip.add(at, Component.literal("✖ 壊れている（鍛冶師に鍛冶台で直してもらおう）").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            return;
        }
        double r = com.chigusa.jsncraft.smithing.Durability.ratio(stack);
        int band = com.chigusa.jsncraft.smithing.Durability.band(stack);
        ChatFormatting col = band == 3 ? ChatFormatting.AQUA : band == 2 ? ChatFormatting.GREEN : band == 1 ? ChatFormatting.YELLOW : ChatFormatting.RED;
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
