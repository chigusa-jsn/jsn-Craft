package com.chigusa.jsncraft.card;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** カード1枚：custom_data の "card" がカードのID */
public class CardItem extends Item {
    public CardItem(Properties p) {
        super(p);
    }

    public static ItemStack of(String id) {
        ItemStack s = new ItemStack(ModCards.CARD.get());
        CompoundTag t = new CompoundTag();
        t.putString("card", id);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    public static String id(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? "" : d.copyTag().getString("card");
    }

    public static CardList.Card def(ItemStack s) {
        return s.getItem() instanceof CardItem ? Cards.get(id(s)) : null;
    }

    public static Component name(CardList.Card c) {
        ResourceLocation rl = ResourceLocation.tryParse(c.entity());
        Component mob = rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl) ? BuiltInRegistries.ENTITY_TYPE.get(rl).getDescription() : Component.literal(c.id());
        return mob;
    }

    @Override
    public Component getName(ItemStack stack) {
        CardList.Card c = def(stack);
        if (c == null) return super.getName(stack);
        return Component.literal(CardStats.FMT[c.rarity()] + "カード：").append(name(c));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        CardList.Card c = def(stack);
        if (c == null) return;
        tip.add(Component.literal(CardStats.FMT[c.rarity()] + "★".repeat(c.rarity() + 1) + " " + CardStats.RARITY[c.rarity()]));
        tip.add(Component.literal("§aバインダーに入れると：" + CardStats.describe(c.stat(), CardStats.amount(c))));
        tip.add(Component.literal("§8アルバムにしまって集めよう"));
    }
}
