package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.tools.ModTools;
import com.chigusa.jsncraft.tools.RodPart;
import com.chigusa.jsncraft.tools.RodParts;
import com.chigusa.jsncraft.tools.RodTableBlockEntity;
import com.chigusa.jsncraft.tools.RodTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** 釣り竿カスタム台の画面：左に竿、右に部品5つ、下に今付いている部品。 */
public class RodTableScreen extends AbstractContainerScreen<RodTableMenu> {
    private Button attach, detach;

    public RodTableScreen(RodTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        attach = addRenderableWidget(Button.builder(Component.literal("取り付ける"), b -> click(0)).bounds(leftPos + 70, topPos + 54, 52, 14).build());
        detach = addRenderableWidget(Button.builder(Component.literal("外す"), b -> click(1)).bounds(leftPos + 124, topPos + 54, 42, 14).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean ok = menu.anglerLevel() >= 0 && menu.rod().getItem() instanceof FishingRodItem;
        attach.active = ok;
        detach.active = ok;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E22);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF6A8AA8);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 76, 0xFF2E4A66);
        for (Slot s : menu.slots) {
            int c = s.index == 0 ? 0xFFC8A040 : s.index < 6 ? 0xFF5A9AC8 : 0xFF3A3A42;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, 0xFFE8F0F8, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF2A2A2A, false);
        g.drawString(font, "竿", 30, 24, 0xFFD8E8F0, false);
        String[] short_ = {"竿", "巻", "餌", "浮", "糸"};
        for (int i = 0; i < 5; i++) g.drawString(font, short_[i], 70 + i * 18 + 8 - font.width(short_[i]) / 2, 24, 0xFFB8D0E8, false);
        int lv = menu.anglerLevel();
        String s = lv < 0 ? "§c取り付けられるのは釣り師だけ" : "§7部品は " + RodTableBlockEntity.maxGrade(lv) + " 段目まで付けられる";
        g.drawString(font, s, 8, 14, 0xFFFFFF, false);
        // 今付いている部品（小さく）
        ItemStack rod = menu.rod();
        if (rod.getItem() instanceof FishingRodItem) {
            int n = 0;
            for (RodPart p : RodPart.values()) if (RodParts.grade(rod, p) > 0) n++;
            g.drawString(font, "§7部品 " + n + "/5", 8, 58, 0xFFFFFF, false);
        }
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
            event.register(ModTools.ROD_TABLE_MENU.get(), RodTableScreen::new);
        }
    }
}
