package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** サーバー → 攻撃した人：ここにダメージの数字を出して。style 0=ふつう 1=会心 2=スキル */
public record DamageNumberPayload(double x, double y, double z, float amount, int style) implements CustomPacketPayload {

    public static final Type<DamageNumberPayload> TYPE = new Type<>(JsnCraft.id("damage_number"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DamageNumberPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, DamageNumberPayload::x,
            ByteBufCodecs.DOUBLE, DamageNumberPayload::y,
            ByteBufCodecs.DOUBLE, DamageNumberPayload::z,
            ByteBufCodecs.FLOAT, DamageNumberPayload::amount,
            ByteBufCodecs.VAR_INT, DamageNumberPayload::style,
            DamageNumberPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
