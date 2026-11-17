package com.chigusa.jsncraft.network;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.cooking.CookingManager;
import com.chigusa.jsncraft.cooking.CookingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** サーバー → クライアント：調理台のレシピ一覧（画面に「何ができるか」を出すため）。番号はサーバーと同じ順。 */
public record CookingRecipesPayload(List<Entry> recipes) implements CustomPacketPayload {

    public record Entry(ItemStack result, int level, List<String> ingredients) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Entry::result,
                ByteBufCodecs.VAR_INT, Entry::level,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Entry::ingredients,
                Entry::new);
    }

    public static final Type<CookingRecipesPayload> TYPE = new Type<>(JsnCraft.id("cooking_recipes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CookingRecipesPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), CookingRecipesPayload::recipes,
            CookingRecipesPayload::new);

    public static CookingRecipesPayload client = new CookingRecipesPayload(List.of());

    public static CookingRecipesPayload create() {
        List<Entry> list = new ArrayList<>();
        for (var e : CookingManager.RECIPES.entries()) {
            CookingRecipe r = e.getValue();
            list.add(new Entry(new ItemStack(BuiltInRegistries.ITEM.get(r.result()), r.count()), r.level(), r.ingredients()));
        }
        return new CookingRecipesPayload(list);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
