package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** サーバー → メンバー：パーティーのみんなのHP・MP（1秒ごと）。空ならパーティーなし。 */
public record PartyStatusPayload(List<Member> members) implements CustomPacketPayload {

    public static final Type<PartyStatusPayload> TYPE = new Type<>(JsnCraft.id("party_status"));

    public record Member(String uuid, String name, float hp, float maxHp, float mana, float maxMana,
                         boolean leader, boolean dead) {
        public static final Codec<Member> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("uuid").forGetter(Member::uuid),
                Codec.STRING.fieldOf("name").forGetter(Member::name),
                Codec.FLOAT.fieldOf("hp").forGetter(Member::hp),
                Codec.FLOAT.fieldOf("max_hp").forGetter(Member::maxHp),
                Codec.FLOAT.fieldOf("mana").forGetter(Member::mana),
                Codec.FLOAT.fieldOf("max_mana").forGetter(Member::maxMana),
                Codec.BOOL.fieldOf("leader").forGetter(Member::leader),
                Codec.BOOL.fieldOf("dead").forGetter(Member::dead)
        ).apply(i, Member::new));
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, PartyStatusPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(Member.CODEC.listOf()).map(PartyStatusPayload::new, PartyStatusPayload::members);

    /** クライアントが覚えている最新のパーティー */
    public static volatile List<Member> client = List.of();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
