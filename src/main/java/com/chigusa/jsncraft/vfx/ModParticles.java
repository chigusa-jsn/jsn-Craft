package com.chigusa.jsncraft.vfx;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * jsn-Craft 専用のエフェクト（粒子）。絵は assets/jsn_craft/textures/particle/ にある。
 *
 * サーバーからは Vfx のメソッドで出す。大きさ・色・寿命は「速さ」の欄に入れて送っている
 * （count を 0 にして送ると、速さの値がそのまま届くため）。
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, JsnCraft.MODID);

    /** 三日月の斬撃（こちらを向く） */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLASH = register("slash");
    /** 足元の魔法陣（地面に平ら、ゆっくり回る） */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MAGIC_CIRCLE = register("magic_circle");
    /** 広がる光の輪（地面に平ら） */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RING = register("ring");
    /** 当たったときの火花 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK = register("spark");
    /** ダメージの数字（絵の置き場所として登録するだけ。出すのはクライアント側） */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DAMAGE_DIGITS = register("damage_digits");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(true));
    }

    private ModParticles() {}
}
