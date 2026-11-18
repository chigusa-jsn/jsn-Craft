package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 武器の振り・技の体の動き（Player Animator）の通信。 */
public final class AnimPayloads {

    /** サーバー → まわりの人：この人にこの動きをさせる */
    public record Play(int entityId, String anim) implements CustomPacketPayload {
        public static final Type<Play> TYPE = new Type<>(JsnCraft.id("player_anim"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Play> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Play::entityId, ByteBufCodecs.stringUtf8(32), Play::anim, Play::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 本人 → サーバー：武器を振った（まわりの人にも見せる） */
    public record Swing(String anim) implements CustomPacketPayload {
        public static final Type<Swing> TYPE = new Type<>(JsnCraft.id("player_swing"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Swing> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(32), Swing::anim, Swing::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private AnimPayloads() {}
}
