package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.network.FishingPayloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 釣りミニゲーム（追いかけ型）。
 * 右クリック（左クリック・スペースでも可）を押している間、緑のキャッチ枠が上がる。離すと落ちる。
 * 枠が魚に重なっている間は右のゲージが溜まり、外れると減る。満タンで釣れる、空で逃げられる。
 * 位置はすべて「下が0、上が1」。
 */
public class FishingScreen extends Screen {
    private static final int BAR_H = 150, BAR_W = 24;
    private static final int KIND_JUNK = 0, KIND_FISH = 1, KIND_TREASURE = 2, KIND_LORD = 3;
    private static final int SMOOTH = 1, DART = 2, SINKER = 3, FLOATER = 4;

    private final FishingPayloads.Start p;
    private final float d; // 難しさ 0〜1
    private final float zoneSize;
    private final RandomSource rnd = RandomSource.create();

    private float zone, zoneVel, prevZone;
    private float fish = 0.5F, fishVel, prevFish = 0.5F, target = 0.5F;
    private float progress = 0.3F, prevProgress = 0.3F;
    private boolean perfect = true, overlap, done, won, sent;
    private int ticks, endTicks;

    public static void open(FishingPayloads.Start p) {
        Minecraft.getInstance().setScreen(new FishingScreen(p));
    }

    public FishingScreen(FishingPayloads.Start p) {
        super(Component.literal("釣り"));
        this.p = p;
        this.d = Mth.clamp(p.difficulty() / 100F, 0, 1);
        float size = 0.32F * (1 + p.zone());
        if (p.kind() == KIND_LORD) size *= 0.8F;
        if (p.rank() == 4) size *= 0.85F; // レジェンドは枠が少し小さい
        this.zoneSize = Mth.clamp(size, 0.1F, 0.7F);
        if (p.behavior() == SINKER) fish = prevFish = target = 0.2F;
        if (p.behavior() == FLOATER) fish = prevFish = target = 0.8F;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean holding() {
        if (minecraft == null) return false;
        long win = minecraft.getWindow().getWindow();
        return GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
                || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_SPACE);
    }

    @Override
    public void tick() {
        prevZone = zone;
        prevFish = fish;
        prevProgress = progress;
        if (done) {
            if (++endTicks > 14) onClose();
            return;
        }
        ticks++;

        // ---- キャッチ枠 ----
        zoneVel += holding() ? 0.005F : -0.0045F;
        zoneVel *= 0.96F; // ふわっと止まるように
        zoneVel = Mth.clamp(zoneVel, -0.035F, 0.035F);
        zone += zoneVel;
        if (zone < 0) {
            zone = 0;
            zoneVel = -zoneVel * 0.35F; // 底で少し跳ねる
            if (Math.abs(zoneVel) < 0.004F) zoneVel = 0;
        }
        if (zone > 1 - zoneSize) {
            zone = 1 - zoneSize;
            zoneVel = 0;
        }

        // ---- 魚 ----
        int b = p.behavior();
        float change = switch (b) {
            case SMOOTH -> 0.01F + d * 0.02F;
            case DART -> 0.02F + d * 0.055F;
            default -> 0.014F + d * 0.035F;
        };
        if (rnd.nextFloat() < change) {
            float t = rnd.nextFloat();
            if (b == SINKER) t = t * t * 0.9F;
            else if (b == FLOATER) t = 1 - (1 - t) * (1 - t) * 0.9F;
            else if (b == DART) t = fish + (rnd.nextBoolean() ? 1 : -1) * (0.3F + rnd.nextFloat() * 0.5F);
            target = Mth.clamp(t, 0.02F, 0.98F);
        }
        float slow = 1 - Math.min(0.6F, p.calm());
        float accel = (0.002F + d * 0.008F) * slow * (b == DART ? 1.6F : 1F);
        fishVel += (target - fish) * accel;
        fishVel += (rnd.nextFloat() - 0.5F) * 0.002F * d * slow;
        fishVel *= 0.88F;
        fish += fishVel;
        if (fish < 0 || fish > 1) {
            fish = Mth.clamp(fish, 0, 1);
            fishVel = 0;
        }

        // ---- ゲージ ----
        overlap = fish >= zone - 0.03F && fish <= zone + zoneSize + 0.03F; // 少しはみ出してもOK
        if (overlap) {
            progress += 0.008F * (1 + p.reel()) * (p.kind() == KIND_LORD ? 0.6F : 1F);
        } else {
            progress -= 0.0024F + d * 0.0024F;
            perfect = false;
        }
        if (progress >= 1) finish(true);
        else if (progress <= 0) finish(false);
        else if (ticks % 6 == 0 && overlap && minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.FISHING_BOBBER_RETRIEVE, 0.25F, 1.2F + progress * 0.6F);
        }
    }

    private void finish(boolean success) {
        done = true;
        won = success;
        progress = Mth.clamp(progress, 0, 1);
        send(success);
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(success ? SoundEvents.PLAYER_SPLASH : SoundEvents.FISHING_BOBBER_SPLASH, 0.6F, success ? 1.4F : 0.6F);
        }
    }

    private void send(boolean success) {
        if (sent) return;
        sent = true;
        PacketDistributor.sendToServer(new FishingPayloads.Result(success, success && perfect));
    }

    /** Escで閉じたり、別の画面が開いたら逃げられたことにする */
    @Override
    public void removed() {
        send(false);
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 背景は暗くしない（周りが見えるように）
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        super.render(g, mouseX, mouseY, pt);
        int shake = ((p.kind() == KIND_LORD || p.rank() == 4) && !done) ? (int) (Math.sin((ticks + pt) * 1.7) * 1.5) : 0;
        int left = width / 2 + 50 + shake;
        int top = height / 2 - BAR_H / 2;

        // 枠
        int frame = p.kind() == KIND_LORD ? 0xFFB03030 : p.kind() == KIND_TREASURE ? 0xFFD0A030
                : p.rank() < 5 ? com.chigusa.jsncraft.fishing.FishRank.byIndex(p.rank()).frameColor : 0xFF6A4A2A;
        g.fill(left - 5, top - 5, left + BAR_W + 17, top + BAR_H + 5, frame);
        g.fill(left - 3, top - 3, left + BAR_W + 15, top + BAR_H + 3, 0xF0141A22);
        // 水
        g.fillGradient(left, top, left + BAR_W, top + BAR_H, 0xFF3C86C8, 0xFF0A2246);

        // キャッチ枠
        float z = Mth.lerp(pt, prevZone, zone);
        int zy1 = top + BAR_H - Math.round((z + zoneSize) * BAR_H);
        int zy2 = top + BAR_H - Math.round(z * BAR_H);
        int zc = overlap ? 0xC070F080 : 0x8050B060;
        g.fill(left + 1, zy1, left + BAR_W - 1, zy2, zc);
        g.fill(left + 1, zy1, left + BAR_W - 1, zy1 + 1, 0xFFB0FFB0);
        g.fill(left + 1, zy2 - 1, left + BAR_W - 1, zy2, 0xFFB0FFB0);

        // 魚
        float f = Mth.lerp(pt, prevFish, fish);
        int fy = top + BAR_H - Math.round(f * BAR_H) - 8;
        fy = Mth.clamp(fy, top - 4, top + BAR_H - 12);
        int fx = left + BAR_W / 2 - 8;
        if (p.icon().isEmpty() && p.kind() == KIND_FISH && p.rank() < 5) {
            var rk = com.chigusa.jsncraft.fishing.FishRank.byIndex(p.rank());
            g.pose().pushPose();
            g.pose().translate(fx + 8, fy + 8, 200);
            g.pose().scale(2, 2, 1);
            g.drawString(font, "?", -font.width("?") / 2, -4, 0xFF000000 | (rk.color.getColor() == null ? 0xFFFFFF : rk.color.getColor()), true);
            g.pose().popPose();
        } else if (p.kind() == KIND_TREASURE || p.kind() == KIND_LORD) {
            boolean lord = p.kind() == KIND_LORD;
            g.pose().pushPose();
            g.pose().translate(fx + 8, fy + 8, 200);
            g.pose().scale(2, 2, 1);
            String mark = lord ? "!!" : "?";
            g.drawString(font, mark, -font.width(mark) / 2, -4, lord ? 0xFFFF4040 : 0xFFFFD040, true);
            g.pose().popPose();
        } else if (!p.icon().isEmpty()) {
            g.renderItem(p.icon(), fx, fy);
        }

        // 釣りゲージ
        int px = left + BAR_W + 4;
        g.fill(px, top, px + 8, top + BAR_H, 0xFF202020);
        float pr = Mth.lerp(pt, prevProgress, progress);
        int ph = Math.round(Mth.clamp(pr, 0, 1) * BAR_H);
        int pc = pr < 0.33F ? 0xFFE04040 : pr < 0.66F ? 0xFFE0C040 : 0xFF50E060;
        g.fill(px + 1, top + BAR_H - ph, px + 7, top + BAR_H, pc);

        // 文字
        int cx = left + (BAR_W + 12) / 2;
        String title = p.kind() == KIND_LORD ? "§c§l海の主だ！" : p.kind() == KIND_TREASURE ? "§6§l何か光ってる！"
                : p.rank() == 4 ? "§6§lレジェンドだ！！" : p.rank() == 3 ? "§d§l大物の気配！"
                : p.rank() == 2 ? "§9§l手ごたえあり！" : "§b§lかかった！";
        g.drawCenteredString(font, title, cx, top - 18, 0xFFFFFF);
        g.drawCenteredString(font, "§7右クリック長押しで引く", cx, top + BAR_H + 9, 0xFFFFFF);
        if (perfect && !done) g.drawCenteredString(font, "§6PERFECT", cx, top + BAR_H + 20, 0xFFFFFF);

        if (done) {
            g.pose().pushPose();
            g.pose().translate(width / 2F, height / 2F - 40, 300);
            g.pose().scale(2, 2, 1);
            String res = won ? (perfect ? "§6§lPERFECT!" : "§b§l釣れた！") : "§7逃げられた…";
            g.drawCenteredString(font, res, 0, 0, 0xFFFFFF);
            g.pose().popPose();
        }
    }
}
