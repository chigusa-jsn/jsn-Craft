package com.chigusa.jsncraft.client.particle;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.vfx.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** 専用エフェクトの見た目を登録する（クライアント専用）。 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ParticleSetup {

    @SubscribeEvent
    public static void onRegister(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SLASH.get(), SlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.MAGIC_CIRCLE.get(), FlatParticle.CircleProvider::new);
        event.registerSpriteSet(ModParticles.RING.get(), FlatParticle.RingProvider::new);
        event.registerSpriteSet(ModParticles.SPARK.get(), SparkParticle.Provider::new);
        // 数字の絵だけ受け取っておく（数字そのものは DamageNumbers から出す）
        event.registerSpriteSet(ModParticles.DAMAGE_DIGITS.get(), sprites -> {
            DamageNumberParticle.SPRITES = sprites;
            return (ParticleProvider<SimpleParticleType>) (type, level, x, y, z, dx, dy, dz) -> null;
        });
    }

    /** サーバーから「ここに数字を出して」と届いたとき */
    public static void spawnDamageNumber(double x, double y, double z, float amount, int style) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.particleEngine.add(new DamageNumberParticle(mc.level, x, y, z, amount, style));
    }

    private ParticleSetup() {}
}
