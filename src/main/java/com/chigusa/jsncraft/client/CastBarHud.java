package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** ため（詠唱）のゲージ。画面の真ん中の少し下に、技の名前と一緒に出る。 */
public final class CastBarHud {

    private static Component name = Component.empty();
    private static long start, end;

    public static void start(Component skill, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        name = skill;
        start = mc.level.getGameTime();
        end = start + ticks;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) return;
        float now = mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false);
        if (now >= end + 3) return; // 終わってすぐ消える
        float t = Math.min(1, (now - start) / Math.max(1, end - start));
        int w = 100, h = 6;
        int x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() / 2 + 28;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xC0000000);
        g.fill(x, y, x + w, y + h, 0xFF1A2A4A);
        int fill = Math.round(w * t);
        g.fill(x, y, x + fill, y + h, t >= 1 ? 0xFFFFFFFF : 0xFF55AAFF);
        g.fill(x, y, x + fill, y + 2, 0x60FFFFFF);
        g.drawCenteredString(mc.font, name, g.guiWidth() / 2, y - 11, 0xFFFFFF);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(JsnCraft.id("cast_bar"), CastBarHud::render);
        }
    }

    private CastBarHud() {}
}
