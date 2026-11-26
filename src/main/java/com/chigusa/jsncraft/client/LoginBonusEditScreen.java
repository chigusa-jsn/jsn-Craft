package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.loginbonus.LoginBonusEditMenu;
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

/** ログインボーナスの編集画面（OPだけ）。 */
public class LoginBonusEditScreen extends AbstractContainerScreen<LoginBonusEditMenu> {
    public LoginBonusEditScreen(LoginBonusEditMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = 93;
    }

    private void btn(String text, int id, int x, int y, int w) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }).bounds(leftPos + x, topPos + y, w, 13).build());
    }

    @Override
    protected void init() {
        super.init();
        btn("◀", 0, 6, 17, 16);
        btn("▶", 1, 154, 17, 16);
        btn("-100", 4, 6, 71, 28);
        btn("-10", 2, 35, 71, 22);
        btn("+10", 3, 119, 71, 22);
        btn("+100", 5, 142, 71, 28);
        btn("日を足す", 6, 190 - 14, 17, 50);
        btn("最後の日を消す", 7, 190 - 14, 32, 70);
        btn("-1", 10, 190 - 14, 62, 18);
        btn("+1", 11, 190 + 26, 62, 18);
        btn("-10", 8, 190 - 14, 92, 22);
        btn("+10", 9, 190 + 30, 92, 22);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF3A2410);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFFF4E8C8);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + 14, 0xFFC04030);
        // 右の設定パネル
        g.fill(x + imageWidth + 2, y, x + imageWidth + 112, y + 112, 0xFF3A2410);
        g.fill(x + imageWidth + 4, y + 2, x + imageWidth + 110, y + 110, 0xFFF4E8C8);
        for (Slot s : menu.slots) {
            boolean reward = s.index < 9;
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, reward ? 0xFFC08030 : 0xFF8A7050);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, reward ? 0xFFFFFAEA : 0xFFB8A888);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, "ログインボーナスの編集", 6, 4, 0xFFFFF4D0, false);
        String day = (menu.day() + 1) + "日目 / " + menu.dayCount() + "日";
        g.drawCenteredString(font, day, imageWidth / 2, 20, 0xFFC03020);
        g.drawString(font, "§8持ち物のアイテムで左クリック：置く（へらない）", 6, 33, 0xFFFFFF, false);
        g.drawString(font, "§8右：1個だけ・手ぶら左：消す・右：+1", 6, 42, 0xFFFFFF, false);
        String money = "お金 " + menu.money();
        g.drawCenteredString(font, money, imageWidth / 2 + 1, 74, 0xFF2A6A2A);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xFF5A3A20, false);
        int rx = imageWidth + 8;
        g.drawString(font, "§8日数", rx, 7, 0xFFFFFF, false);
        g.drawString(font, "§8連続ログインのおまけ", rx, 50, 0xFFFFFF, false);
        g.drawString(font, menu.streakEvery() + "日ごと", rx + 26, 65, 0xFF5A3A20, false);
        g.drawString(font, "§8おまけのお金", rx, 81, 0xFFFFFF, false);
        g.drawString(font, String.valueOf(menu.streakMoney()), rx + 26, 95, 0xFF2A6A2A, false);
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
            event.register(LoginBonusEditMenu.TYPE.get(), LoginBonusEditScreen::new);
        }
    }
}
