package com.chigusa.jsncraft.vfx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 回る魔法陣のエフェクトに渡す値。
 * size：半径（ブロック）、ticks：出ている時間、style：下の STYLE_ の組み合わせ。
 */
public record CircleOptions(int pattern, float size, int ticks, int style) implements ParticleOptions {

    /** ため：小さく出て、時間いっぱいかけて大きくなり、回るのも速くなる */
    public static final int CHARGE = 1;
    /** 空に浮かぶ魔法陣（ゆっくり回る） */
    public static final int SKY = 2;
    /** 逆回り */
    public static final int REVERSE = 4;
    /** 発動の光：パッと広がって消える */
    public static final int FLASH = 8;

    public static final MapCodec<CircleOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("pattern").forGetter(CircleOptions::pattern),
            Codec.FLOAT.fieldOf("size").forGetter(CircleOptions::size),
            Codec.INT.fieldOf("ticks").forGetter(CircleOptions::ticks),
            Codec.INT.fieldOf("style").forGetter(CircleOptions::style)
    ).apply(i, CircleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CircleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CircleOptions::pattern,
            ByteBufCodecs.FLOAT, CircleOptions::size,
            ByteBufCodecs.VAR_INT, CircleOptions::ticks,
            ByteBufCodecs.VAR_INT, CircleOptions::style,
            CircleOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SPELL_CIRCLE.get();
    }

    /** 登録用 */
    public static ParticleType<CircleOptions> type() {
        return new ParticleType<>(false) {
            @Override
            public MapCodec<CircleOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, CircleOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
