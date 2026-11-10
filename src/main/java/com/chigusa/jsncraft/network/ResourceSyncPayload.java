package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** サーバー → 本人：今のマナとスタミナ（ゲージ表示用）。最大値は能力値として別に同期される。 */
public record ResourceSyncPayload(float mana, float stamina) implements CustomPacketPayload {

    public static final Type<ResourceSyncPayload> TYPE = new Type<>(JsnCraft.id("resource_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ResourceSyncPayload::mana,
            ByteBufCodecs.FLOAT, ResourceSyncPayload::stamina,
            ResourceSyncPayload::new);

    /** クライアントが覚えている値 */
    public static volatile float clientMana = 0;
    public static volatile float clientStamina = 0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
