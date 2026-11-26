package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.farming.CrossbreedMenu;
import com.chigusa.jsncraft.farming.ModFarming;
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

/** 交配台の画面：種 ＋ 種 → 種。 */
public class CrossbreedScreen extends AbstractContainerScreen<CrossbreedMenu> {
    private Button btn;

    public CrossbreedScreen(CrossbreedMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();
        btn = addRenderableWidget(Button.builder(Component.literal("交配する"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 92, topPos + 54, 60, 16).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        btn.active = menu.farmer() && menu.state() == 0;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1E1E22);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF8AA06A);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 76, 0xFF4A6034);
        for (Slot s : menu.slots) {
            int c = s.index < 2 ? 0xFF6A9A4A : s.index == 2 ? 0xFFC8A040 : 0xFF3A3A42;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, c);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 6, 0xFFF0F0E0, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF2A2A2A, false);
        g.drawString(font, "＋", 52, 34, 0xFFFFE080, false);
        g.drawString(font, "→", 100, 34, 0xFFFFE080, false);
        String s = !menu.farmer() ? "§c交配できるのは農家だけ" : switch (menu.state()) {
            case 0 -> "§a同じ種を2つ → ランクアップ";
            case 2 -> "§c同じ種を2つ入れよう";
            case 3 -> "§c右の枠を空けよう";
            default -> "§7同じ種を2つ入れよう（★5どうしで黄金へ）";
        };
        g.drawString(font, s, 8, 18, 0xFFFFFF, false);
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
            event.register(ModFarming.CROSSBREED_MENU.get(), CrossbreedScreen::new);
        }
    }
}
