package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.PartyStatusPayload;
import com.chigusa.jsncraft.network.PartyStatusPayload.Member;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.List;

/**
 * パーティーの仲間のHP・MPを、画面の左側（真ん中あたり）に並べる。自分は出さない。
 *
 *   ★ なかま1
 *   [■■■■■ 18 / 20]
 *   [■■■   ]  ← MP（細い）
 */
public final class PartyHud {

    private static final float SCALE = 0.6F;
    private static final int W = 110, BAR_H = 8, MP_H = 3, ENTRY_H = 26;

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        List<Member> members = PartyStatusPayload.client;
        if (members.isEmpty()) return;
        String self = mc.player.getUUID().toString();
        long others = members.stream().filter(m -> !m.uuid().equals(self)).count();
        if (others == 0) return;

        Font font = mc.font;
        int totalH = (int) (others * ENTRY_H * SCALE);
        int y0 = g.guiHeight() / 2 - totalH / 2;

        g.pose().pushPose();
        g.pose().translate(4, y0, 0);
        g.pose().scale(SCALE, SCALE, 1F);

        int y = 0;
        for (Member m : members) {
            if (m.uuid().equals(self)) continue;
            String name = (m.leader() ? "★ " : "") + m.name();
            g.drawString(font, name, 0, y, m.dead() ? 0xFF888888 : 0xFFFFFFFF, true);
            int by = y + 10;
            // HP
            g.fill(-1, by - 1, W + 1, by + BAR_H + 1, 0xC0000000);
            g.fill(0, by, W, by + BAR_H, 0xFF5A1414);
            float hpRatio = m.maxHp() <= 0 ? 0 : Math.max(0, Math.min(1, m.hp() / m.maxHp()));
            int hpColor = hpRatio > 0.5F ? 0xFFD83A3A : hpRatio > 0.25F ? 0xFFE08A2A : 0xFFFF2020;
            g.fill(0, by, Math.round(W * hpRatio), by + BAR_H, hpColor);
            String hp = m.dead() ? "戦闘不能" : (int) Math.ceil(m.hp()) + " / " + (int) Math.ceil(m.maxHp());
            g.drawString(font, hp, (W - font.width(hp)) / 2, by, 0xFFFFFFFF, true);
            // MP（細い線）
            int my = by + BAR_H + 2;
            g.fill(0, my, W, my + MP_H, 0xFF142A5A);
            float mpRatio = m.maxMana() <= 0 ? 0 : Math.max(0, Math.min(1, m.mana() / m.maxMana()));
            g.fill(0, my, Math.round(W * mpRatio), my + MP_H, 0xFF3A7BD8);
            y += ENTRY_H;
        }
        g.pose().popPose();
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(JsnCraft.id("party"), PartyHud::render);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Reset {
        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            PartyStatusPayload.client = List.of();
        }
    }

    private PartyHud() {}
}
