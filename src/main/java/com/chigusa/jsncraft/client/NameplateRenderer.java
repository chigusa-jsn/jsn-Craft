package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.ClientJobCache;
import com.chigusa.jsncraft.network.JobSyncPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * プレイヤーの名前の上に、3段で表示する：
 *
 *     《肩書》
 *   [戦闘] 戦士 Lv.5
 *   [生活] 木こり Lv.3
 *      プレイヤー名      ← バニラの名札
 *
 * バニラの名札は1行しか出せないので、名札とは別に自分で文字を描いている。
 */
@EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
public final class NameplateRenderer {

    private static final double MAX_DISTANCE_SQ = 64 * 64;
    private static final int LINE_HEIGHT = 10;
    /** 文字の大きさ（バニラの名札は 0.025） */
    private static final float SCALE = 0.02F;

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) return;

        JobSyncPayload info = ClientJobCache.get(player.getId());
        if (info == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        // 自分の頭上は、F5（三人称）のときだけ表示
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) return;
        if (mc.player != null && player.isInvisibleTo(mc.player)) return;
        if (mc.getEntityRenderDispatcher().distanceToSqr(player) > MAX_DISTANCE_SQ) return;

        // 下の段から順に積む
        List<Component> lines = new ArrayList<>();
        if (info.lifeLevel() > 0) {
            lines.add(Component.literal("[生活] ").withStyle(ChatFormatting.GREEN)
                    .append(info.lifeName().copy())
                    .append(Component.literal(" Lv." + info.lifeLevel()).withStyle(ChatFormatting.WHITE)));
        }
        if (info.combatLevel() > 0) {
            lines.add(Component.literal("[戦闘] ").withStyle(ChatFormatting.RED)
                    .append(info.combatName().copy())
                    .append(Component.literal(" Lv." + info.combatLevel()).withStyle(ChatFormatting.WHITE)));
        }
        if (!info.title().isEmpty()) {
            lines.add(Component.literal("《" + info.title() + "》").withStyle(ChatFormatting.GOLD));
        }
        // F5で自分を見ているときは、マイクラの名札が出ないので自分の名前も一番下に出す
        boolean self = player == mc.player;
        if (self) lines.add(0, player.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
        if (lines.isEmpty()) return;

        float partialTick = event.getPartialTick();
        Vec3 anchor = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, player.getViewYRot(partialTick));
        if (anchor == null) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // バニラの名札と同じ位置・向き・大きさに合わせる
        pose.translate(anchor.x, anchor.y + 0.5, anchor.z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(SCALE, -SCALE, SCALE);

        Matrix4f matrix = pose.last().pose();
        Font font = mc.font;
        boolean sneaking = player.isDiscrete();
        int background = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        int light = event.getPackedLight();

        // 名札(y=0)の1段上から、上へ積んでいく
        int y = self ? 0 : -LINE_HEIGHT;
        for (Component line : lines) {
            float x = -font.width(line) / 2.0F;
            // 壁越しにうっすら見える層（スニーク中は出さない）
            font.drawInBatch(line, x, y, 0x20FFFFFF, false, matrix, event.getMultiBufferSource(),
                    sneaking ? Font.DisplayMode.NORMAL : Font.DisplayMode.SEE_THROUGH, background, light);
            // はっきり見える層
            if (!sneaking) {
                font.drawInBatch(line, x, y, 0xFFFFFFFF, false, matrix, event.getMultiBufferSource(),
                        Font.DisplayMode.NORMAL, 0, light);
            }
            y -= LINE_HEIGHT;
        }
        pose.popPose();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientJobCache.clear();
    }

    private NameplateRenderer() {}
}
