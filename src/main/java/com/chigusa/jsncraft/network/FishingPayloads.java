package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** 釣りミニゲームの通信。 */
public final class FishingPayloads {

    /**
     * サーバー → 本人：ミニゲーム開始。
     * kindBehavior = 種類 × 64 + ランク × 8 + 動き方
     * （種類 0ゴミ 1魚 2お宝 3主 ／ ランク 0コモン〜4レジェンド、7なし ／ 動き方 0ふつう 1なめらか 2すばやい 3沈む 4浮く）
     */
    public record Start(int difficulty, int kindBehavior, float zone, float calm, float reel, ItemStack icon)
            implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(JsnCraft.id("fishing_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Start> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Start::difficulty,
                ByteBufCodecs.VAR_INT, Start::kindBehavior,
                ByteBufCodecs.FLOAT, Start::zone,
                ByteBufCodecs.FLOAT, Start::calm,
                ByteBufCodecs.FLOAT, Start::reel,
                ItemStack.OPTIONAL_STREAM_CODEC, Start::icon,
                Start::new);

        public int kind() { return kindBehavior / 64; }
        public int rank() { return (kindBehavior / 8) % 8; }
        public int behavior() { return kindBehavior % 8; }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 本人 → サーバー：ミニゲームの結果 */
    public record Result(boolean success, boolean perfect) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(JsnCraft.id("fishing_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Result::success,
                ByteBufCodecs.BOOL, Result::perfect,
                Result::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private FishingPayloads() {}
}
