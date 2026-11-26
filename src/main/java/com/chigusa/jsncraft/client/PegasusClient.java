package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.network.PegasusFlapPayload;
import com.chigusa.jsncraft.ranch.Mutant;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 天馬で空を飛ぶ：乗って空中でジャンプを押すと羽ばたいて上がる（回数あり）。前へ進むキーで空中を進む。
 * 落ちるときはゆっくり滑空する。地面に降りると羽ばたく回数が戻る。
 * 馬の動きは乗っている本人のパソコンで決まるので、ここで動かす。
 */
@EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
public final class PegasusClient {
    public static final int MAX_FLAPS = 12;
    private static int flaps = MAX_FLAPS;
    private static boolean jumpWas;
    private static int groundTicks;
    /** 最後に羽ばたいた時刻（羽の動きに使う） */
    public static long lastFlap;

    public static boolean isPegasus(net.minecraft.world.entity.Entity e) {
        return e instanceof AbstractHorse && RanchHud.mutant(e) == Mutant.PEGASUS;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !(p.getVehicle() instanceof AbstractHorse horse) || !isPegasus(horse) || p.getVehicle().getControllingPassenger() != p) {
            jumpWas = false;
            return;
        }
        boolean jump = mc.options.keyJump.isDown();
        boolean pressed = jump && !jumpWas;
        jumpWas = jump;
        if (horse.onGround()) {
            if (++groundTicks > 10 && flaps < MAX_FLAPS) {
                flaps = MAX_FLAPS;
            }
            return;
        }
        groundTicks = 0;
        Vec3 v = horse.getDeltaMovement();
        // 羽ばたく
        if (pressed) {
            if (flaps > 0) {
                flaps--;
                lastFlap = horse.level().getGameTime();
                v = new Vec3(v.x, Math.max(v.y, 0) + 0.55, v.z);
                PacketDistributor.sendToServer(new PegasusFlapPayload(horse.getId()));
                p.displayClientMessage(Component.literal("羽ばたき ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("◆".repeat(flaps)).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("◇".repeat(MAX_FLAPS - flaps)).withStyle(ChatFormatting.DARK_GRAY)), true);
            } else {
                p.displayClientMessage(Component.literal("羽が疲れた…地面で休もう").withStyle(ChatFormatting.GRAY), true);
            }
        }
        // 空中を進む（向いている方へ）
        float fwd = p.zza;
        float yaw = horse.getYRot() * Mth.DEG_TO_RAD;
        double speed = 0.75;
        Vec3 dir = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)).scale(Math.max(0, fwd) * speed);
        double vx = Mth.lerp(0.12, v.x, dir.x), vz = Mth.lerp(0.12, v.z, dir.z);
        // 滑空：ゆっくり落ちる
        double vy = Math.max(v.y, -0.16);
        horse.setDeltaMovement(vx, vy, vz);
        horse.fallDistance = 0;
    }

    private PegasusClient() {}
}
