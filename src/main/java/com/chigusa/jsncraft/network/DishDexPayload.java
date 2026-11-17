package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.CookingManager;
import com.chigusa.jsncraft.cooking.CookingRecipe;
import com.chigusa.jsncraft.cooking.DishItem;
import com.chigusa.jsncraft.data.ModAttachments;
import com.chigusa.jsncraft.fishing.FishDexData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** サーバー → 本人：料理図鑑（全部の料理と、自分の記録）。材料の名前はクライアントで訳す。 */
public record DishDexPayload(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(int no, ItemStack icon, int rank, int level, List<String> ingredients, int count, int best) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of(
                (buf, e) -> {
                    buf.writeVarInt(e.no());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.icon());
                    buf.writeVarInt(e.rank());
                    buf.writeVarInt(e.level());
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, e.ingredients());
                    buf.writeVarInt(e.count());
                    buf.writeVarInt(e.best());
                },
                buf -> new Entry(buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(),
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf), buf.readVarInt(), buf.readVarInt()));
    }

    public static final Type<DishDexPayload> TYPE = new Type<>(JsnCraft.id("dish_dex"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DishDexPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), DishDexPayload::entries,
            DishDexPayload::new);

    public static DishDexPayload client = new DishDexPayload(List.of());

    public static DishDexPayload of(ServerPlayer player) {
        FishDexData dex = player.getData(ModAttachments.DISH_DEX);
        List<Entry> list = new ArrayList<>();
        List<java.util.Map.Entry<net.minecraft.resources.ResourceLocation, CookingRecipe>> all = new ArrayList<>(CookingManager.RECIPES.entries());
        all.addAll(CookingManager.BREWING.entries());
        for (var e : all) {
            CookingRecipe r = e.getValue();
            if (r.no() >= 999 || !BuiltInRegistries.ITEM.containsKey(r.result())) continue;
            ItemStack icon = new ItemStack(BuiltInRegistries.ITEM.get(r.result()));
            if (!(icon.getItem() instanceof DishItem)) continue;
            FishDexData.Rec rec = dex.get(e.getKey().toString());
            list.add(new Entry(r.no(), icon, r.rankEnum().ordinal(), r.level(), r.ingredients(),
                    rec == null ? 0 : rec.count(), rec == null ? 0 : (int) rec.best()));
        }
        list.sort(Comparator.comparingInt(Entry::no));
        return new DishDexPayload(list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
