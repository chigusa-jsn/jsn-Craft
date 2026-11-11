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
 * 経験値バーのすぐ上（ハートと満腹度があった場所）に、4本のゲージを2列で出す。
 *
 *   HP [■■■■ 18 / 24 ■■]   MP [■■■ 30 / 80 ■■■]
 *   ST [■■■■ 95 / 100 ■]   満腹 [■■■ 14 / 20 ■■]
 *
 * バニラのハート・満腹度・防御力のアイコンは隠す。
 */
public final class GaugeHud {

    private static final Set<ResourceLocation> HIDDEN = Set.of(
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.ARMOR_LEVEL);

    /** ゲージ全体の大きさ（1.0 で文字と同じ大きさ） */
    private static final float SCALE = 0.6F;
    /** ホットバーの幅（バニラと同じ 182） */
    private static final int HOTBAR_W = 182;
    private static final int LABEL_W = 22, BAR_H = 9, ROW = 11, GAP = 6;
    /** ゲージが使う高さ（画面の単位）。空気の泡などをこの分だけ上にずらす */
    private static final int HEIGHT = (int) Math.ceil(ROW * 2 * SCALE) + 2;
    /** 画面の下からどれだけ上に置くか（経験値バーとレベルの数字をよける） */
    private static final int BOTTOM = 36;
    /** バニラのハート1段（39）に比べて、上に積むものをずらす量 */
    private static final int SHIFT = BOTTOM + HEIGHT + 10 - 39;

    private static boolean shouldDraw() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.gameMode != null && mc.gameMode.canHurtPlayer()
                && !mc.options.hideGui;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (!shouldDraw()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        Font font = mc.font;

        int sw = g.guiWidth(), sh = g.guiHeight();
        int x0 = sw / 2 - HOTBAR_W / 2;
        int y0 = sh - BOTTOM - HEIGHT; // 経験値バーとレベルの数字のすぐ上

        float maxMana = (float) p.getAttributeValue(ModAttributes.MAX_MANA);
        float maxSt = (float) p.getAttributeValue(ModAttributes.MAX_STAMINA);
        float hp = p.getHealth() + p.getAbsorptionAmount();

        g.pose().pushPose();
        g.pose().translate(x0, y0, 0);
        g.pose().scale(SCALE, SCALE, 1F);

        int total = (int) (HOTBAR_W / SCALE);
        int colW = (total - GAP) / 2;
        int rx = colW + GAP;

        gauge(g, font, 0, 0, colW, "HP", hp, p.getMaxHealth(), 0xFFD83A3A, 0xFF5A1414);
        gauge(g, font, rx, 0, colW, "MP", ResourceSyncPayload.clientMana, maxMana, 0xFF3A7BD8, 0xFF142A5A);
        gauge(g, font, 0, ROW, colW, "ST", ResourceSyncPayload.clientStamina, maxSt, 0xFFE0C23A, 0xFF5A4A14);
        gauge(g, font, rx, ROW, colW, "満腹", p.getFoodData().getFoodLevel(), 20, 0xFFE0802A, 0xFF5A3014);

        // 防御力は、HPの左に小さく
        String armor = "防御 " + p.getArmorValue();
        g.drawString(font, armor, -font.width(armor) - 4, 1, 0xFFB0C4DE, true);

        g.pose().popPose();
    }

    private static void gauge(GuiGraphics g, Font font, int x, int y, int w, String label, float value, float max, int color, int back) {
        g.drawString(font, label, x, y + 1, 0xFFFFFFFF, true);
        int bx = x + LABEL_W, bw = w - LABEL_W;
        g.fill(bx - 1, y - 1, bx + bw + 1, y + BAR_H + 1, 0xC0000000);
        g.fill(bx, y, bx + bw, y + BAR_H, back);
        float ratio = max <= 0 ? 0 : Math.max(0, Math.min(1, value / max));
        g.fill(bx, y, bx + Math.round(bw * ratio), y + BAR_H, color);
        g.fill(bx, y, bx + Math.round(bw * ratio), y + 2, 0x40FFFFFF); // 上のつや
        // 数字はバーの真ん中に重ねる
        String text = (int) Math.ceil(value) + " / " + (int) Math.ceil(max);
        g.drawString(font, text, bx + (bw - font.width(text)) / 2, y + 1, 0xFFFFFFFF, true);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            // ハートの場所に描く（空気の泡などより先）
            event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, JsnCraft.id("gauges"), GaugeHud::render);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Hide {
        @SubscribeEvent
        public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
            if (!HIDDEN.contains(event.getName()) || !shouldDraw()) return;
            event.setCanceled(true);
            // バニラが使うはずだった高さの代わりに、ゲージの高さ分だけ上にずらす（空気の泡などが重ならないように）
            var gui = Minecraft.getInstance().gui;
            if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) gui.leftHeight += SHIFT;
            if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) gui.rightHeight += SHIFT;
        }
    }

    private GaugeHud() {}
}
