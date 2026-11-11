package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** サーバー → 本人：次に開くメニューを、このタブから始めてね（受付嬢に話しかけたら依頼タブ）。 */
public record OpenTabPayload(int tab) implements CustomPacketPayload {

    public static final int TAB_QUEST = 3;

    public static final Type<OpenTabPayload> TYPE = new Type<>(JsnCraft.id("open_tab"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTabPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenTabPayload::tab,
            OpenTabPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
