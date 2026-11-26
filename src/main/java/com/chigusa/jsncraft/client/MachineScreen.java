package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.furnace.MachineKind;
import com.chigusa.jsncraft.furnace.MachineMenu;
import com.chigusa.jsncraft.furnace.ModFurnaces;
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

/** 職業の炉の画面：左に入れる所、右に出てくる所、真ん中にスイッチ。大きい炉は燃料の枠も。 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private Button switchBtn;

    public MachineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        switchBtn = addRenderableWidget(Button.builder(Component.literal("▶ 開始"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 64, topPos + 58, 48, 14).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean run = menu.running();
        switchBtn.setMessage(Component.literal(run ? "■ 停止" : "▶ 開始"));
        switchBtn.active = run || menu.viewerCan();
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        boolean big = menu.kind() == MachineKind.BIG_FURNACE;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E22);
        boolean saw = menu.kind() == MachineKind.SAWMILL;
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, big ? 0xFF8A6A5A : saw ? 0xFF9A7A50 : 0xFF6A6A6E);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 76, big ? 0xFF4A2E26 : saw ? 0xFF5A4028 : 0xFF3A3A40);
        for (Slot s : menu.slots) {
            if (!s.isActive()) continue;
            int c = s.index < 3 ? 0xFF4A6A9A : s.index < 6 ? 0xFFB06A20 : s.index == 6 ? 0xFFC03020 : 0xFF3A3A42;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
        // 進み具合
        int ax = x + 82, ay = y + 42, w = 28;
        g.fill(ax, ay, ax + w, ay + 6, 0xFF2A2A2E);
        int time = menu.kind().time;
        int p = menu.running() ? Math.min(w, menu.progress() * w / time) : 0;
        if (p > 0) g.fill(ax, ay, ax + p, ay + 6, 0xFFFF8030);
        // 燃料の炎
        if (big && menu.burnMax() > 0 && menu.burn() > 0) {
            int h = Math.max(1, 12 * menu.burn() / menu.burnMax());
            g.fill(x + 28, y + 56 + 14 - h, x + 38, y + 70, 0xFFFF7020);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, 0xFFF0E0D0, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF2A2A2A, false);
        MachineKind k = menu.kind();
        String s;
        if (menu.running()) s = (k == MachineKind.SAWMILL ? "§e挽いています… ×" : "§e焼いています… ×") + String.format("%.2f", menu.bonusPercent() / 100.0);
        else if (!menu.viewerCan()) s = "§c動かせるのは" + k.job + "だけ";
        else if (menu.stopReason() == 2) s = "§c出口がいっぱい";
        else if (menu.stopReason() == 3) s = "§c燃料切れ";
        else s = "§7多めに取れる ×" + String.format("%.2f", menu.bonusPercent() / 100.0);
        g.drawString(font, s, imageWidth - 8 - font.width(s.replaceAll("§.", "")), 6, 0xFFFFFF, false);
        g.drawString(font, switch (k) {
            case BIG_FURNACE -> "鉱石・原石・砂は多めに";
            case COKE_OVEN -> "石炭→コークス／石→石炭";
            case SAWMILL -> "原木・竹→板（多めに）";
        }, 8, 40, 0xFFD8D8D8, false);
        if (k == MachineKind.BIG_FURNACE) g.drawString(font, "燃料", 8, 60, 0xFFD8D8D8, false);
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
            event.register(ModFurnaces.MENU.get(), MachineScreen::new);
        }
    }
}
