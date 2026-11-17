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
    private Button enhanceBtn, reforge1Btn, reforge2Btn, repairBtn;

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
        enhanceBtn = addRenderableWidget(Button.builder(Component.literal("強化する"), b -> click(0)).bounds(x, topPos + 6, 72, 14).build());
        reforge1Btn = addRenderableWidget(Button.builder(Component.literal("鍛え直す"), b -> click(1)).bounds(x, topPos + 22, 72, 14).build());
        reforge2Btn = addRenderableWidget(Button.builder(Component.literal("鍛え直す(2)"), b -> click(2)).bounds(x, topPos + 38, 72, 14).build());
        repairBtn = addRenderableWidget(Button.builder(Component.literal("修理する"), b -> click(3)).bounds(x, topPos + 54, 72, 14).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int f = menu.flags();
        enhanceBtn.active = (f & ForgeMenu.CAN_ENHANCE) != 0;
        reforge1Btn.active = (f & ForgeMenu.CAN_REFORGE1) != 0;
        reforge2Btn.visible = menu.traitSlots() >= 2;
        reforge2Btn.active = (f & ForgeMenu.CAN_REFORGE2) != 0;
        repairBtn.active = (f & ForgeMenu.CAN_REPAIR) != 0;
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
        switch (st) {
            case ForgeMenu.ST_EMPTY -> line1 = "§7武器・防具・道具を置こう";
            case ForgeMenu.ST_NOT_SMITH -> line1 = "§c鍛えられるのは鍛冶師だけです";
            case ForgeMenu.ST_CANT -> line1 = "§7これは強化できない（修理のみ）";
            case ForgeMenu.ST_MAX -> line1 = "§6+" + SmithData.plus(item) + "（今の上限 +" + menu.maxPlus() + "）";
            default -> {
                int plus = SmithData.plus(item);
                line1 = "§f+" + plus + " → +" + (plus + 1) + "  §e成功率 " + menu.ratePermille() / 10 + "%";
                line2 = "§7必要：" + SmithingService.TIER_NAMES[SmithingService.tierFor(plus + 1)] + "の強化石";
            }
        }
        g.drawString(font, line1, 8, 48, 0xFFFFFF, false);
        if (!line2.isEmpty()) g.drawString(font, line2, 8, 59, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        renderTooltip(g, mouseX, mouseY);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(ModSmithing.FORGE_MENU.get(), ForgeScreen::new);
        }
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
            int tier = ModSmithing.stoneTier(stack);
            if (tier >= 0) {
                int from = tier * 3 + 1;
                tip.add(Component.literal("+" + from + "〜+" + (from + 2) + " の強化に使う").withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
