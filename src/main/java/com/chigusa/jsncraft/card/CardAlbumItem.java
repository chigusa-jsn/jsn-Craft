package com.chigusa.jsncraft.card;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** カードアルバム：集めたカードをしまっておく図鑑（カードID → 枚数） */
public class CardAlbumItem extends Item {
    public CardAlbumItem(Properties p) {
        super(p);
    }

    public static Map<String, Integer> cards(ItemStack s) {
        Map<String, Integer> out = new LinkedHashMap<>();
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null) return out;
        CompoundTag t = d.copyTag().getCompound("album");
        for (String k : t.getAllKeys()) if (Cards.get(k) != null && t.getInt(k) > 0) out.put(k, t.getInt(k));
        return out;
    }

    public static void setCards(ItemStack s, Map<String, Integer> cards) {
        CompoundTag t = s.has(DataComponents.CUSTOM_DATA) ? s.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        CompoundTag a = new CompoundTag();
        cards.forEach((k, v) -> {
            if (v > 0) a.putInt(k, v);
        });
        t.put("album", a);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide && ModCards.openAlbum != null) ModCards.openAlbum.accept(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        Map<String, Integer> c = cards(stack);
        tip.add(Component.literal("§7集めた種類：§f" + c.size() + " / " + CardList.ALL.size()));
        tip.add(Component.literal("§8右クリックで開く"));
    }
}
