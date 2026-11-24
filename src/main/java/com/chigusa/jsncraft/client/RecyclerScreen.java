package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.smithing.ModSmithing;
import com.chigusa.jsncraft.smithing.RecyclerBlockEntity;
import com.chigusa.jsncraft.smithing.RecyclerMenu;
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

/** 分解機の画面：左に入れる所、右に出てくる所、真ん中にスイッチ。 */
public class RecyclerScreen extends AbstractContainerScreen<RecyclerMenu> {
    private Button switchBtn;

    public RecyclerScreen(RecyclerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        switchBtn = addRenderableWidget(Button.builder(Component.literal("開始"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 64, topPos + 46, 48, 14).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean run = menu.running();
        switchBtn.setMessage(Component.literal(run ? "■ 停止" : "▶ 開始"));
        switchBtn.active = run || menu.viewerIsSmith();
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E22);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF7A7A82);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 72, 0xFF4E5258);
        for (Slot s : menu.slots) {
            int c = s.index < RecyclerBlockEntity.IN ? 0xFF4A6A9A : s.index < RecyclerBlockEntity.IN + RecyclerBlockEntity.OUT ? 0xFFB06A20 : 0xFF3A3A42;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
        // 進み具合の矢印
        int ax = x + 66, ay = y + 30, w = 44;
        g.fill(ax, ay, ax + w, ay + 6, 0xFF2A2A2E);
        int p = menu.running() ? menu.progress() * w / RecyclerBlockEntity.TIME : 0;
        if (p > 0) g.fill(ax, ay, ax + p, ay + 6, 0xFFE8A040);
        g.fill(ax + w, ay - 3, ax + w + 2, ay + 9, 0xFF2A2A2E);
        g.fill(ax + w + 2, ay - 1, ax + w + 4, ay + 7, 0xFF2A2A2E);
        if (menu.running() && (System.currentTimeMillis() / 150) % 2 == 0) g.fill(ax + 18, ay - 8, ax + 26, ay - 4, 0xFFFFD070);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0xFFE8E8E8, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF2A2A2A, false);
        String s;
        if (menu.running()) s = "§e分解中… 戻り " + menu.ratePercent() + "%";
        else if (!menu.viewerIsSmith()) s = "§c動かせるのは鍛冶師だけ";
        else if (menu.stopReason() == 2) s = "§c出口がいっぱい";
        else s = "§7戻り " + menu.ratePercent() + "%";
        g.drawString(font, s, imageWidth - 8 - font.width(s.replaceAll("§.", "")), 6, 0xFFFFFF, false);
        g.drawString(font, "入れる", 8, 62, 0xFFD8D8D8, false);
        g.drawString(font, "出てくる", 116, 62, 0xFFD8D8D8, false);
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
            event.register(ModSmithing.RECYCLER_MENU.get(), RecyclerScreen::new);
        }
    }
}
