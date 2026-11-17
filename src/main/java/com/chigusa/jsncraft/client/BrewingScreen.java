package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.BrewingMenu;
import com.chigusa.jsncraft.cooking.DishItem;
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

/** 酒樽の画面。材料4つ→「仕込む」→ 時間がたつと「樽出し」できる。 */
public class BrewingScreen extends AbstractContainerScreen<BrewingMenu> {
    private Button startBtn, bottleBtn;

    public BrewingScreen(BrewingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        startBtn = addRenderableWidget(Button.builder(Component.literal("仕込む"), b -> click(0))
                .bounds(leftPos + 110, topPos + 46, 58, 14).build());
        bottleBtn = addRenderableWidget(Button.builder(Component.literal("樽出し"), b -> click(1))
                .bounds(leftPos + 110, topPos + 46, 58, 14).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int st = menu.status();
        boolean brewing = st == BrewingMenu.ST_BREWING || st == BrewingMenu.ST_DONE || st == BrewingMenu.ST_FULL;
        startBtn.visible = !brewing;
        startBtn.active = st == BrewingMenu.ST_READY;
        bottleBtn.visible = brewing;
        bottleBtn.active = st == BrewingMenu.ST_DONE;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2A1A10);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFFA8845A);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 72, 0xFF8A6A42);
        for (Slot s : menu.slots) {
            int c = s.index == 4 ? 0xFF8A6A20 : 0xFF4A3A2A;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
        // 熟成のゲージ
        int ax = x + 70, ay = y + 30, w = 56;
        g.fill(ax, ay, ax + w, ay + 6, 0xFF3A2A1A);
        int st = menu.status();
        if (st == BrewingMenu.ST_BREWING || st == BrewingMenu.ST_DONE || st == BrewingMenu.ST_FULL) {
            int fillW = menu.permille() * w / 1000;
            int col = st == BrewingMenu.ST_BREWING ? 0xFFC08A30 : 0xFFE8C040;
            g.fill(ax, ay, ax + fillW, ay + 6, col);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0x2A1A10, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x2A1A10, false);
        int st = menu.status();
        String text = switch (st) {
            case BrewingMenu.ST_READY -> "§2仕込めます";
            case BrewingMenu.ST_LEVEL -> "§c調理師のレベルが足りません";
            case BrewingMenu.ST_NOT_CHEF -> "§c仕込めるのは炎の調理師だけです";
            case BrewingMenu.ST_BREWING -> "§6熟成中… " + menu.permille() / 10 + "%";
            case BrewingMenu.ST_DONE -> "§a飲み頃！ " + DishItem.stars(menu.quality()) + "（寝かせると★が上がる）";
            case BrewingMenu.ST_FULL -> "§c出来上がりの枠を空けてください";
            default -> "§7材料を入れてください";
        };
        g.drawString(font, text, 8, 62, 0xFFFFFF, false);
        int idx = menu.recipeIndex();
        var list = BrewingRecipesCache.list();
        if (idx >= 0 && idx < list.size()) {
            var e = list.get(idx);
            g.renderFakeItem(e.result(), 152, 4);
            String t = e.result().getHoverName().getString() + " §8Lv" + e.level();
            g.drawString(font, t, 148 - font.width(t), 8, 0x2A1A10, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        renderTooltip(g, mouseX, mouseY);
    }

    /** 酒樽のレシピ一覧（調理台のレシピと同じ通信で、別の名前で受け取る） */
    public static final class BrewingRecipesCache {
        public static CookingRecipesPayload brewing = new CookingRecipesPayload(true, java.util.List.of());

        public static java.util.List<CookingRecipesPayload.Entry> list() {
            return brewing.recipes();
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent
        public static void onScreens(RegisterMenuScreensEvent event) {
            event.register(ModCooking.BREWING_MENU.get(), BrewingScreen::new);
        }
    }
}
