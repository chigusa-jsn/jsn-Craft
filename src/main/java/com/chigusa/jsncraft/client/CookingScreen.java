package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.CookingMenu;
import com.chigusa.jsncraft.cooking.ModCooking;
import com.chigusa.jsncraft.network.CookingRecipesPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** 調理台の画面。材料4つ＋隠し味1つ→「調理する」で料理ができる。 */
public class CookingScreen extends AbstractContainerScreen<CookingMenu> {
    private Button cookBtn;

    public CookingScreen(CookingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        cookBtn = addRenderableWidget(Button.builder(Component.literal("調理する"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 110, topPos + 46, 58, 14).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        cookBtn.active = menu.status() == CookingMenu.ST_READY;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF3A2A20);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFFC6B49A);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 72, 0xFFB09A7C);
        for (Slot s : menu.slots) {
            int c = s.index == 4 ? 0xFF6A4A8A : s.index == 5 ? 0xFF8A6A20 : 0xFF5A4A3A;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
        // 炎の矢印（進み具合）
        int ax = x + 102, ay = y + 30;
        g.fill(ax, ay + 3, ax + 24, ay + 7, 0xFF5A4A3A);
        if (menu.total() > 0) {
            int w = menu.progress() * 24 / Math.max(1, menu.total());
            g.fill(ax, ay + 3, ax + w, ay + 7, 0xFFFF7A20);
        }
        g.fill(ax + 24, ay + 1, ax + 26, ay + 9, menu.total() > 0 ? 0xFFFF7A20 : 0xFF5A4A3A);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0x3A2A20, false);
        g.drawString(font, "隠し味", 72, 45, 0x5A3A7A, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x3A2A20, false);
        // できる料理
        int idx = menu.recipeIndex();
        var list = CookingRecipesPayload.client.recipes();
        String status = switch (menu.status()) {
            case CookingMenu.ST_READY -> "§2調理できます";
            case CookingMenu.ST_LEVEL -> "§c調理師のレベルが足りません";
            case CookingMenu.ST_NOT_CHEF -> "§c炎の調理師だけが料理できます";
            case CookingMenu.ST_COOKING -> "§6調理中…";
            case CookingMenu.ST_FULL -> "§c出来上がりを取り出してください";
            case CookingMenu.ST_BAD_SEASONING -> "§cそれは隠し味に使えません";
            default -> "§7材料を入れてください";
        };
        g.drawString(font, status, 8, 62, 0xFFFFFF, false);
        if (idx >= 0 && idx < list.size() && menu.status() != CookingMenu.ST_COOKING) {
            var e = list.get(idx);
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            g.renderFakeItem(e.result(), 152, 4);
            g.pose().popPose();
            String name = e.result().getHoverName().getString();
            String text = name + " §8Lv" + e.level();
            g.drawString(font, text, 148 - font.width(text), 8, 0x3A2A20, false);
        }
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
            event.register(ModCooking.COOKING_MENU.get(), CookingScreen::new);
        }
    }
}
