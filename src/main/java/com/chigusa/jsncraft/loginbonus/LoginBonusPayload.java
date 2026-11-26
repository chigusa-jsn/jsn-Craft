package com.chigusa.jsncraft.loginbonus;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** サーバー → 本人：ログインボーナスのカレンダー。open なら画面を開く。 */
public record LoginBonusPayload(List<Day> days, int stamps, boolean canClaim, int streak, int total, int streakEvery,
                                List<String> streakRewards, boolean open) implements CustomPacketPayload {

    public record Day(List<ItemStack> icons, List<String> labels) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Day> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Day::icons,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Day::labels,
                Day::new);
    }

    public static final Type<LoginBonusPayload> TYPE = new Type<>(JsnCraft.id("login_bonus"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LoginBonusPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                Day.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, p.days());
                buf.writeVarInt(p.stamps());
                buf.writeBoolean(p.canClaim());
                buf.writeVarInt(p.streak());
                buf.writeVarInt(p.total());
                buf.writeVarInt(p.streakEvery());
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, p.streakRewards());
                buf.writeBoolean(p.open());
            },
            buf -> new LoginBonusPayload(Day.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf), buf.readVarInt(), buf.readBoolean(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf), buf.readBoolean()));

    public static LoginBonusPayload of(ServerPlayer p, boolean open) {
        LoginBonus.Table t = LoginBonus.table(p.server);
        LoginBonusData d = LoginBonus.data(p);
        boolean can = LoginBonus.canClaim(p);
        int stamps = d.stamps;
        if (can && stamps >= t.days().size()) stamps = 0; // 次の周
        List<Day> days = new ArrayList<>();
        for (List<LoginBonus.Reward> rs : t.days()) {
            List<ItemStack> icons = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (LoginBonus.Reward r : rs) {
                icons.add(LoginBonus.icon(r));
                labels.add(LoginBonus.label(r));
            }
            days.add(new Day(icons, labels));
        }
        List<String> sl = new ArrayList<>();
        for (LoginBonus.Reward r : t.streakRewards()) sl.add(LoginBonus.label(r));
        return new LoginBonusPayload(days, stamps, can, d.streak, d.total, t.streakEvery(), sl, open);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
