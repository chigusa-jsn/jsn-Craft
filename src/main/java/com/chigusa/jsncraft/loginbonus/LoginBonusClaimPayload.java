package com.chigusa.jsncraft.loginbonus;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 本人 → サーバー：今日のログインボーナスを受け取る */
public record LoginBonusClaimPayload() implements CustomPacketPayload {
    public static final LoginBonusClaimPayload INSTANCE = new LoginBonusClaimPayload();
    public static final Type<LoginBonusClaimPayload> TYPE = new Type<>(JsnCraft.id("login_bonus_claim"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoginBonusClaimPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
