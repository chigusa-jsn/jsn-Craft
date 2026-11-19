package com.chigusa.jsncraft.client;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.vfx.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 星銀の武器を持っている人のまわりに、光るきらめき（TwinkleParticle）を出す。
 * 絵に描いた点のきらめきは、手に持つと四角いブロックになってしまうので、そのかわり。
 */
@EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
public final class StarSilverSparkle {

    static boolean starSilver(ItemStack s) {
        if (s.isEmpty()) return false;
        var id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id.getNamespace().equals(JsnCraft.MODID) && id.getPath().startsWith("star_silver_")
                && !id.getPath().equals("star_silver_template") && !id.getPath().equals("star_silver_ingot");
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        RandomSource r = mc.level.random;
        for (AbstractClientPlayer p : mc.level.players()) {
            if (p.isInvisible() || p.distanceToSqr(mc.player) > 48 * 48) continue;
            boolean main = starSilver(p.getMainHandItem()), off = starSilver(p.getOffhandItem());
            if (!main && !off) continue;
            if (r.nextFloat() > 0.45F) continue;
            boolean right = main && (!off || r.nextBoolean());
            boolean rightHanded = p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
            boolean sideRight = right == rightHanded;
            Vec3 at;
            boolean firstPerson = p == mc.player && mc.options.getCameraType().isFirstPerson();
            if (firstPerson) {
                // 自分の視点：画面の右下（武器のあたり）
                Vec3 look = p.getViewVector(1.0F), up = p.getUpVector(1.0F), side = look.cross(up).normalize();
                at = p.getEyePosition(1.0F).add(look.scale(0.9)).add(side.scale(sideRight ? 0.45 : -0.45)).add(up.scale(-0.35))
                        .add((r.nextDouble() - 0.5) * 0.35, (r.nextDouble() - 0.5) * 0.4, (r.nextDouble() - 0.5) * 0.35);
            } else {
                // まわりから見たとき：手と武器のあたり
                double yaw = Math.toRadians(p.yBodyRot);
                Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), rightV = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
                at = p.position().add(rightV.scale(sideRight ? 0.4 : -0.4)).add(fwd.scale(0.25 + r.nextDouble() * 0.5))
                        .add((r.nextDouble() - 0.5) * 0.4, 0.6 + r.nextDouble() * 1.0, (r.nextDouble() - 0.5) * 0.4);
            }
            mc.level.addParticle(ModParticles.TWINKLE.get(), at.x, at.y, at.z,
                    (r.nextDouble() - 0.5) * 0.01, 0.002 + r.nextDouble() * 0.006, (r.nextDouble() - 0.5) * 0.01);
        }
    }

    private StarSilverSparkle() {}
}
