package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.dodge.DodgeService;
import com.chigusa.jsncraft.network.DodgePayload;
import com.chigusa.jsncraft.network.ResourceSyncPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** ドッジロールのキー（はじめは左Alt）。押している移動キーの向きへ転がる。何も押していなければ後ろへ。 */
public final class DodgeClient {
    public static final KeyMapping DODGE = new KeyMapping("key.jsn_craft.dodge", GLFW.GLFW_KEY_LEFT_ALT, "key.categories.jsn_craft");
    private static long last = -1000;

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onKeys(RegisterKeyMappingsEvent event) {
            event.register(DODGE);
        }
    }

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Tick {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            while (DODGE.consumeClick()) {
                if (mc.player != null && mc.screen == null) tryDodge(mc.player);
            }
        }
    }

    private static void tryDodge(LocalPlayer p) {
        long now = p.level().getGameTime();
        if (now - last < DodgeService.COOLDOWN) return;
        if (!p.onGround() || p.isPassenger() || p.isFallFlying() || p.getAbilities().flying || p.isSpectator()) return;
        if (!p.isCreative() && ResourceSyncPayload.clientStamina < DodgeService.COST * 0.5F) return;   // 足りない分はサーバーが確かめる
        float fwd = p.input.forwardImpulse, side = p.input.leftImpulse;
        int dir;
        if (Math.abs(side) > Math.abs(fwd)) dir = side > 0 ? 2 : 3;
        else dir = fwd > 0 ? 0 : 1;
        if (fwd == 0 && side == 0) { dir = 1; fwd = -1; }
        // 向いている方向を基準に、転がる向きを決める
        float yaw = (float) Math.toRadians(p.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 move = forward.scale(Math.signum(fwd)).add(left.scale(Math.signum(side)));
        if (move.lengthSqr() < 1e-4) move = forward.scale(-1);
        move = move.normalize().scale(0.95);
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(move.x, Math.max(v.y, 0.18), move.z);
        last = now;
        PlayerAnimClient.play(p, switch (dir) {
            case 1 -> "dodge_back";
            case 2 -> "dodge_left";
            case 3 -> "dodge_right";
            default -> "dodge_forward";
        });
        PacketDistributor.sendToServer(new DodgePayload(dir));
    }

    private DodgeClient() {}
}
