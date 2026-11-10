package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.ResourceSyncPayload;
import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Set;

/**
 * 画面左上に、HP・MP・スタミナ・満腹度の4本のゲージを数字つきで出す。
 * その代わり、バニラのハート・満腹度・防御力のアイコンは隠す。
 */
public final class GaugeHud {

    private static final Set<ResourceLocation> HIDDEN = Set.of(
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.ARMOR_LEVEL);

    private static final int X = 4, Y = 4, LABEL_W = 26, BAR_W = 90, BAR_H = 7, ROW = 10;

    private static boolean shouldDraw() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.gameMode != null && mc.gameMode.canHurtPlayer()
                && !mc.options.hideGui && !mc.getDebugOverlay().showDebugScreen();
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (!shouldDraw()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        Font font = mc.font;

        float maxMana = (float) p.getAttributeValue(ModAttributes.MAX_MANA);
        float maxSt = (float) p.getAttributeValue(ModAttributes.MAX_STAMINA);

        int y = Y;
        gauge(g, font, y, "HP", p.getHealth() + p.getAbsorptionAmount(), p.getMaxHealth(), 0xFFD83A3A, 0xFF5A1414);
        String armor = "防御 " + p.getArmorValue();
        g.drawString(font, armor, X + LABEL_W + BAR_W + 4 + font.width(valueText(p.getHealth() + p.getAbsorptionAmount(), p.getMaxHealth())) + 8,
                y, 0xFFB0C4DE, true);
        y += ROW;
        gauge(g, font, y, "MP", ResourceSyncPayload.clientMana, maxMana, 0xFF3A7BD8, 0xFF142A5A);
        y += ROW;
        gauge(g, font, y, "ST", ResourceSyncPayload.clientStamina, maxSt, 0xFFE0C23A, 0xFF5A4A14);
        y += ROW;
        gauge(g, font, y, "満腹", p.getFoodData().getFoodLevel(), 20, 0xFFE0802A, 0xFF5A3014);
    }

    private static void gauge(GuiGraphics g, Font font, int y, String label, float value, float max, int color, int back) {
        g.drawString(font, label, X, y, 0xFFFFFFFF, true);
        int bx = X + LABEL_W, by = y;
        // 枠と背景
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, 0xC0000000);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, back);
        // 中身
        float ratio = max <= 0 ? 0 : Math.max(0, Math.min(1, value / max));
        int w = Math.round(BAR_W * ratio);
        g.fill(bx, by, bx + w, by + BAR_H, color);
        g.fill(bx, by, bx + w, by + 2, 0x40FFFFFF); // 上のつや
        // 数字
        g.drawString(font, valueText(value, max), bx + BAR_W + 4, y, 0xFFFFFFFF, true);
    }

    private static String valueText(float value, float max) {
        return (int) Math.ceil(value) + " / " + (int) Math.ceil(max);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(JsnCraft.id("gauges"), GaugeHud::render);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Hide {
        @SubscribeEvent
        public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
            if (HIDDEN.contains(event.getName()) && shouldDraw()) {
                event.setCanceled(true);
            }
        }
    }

    private GaugeHud() {}
}
