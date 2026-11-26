package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.dex.MonsterDex;
import com.chigusa.jsncraft.dex.MonsterDexData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** サーバー → 本人：モンスター図鑑とフィギュア図鑑。claimed は もらったご褒美の数（モンスター・フィギュア）。 */
public record MonsterDexPayload(List<Mon> monsters, List<Fig> figures, int monsterClaimed, int figureClaimed) implements CustomPacketPayload {

    public record Mon(String id, int kills, float hp, float atk, String weakness) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Mon> STREAM_CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.id());
                    buf.writeVarInt(e.kills());
                    buf.writeFloat(e.hp());
                    buf.writeFloat(e.atk());
                    buf.writeUtf(e.weakness());
                },
                buf -> new Mon(buf.readUtf(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readUtf()));
    }

    public record Fig(String id, int rarity, boolean have) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Fig> STREAM_CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeUtf(e.id());
                    buf.writeVarInt(e.rarity());
                    buf.writeBoolean(e.have());
                },
                buf -> new Fig(buf.readUtf(), buf.readVarInt(), buf.readBoolean()));
    }

    public static final Type<MonsterDexPayload> TYPE = new Type<>(JsnCraft.id("monster_dex"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MonsterDexPayload> STREAM_CODEC = StreamCodec.composite(
            Mon.STREAM_CODEC.apply(ByteBufCodecs.list()), MonsterDexPayload::monsters,
            Fig.STREAM_CODEC.apply(ByteBufCodecs.list()), MonsterDexPayload::figures,
            ByteBufCodecs.VAR_INT, MonsterDexPayload::monsterClaimed,
            ByteBufCodecs.VAR_INT, MonsterDexPayload::figureClaimed,
            MonsterDexPayload::new);

    public static MonsterDexPayload client = new MonsterDexPayload(List.of(), List.of(), 0, 0);

    public static MonsterDexPayload of(ServerPlayer player) {
        MonsterDexData d = MonsterDex.data(player);
        List<Mon> mons = new ArrayList<>();
        for (String id : MonsterDex.monsters()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) continue;
            EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.get(rl);
            float[] st = MonsterDex.stats(t);
            mons.add(new Mon(id, d.kills(id), st[0], st[1], MonsterDex.weakness(t)));
        }
        List<Fig> figs = new ArrayList<>();
        for (Map.Entry<String, Integer> e : MonsterDex.figures(player.server).entrySet())
            figs.add(new Fig(e.getKey(), e.getValue(), d.hasFigure(e.getKey())));
        return new MonsterDexPayload(mons, figs, 0, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
