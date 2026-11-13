package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.ResourceSyncPayload;
import com.chigusa.jsncraft.network.SkillPayloads.SkillView;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * スキルモードの間、ホットバーの代わりにスキルバー（5枠）を出す。
 * 待ち時間中の枠は暗くなって残り秒数が出る。スタミナ・マナが足りないと赤くなる。
 */
public final class SkillBarHud {

    private static final int SLOT = 22;

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return ClientSkills.skillMode && mc.player != null && !mc.player.isSpectator() && !mc.options.hideGui;
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (!active()) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int w = SLOT * ClientSkills.SLOTS;
        int x0 = g.guiWidth() / 2 - w / 2;
        int y0 = g.guiHeight() - SLOT - 1;

        // 枠全体
        g.fill(x0 - 3, y0 - 3, x0 + w + 3, y0 + SLOT + 1, 0xC0101030);
        g.fill(x0 - 3, y0 - 3, x0 + w + 3, y0 - 2, 0xFF55AAFF);

        for (int i = 0; i < ClientSkills.SLOTS; i++) {
            int x = x0 + i * SLOT;
            g.fill(x + 1, y0, x + SLOT - 1, y0 + SLOT - 2, 0xFF22223A);
            SkillView v = ClientSkills.find(ClientSkills.slot(i));
            if (v != null) {
                g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(v.icon())), x + 3, y0 + 2);
                boolean lacking = ResourceSyncPayload.clientStamina < v.stamina() || ResourceSyncPayload.clientMana < v.mana();
                float left = ClientSkills.cooldownLeft(i);
                // ここから先はアイコンより手前に描く（アイテムの絵は奥行きが手前なので、そのままだと隠れる）
                g.pose().pushPose();
                g.pose().translate(0, 0, 300);
                if (left > 0 && v.cooldown() > 0) {
                    int h = Math.round((SLOT - 2) * Math.min(1, left / v.cooldown()));
                    g.fill(x + 1, y0 + SLOT - 2 - h, x + SLOT - 1, y0 + SLOT - 2, 0xA0000000);
                    String sec = left >= 10 ? String.valueOf((int) Math.ceil(left)) : String.format("%.1f", left);
                    g.drawCenteredString(font, sec, x + SLOT / 2, y0 + 7, 0xFFFFFF55);
                } else if (lacking) {
                    g.fill(x + 1, y0, x + SLOT - 1, y0 + SLOT - 2, 0x70FF0000);
                }
                g.pose().popPose();
            }
            // キー番号（アイコンより手前）
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.drawString(font, String.valueOf(i + 1), x + 2, y0 + 1, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
        g.drawString(font, "R:もどる", x0 + w + 6, y0 + 7, 0xFF8899BB, true);
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.HOTBAR, JsnCraft.id("skill_bar"), SkillBarHud::render);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Hide {
        @SubscribeEvent
        public static void onLayer(RenderGuiLayerEvent.Pre event) {
            if (event.getName().equals(VanillaGuiLayers.HOTBAR) && active()) event.setCanceled(true);
        }
    }

    private SkillBarHud() {}
}
